package com.example.livegeoguessr.data.repository

import com.example.livegeoguessr.factory.PostFactory
import android.graphics.Bitmap
import com.example.livegeoguessr.domain.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
            val friendUids = getFriendUids()

            android.util.Log.d("PostRepository", "Current user: ${auth.currentUser?.uid}")
            android.util.Log.d("PostRepository", "Friend UIDs: $friendUids")

            if (friendUids.isEmpty()) {
                android.util.Log.d("PostRepository", "No friends found")
                return emptyList()
            }

            val postsFromFriends = mutableListOf<Post>()

            friendUids.chunked(30).forEach { uidChunk ->
                android.util.Log.d("PostRepository", "Query posts where userId in: $uidChunk")

                val snapshot = firestore
                    .collection("posts")
                    .whereIn("userId", uidChunk)
                    .get()
                    .await()

                android.util.Log.d("PostRepository", "Posts found in chunk: ${snapshot.size()}")

                snapshot.documents.forEach { document ->
                    android.util.Log.d(
                        "PostRepository",
                        "Post ${document.id}: userId=${document.getString("userId")}, user=${document.getString("user")}"
                    )
                }

                val posts = snapshot.documents.mapNotNull { document: DocumentSnapshot ->
                    PostFactory.fromDocument(document)
                }

                postsFromFriends.addAll(posts)
            }

            android.util.Log.d("PostRepository", "Total posts from friends: ${postsFromFriends.size}")

            postsFromFriends
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Error loading friend posts", e)
            throw e
        }
    }
    suspend fun getMyPosts(): List<Post> {
        return try {
            val currentUser = auth.currentUser
                ?: throw IllegalStateException("User is not logged in")

            val snapshot = firestore
                .collection("posts")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                PostFactory.fromDocument(document)
            }
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Error loading my posts", e)
            throw e
        }
    }
    private suspend fun getFriendUids(): List<String> {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("User is not logged in")

        val snapshot = firestore
            .collection("users")
            .document(currentUser.uid)
            .collection("friends")
            .get()
            .await()

        return snapshot.documents.map { document ->
            document.id
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

        val imageBytes = withContext(Dispatchers.Default) {
            bitmap.toJpegByteArray()
        }

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

        firestore
            .collection("users")
            .document(userId)
            .update(
                mapOf(
                    "stats.postsCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
        return Post(
            id = postId,
            authorUid = userId,
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