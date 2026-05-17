package com.example.livegeoguessr.data.repository

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {
    suspend fun login(email: String, password: String): Boolean {
        delay(2000)
        return true
    }

    suspend fun register(email: String, password: String): Boolean {
        delay(2000)
        return true
    }
}
