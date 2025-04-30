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
import com.example.greenmeter.R

@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

        ) {
        val context = LocalContext.current

        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.welcome_screen, null)
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val getStartedButton = view.findViewById<Button>(R.id.btnGetStarted)
                getStartedButton.setOnClickListener {
                    Log.d("Welcome", "Get Started clicked")
                    navController.navigate("login")
                }
                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}