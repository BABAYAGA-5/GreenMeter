package com.example.greenmeter.views

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
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
import com.example.greenmeter.backend.Authentification
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
                val logoutButton = view.findViewById<Button>(R.id.logoutButton)
                val saveChangesButton = view.findViewById<Button>(R.id.saveChangesButton)
                backButton.setOnClickListener {
                    Log.d("Profile", "Back clicked")
                    navController.navigate("home")
                }

                logoutButton.setOnClickListener {
                    Log.d("Profile", "Logout clicked")
                    Firebase.auth.signOut()
                    navController.navigate("login")
                }

                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}