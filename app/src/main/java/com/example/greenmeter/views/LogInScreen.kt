package com.example.greenmeter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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

@Composable
fun LogInScreen (navController: NavController) {
    Column(
    Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,

    ) {
        val context = LocalContext.current

        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.login_screen, null)
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val email = view.findViewById<EditText>(R.id.email)
                val password = view.findViewById<EditText>(R.id.password)
                val LogInButton = view.findViewById<Button>(R.id.LoginButton)
                val signup = view.findViewById<TextView>(R.id.SignUp)
                val forgotpassword = view.findViewById<TextView>(R.id.forgot_password)



                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}