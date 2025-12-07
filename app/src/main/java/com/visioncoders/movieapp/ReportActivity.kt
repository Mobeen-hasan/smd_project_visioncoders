package com.visioncoders.movieapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import com.visioncoders.movieapp.api.ReportService
import com.visioncoders.movieapp.api.UploadResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ReportActivity : AppCompatActivity() {


    private val BASE_URL = "http://192.168.0.102/movieapp/"

    private lateinit var etReason: EditText
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var targetUserId: String = ""
    private var targetUserName: String = "Unknown User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        targetUserId = intent.getStringExtra("TARGET_USER_ID") ?: return
        etReason = findViewById(R.id.etReportReason)

        setupUserInfo()

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { etReason.setText("") }

        findViewById<Button>(R.id.btnReportUser).setOnClickListener {
            submitReport()
        }
    }

    private fun setupUserInfo() {
        db.collection("users").document(targetUserId).get().addOnSuccessListener { doc ->
            val user = doc.toObject(User::class.java) ?: return@addOnSuccessListener
            targetUserName = user.name

            findViewById<TextView>(R.id.tvUserName).text = user.name
            val handleText = if (user.handle.isNotEmpty()) user.handle else "@${user.name.replace(" ", "").lowercase()}"
            findViewById<TextView>(R.id.tvUserHandle).text = handleText

            val imgProfile = findViewById<ImageView>(R.id.imgProfile)
            if (user.profileImage.isNotEmpty()) {
                Glide.with(this).load(user.profileImage).placeholder(R.drawable.profile).into(imgProfile)
            }
        }
    }

    private fun submitReport() {
        val reason = etReason.text.toString().trim()
        val currentUser = auth.currentUser

        if (currentUser == null) return
        if (reason.isEmpty()) {
            Toast.makeText(this, "Please provide a reason", Toast.LENGTH_SHORT).show()
            return
        }

        val gson = GsonBuilder().setLenient().create()
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val service = retrofit.create(ReportService::class.java)

        service.submitReport(
            reporterId = currentUser.uid,
            reporterName = currentUser.displayName ?: "Anonymous",
            reportedUserId = targetUserId,
            reportedUserName = targetUserName,
            reason = reason
        ).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {


                    sendReportNotification(currentUser.uid, currentUser.displayName ?: "Anonymous")


                    Toast.makeText(this@ReportActivity, "Report Submitted Successfully", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@ReportActivity, "Submission Failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Toast.makeText(this@ReportActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendReportNotification(reporterId: String, reporterName: String) {

        db.collection("users").document(reporterId).get().addOnSuccessListener { doc ->
            val myImage = doc.getString("profileImage") ?: ""

            val notif = hashMapOf(
                "type" to "report",
                "fromId" to "system",
                "fromName" to "Safety Team",
                "fromImage" to "",
                "toId" to targetUserId,
                "targetId" to "",
                "message" to "You have been reported for violating guidelines.",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("notifications").add(notif)

            // 2. Send Push Notification (Node.js)
            NotificationHelper.sendPush(
                targetUserId = targetUserId,
                title = "Account Warning",
                body = "You have been reported by a user."
            )
        }
    }
}