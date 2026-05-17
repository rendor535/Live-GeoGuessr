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
            Post(user = "Marcin", imageUrl = "https://picsum.photos/200/300", latitude = 52.2297, longitude = 21.0122),
            Post(user = "Kamil", imageUrl = "https://picsum.photos/200/300", latitude = 50.0647, longitude = 19.9450),
            Post(user = "Sławek", imageUrl = "https://picsum.photos/200/300", latitude = 51.1079, longitude = 17.0385)
        )
    }
}