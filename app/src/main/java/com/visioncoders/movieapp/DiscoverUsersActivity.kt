package com.visioncoders.movieapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DiscoverUsersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: TextInputEditText
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Data Storage
    private var allUsers = listOf<User>()
    private var sentRequests = mutableSetOf<String>() // IDs of people I sent requests to
    private var myFriends = mutableSetOf<String>()    // IDs of my friends


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover_users)

        recyclerView = findViewById(R.id.recyclerViewUsers)
        searchInput = findViewById(R.id.etSearch)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load everything
        fetchAllData()
        setupSearch()

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun fetchAllData() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Get Users
        db.collection("users").get().addOnSuccessListener { userDocs ->
            val users = mutableListOf<User>()
            for (doc in userDocs) {
                val user = doc.toObject(User::class.java)
                if (user.id != currentUserId) users.add(user)
            }
            allUsers = users

            // 2. Get Sent Requests
            db.collection("friend_requests")
                .whereEqualTo("fromId", currentUserId)
                .get()
                .addOnSuccessListener { reqDocs ->
                    for (doc in reqDocs) {
                        // Store the ID of the person I sent it to
                        sentRequests.add(doc.getString("toId") ?: "")
                    }

                    // 3. Get Friends
                    db.collection("users").document(currentUserId)
                        .collection("friends")
                        .get()
                        .addOnSuccessListener { friendDocs ->
                            for (doc in friendDocs) {
                                myFriends.add(doc.id) // The doc ID is the friend's User ID
                            }

                            updateAdapter(allUsers)
                        }
                }
        }
    }

    private fun updateAdapter(users: List<User>) {
        val adapter = UserAdapter(
            users,
            sentRequests, // Pass the sets to adapter
            myFriends,
            onUserClick = { user ->
                val intent = Intent(this, UserProfileActivity::class.java)
                intent.putExtra("TARGET_USER_ID", user.id)
                startActivity(intent)
            },
            onRequestClick = { user ->
                sendFriendRequest(user)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val filtered = if (query.isEmpty()) allUsers else allUsers.filter {
                    it.name.contains(query, ignoreCase = true)
                }
                updateAdapter(filtered) // Re-bind adapter to refresh states
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun sendFriendRequest(targetUser: User) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Fetch my profile image first
        db.collection("users").document(currentUserId).get().addOnSuccessListener { doc ->
            val myImage = doc.getString("profileImage") ?: ""
            val myName = auth.currentUser?.displayName ?: "Unknown"

            val request = hashMapOf(
                "fromId" to currentUserId,
                "toId" to targetUser.id,
                "fromName" to myName,
                "fromImage" to myImage,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("friend_requests").add(request).addOnSuccessListener {
                Toast.makeText(this, "Request Sent", Toast.LENGTH_SHORT).show()

                NotificationHelper.sendPush(
                    targetUserId = targetUser.id,
                    title = "Friend Request",
                    body = "${auth.currentUser?.displayName} sent you a friend request."
                )

                // Optional: Update the 'sentRequests' set locally so the UI updates immediately without fetching data again
                sentRequests.add(targetUser.id)

            }
        }
    }

}