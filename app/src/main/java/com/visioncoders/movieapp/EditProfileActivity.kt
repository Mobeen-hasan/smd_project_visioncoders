package com.visioncoders.movieapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etHandle: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var imgProfile: ShapeableImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentProfileImageUrl: String = ""

    private val BASE_URL = "http://192.168.0.102/movieapp/"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imgProfile.setImageURI(uri)
                uploadImageToBackend(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // 1. Bind Views
        etName = findViewById(R.id.etFullName)
        etHandle = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        imgProfile = findViewById(R.id.imgProfile)

        // 2. Load Current Data
        fetchUserData()

        // 3. Setup Buttons
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnSave).setOnClickListener { saveChanges() }
        findViewById<Button>(R.id.btnSaveChanges).setOnClickListener { saveChanges() }

        // 4. Image Upload Click
        findViewById<TextView>(R.id.tvChangePhoto).setOnClickListener { openGallery() }
        findViewById<android.view.View>(R.id.btnUpload).setOnClickListener { openGallery() }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    etName.setText(user.name)
                    etHandle.setText(user.handle)
                    etEmail.setText(user.email)
                    //etPhone.setText(user.bio)

                    currentProfileImageUrl = user.profileImage
                    if (currentProfileImageUrl.isNotEmpty()) {
                        Glide.with(this).load(currentProfileImageUrl).into(imgProfile)
                    }
                }
            }
        }
    }

    private fun saveChanges() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        val newName = etName.text.toString().trim()
        val newHandle = etHandle.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()
        val newBio = etPhone.text.toString().trim()

        if (newName.isEmpty() || newHandle.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Check if Email Changed
        if (newEmail != user.email) {
            user.updateEmail(newEmail)
                .addOnSuccessListener {
                    updateFirestore(userId, newName, newHandle, newEmail, newBio)
                }
                .addOnFailureListener { e ->
                    when (e) {
                        is FirebaseAuthRecentLoginRequiredException -> {
                            showReAuthDialog(newEmail, newName, newHandle, newBio)
                        }
                        is FirebaseAuthUserCollisionException -> {
                            Toast.makeText(this, "That email is already in use", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        } else {
            updateFirestore(userId, newName, newHandle, newEmail, newBio)
        }
    }

    private fun showReAuthDialog(newEmail: String, newName: String, newHandle: String, newBio: String) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Enter password to verify"
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50
        params.rightMargin = 50
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Verify Identity")
            .setMessage("For security, please enter your password to change your email.")
            .setView(container)
            .setPositiveButton("Verify") { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) {
                    reAuthenticateAndSave(password, newEmail, newName, newHandle, newBio)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reAuthenticateAndSave(password: String, newEmail: String, newName: String, newHandle: String, newBio: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updateEmail(newEmail)
                    .addOnSuccessListener {
                        updateFirestore(user.uid, newName, newHandle, newEmail, newBio)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update email: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFirestore(userId: String, name: String, handle: String, email: String, bio: String) {
        val user = auth.currentUser ?: return

        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
            displayName = name
            if (currentProfileImageUrl.isNotEmpty()) {
                photoUri = Uri.parse(currentProfileImageUrl)
            }
        }

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                val updates = hashMapOf<String, Any>(
                    "name" to name,
                    "handle" to handle,
                    "email" to email,
                    "bio" to bio,
                    "searchName" to name.lowercase(),
                    "profileImage" to currentProfileImageUrl
                )

                db.collection("users").document(userId).update(updates)
                    .addOnSuccessListener {

                        updateUserContentEverywhere(userId, name, currentProfileImageUrl)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Failed to update Auth Profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserContentEverywhere(userId: String, newName: String, newImage: String) {
        val batch = db.batch()


        db.collection("reviews").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { reviewDocs ->
                for (doc in reviewDocs) {
                    batch.update(doc.reference, "username", newName)
                    batch.update(doc.reference, "userImage", newImage)
                }


                db.collectionGroup("replies").whereEqualTo("userId", userId).get()
                    .addOnSuccessListener { replyDocs ->
                        for (doc in replyDocs) {
                            batch.update(doc.reference, "username", newName)
                            batch.update(doc.reference, "userImage", newImage)
                        }


                        batch.commit().addOnSuccessListener {
                            Toast.makeText(this, "Profile & Activity Updated!", Toast.LENGTH_SHORT).show()
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Save Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .addOnFailureListener { e ->

                        batch.commit().addOnSuccessListener {
                            Toast.makeText(this, "Updated Reviews (Replies failed - check Logcat)", Toast.LENGTH_LONG).show()
                            finish()
                        }


                        android.util.Log.e("Firestore", "Error updating replies: ${e.message}")
                    }
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun uploadImageToBackend(imageUri: Uri) {
        val file = getFileFromUri(imageUri) ?: return

        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        val gson = GsonBuilder().setLenient().create()
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val service = retrofit.create(FileUploadService::class.java)
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        service.uploadProfileImage(body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    currentProfileImageUrl = response.body()!!.url ?: ""
                    Toast.makeText(this@EditProfileActivity, "Image uploaded!", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMsg = response.body()?.message ?: "Upload failed"
                    Toast.makeText(this@EditProfileActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
}