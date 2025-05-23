package com.example.greenmeter.views

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
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
import com.google.firebase.ktx.Firebase

@Composable
fun ProfileScreen (navController: NavController) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

        ) {
        val context = LocalContext.current

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
                backButton.setOnClickListener {
                    Log.d("Profile", "Back clicked")
                    navController.navigate("home")
                }

                logoutButton.setOnClickListener {
                    Log.d("Profile", "Logout clicked")
                    Firebase.auth.signOut()
                    navController.navigate("login")
                }

                val userinfos = Userinfos()
                userinfos.getUserInfos(Firebase.auth.currentUser?.uid.toString()) { userData ->
                    if (userData != null) {
                        println("User data: $userData")
                    } else {
                        println("No user data found or an error occurred.")
                    }

                    val firstNameEditText = view.findViewById<EditText>(R.id.firstNameEditText)
                    val lastNameEditText = view.findViewById<EditText>(R.id.lastNameEditText)
                    val phoneEditText = view.findViewById<EditText>(R.id.phoneEditText)
                    val emailTextView = view.findViewById<TextView>(R.id.emailTextView)
                    val changePasswordTextView = view.findViewById<TextView>(R.id.changePasswordTextView)

                    firstNameEditText.setText(userData?.get("firstName").toString())
                    lastNameEditText.setText(userData?.get("lastName").toString())
                    phoneEditText.setText(userData?.get("phoneNumber").toString())
                    emailTextView.setText(Firebase.auth.currentUser?.email.toString())

                    changePasswordTextView.setOnClickListener {
                        Log.d("Profile", "Change Password clicked")
                        navController.navigate("changepassword")
                    }

                    saveChangesButton.setOnClickListener {
                        Log.d("Profile", "Save Changes clicked")
                        val firstName = firstNameEditText.text.toString()
                        val lastName = lastNameEditText.text.toString()
                        val phoneNumber = phoneEditText.text.toString()

                        userinfos.updateUserInfos(Firebase.auth.currentUser?.uid.toString(), firstName, lastName, phoneNumber)
                    }

                }

                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}