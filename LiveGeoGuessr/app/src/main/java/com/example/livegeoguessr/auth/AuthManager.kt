package com.example.livegeoguessr.auth

import android.content.Context
import com.example.livegeoguessr.data.repository.AccountAuthProvider
import com.example.livegeoguessr.data.repository.AuthRepository
import com.example.livegeoguessr.data.repository.UserRepository
import com.example.livegeoguessr.domain.model.UserProfile

object AuthManager {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    fun getCurrentUid(): String? {
        return authRepository.currentUid()
    }

    fun getCurrentAuthProvider(): AccountAuthProvider {
        return authRepository.currentAuthProvider()
    }

    suspend fun loginWithGoogle(
        context: Context
    ): UserProfile? {
        authRepository.loginWithGoogle(context)
        return getCurrentUserProfile()
    }

    suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = authRepository.currentUid()
            ?: return null

        return userRepository.getUserProfile(uid)
    }

    suspend fun reauthenticateWithPassword(
        password: String
    ) {
        authRepository.reauthenticateWithPassword(password)
    }

    suspend fun reauthenticateWithGoogle(
        context: Context
    ) {
        authRepository.reauthenticateWithGoogle(context)
    }

    suspend fun deleteAccount() {
        authRepository.deleteAccount()
    }

    fun logout() {
        authRepository.logout()
    }
}