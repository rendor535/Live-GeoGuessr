package com.example.livegeoguessr.domain.model

data class Post (
    val id: String,
    val authorUid: String = "",
    val user: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val authorPhotoUrl: String? = null
)
