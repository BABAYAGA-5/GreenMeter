package com.example.greenmeter.views

import android.util.Log
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
import com.example.greenmeter.R
import com.example.greenmeter.backend.Authentification

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
                val error = view.findViewById<TextView>(R.id.error)

                signup.setOnClickListener {
                    Log.d("SignUp", "SignUp clicked")
                    navController.navigate("signup")
                }

                LogInButton.setOnClickListener {
                    Log.d("LogIn", "LogIn clicked")
                    val emailText = email.text.toString()
                    val passwordText = password.text.toString()
                    if (emailText.isNotEmpty() && passwordText.isNotEmpty()) {
                        Log.d("LogIn", "Email: $emailText, Password: $passwordText")
                        Authentification().login(emailText, passwordText) { success ->
                            if (success) {
                                Log.d("LogIn", "Login successful")
                                navController.navigate("home")
                            } else {
                                Log.d("LogIn", "Login failed")
                                error.text = "Login failed. Please check your credentials."
                            }
                        }
                    } else {
                        Log.d("LogIn", "Please fill in all fields")
                    }
                }

                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}