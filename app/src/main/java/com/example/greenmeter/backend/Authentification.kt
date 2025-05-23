package com.example.greenmeter.backend

import android.util.Log
import com.example.greenmeter.model.User
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore

class Authentification
{
    fun signUp(user: User) {
        Firebase.auth.createUserWithEmailAndPassword(user.email, user.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userMetadata = Firebase.auth.currentUser
                    userMetadata!!.updateProfile(
                        com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName("${user.firstName} ${user.lastName}")
                            .build()
                    ).addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            Log.d("SignUp", "Display name set successfully.")
                        } else {
                            Log.w("SignUp", "Failed to set display name: ${profileTask.exception?.message}")
                        }
                    }

                    userMetadata.sendEmailVerification()
                        .addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                Log.d("SignUp", "Email sent.")
                            } else {
                                Log.w("SignUp", "Email verification failed: ${emailTask.exception?.message}")
                            }
                        }

                    user.uid = userMetadata.uid
                    val db = Firebase.firestore
                    val userinfos = hashMapOf(
                        "firstName" to user.firstName,
                        "lastName" to user.lastName,
                        "phoneNumber" to user.phoneNumber,
                    )
                    val collectionName = "users"
                    db.collection(collectionName).document(user.uid.toString()).set(userinfos)
                        .addOnSuccessListener {
                            Log.d("SignUp", "User data saved successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.w("SignUp", "Error saving user data: ${e.message}")
                        }
                } else {
                    Log.w("SignUp", "User creation failed: ${task.exception?.message}")
                }
            }
    }
    fun login(email: String, password: String, callback: (Boolean) -> Unit) {
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Login", "User logged in successfully.")
                    callback(true)
                } else {
                    Log.w("Login", "User login failed: ${task.exception?.message}")
                    callback(false)
                }
            }
    }
}