package com.visioncoders.movieapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var btnRequest: Button
    private lateinit var recyclerView: RecyclerView
    private var targetUserId: String = ""
    private lateinit var watchlistSection: View

    // Track the current relationship state
    private var currentStatus: String = "not_friends"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        targetUserId = intent.getStringExtra("TARGET_USER_ID") ?: return

        val imgProfile = findViewById<ShapeableImageView>(R.id.imgProfile)
        val tvName = findViewById<TextView>(R.id.tvUserName)
        val tvHandle = findViewById<TextView>(R.id.tvUserHandle)
        btnRequest = findViewById(R.id.btnSendRequest)
        watchlistSection = findViewById(R.id.layoutWatchlistSection)

        recyclerView = findViewById(R.id.recyclerViewWatchlist)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        fetchUserDetails(tvName, tvHandle, imgProfile)
        checkRelationshipStatus()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnReport).setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            intent.putExtra("TARGET_USER_ID", targetUserId)
            startActivity(intent)
        }


        btnRequest.setOnClickListener {
            when (currentStatus) {
                "not_friends" -> sendFriendRequest()
                "friends" -> showUnfriendDialog() // Show confirmation
                "requested" -> Toast.makeText(this, "Request already sent", Toast.LENGTH_SHORT).show()
                "accept" -> acceptFriendRequest()
            }
        }
    }

    private fun checkRelationshipStatus() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Check if Friends
        db.collection("users").document(currentUserId)
            .collection("friends").document(targetUserId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    currentStatus = "friends"
                    setButtonState("Friends")

                    // Show Watchlist only for friends
                    watchlistSection.visibility = View.VISIBLE
                    fetchUserWatchlist()
                } else {
                    watchlistSection.visibility = View.GONE

                    // 2. Check if Request Sent
                    db.collection("friend_requests")
                        .whereEqualTo("fromId", currentUserId)
                        .whereEqualTo("toId", targetUserId)
                        .get()
                        .addOnSuccessListener { reqDocs ->
                            if (!reqDocs.isEmpty) {
                                currentStatus = "requested"
                                setButtonState("Requested")
                            } else {
                                // 3. Check if they sent ME a request
                                db.collection("friend_requests")
                                    .whereEqualTo("fromId", targetUserId)
                                    .whereEqualTo("toId", currentUserId)
                                    .get()
                                    .addOnSuccessListener { reverseDocs ->
                                        if (!reverseDocs.isEmpty) {
                                            currentStatus = "accept"
                                            setButtonState("Accept")
                                        } else {
                                            currentStatus = "not_friends"
                                            setButtonState("Add Friend")
                                        }
                                    }
                            }
                        }
                }
            }
    }

    private fun setButtonState(status: String) {
        when(status) {
            "Friends" -> {
                btnRequest.text = "Friends" // Or "Unfriend"
                btnRequest.isEnabled = true // Enable it so we can click to unfriend
                btnRequest.alpha = 1.0f
            }
            "Requested" -> {
                btnRequest.text = "Requested"
                btnRequest.isEnabled = false
                btnRequest.alpha = 0.5f
            }
            "Accept" -> {
                btnRequest.text = "Accept Request"
                btnRequest.isEnabled = true
                btnRequest.alpha = 1.0f
            }
            "Add Friend" -> {
                btnRequest.text = "Add Friend"
                btnRequest.isEnabled = true
                btnRequest.alpha = 1.0f
            }
        }
    }

    private fun showUnfriendDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unfriend?")
            .setMessage("Are you sure you want to remove this user from your friends?")
            .setPositiveButton("Unfriend") { _, _ -> unfriendUser() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun unfriendUser() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Remove from MY friends list
        db.collection("users").document(currentUserId)
            .collection("friends").document(targetUserId)
            .delete()
            .addOnSuccessListener {
                // Remove from THEIR friends list
                db.collection("users").document(targetUserId)
                    .collection("friends").document(currentUserId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Unfriended", Toast.LENGTH_SHORT).show()

                        // Reset UI
                        currentStatus = "not_friends"
                        setButtonState("Add Friend")
                        watchlistSection.visibility = View.GONE
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to unfriend", Toast.LENGTH_SHORT).show()
            }
    }



    private fun fetchUserDetails(name: TextView, handle: TextView, img: ShapeableImageView) {
        db.collection("users").document(targetUserId).get().addOnSuccessListener { doc ->
            val user = doc.toObject(User::class.java) ?: return@addOnSuccessListener
            name.text = user.name
            handle.text = user.handle.ifEmpty { "@${user.name.replace(" ", "")}" }

            if (user.profileImage.isNotEmpty()) {
                Glide.with(this).load(user.profileImage).placeholder(R.drawable.profile).into(img)
            }
        }
    }

    private fun fetchUserWatchlist() {
        db.collection("watchlist").whereEqualTo("userId", targetUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { documents ->
                val items = documents.toObjects(WatchlistItem::class.java)
                recyclerView.adapter = SimplePosterAdapter(items)
            }
    }

    private fun sendFriendRequest() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("users").document(currentUserId).get().addOnSuccessListener { doc ->
            val myImage = doc.getString("profileImage") ?: ""
            val myName = auth.currentUser?.displayName ?: "Unknown"

            val request = hashMapOf(
                "fromId" to currentUserId,
                "toId" to targetUserId,
                "fromName" to myName,
                "fromImage" to myImage,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("friend_requests").add(request).addOnSuccessListener {
                currentStatus = "requested"
                setButtonState("Requested")
                Toast.makeText(this, "Request Sent", Toast.LENGTH_SHORT).show()

                NotificationHelper.sendPush(
                    targetUserId = targetUserId,
                    title = "Friend Request",
                    body = "${auth.currentUser?.displayName} sent you a friend request."
                )
            }
        }
    }
    
    private fun acceptFriendRequest() {

    }
}