package com.example.livegeoguessr.factory

import com.example.livegeoguessr.domain.model.Post
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

object PostFactory {

    fun fromDocument(document: DocumentSnapshot): Post? {
        val imageUrl = document.getString("imageUrl") ?: return null
        val latitude = document.getDouble("latitude") ?: return null
        val longitude = document.getDouble("longitude") ?: return null
        val user = document.getString("user") ?: ""
        val authorUid = document.getString("userId") ?: ""

        return Post(
            id = document.id,
            authorUid = authorUid,
            user = user,
            imageUrl = imageUrl,
            latitude = latitude,
            longitude = longitude
        )
    }

    fun toFirestoreMap(
        userId: String,
        userName: String,
        imageUrl: String,
        latitude: Double,
        longitude: Double
    ): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "user" to userName,
            "imageUrl" to imageUrl,
            "latitude" to latitude,
            "longitude" to longitude,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}