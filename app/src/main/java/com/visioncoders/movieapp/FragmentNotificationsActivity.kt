package com.visioncoders.movieapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class FragmentNotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_notifications)

        recyclerView = findViewById(R.id.recycler_view_notifications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.iv_movie_poster).setOnClickListener { finish() }

        fetchAllNotifications()
    }

    private fun fetchAllNotifications() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Fetch Friend Requests
        val reqTask = db.collection("friend_requests")
            .whereEqualTo("toId", currentUserId)
            .get()

        // 2. Fetch Social Notifications (Likes/Replies)
        val notifTask = db.collection("notifications")
            .whereEqualTo("toId", currentUserId)
            .get()

        // 3. Wait for BOTH to finish
        Tasks.whenAllSuccess<QuerySnapshot>(reqTask, notifTask).addOnSuccessListener { results ->
            val allItems = mutableListOf<Any>()

            // Process Requests
            for (doc in results[0]) {
                val req = doc.toObject(FriendRequest::class.java).copy(id = doc.id)
                allItems.add(req)
            }

            // Process Notifications
            for (doc in results[1]) {
                val notif = doc.toObject(AppNotification::class.java).copy(id = doc.id)
                allItems.add(notif)
            }

            // 4. Sort by Timestamp (Newest first)
            allItems.sortByDescending {
                when (it) {
                    is FriendRequest -> it.timestamp
                    is AppNotification -> it.timestamp
                    else -> 0L
                }
            }

            // 5. Setup Adapter
            adapter = NotificationAdapter(
                allItems,
                onReqAccept = { req -> acceptRequest(req) },
                onReqDelete = { req -> deleteRequest(req) },
                onNotifDelete = { notif -> deleteNotification(notif) }
            )
            recyclerView.adapter = adapter
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun acceptRequest(request: FriendRequest) {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Add to My Friends
        val myFriend = hashMapOf("friendId" to request.fromId)
        db.collection("users").document(currentUserId)
            .collection("friends").document(request.fromId).set(myFriend)

        // 2. Add Me to Their Friends
        val theirFriend = hashMapOf("friendId" to currentUserId)
        db.collection("users").document(request.fromId)
            .collection("friends").document(currentUserId).set(theirFriend)

        // 3. Delete Request
        deleteRequest(request)
        Toast.makeText(this, "Friend Added!", Toast.LENGTH_SHORT).show()

        NotificationHelper.sendPush(
            targetUserId = request.fromId, // The person who sent the request
            title = "Request Accepted",
            body = "${auth.currentUser?.displayName} is now your friend."
        )
    }

    private fun deleteRequest(request: FriendRequest) {
        db.collection("friend_requests").document(request.id)
            .delete()
            .addOnSuccessListener {
                if (::adapter.isInitialized) {
                    adapter.removeItem(request)
                }
            }
    }

    private fun deleteNotification(notif: AppNotification) {
        db.collection("notifications").document(notif.id)
            .delete()
            .addOnSuccessListener {
                if (::adapter.isInitialized) {
                    adapter.removeItem(notif)
                    Toast.makeText(this, "Notification cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to clear", Toast.LENGTH_SHORT).show()
            }
    }
}