package com.example.greenmeter.backend

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
}