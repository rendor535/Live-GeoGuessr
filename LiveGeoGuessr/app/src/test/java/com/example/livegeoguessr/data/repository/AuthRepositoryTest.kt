package com.example.livegeoguessr.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserInfo
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import sun.misc.Unsafe

class AuthRepositoryTest {

    private lateinit var firebaseApp: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions
    private lateinit var userRepository: UserRepository
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var repository: AuthRepository

    @BeforeEach
    fun setUp() {
        firebaseApp = mockk()
        auth = mockk()
        functions = mockk()
        userRepository = mockk()
        firebaseUser = mockk()

        repository = createRepositoryWithoutConstructor()

        setPrivateField(
            target = repository,
            fieldName = "firebaseApp",
            value = firebaseApp
        )

        setPrivateField(
            target = repository,
            fieldName = "auth",
            value = auth
        )

        setPrivateField(
            target = repository,
            fieldName = "functions",
            value = functions
        )

        setPrivateField(
            target = repository,
            fieldName = "userRepository",
            value = userRepository
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // -------------------------------------------------------------------------
    // isLoggedIn
    // -------------------------------------------------------------------------

    @Test
    fun `isLoggedIn returns true when current user exists`() {
        every { auth.currentUser } returns firebaseUser

        assertTrue(repository.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns false when current user is null`() {
        every { auth.currentUser } returns null

        assertFalse(repository.isLoggedIn())
    }

    // -------------------------------------------------------------------------
    // currentUid
    // -------------------------------------------------------------------------

    @Test
    fun `currentUid returns uid when user is logged in`() {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "user1"

        assertEquals(
            "user1",
            repository.currentUid()
        )
    }

    @Test
    fun `currentUid returns null when user is not logged in`() {
        every { auth.currentUser } returns null

        assertNull(repository.currentUid())
    }

    // -------------------------------------------------------------------------
    // currentAuthProvider
    // -------------------------------------------------------------------------

    @Test
    fun `currentAuthProvider returns EMAIL for password provider`() {
        val provider = providerInfo(
            EmailAuthProvider.PROVIDER_ID
        )

        every { auth.currentUser } returns firebaseUser
        every {
            firebaseUser.providerData
        } returns mutableListOf(provider)

        assertEquals(
            AccountAuthProvider.EMAIL,
            repository.currentAuthProvider()
        )
    }

    @Test
    fun `currentAuthProvider returns GOOGLE for google provider`() {
        val provider = providerInfo(
            GoogleAuthProvider.PROVIDER_ID
        )

        every { auth.currentUser } returns firebaseUser
        every {
            firebaseUser.providerData
        } returns mutableListOf(provider)

        assertEquals(
            AccountAuthProvider.GOOGLE,
            repository.currentAuthProvider()
        )
    }

    @Test
    fun `currentAuthProvider gives EMAIL priority when both providers exist`() {
        val googleProvider = providerInfo(
            GoogleAuthProvider.PROVIDER_ID
        )

        val emailProvider = providerInfo(
            EmailAuthProvider.PROVIDER_ID
        )

        every { auth.currentUser } returns firebaseUser
        every {
            firebaseUser.providerData
        } returns mutableListOf(
            googleProvider,
            emailProvider
        )

        assertEquals(
            AccountAuthProvider.EMAIL,
            repository.currentAuthProvider()
        )
    }

    @Test
    fun `currentAuthProvider returns UNKNOWN for unsupported provider`() {
        val provider = providerInfo("github.com")

        every { auth.currentUser } returns firebaseUser
        every {
            firebaseUser.providerData
        } returns mutableListOf(provider)

        assertEquals(
            AccountAuthProvider.UNKNOWN,
            repository.currentAuthProvider()
        )
    }

    @Test
    fun `currentAuthProvider returns UNKNOWN for empty provider list`() {
        every { auth.currentUser } returns firebaseUser
        every {
            firebaseUser.providerData
        } returns mutableListOf()

        assertEquals(
            AccountAuthProvider.UNKNOWN,
            repository.currentAuthProvider()
        )
    }

    @Test
    fun `currentAuthProvider returns UNKNOWN when user is not logged in`() {
        every { auth.currentUser } returns null

        assertEquals(
            AccountAuthProvider.UNKNOWN,
            repository.currentAuthProvider()
        )
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    fun `login trims email and returns true on success`() = runTest {
        val authResult = mockk<AuthResult>()

        every {
            auth.signInWithEmailAndPassword(
                "user@test.com",
                "password"
            )
        } returns Tasks.forResult(authResult)

        val result = repository.login(
            email = "   user@test.com   ",
            password = "password"
        )

        assertTrue(result)

        verify(exactly = 1) {
            auth.signInWithEmailAndPassword(
                "user@test.com",
                "password"
            )
        }
    }

    @Test
    fun `login returns false when Firebase login fails`() = runTest {
        every {
            auth.signInWithEmailAndPassword(
                "user@test.com",
                "wrong"
            )
        } returns Tasks.forException(
            IllegalStateException("Login failed")
        )

        val result = repository.login(
            email = "user@test.com",
            password = "wrong"
        )

        assertFalse(result)
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    fun `register trims email creates profile and returns true`() = runTest {
        val authResult = mockk<AuthResult>()

        every {
            auth.createUserWithEmailAndPassword(
                "user@test.com",
                "password"
            )
        } returns Tasks.forResult(authResult)

        every {
            authResult.user
        } returns firebaseUser

        coEvery {
            userRepository.createUserIfNotExists(firebaseUser)
        } just Runs

        val result = repository.register(
            email = "   user@test.com   ",
            password = "password"
        )

        assertTrue(result)

        coVerify(exactly = 1) {
            userRepository.createUserIfNotExists(firebaseUser)
        }
    }

    @Test
    fun `register returns true and skips profile creation when result user is null`() =
        runTest {
            val authResult = mockk<AuthResult>()

            every {
                auth.createUserWithEmailAndPassword(
                    "user@test.com",
                    "password"
                )
            } returns Tasks.forResult(authResult)

            every {
                authResult.user
            } returns null

            val result = repository.register(
                email = "user@test.com",
                password = "password"
            )

            assertTrue(result)

            coVerify(exactly = 0) {
                userRepository.createUserIfNotExists(any())
            }
        }

    @Test
    fun `register returns false when Firebase registration fails`() =
        runTest {
            every {
                auth.createUserWithEmailAndPassword(
                    "user@test.com",
                    "password"
                )
            } returns Tasks.forException(
                IllegalStateException("Registration failed")
            )

            val result = repository.register(
                email = "user@test.com",
                password = "password"
            )

            assertFalse(result)

            coVerify(exactly = 0) {
                userRepository.createUserIfNotExists(any())
            }
        }

    @Test
    fun `register returns false when profile creation fails`() = runTest {
        val authResult = mockk<AuthResult>()

        every {
            auth.createUserWithEmailAndPassword(
                "user@test.com",
                "password"
            )
        } returns Tasks.forResult(authResult)

        every {
            authResult.user
        } returns firebaseUser

        coEvery {
            userRepository.createUserIfNotExists(firebaseUser)
        } throws IllegalStateException("Profile creation failed")

        val result = repository.register(
            email = "user@test.com",
            password = "password"
        )

        assertFalse(result)
    }

    // -------------------------------------------------------------------------
    // loginWithGoogle
    // -------------------------------------------------------------------------

    @Test
    fun `loginWithGoogle propagates Credential Manager initialization error`() {
        val context = mockk<Context>()

        mockkObject(CredentialManager.Companion)

        every {
            CredentialManager.create(context)
        } throws IllegalStateException(
            "Credential Manager unavailable"
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.loginWithGoogle(context)
            }
        }

        verify(exactly = 0) {
            auth.signInWithCredential(any())
        }

        coVerify(exactly = 0) {
            userRepository.createUserIfNotExists(any())
        }
    }

    // -------------------------------------------------------------------------
    // reauthenticateWithPassword
    // -------------------------------------------------------------------------

    @Test
    fun `reauthenticateWithPassword throws when user is not logged in`() {
        every { auth.currentUser } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.reauthenticateWithPassword(
                    "password"
                )
            }
        }
    }

    @Test
    fun `reauthenticateWithPassword throws when user has no email`() {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.email } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.reauthenticateWithPassword(
                    "password"
                )
            }
        }

        verify(exactly = 0) {
            firebaseUser.reauthenticate(any())
        }
    }

    @Test
    fun `reauthenticateWithPassword reauthenticates and refreshes token`() =
        runTest {
            val credential = mockk<AuthCredential>()
            val tokenResult = mockk<GetTokenResult>()

            every { auth.currentUser } returns firebaseUser
            every {
                firebaseUser.email
            } returns "user@test.com"

            mockkStatic(EmailAuthProvider::class)

            every {
                EmailAuthProvider.getCredential(
                    "user@test.com",
                    "password"
                )
            } returns credential

            every {
                firebaseUser.reauthenticate(credential)
            } returns Tasks.forResult(null)

            every {
                firebaseUser.getIdToken(true)
            } returns Tasks.forResult(tokenResult)

            repository.reauthenticateWithPassword(
                "password"
            )

            verifyOrder {
                firebaseUser.reauthenticate(credential)
                firebaseUser.getIdToken(true)
            }
        }

    @Test
    fun `reauthenticateWithPassword does not refresh token when reauthentication fails`() {
        val credential = mockk<AuthCredential>()

        every { auth.currentUser } returns firebaseUser
        every {
            firebaseUser.email
        } returns "user@test.com"

        mockkStatic(EmailAuthProvider::class)

        every {
            EmailAuthProvider.getCredential(
                "user@test.com",
                "password"
            )
        } returns credential

        every {
            firebaseUser.reauthenticate(credential)
        } returns Tasks.forException(
            IllegalStateException("Reauthentication failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.reauthenticateWithPassword(
                    "password"
                )
            }
        }

        verify(exactly = 0) {
            firebaseUser.getIdToken(true)
        }
    }

    @Test
    fun `reauthenticateWithPassword does not reauthenticate when credential creation fails`() {
        every { auth.currentUser } returns firebaseUser
        every {
            firebaseUser.email
        } returns "user@test.com"

        mockkStatic(EmailAuthProvider::class)

        every {
            EmailAuthProvider.getCredential(
                "user@test.com",
                "password"
            )
        } throws IllegalArgumentException(
            "Invalid credential"
        )

        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                repository.reauthenticateWithPassword(
                    "password"
                )
            }
        }

        verify(exactly = 0) {
            firebaseUser.reauthenticate(any())
        }
    }

    // -------------------------------------------------------------------------
    // reauthenticateWithGoogle
    // -------------------------------------------------------------------------

    @Test
    fun `reauthenticateWithGoogle throws when user is not logged in`() {
        val context = mockk<Context>()

        every { auth.currentUser } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.reauthenticateWithGoogle(context)
            }
        }

        verify(exactly = 0) {
            firebaseUser.reauthenticate(any())
        }
    }

    @Test
    fun `reauthenticateWithGoogle does not reauthenticate when Credential Manager fails`() {
        val context = mockk<Context>()

        every { auth.currentUser } returns firebaseUser

        mockkObject(CredentialManager.Companion)

        every {
            CredentialManager.create(context)
        } throws IllegalStateException(
            "Credential Manager unavailable"
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.reauthenticateWithGoogle(context)
            }
        }

        verify(exactly = 0) {
            firebaseUser.reauthenticate(any())
        }

        verify(exactly = 0) {
            firebaseUser.getIdToken(true)
        }
    }

    // -------------------------------------------------------------------------
    // deleteAccount
    // -------------------------------------------------------------------------

    @Test
    fun `deleteAccount throws when user is not logged in`() {
        every { auth.currentUser } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.deleteAccount()
            }
        }

        verify(exactly = 0) {
            functions.getHttpsCallable(any())
        }

        verify(exactly = 0) {
            auth.signOut()
        }
    }

    @Test
    fun `deleteAccount refreshes token calls function and signs out`() =
        runTest {
            val tokenResult = mockk<GetTokenResult>()
            val callable = mockk<HttpsCallableReference>()
            val callableResult = mockk<HttpsCallableResult>()

            every { auth.currentUser } returns firebaseUser

            every {
                firebaseUser.getIdToken(true)
            } returns Tasks.forResult(tokenResult)

            every {
                functions.getHttpsCallable("deleteAccount")
            } returns callable

            every {
                callable.call(
                    mapOf(
                        "confirm" to true
                    )
                )
            } returns Tasks.forResult(callableResult)

            every {
                auth.signOut()
            } just Runs

            repository.deleteAccount()

            verifyOrder {
                firebaseUser.getIdToken(true)
                functions.getHttpsCallable("deleteAccount")

                callable.call(
                    mapOf(
                        "confirm" to true
                    )
                )

                auth.signOut()
            }
        }

    @Test
    fun `deleteAccount does not call function when token refresh fails`() {
        every { auth.currentUser } returns firebaseUser

        every {
            firebaseUser.getIdToken(true)
        } returns Tasks.forException(
            IllegalStateException("Token refresh failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.deleteAccount()
            }
        }

        verify(exactly = 0) {
            functions.getHttpsCallable(any())
        }

        verify(exactly = 0) {
            auth.signOut()
        }
    }

    @Test
    fun `deleteAccount does not sign out when cloud function fails`() {
        val tokenResult = mockk<GetTokenResult>()
        val callable = mockk<HttpsCallableReference>()

        every { auth.currentUser } returns firebaseUser

        every {
            firebaseUser.getIdToken(true)
        } returns Tasks.forResult(tokenResult)

        every {
            functions.getHttpsCallable("deleteAccount")
        } returns callable

        every {
            callable.call(
                mapOf(
                    "confirm" to true
                )
            )
        } returns Tasks.forException(
            IllegalStateException("Function failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.deleteAccount()
            }
        }

        verify(exactly = 0) {
            auth.signOut()
        }
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Test
    fun `logout signs out current Firebase session`() {
        every {
            auth.signOut()
        } just Runs

        repository.logout()

        verify(exactly = 1) {
            auth.signOut()
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun providerInfo(
        providerId: String
    ): UserInfo {
        val provider = mockk<UserInfo>()

        every {
            provider.providerId
        } returns providerId

        return provider
    }

    private fun createRepositoryWithoutConstructor(): AuthRepository {
        @Suppress("UNCHECKED_CAST")
        return unsafe.allocateInstance(
            AuthRepository::class.java
        ) as AuthRepository
    }

    private fun setPrivateField(
        target: Any,
        fieldName: String,
        value: Any?
    ) {
        val field = target.javaClass.getDeclaredField(fieldName)

        unsafe.putObject(
            target,
            unsafe.objectFieldOffset(field),
            value
        )
    }

    companion object {
        private val unsafe: Unsafe by lazy {
            val field = Unsafe::class.java.getDeclaredField(
                "theUnsafe"
            )

            field.isAccessible = true
            field.get(null) as Unsafe
        }
    }
}