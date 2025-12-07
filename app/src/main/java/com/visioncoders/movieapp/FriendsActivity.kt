package com.visioncoders.movieapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        recyclerView = findViewById(R.id.recyclerFriends)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        fetchFriends()
    }

    private fun fetchFriends() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Get List of Friend IDs
        db.collection("users").document(currentUserId).collection("friends")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No friends yet", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val friendIds = documents.map { it.id }

                // 2. Fetch User Details for those IDs

                val friendsList = mutableListOf<User>()
                var loadedCount = 0

                for (id in friendIds) {
                    db.collection("users").document(id).get().addOnSuccessListener { userDoc ->
                        val user = userDoc.toObject(User::class.java)
                        if (user != null) {
                            friendsList.add(user)
                        }
                        loadedCount++

                        // When all are loaded, it show list
                        if (loadedCount == friendIds.size) {
                            recyclerView.adapter = FriendAdapter(friendsList) { friend ->
                                // Open Friend's Profile
                                val intent = Intent(this, UserProfileActivity::class.java)
                                intent.putExtra("TARGET_USER_ID", friend.id)
                                startActivity(intent)
                            }
                        }
                    }
                }
            }
    }
}