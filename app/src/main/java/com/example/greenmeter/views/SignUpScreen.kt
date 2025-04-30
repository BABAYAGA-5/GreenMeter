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
import android.widget.EditText
import android.widget.TextView
import com.example.greenmeter.R
import com.example.greenmeter.backend.Authentification
import com.example.greenmeter.model.User

@Composable
fun SignUpScreen(navController: NavController) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

        ) {
        val context = LocalContext.current

        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.signup_screen, null)
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val firstName = view.findViewById<EditText>(R.id.firstName)
                val lastName = view.findViewById<EditText>(R.id.lastName)
                val phoneNumber = view.findViewById<EditText>(R.id.phoneNumber)
                val email = view.findViewById<EditText>(R.id.email)
                val password = view.findViewById<EditText>(R.id.password)
                val passwordconfirm = view.findViewById<EditText>(R.id.passwordconfirm)
                val SignUpButton = view.findViewById<Button>(R.id.SignUpButton)
                val login = view.findViewById<TextView>(R.id.login)

                SignUpButton.setOnClickListener {
                    Log.d("SignUp", "SignUp clicked")
                    val user: User = User()
                    user.firstName = firstName.text.toString()
                    user.lastName = lastName.text.toString()
                    user.phoneNumber = phoneNumber.text.toString()
                    user.email = email.text.toString()
                    user.password = password.text.toString()

                    Authentification().signUp(user)
                    Log.d("SignUp", "User: $user")
                    navController.navigate("login")

                }
                passwordconfirm.setOnEditorActionListener { _, _, _ ->
                    if (password.text.toString() != passwordconfirm.text.toString()) {
                        passwordconfirm.error = "Passwords do not match"
                        SignUpButton.isEnabled = false
                    }
                    else {
                        passwordconfirm.error = null
                        SignUpButton.isEnabled = true
                    }
                    false
                }
                login.setOnClickListener {
                    Log.d("LogIn", "LogIn clicked")
                    navController.navigate("login")
                }

                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}