package com.example.livegeoguessr.data.repository

import com.example.livegeoguessr.domain.model.Post
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
) {
    suspend fun getPosts(): List<Post> {
        delay(3000)
        return listOf(
            Post(user = "Marcin"),
            Post(user = "Kamil"),
            Post(user = "Sławek")
        )
    }
}