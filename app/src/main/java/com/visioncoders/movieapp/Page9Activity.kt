package com.visioncoders.movieapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import com.visioncoders.movieapp.api.FileUploadService
import com.visioncoders.movieapp.api.UploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream

class Page9Activity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var imgProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView

    private val BASE_URL = "http://192.168.0.102/movieapp/"

    // Gallery Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                imgProfile.setImageURI(imageUri) // Show temporarily
                uploadImageToXampp(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page9)

        // Bind Views
        imgProfile = findViewById(R.id.image_profile_pic)
        tvName = findViewById(R.id.text_profile_name)
        tvEmail = findViewById(R.id.text_profile_email)

        val tvMovieCount = findViewById<TextView>(R.id.text_movie_stats)
        val tvShowCount = findViewById<TextView>(R.id.text_show_stats)
        val tvWatchlistCount = findViewById<TextView>(R.id.text_watchlist_count)
        val tvReviewCount = findViewById<TextView>(R.id.text_reviews_count)

        val user = auth.currentUser
        if (user != null) {
            fetchStats(user.uid, tvMovieCount, tvShowCount, tvWatchlistCount, tvReviewCount)
        }

        imgProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        findViewById<android.view.View>(R.id.item_edit_profile).setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }


        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        fetchUserInfo()
    }

    private fun fetchUserInfo() {
        val user = auth.currentUser ?: return

        // 1. Set basic info immediately
        tvEmail.text = user.email

        // 2. Fetch latest Name/Image from Firestore
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("name")
                val profileUrl = doc.getString("profileImage")

                // Update UI
                tvName.text = name ?: "User"
                if (!profileUrl.isNullOrEmpty()) {
                    Glide.with(this).load(profileUrl).placeholder(R.drawable.profile).into(imgProfile)
                }
            }
        }
    }

    private fun setupNavigation() {
        // Watchlist
        findViewById<android.view.View>(R.id.card_watchlist).setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
        }

        // Reviews
        findViewById<android.view.View>(R.id.card_reviews).setOnClickListener {
            startActivity(Intent(this, ReviewsActivity::class.java))
        }

        // Friends
        findViewById<android.view.View>(R.id.item_friends).setOnClickListener {
            startActivity(Intent(this, FriendsActivity::class.java))
        }

        // Notifications
        findViewById<android.view.View>(R.id.item_notifications).setOnClickListener {
            startActivity(Intent(this, FragmentNotificationsActivity::class.java))
        }

        // Logout
        findViewById<android.view.View>(R.id.button_logout).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Bottom Navigation
        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java)); finish()
        }
        findViewById<android.view.View>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, DiscoverActivity::class.java)); finish()
        }
        findViewById<android.view.View>(R.id.navWatchlist).setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java)); finish()
        }
        findViewById<android.view.View>(R.id.navReviews).setOnClickListener {
            startActivity(Intent(this, ReviewsActivity::class.java)); finish()
        }
    }

    private fun uploadImageToXampp(imageUri: Uri) {
        val file = getFileFromUri(imageUri)
        if (file == null) {
            Toast.makeText(this, "Error getting file", Toast.LENGTH_SHORT).show()
            return
        }

        val mediaType = "image/*".toMediaTypeOrNull()
        val requestFile = RequestBody.create(mediaType, file)
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        // 1. Create Lenient Gson
        val gson = GsonBuilder()
            .setLenient()
            .create()

        // 2. Create Logging Interceptor
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // 3. Build Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val service = retrofit.create(FileUploadService::class.java)
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()

        service.uploadProfileImage(body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val finalUrl = response.body()!!.url
                    if (finalUrl != null) {
                        saveUrlToFirestore(finalUrl)
                    }
                } else {
                    Toast.makeText(this@Page9Activity, "Upload Failed: Code ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Toast.makeText(this@Page9Activity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUrlToFirestore(url: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("profileImage", url)
            .addOnSuccessListener {
                fetchUserInfo() // Refresh UI immediately after upload
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload", ".jpg", cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun fetchStats(uid: String, movieTv: TextView, showTv: TextView, watchTv: TextView, revTv: TextView) {
        db.collection("watchlist").whereEqualTo("userId", uid).get().addOnSuccessListener { docs ->
            val movies = docs.documents.count { it.getString("mediaType") == "movie" }
            val shows = docs.documents.count { it.getString("mediaType") == "tv" }
            watchTv.text = "${docs.size()} items to watch"
            movieTv.text = "$movies Movies"
            showTv.text = "$shows Shows"
        }
        db.collection("reviews").whereEqualTo("userId", uid).get().addOnSuccessListener { docs ->
            revTv.text = "${docs.size()} reviews written"
        }
    }
}