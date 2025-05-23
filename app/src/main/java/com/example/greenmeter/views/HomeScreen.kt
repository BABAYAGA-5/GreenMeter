package com.example.location

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.example.greenmeter.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

        ) {
        val context = LocalContext.current

        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.home_screen, null)
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val logoutButton = view.findViewById<ImageButton>(R.id.logoutButton)
                logoutButton.setOnClickListener {
                    Log.d("HomeScreen", "Logout clicked")
                    Firebase.auth.signOut()
                    navController.navigate("login")
                }
                val greetingText = view.findViewById<TextView>(R.id.greetingText)
                val datetime = view.findViewById<TextView>(R.id.datetime)
                val curentdatetime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(java.util.Date())
                datetime.text = curentdatetime
                val currenttime = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
                when (currenttime) {
                    in "00:00:00".."06:00:00" -> greetingText.text = "Good Night"
                    in "06:00:01".."12:00:00" -> greetingText.text = "Good Morning"
                    in "12:00:01".."18:00:00" -> greetingText.text = "Good Afternoon"
                    in "18:00:01".."23:59:59" -> greetingText.text = "Good Evening"
                }
                val displayName = view.findViewById<TextView>(R.id.displayName)
                displayName.text = "${Firebase.auth.currentUser?.displayName}"
                val profileButton = view.findViewById<ImageView>(R.id.profileButton)
                val homeButton = view.findViewById<ImageView>(R.id.homeButton)
                val analyticsButton = view.findViewById<ImageView>(R.id.analyticsButton)
                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}