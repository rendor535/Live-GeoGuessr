package com.example.livegeoguessr.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.livegeoguessr.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions

enum class AccountAuthProvider {
    EMAIL,
    GOOGLE,
    UNKNOWN
}

@Singleton
class AuthRepository @Inject constructor() {

    private val firebaseApp = FirebaseApp.getInstance()

    private val auth = FirebaseAuth.getInstance(firebaseApp)

    private val functions = FirebaseFunctions.getInstance(
        firebaseApp,
        "us-central1"
    )

    private val userRepository = UserRepository()

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun currentUid(): String? {
        return auth.currentUser?.uid
    }

    fun currentAuthProvider(): AccountAuthProvider {
        val providerIds = auth.currentUser
            ?.providerData
            ?.map { provider -> provider.providerId }
            .orEmpty()

        return when {
            EmailAuthProvider.PROVIDER_ID in providerIds -> {
                AccountAuthProvider.EMAIL
            }

            GoogleAuthProvider.PROVIDER_ID in providerIds -> {
                AccountAuthProvider.GOOGLE
            }

            else -> {
                AccountAuthProvider.UNKNOWN
            }
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(
                email.trim(),
                password
            ).await()

            true
        } catch (exception: Exception) {
            false
        }
    }

    suspend fun register(
        email: String,
        password: String
    ): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(
                email.trim(),
                password
            ).await()

            result.user?.let { firebaseUser ->
                userRepository.createUserIfNotExists(firebaseUser)
            }

            true
        } catch (exception: Exception) {
            false
        }
    }

    suspend fun loginWithGoogle(context: Context) {
        val firebaseCredential = getGoogleCredential(context)

        val authResult = auth
            .signInWithCredential(firebaseCredential)
            .await()

        val firebaseUser = authResult.user
            ?: error("Firebase user is null")

        userRepository.createUserIfNotExists(firebaseUser)
    }

    suspend fun reauthenticateWithPassword(password: String) {
        val currentUser = auth.currentUser
            ?: error("User is not logged in")

        val email = currentUser.email
            ?: error("User does not have an email address")

        val credential = EmailAuthProvider.getCredential(
            email,
            password
        )

        currentUser
            .reauthenticate(credential)
            .await()

        /*
         * Wymuszamy wydanie świeżego ID tokenu z nowym auth_time.
         */
        currentUser
            .getIdToken(true)
            .await()
    }

    suspend fun reauthenticateWithGoogle(context: Context) {
        val currentUser = auth.currentUser
            ?: error("User is not logged in")

        val firebaseCredential = getGoogleCredential(context)

        currentUser
            .reauthenticate(firebaseCredential)
            .await()

        currentUser
            .getIdToken(true)
            .await()
    }

    suspend fun deleteAccount() {
        val currentUser = auth.currentUser
            ?: error("User is not logged in")

        /*
         * Funkcja musi otrzymać najnowszy token po reautoryzacji.
         */
        currentUser
            .getIdToken(true)
            .await()

        functions
            .getHttpsCallable("deleteAccount")
            .call(
                mapOf(
                    "confirm" to true
                )
            )
            .await()

        /*
         * Konto zostało już usunięte przez Admin SDK.
         * signOut czyści lokalny stan FirebaseAuth.
         */
        auth.signOut()
    }

    fun logout() {
        auth.signOut()
    }

    private suspend fun getGoogleCredential(
        context: Context
    ): AuthCredential {
        val credentialManager = CredentialManager.create(context)

        val webClientId = context.getString(
            R.string.default_web_client_id
        )

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

        if (
            credential !is CustomCredential ||
            credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Invalid Google credential")
        }

        val googleCredential =
            GoogleIdTokenCredential.createFrom(
                credential.data
            )

        return GoogleAuthProvider.getCredential(
            googleCredential.idToken,
            null
        )
    }
}