package com.example.livegeoguessr.data.repository

import com.example.livegeoguessr.factory.PostFactory
import android.graphics.Bitmap
import com.example.livegeoguessr.domain.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    suspend fun getPosts(): List<Post> {
        return try {
            val snapshot = firestore
                .collection("posts")
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                PostFactory.fromDocument(document)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addPost(
        bitmap: Bitmap,
        latitude: Double,
        longitude: Double
    ): Post {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("User is not logged in")

        val userId = currentUser.uid
        val userName = currentUser.displayName
            ?: currentUser.email
            ?: "Unknown user"

        val postDocument = firestore.collection("posts").document()
        val postId = postDocument.id

        val imageBytes = bitmap.toJpegByteArray()

        val imageRef = storage.reference
            .child("posts/$userId/$postId.jpg")

        imageRef.putBytes(imageBytes).await()

        val imageUrl = imageRef.downloadUrl.await().toString()

        val postData = PostFactory.toFirestoreMap(
            userId = userId,
            userName = userName,
            imageUrl = imageUrl,
            latitude = latitude,
            longitude = longitude
        )

        postDocument.set(postData).await()

        return Post(
            id = postId,
            user = userName,
            imageUrl = imageUrl,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun Bitmap.toJpegByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }
}