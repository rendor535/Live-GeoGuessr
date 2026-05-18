package com.example.livegeoguessr.domain.model

data class Post (
    val user: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double
)
