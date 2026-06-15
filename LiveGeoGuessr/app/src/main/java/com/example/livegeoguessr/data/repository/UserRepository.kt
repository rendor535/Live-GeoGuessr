package com.example.livegeoguessr.data.repository

import com.example.livegeoguessr.data.remote.firebase.FirebaseModule
import com.example.livegeoguessr.domain.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri

@Singleton
class UserRepository @Inject constructor(){

    private val users = FirebaseModule.firestore.collection("users")
    private val storage = FirebaseModule.storage
    suspend fun createUserIfNotExists(firebaseUser: FirebaseUser) {
        val uid = firebaseUser.uid
        val ref = users.document(uid)
        val snapshot = ref.get().await()

        if (!snapshot.exists()) {
            val data = hashMapOf(
                "uid" to uid,
                "email" to (firebaseUser.email ?: ""),
                "displayName" to (firebaseUser.displayName ?: ""),
                "nickname" to (
                        firebaseUser.displayName
                            ?: firebaseUser.email?.substringBefore("@")
                            ?: "Player"
                        ),
                "photoUrl" to firebaseUser.photoUrl?.toString(),
                "avatarPath" to null,
                "isBanned" to false,
                "stats" to hashMapOf(
                    "pointsTotal" to 0,
                    "guessesCount" to 0,
                    "postsCount" to 0,
                    "friendsCount" to 0,
                    "bestGuessMeters" to null
                ),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastLoginAt" to FieldValue.serverTimestamp()
            )

            ref.set(data).await()
        } else {
            ref.update(
                mapOf(
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }
    suspend fun saveProfile(uid: String, nickname: String) {
        users.document(uid)
            .update(
                mapOf(
                    "nickname" to nickname,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }
    suspend fun getUserProfile(uid: String): UserProfile? {
        val snapshot = users.document(uid).get().await()
        return snapshot.toObject(UserProfile::class.java)
    }

    suspend fun updateProfilePicture(uid: String, imageUri: Uri): String {
        val avatarPath = "avatars/$uid/profile_${System.currentTimeMillis()}.jpg"
        val avatarRef = storage.reference.child(avatarPath)

        avatarRef.putFile(imageUri).await()

        val downloadUrl = avatarRef.downloadUrl.await().toString()

        users.document(uid)
            .update(
                mapOf(
                    "photoUrl" to downloadUrl,
                    "avatarPath" to avatarPath,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()

        return downloadUrl
    }
}