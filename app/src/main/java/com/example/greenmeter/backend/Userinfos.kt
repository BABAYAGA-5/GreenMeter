package com.example.greenmeter.backend

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Userinfos
{
    fun getUserInfos(userId: String, callback: (Map<String, Any>?) -> Unit) {
        val db = Firebase.firestore
        val collectionName = "users"
        db.collection(collectionName).document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    callback(document.data)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                callback(null)
            }
    }

    fun updateUserInfos(
        userId: String,
        firstName: String,
        lastName: String,
        userData: String,
    ) {
        val db = Firebase.firestore
        val collectionName = "users"
        val userinfos = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "userData" to userData
        )
        db.collection(collectionName).document(userId).set(userinfos)
            .addOnSuccessListener {
                println("User data updated successfully.")
            }
            .addOnFailureListener { e ->
                println("Error updating user data: ${e.message}")
            }
        val user = Firebase.auth.currentUser
        user?.updateProfile(
            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName("$firstName $lastName")
                .build()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                println("User display name updated successfully.")
            } else {
                println("Error updating user display name: ${task.exception?.message}")
            }
        }
    }
}