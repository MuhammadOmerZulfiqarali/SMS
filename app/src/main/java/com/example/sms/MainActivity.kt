package com.example.sms

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var storage: StorageReference
    private lateinit var imageView: ImageView
    private var imageUri: Uri? = null
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1002
    }

    // For result callback with the ActivityResultContracts API
    private val getImageResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            imageView.setImageURI(it)
        } ?: run {
            Log.e("MainActivity", "Image URI is null")
            Toast.makeText(this, "Image selection failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val usernameEditText = findViewById<EditText>(R.id.edt_username)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginBtn)
        imageView = findViewById(R.id.imageView)
        val signUpButton = findViewById<Button>(R.id.signupBtn)

        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Initialize Firebase components
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Check for permissions
        checkPermissions()

        // Set click listener for image view to pick an image from gallery
        imageView.setOnClickListener {
            getImageResult.launch("image/*")
        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase authentication with email and password
            auth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("MainActivity", "Login successful")
                        val userId = auth.currentUser?.uid
                        userId?.let {
                            proceedToChat(it, username, password)
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 (API 33) and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_CODE_PERMISSION)
            }
        } else {
            // For Android 11 (API 30) and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION)
            }
        }
    }

    private fun proceedToChat(userId: String, username: String, password: String) {
        val user = auth.currentUser
        if (user != null) {
            if (imageUri != null) {
                uploadImageAndStoreData(userId, username, password)
            } else {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageAndStoreData(userId: String, username: String, password: String) {
        Log.d("MainActivity", "Preparing to upload image")

        // Create a reference to Firebase Storage
        val imageRef = storage.child("images/${UUID.randomUUID()}.jpg")

        // Check if URI is not null before attempting to upload
        imageUri?.let { uri ->
            // Upload image to Firebase Storage
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    Log.d("MainActivity", "Image upload successful")
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Store user data along with the image URL in Firebase Database
                        val userData = mapOf(
                            "username" to username,
                            "password" to password,
                            "imageUrl" to downloadUri.toString()
                        )

                        // Save user data in the database
                        database.child("users").child(userId).setValue(userData)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, ChatActivity::class.java)
                                    intent.putExtra("userId", userId)  // Pass userId to the chat activity
                                    startActivity(intent)
                                } else {
                                    Log.e("MainActivity", "Failed to save user data: ${task.exception?.message}")
                                    Toast.makeText(this, "Failed to save data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }.addOnFailureListener { e ->
                        Log.e("MainActivity", "Failed to retrieve download URL: ${e.message}")
                        Toast.makeText(this, "Failed to retrieve download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Image upload failed: ${e.message}")
                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Log.e("MainActivity", "Image URI is null")
            Toast.makeText(this, "Image URI is null", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
