package com.example.livegeoguessr.data.repository

import com.example.livegeoguessr.data.remote.firebase.FirebaseModule
import com.example.livegeoguessr.domain.model.UserProfile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class FriendRequestData(
    val id: String,
    val fromUid: String,
    val toUid: String,
    val fromNickname: String,
    val fromDisplayName: String,
    val fromPhotoUrl: String?,
    val toNickname: String,
    val toDisplayName: String,
    val toPhotoUrl: String?,
    val status: String
)

@Singleton
class FriendRepository @Inject constructor() {

    private val auth = FirebaseModule.auth
    private val firestore = FirebaseModule.firestore
    private val functions = FirebaseModule.functions

    private val users = firestore.collection("users")
    private val friendRequests = firestore.collection("friendRequests")

    private fun currentUid(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User is not logged in")
    }

    suspend fun getUsersForFriendSearch(): List<UserProfile> {
        val currentUid = currentUid()

        val friendsSnapshot = users
            .document(currentUid)
            .collection("friends")
            .get()
            .await()

        val friendUids = friendsSnapshot.documents.map { it.id }.toSet()

        val incomingRequestUids = getIncomingRequests()
            .map { it.fromUid }
            .toSet()

        val outgoingRequestUids = getOutgoingRequests()
            .map { it.toUid }
            .toSet()

        val excludedUids = friendUids + incomingRequestUids + outgoingRequestUids + currentUid

        val snapshot = users
            .limit(200)
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { document -> document.toObject(UserProfile::class.java) }
            .filter { user -> user.uid !in excludedUids }
    }

    suspend fun getFriends(): List<UserProfile> {
        val currentUid = currentUid()

        val friendsSnapshot = users
            .document(currentUid)
            .collection("friends")
            .get()
            .await()

        val friendUids = friendsSnapshot.documents.map { document -> document.id }

        return friendUids.mapNotNull { friendUid ->
            users
                .document(friendUid)
                .get()
                .await()
                .toObject(UserProfile::class.java)
        }
    }

    suspend fun getIncomingRequests(): List<FriendRequestData> {
        val currentUid = currentUid()

        val snapshot = friendRequests
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "pending")
            .get()
            .await()

        return snapshot.documents.map { document ->
            FriendRequestData(
                id = document.id,
                fromUid = document.getString("fromUid") ?: "",
                toUid = document.getString("toUid") ?: "",
                fromNickname = document.getString("fromNickname") ?: "Player",
                fromDisplayName = document.getString("fromDisplayName") ?: "",
                fromPhotoUrl = document.getString("fromPhotoUrl"),
                toNickname = document.getString("toNickname") ?: "Player",
                toDisplayName = document.getString("toDisplayName") ?: "",
                toPhotoUrl = document.getString("toPhotoUrl"),
                status = document.getString("status") ?: "pending"
            )
        }
    }

    suspend fun getOutgoingRequests(): List<FriendRequestData> {
        val currentUid = currentUid()

        val snapshot = friendRequests
            .whereEqualTo("fromUid", currentUid)
            .whereEqualTo("status", "pending")
            .get()
            .await()

        return snapshot.documents.map { document ->
            FriendRequestData(
                id = document.id,
                fromUid = document.getString("fromUid") ?: "",
                toUid = document.getString("toUid") ?: "",
                fromNickname = document.getString("fromNickname") ?: "Player",
                fromDisplayName = document.getString("fromDisplayName") ?: "",
                fromPhotoUrl = document.getString("fromPhotoUrl"),
                toNickname = document.getString("toNickname") ?: "Player",
                toDisplayName = document.getString("toDisplayName") ?: "",
                toPhotoUrl = document.getString("toPhotoUrl"),
                status = document.getString("status") ?: "pending"
            )
        }
    }

    suspend fun sendFriendRequest(toUid: String) {
        functions
            .getHttpsCallable("sendFriendRequest")
            .call(mapOf("toUid" to toUid))
            .await()
    }

    suspend fun acceptFriendRequest(requestId: String) {
        functions
            .getHttpsCallable("acceptFriendRequest")
            .call(mapOf("requestId" to requestId))
            .await()
    }

    suspend fun rejectFriendRequest(requestId: String) {
        functions
            .getHttpsCallable("rejectFriendRequest")
            .call(mapOf("requestId" to requestId))
            .await()
    }

    suspend fun removeFriend(friendUid: String) {
        functions
            .getHttpsCallable("removeFriend")
            .call(mapOf("friendUid" to friendUid))
            .await()
    }
}