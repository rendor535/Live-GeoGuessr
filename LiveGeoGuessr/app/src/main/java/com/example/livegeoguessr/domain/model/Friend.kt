package com.example.livegeoguessr.domain.model

data class Friend(
    val uid: String = "",
    val nickname: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
)