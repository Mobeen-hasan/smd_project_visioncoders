package com.visioncoders.movieapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.gson.Gson
import kotlinx.coroutines.launch

class ReviewRepliesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etReply: EditText
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var reviewId: String = ""
    private var reviewOwnerId: String = ""
    private var myProfileUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_replies)

        reviewId = intent.getStringExtra("REVIEW_ID") ?: run {
            finish()
            return
        }
        reviewOwnerId = intent.getStringExtra("REVIEW_OWNER_ID") ?: ""

        checkReviewExists()
    }

    private fun checkReviewExists() {
        db.collection("reviews").document(reviewId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    setupUI()
                } else {
                    Toast.makeText(this, "This review no longer exists.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading review.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupUI() {
        recyclerView = findViewById(R.id.recyclerReplies)
        etReply = findViewById(R.id.etReply)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnSend).setOnClickListener { sendReply() }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        fetchReplies()
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                myProfileUrl = doc.getString("profileImage") ?: ""
            }
        }
    }

    private fun fetchReplies() {
        db.collection("reviews").document(reviewId).collection("replies")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    val replies = value.toObjects(ReviewReply::class.java)
                    recyclerView.adapter = ReplyAdapter(replies)

                    // Scroll to bottom when new reply arrives
                    if (replies.isNotEmpty()) {
                        recyclerView.scrollToPosition(replies.size - 1)
                    }
                }
            }
    }

    private fun sendReply() {
        val text = etReply.text.toString().trim()
        val user = auth.currentUser ?: return
        if (text.isEmpty()) return

        val reply = ReviewReply(
            reviewId = reviewId,
            userId = user.uid,
            username = user.displayName ?: "User",
            text = text,
            timestamp = System.currentTimeMillis(),
            userImage = myProfileUrl
        )

        if (NetworkUtils.isInternetAvailable(this)) {
            // ONLINE MODE
            db.collection("reviews").document(reviewId).collection("replies")
                .add(reply)
                .addOnSuccessListener {
                    etReply.setText("")
                    sendNotification()
                }
        } else {
            // OFFLINE MODE
            saveReplyToQueue(reply)
        }
    }

    private fun saveReplyToQueue(reply: ReviewReply) {
        val gson = Gson()
        val jsonPayload = gson.toJson(reply)
        val syncRequest = SyncRequest(type = "REPLY", payload = jsonPayload)

        lifecycleScope.launch {
            AppDatabase.getDatabase(this@ReviewRepliesActivity).appDao().addToQueue(syncRequest)
            scheduleSyncWorker()
            Toast.makeText(this@ReviewRepliesActivity, "Offline. Reply queued!", Toast.LENGTH_SHORT).show()
            etReply.setText("")
        }
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "MovieAppSync",
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun sendNotification() {
        val currentUser = auth.currentUser ?: return

        // 1. Check: Don't notify if I am replying to myself
        if (reviewOwnerId.isEmpty() || reviewOwnerId == currentUser.uid) return

        // 2. Save to Firestore (In-App Notification)
        val notification = AppNotification(
            type = "reply",
            fromId = currentUser.uid,
            fromName = currentUser.displayName ?: "User",
            fromImage = myProfileUrl,
            toId = reviewOwnerId,
            targetId = reviewId,
            message = "replied to your review.",
            timestamp = System.currentTimeMillis()
        )

        db.collection("notifications").add(notification)

        // 3. Push Notification (Using the Helper Object)
        NotificationHelper.sendPush(
            targetUserId = reviewOwnerId,
            title = "New Reply",
            body = "${currentUser.displayName ?: "Someone"} replied to your review."
        )
    }
}