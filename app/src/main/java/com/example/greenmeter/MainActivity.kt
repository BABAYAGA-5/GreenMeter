package com.example.greenmeter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.greenmeter.views.AddDevice
import com.example.greenmeter.views.DeviceDetails
import com.example.greenmeter.views.LogInScreen
import com.example.greenmeter.views.ProfileScreen
import com.example.location.HomeScreen
import com.example.location.SignUpScreen
import com.example.location.WelcomeScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseFirestore.setLoggingEnabled(true)
        val user = Firebase.auth.currentUser
        val startDestination: String
        if (user != null) {
            startDestination = "home"
        } else {
            startDestination = "welcome"
        }

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = startDestination) {
                composable("welcome") {
                    WelcomeScreen(navController)
                }
                composable("login") {
                    LogInScreen(navController)
                }
                composable("signup") {
                    SignUpScreen(navController)
                }
                composable("home") {
                    HomeScreen(navController)
                }
                composable("profile") {
                    ProfileScreen(navController)
                }
                composable("details/{deviceId}") { backStackEntry ->
                    val deviceId = backStackEntry.arguments?.getString("deviceId")
                    if (deviceId != null) {
                        DeviceDetails(deviceId, navController)
                    }
                }
                composable("AddDevice") {
                    AddDevice(navController)
                }
            }
        }
    }
}