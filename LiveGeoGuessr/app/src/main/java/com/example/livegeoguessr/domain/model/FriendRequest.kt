package com.example.livegeoguessr.domain.model

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromNickname: String = "",
    val fromPhotoUrl: String? = null,
    val toNickname: String = "",
    val toPhotoUrl: String? = null,
    val status: String = "pending",
)