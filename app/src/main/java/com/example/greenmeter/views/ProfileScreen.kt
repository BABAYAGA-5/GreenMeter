package com.example.greenmeter.views

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.greenmeter.R
import com.example.greenmeter.backend.Userinfos
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import android.content.SharedPreferences

@Composable
fun ProfileScreen(navController: NavController) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val context = LocalContext.current
        val sharedPreferences = context.getSharedPreferences("GreenMeterPrefs", Context.MODE_PRIVATE)

        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.profile_screen, null)
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val backButton = view.findViewById<ImageButton>(R.id.backButton)
                val logoutButton = view.findViewById<ImageButton>(R.id.logoutButton)
                val saveChangesButton = view.findViewById<ImageButton>(R.id.saveChangesButton)
                val notificationsSwitch = view.findViewById<Switch>(R.id.notificationsSwitch)
                val changePasswordButton = view.findViewById<Button>(R.id.changePasswordButton)

                // Initialize notification switch state from SharedPreferences
                notificationsSwitch.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)

                backButton.setOnClickListener {
                    Log.d("Profile", "Back clicked")
                    navController.navigate("home")
                }

                logoutButton.setOnClickListener {
                    Log.d("Profile", "Logout clicked")
                    Firebase.auth.signOut()
                    navController.navigate("login")
                }

                // Handle notification switch changes
                notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
                    Log.d("Profile", "Notifications ${if (isChecked) "enabled" else "disabled"}")
                    sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
                    
                    // Update notification setting in Firebase
                    val userId = Firebase.auth.currentUser?.uid
                    if (userId != null) {
                        FirebaseDatabase.getInstance().getReference("users")
                            .child(userId)
                            .child("notificationsEnabled")
                            .setValue(isChecked)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Notification settings updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to update notification settings",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("Profile", "Error updating notifications setting", e)
                            }
                    }
                }

                // Handle password reset
                changePasswordButton.setOnClickListener {
                    val user = Firebase.auth.currentUser
                    val email = user?.email
                    
                    if (email != null) {
                        Firebase.auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Password reset email sent to $email",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to send password reset email: ${task.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Log.e("Profile", "Error sending password reset email", task.exception)
                                }
                            }
                    } else {
                        Toast.makeText(
                            context,
                            "No email address found for your account",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                val userinfos = Userinfos()
                userinfos.getUserInfos(Firebase.auth.currentUser?.uid.toString()) { userData ->
                    if (userData != null) {
                        println("User data: $userData")
                        
                        // Set notification switch state from Firebase data
                        val notificationsEnabled = userData["notificationsEnabled"] as? Boolean ?: true
                        notificationsSwitch.isChecked = notificationsEnabled
                        sharedPreferences.edit().putBoolean("notifications_enabled", notificationsEnabled).apply()
                        
                        val firstNameEditText = view.findViewById<EditText>(R.id.firstNameEditText)
                        val lastNameEditText = view.findViewById<EditText>(R.id.lastNameEditText)
                        val phoneEditText = view.findViewById<EditText>(R.id.phoneEditText)
                        val emailTextView = view.findViewById<TextView>(R.id.emailTextView)

                        firstNameEditText.setText(userData["firstName"].toString())
                        lastNameEditText.setText(userData["lastName"].toString())
                        phoneEditText.setText(userData["phoneNumber"].toString())
                        emailTextView.text = Firebase.auth.currentUser?.email.toString()

                        saveChangesButton.setOnClickListener {
                            Log.d("Profile", "Save Changes clicked")
                            val firstName = firstNameEditText.text.toString()
                            val lastName = lastNameEditText.text.toString()
                            val phoneNumber = phoneEditText.text.toString()

                            userinfos.updateUserInfos(
                                Firebase.auth.currentUser?.uid.toString(),
                                firstName,
                                lastName,
                                phoneNumber,
                                notificationsEnabled = notificationsSwitch.isChecked
                            )
                            
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        println("No user data found or an error occurred.")
                        Toast.makeText(
                            context,
                            "Failed to load user data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}