package com.example.livegeoguessr.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.livegeoguessr.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val userRepository: UserRepository = UserRepository()

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun currentUid(): String? {
        return auth.currentUser?.uid
    }

    suspend fun login(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(
                email.trim(),
                password
            ).await()

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun register(email: String, password: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(
                email.trim(),
                password
            ).await()

            result.user?.let { firebaseUser ->
                UserRepository().createUserIfNotExists(firebaseUser)
            }

            true
        } catch (e: Exception) {
            false
        }
    }
    suspend fun loginWithGoogle(context: Context) {
        val credentialManager = CredentialManager.create(context)

        val webClientId = context.getString(R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = context
        )

        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)

            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleCredential.idToken,
                null
            )

            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user ?: error("Firebase user is null")

            userRepository.createUserIfNotExists(firebaseUser)
        } else {
            error("Invalid Google credential")
        }
    }

    fun logout() {
        auth.signOut()
    }
}
