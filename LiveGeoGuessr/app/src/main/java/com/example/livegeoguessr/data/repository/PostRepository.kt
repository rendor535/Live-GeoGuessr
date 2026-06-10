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
import com.example.livegeoguessr.domain.model.GuessedPost
import com.google.firebase.firestore.FieldPath
import com.example.livegeoguessr.domain.model.PublicUser
@Singleton
class PostRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val TAG = "PostRepositoryDebug"
    suspend fun getPosts(): List<Post> {
        return try {
            val friendUids = getFriendUids()
            val guessedPostIds = getMyGuessedPostIds()
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
            android.util.Log.d("PostRepository", "Guessed post IDs: $guessedPostIds")

            val notGuessedPosts = postsFromFriends.filter { post ->
                post.id !in guessedPostIds
            }

            android.util.Log.d("PostRepository", "Posts after filtering guessed: ${notGuessedPosts.size}")

            attachAuthorProfiles(notGuessedPosts)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Error loading friend posts", e)
            throw e
        }
    }
    private suspend fun attachAuthorProfiles(posts: List<Post>): List<Post> {
        if (posts.isEmpty()) {
            return posts
        }

        val authorUids = posts
            .map { it.authorUid }
            .filter { it.isNotBlank() }
            .distinct()

        if (authorUids.isEmpty()) {
            return posts
        }

        val usersByUid = mutableMapOf<String, PublicUser>()

        authorUids.chunked(30).forEach { uidChunk ->
            val snapshot = firestore
                .collection("users")
                .whereIn(FieldPath.documentId(), uidChunk)
                .get()
                .await()

            snapshot.documents.forEach { document ->
                usersByUid[document.id] = PublicUser(
                    uid = document.id,
                    nickname = document.getString("nickname") ?: "",
                    displayName = document.getString("displayName") ?: "",
                    photoUrl = document.getString("photoUrl")
                )
            }
        }

        return posts.map { post ->
            val author = usersByUid[post.authorUid]

            val displayName =
                author?.nickname?.takeIf { it.isNotBlank() }
                    ?: author?.displayName?.takeIf { it.isNotBlank() }
                    ?: post.user

            post.copy(
                user = displayName,
                authorPhotoUrl = author?.photoUrl ?: post.authorPhotoUrl
            )
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

            val posts = snapshot.documents.mapNotNull { document ->
                PostFactory.fromDocument(document)
            }

            attachAuthorProfiles(posts)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Error loading my posts", e)
            throw e
        }
    }
    suspend fun getMyGuessedPosts(): List<GuessedPost> {
        val guessDocuments = getMyGuessDocuments()
        val guessedData = guessDocuments.mapNotNull { document ->
            val postId = document.getString("postId") ?: return@mapNotNull null
            GuessedPostData(
                postId = postId,
                distanceMeters = document.getDouble("distanceMeters") ?: 0.0,
                points = document.getLong("points")?.toInt() ?: 0
            )
        }

        val postsById = getPostsByIds(
            guessedData.map { it.postId }
        )

        return guessedData.mapNotNull { guess ->
            val post = postsById[guess.postId] ?: return@mapNotNull null

            GuessedPost(
                post = post,
                distanceMeters = guess.distanceMeters,
                points = guess.points
            )
        }
    }

    private suspend fun getMyGuessedPostIds(): Set<String> {
        return getMyGuessDocuments()
            .mapNotNull { document -> document.getString("postId") }
            .toSet()
    }

    private suspend fun getMyGuessDocuments(): List<DocumentSnapshot> {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("User is not logged in")

        val snapshot = firestore
            .collection("guesses")
            .whereEqualTo("userUid", currentUser.uid)
            .get()
            .await()
        return snapshot.documents.sortedByDescending { document ->
            document.getTimestamp("createdAt")?.seconds ?: 0L
        }
    }

    private suspend fun getPostsByIds(postIds: List<String>): Map<String, Post> {
        if (postIds.isEmpty()) {
            return emptyMap()
        }

        val posts = mutableMapOf<String, Post>()

        postIds.distinct().chunked(30).forEach { chunk ->
            val snapshot = firestore
                .collection("posts")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            snapshot.documents.forEach { document ->
                PostFactory.fromDocument(document)?.let { post ->
                    posts[post.id] = post
                }
            }
        }

        val enrichedPosts = attachAuthorProfiles(posts.values.toList())

        return enrichedPosts.associateBy { post ->
            post.id
        }
    }

    private data class GuessedPostData(
        val postId: String,
        val distanceMeters: Double,
        val points: Int
    )
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

        val userDocument = firestore
            .collection("users")
            .document(userId)
            .get()
            .await()

        val userName =
            userDocument.getString("nickname")?.takeIf { it.isNotBlank() }
                ?: userDocument.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: currentUser.displayName
                ?: currentUser.email
                ?: "Unknown user"

        val userPhotoUrl =
            userDocument.getString("photoUrl")
                ?: currentUser.photoUrl?.toString()

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
            userPhotoUrl = userPhotoUrl,
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
            longitude = longitude,
            authorPhotoUrl = userPhotoUrl
        )
    }
    private fun Bitmap.toJpegByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }
}