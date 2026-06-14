package com.example.livegeoguessr.data.repository

import android.net.Uri
import com.example.livegeoguessr.data.remote.firebase.FirebaseModule
import com.example.livegeoguessr.domain.model.UserProfile
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var usersCollection: CollectionReference

    private lateinit var repository: UserRepository

    @BeforeEach
    fun setUp() {
        firestore = mockk()
        storage = mockk()
        usersCollection = mockk()

        /*
         * UserRepository odczytuje zależności z FirebaseModule
         * podczas tworzenia obiektu.
         */
        mockkObject(FirebaseModule)

        every {
            FirebaseModule.firestore
        } returns firestore

        every {
            FirebaseModule.storage
        } returns storage

        every {
            firestore.collection("users")
        } returns usersCollection

        repository = UserRepository()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(FirebaseModule)
    }

    // -------------------------------------------------------------------------
    // createUserIfNotExists
    // -------------------------------------------------------------------------

    @Test
    fun `createUserIfNotExists creates new user using display name as nickname`() =
        runTest {
            val firebaseUser = mockk<FirebaseUser>()
            val userReference = mockk<DocumentReference>()
            val snapshot = mockk<DocumentSnapshot>()
            val photoUri = mockk<Uri>()

            val savedData = slot<Any>()

            every {
                firebaseUser.uid
            } returns "user1"

            every {
                firebaseUser.email
            } returns "user@test.com"

            every {
                firebaseUser.displayName
            } returns "User Name"

            every {
                firebaseUser.photoUrl
            } returns photoUri

            every {
                photoUri.toString()
            } returns "https://example.com/photo.jpg"

            every {
                usersCollection.document("user1")
            } returns userReference

            every {
                userReference.get()
            } returns Tasks.forResult(snapshot)

            every {
                snapshot.exists()
            } returns false

            every {
                userReference.set(capture(savedData))
            } returns Tasks.forResult(null)

            repository.createUserIfNotExists(firebaseUser)

            val data = savedData.captured as Map<*, *>

            assertEquals("user1", data["uid"])
            assertEquals("user@test.com", data["email"])
            assertEquals("User Name", data["displayName"])
            assertEquals("User Name", data["nickname"])
            assertEquals(
                "https://example.com/photo.jpg",
                data["photoUrl"]
            )
            assertNull(data["avatarPath"])
            assertEquals(false, data["isBanned"])

            val stats = data["stats"] as Map<*, *>

            assertEquals(0, stats["pointsTotal"])
            assertEquals(0, stats["guessesCount"])
            assertEquals(0, stats["postsCount"])
            assertEquals(0, stats["friendsCount"])
            assertNull(stats["bestGuessMeters"])

            assertTrue(data.containsKey("createdAt"))
            assertTrue(data.containsKey("updatedAt"))
            assertTrue(data.containsKey("lastLoginAt"))

            verify(exactly = 1) {
                userReference.set(any())
            }

            verify(exactly = 0) {
                userReference.update(any<Map<String, Any>>())
            }
        }

    @Test
    fun `createUserIfNotExists uses email prefix when display name is missing`() =
        runTest {
            val firebaseUser = mockk<FirebaseUser>()
            val userReference = mockk<DocumentReference>()
            val snapshot = mockk<DocumentSnapshot>()

            val savedData = slot<Any>()

            every {
                firebaseUser.uid
            } returns "user2"

            every {
                firebaseUser.email
            } returns "player123@example.com"

            every {
                firebaseUser.displayName
            } returns null

            every {
                firebaseUser.photoUrl
            } returns null

            every {
                usersCollection.document("user2")
            } returns userReference

            every {
                userReference.get()
            } returns Tasks.forResult(snapshot)

            every {
                snapshot.exists()
            } returns false

            every {
                userReference.set(capture(savedData))
            } returns Tasks.forResult(null)

            repository.createUserIfNotExists(firebaseUser)

            val data = savedData.captured as Map<*, *>

            assertEquals("player123@example.com", data["email"])
            assertEquals("", data["displayName"])
            assertEquals("player123", data["nickname"])
            assertNull(data["photoUrl"])
        }

    @Test
    fun `createUserIfNotExists uses Player when email and display name are missing`() =
        runTest {
            val firebaseUser = mockk<FirebaseUser>()
            val userReference = mockk<DocumentReference>()
            val snapshot = mockk<DocumentSnapshot>()

            val savedData = slot<Any>()

            every {
                firebaseUser.uid
            } returns "user3"

            every {
                firebaseUser.email
            } returns null

            every {
                firebaseUser.displayName
            } returns null

            every {
                firebaseUser.photoUrl
            } returns null

            every {
                usersCollection.document("user3")
            } returns userReference

            every {
                userReference.get()
            } returns Tasks.forResult(snapshot)

            every {
                snapshot.exists()
            } returns false

            every {
                userReference.set(capture(savedData))
            } returns Tasks.forResult(null)

            repository.createUserIfNotExists(firebaseUser)

            val data = savedData.captured as Map<*, *>

            assertEquals("", data["email"])
            assertEquals("", data["displayName"])
            assertEquals("Player", data["nickname"])
            assertNull(data["photoUrl"])
        }

    @Test
    fun `createUserIfNotExists updates timestamps when user already exists`() =
        runTest {
            val firebaseUser = mockk<FirebaseUser>()
            val userReference = mockk<DocumentReference>()
            val snapshot = mockk<DocumentSnapshot>()

            val updatedData = slot<Map<String, Any>>()

            every {
                firebaseUser.uid
            } returns "user1"

            every {
                usersCollection.document("user1")
            } returns userReference

            every {
                userReference.get()
            } returns Tasks.forResult(snapshot)

            every {
                snapshot.exists()
            } returns true

            every {
                userReference.update(capture(updatedData))
            } returns Tasks.forResult(null)

            repository.createUserIfNotExists(firebaseUser)

            assertEquals(
                setOf("lastLoginAt", "updatedAt"),
                updatedData.captured.keys
            )

            verify(exactly = 0) {
                userReference.set(any())
            }

            verify(exactly = 1) {
                userReference.update(any<Map<String, Any>>())
            }
        }

    @Test
    fun `createUserIfNotExists propagates document read error`() {
        val firebaseUser = mockk<FirebaseUser>()
        val userReference = mockk<DocumentReference>()

        every {
            firebaseUser.uid
        } returns "user1"

        every {
            usersCollection.document("user1")
        } returns userReference

        every {
            userReference.get()
        } returns Tasks.forException(
            IllegalStateException("Firestore read failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.createUserIfNotExists(firebaseUser)
            }
        }

        verify(exactly = 0) {
            userReference.set(any())
        }

        verify(exactly = 0) {
            userReference.update(any<Map<String, Any>>())
        }
    }

    @Test
    fun `createUserIfNotExists propagates new user save error`() {
        val firebaseUser = mockk<FirebaseUser>()
        val userReference = mockk<DocumentReference>()
        val snapshot = mockk<DocumentSnapshot>()

        every {
            firebaseUser.uid
        } returns "user1"

        every {
            firebaseUser.email
        } returns "user@test.com"

        every {
            firebaseUser.displayName
        } returns "User"

        every {
            firebaseUser.photoUrl
        } returns null

        every {
            usersCollection.document("user1")
        } returns userReference

        every {
            userReference.get()
        } returns Tasks.forResult(snapshot)

        every {
            snapshot.exists()
        } returns false

        every {
            userReference.set(any())
        } returns Tasks.forException(
            IllegalStateException("Firestore save failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.createUserIfNotExists(firebaseUser)
            }
        }
    }

    @Test
    fun `createUserIfNotExists propagates existing user update error`() {
        val firebaseUser = mockk<FirebaseUser>()
        val userReference = mockk<DocumentReference>()
        val snapshot = mockk<DocumentSnapshot>()

        every {
            firebaseUser.uid
        } returns "user1"

        every {
            usersCollection.document("user1")
        } returns userReference

        every {
            userReference.get()
        } returns Tasks.forResult(snapshot)

        every {
            snapshot.exists()
        } returns true

        every {
            userReference.update(any<Map<String, Any>>())
        } returns Tasks.forException(
            IllegalStateException("Firestore update failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.createUserIfNotExists(firebaseUser)
            }
        }
    }

    // -------------------------------------------------------------------------
    // getUserProfile
    // -------------------------------------------------------------------------

    @Test
    fun `getUserProfile returns mapped profile`() = runTest {
        val userReference = mockk<DocumentReference>()
        val snapshot = mockk<DocumentSnapshot>()
        val profile = mockk<UserProfile>()

        every {
            usersCollection.document("user1")
        } returns userReference

        every {
            userReference.get()
        } returns Tasks.forResult(snapshot)

        every {
            snapshot.toObject(UserProfile::class.java)
        } returns profile

        val result = repository.getUserProfile("user1")

        assertEquals(profile, result)
    }

    @Test
    fun `getUserProfile returns null when document cannot be mapped`() =
        runTest {
            val userReference = mockk<DocumentReference>()
            val snapshot = mockk<DocumentSnapshot>()

            every {
                usersCollection.document("missing-user")
            } returns userReference

            every {
                userReference.get()
            } returns Tasks.forResult(snapshot)

            every {
                snapshot.toObject(UserProfile::class.java)
            } returns null

            val result = repository.getUserProfile(
                "missing-user"
            )

            assertNull(result)
        }

    @Test
    fun `getUserProfile propagates Firestore error`() {
        val userReference = mockk<DocumentReference>()

        every {
            usersCollection.document("user1")
        } returns userReference

        every {
            userReference.get()
        } returns Tasks.forException(
            IllegalStateException("Firestore failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.getUserProfile("user1")
            }
        }
    }

    // -------------------------------------------------------------------------
    // updateProfilePicture
    // -------------------------------------------------------------------------

    @Test
    fun `updateProfilePicture uploads file updates user and returns download url`() =
        runTest {
            val imageUri = mockk<Uri>()
            val downloadUri = mockk<Uri>()

            val storageRoot = mockk<StorageReference>()
            val avatarReference = mockk<StorageReference>()
            val userReference = mockk<DocumentReference>()

            val avatarPath = slot<String>()
            val updateData = slot<Map<String, Any>>()

            every {
                storage.reference
            } returns storageRoot

            every {
                storageRoot.child(capture(avatarPath))
            } returns avatarReference

            every {
                avatarReference.putFile(imageUri)
            } returns successfulUploadTask()

            every {
                downloadUri.toString()
            } returns "https://example.com/avatar.jpg"

            every {
                avatarReference.downloadUrl
            } returns Tasks.forResult(downloadUri)

            every {
                usersCollection.document("user1")
            } returns userReference

            every {
                userReference.update(capture(updateData))
            } returns Tasks.forResult(null)

            val result = repository.updateProfilePicture(
                uid = "user1",
                imageUri = imageUri
            )

            assertEquals(
                "https://example.com/avatar.jpg",
                result
            )

            assertTrue(
                avatarPath.captured.startsWith(
                    "avatars/user1/profile_"
                )
            )

            assertTrue(
                avatarPath.captured.endsWith(".jpg")
            )

            assertEquals(
                "https://example.com/avatar.jpg",
                updateData.captured["photoUrl"]
            )

            assertEquals(
                avatarPath.captured,
                updateData.captured["avatarPath"]
            )

            assertTrue(
                updateData.captured.containsKey("updatedAt")
            )

            verify(exactly = 1) {
                avatarReference.putFile(imageUri)
            }

            verify(exactly = 1) {
                avatarReference.downloadUrl
            }

            verify(exactly = 1) {
                userReference.update(any<Map<String, Any>>())
            }
        }

    @Test
    fun `updateProfilePicture stops when upload fails`() {
        val imageUri = mockk<Uri>()

        val storageRoot = mockk<StorageReference>()
        val avatarReference = mockk<StorageReference>()

        every {
            storage.reference
        } returns storageRoot

        every {
            storageRoot.child(any())
        } returns avatarReference

        every {
            avatarReference.putFile(imageUri)
        } returns failedUploadTask(
            IllegalStateException("Upload failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.updateProfilePicture(
                    uid = "user1",
                    imageUri = imageUri
                )
            }
        }

        verify(exactly = 0) {
            avatarReference.downloadUrl
        }

        verify(exactly = 0) {
            usersCollection.document(any())
        }
    }

    @Test
    fun `updateProfilePicture stops when download url retrieval fails`() {
        val imageUri = mockk<Uri>()

        val storageRoot = mockk<StorageReference>()
        val avatarReference = mockk<StorageReference>()

        every {
            storage.reference
        } returns storageRoot

        every {
            storageRoot.child(any())
        } returns avatarReference

        every {
            avatarReference.putFile(imageUri)
        } returns successfulUploadTask()

        every {
            avatarReference.downloadUrl
        } returns Tasks.forException(
            IllegalStateException("Download URL failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.updateProfilePicture(
                    uid = "user1",
                    imageUri = imageUri
                )
            }
        }

        verify(exactly = 0) {
            usersCollection.document(any())
        }
    }

    @Test
    fun `updateProfilePicture propagates user document update error`() {
        val imageUri = mockk<Uri>()
        val downloadUri = mockk<Uri>()

        val storageRoot = mockk<StorageReference>()
        val avatarReference = mockk<StorageReference>()
        val userReference = mockk<DocumentReference>()

        every {
            storage.reference
        } returns storageRoot

        every {
            storageRoot.child(any())
        } returns avatarReference

        every {
            avatarReference.putFile(imageUri)
        } returns successfulUploadTask()

        every {
            downloadUri.toString()
        } returns "https://example.com/avatar.jpg"

        every {
            avatarReference.downloadUrl
        } returns Tasks.forResult(downloadUri)

        every {
            usersCollection.document("user1")
        } returns userReference

        every {
            userReference.update(any<Map<String, Any>>())
        } returns Tasks.forException(
            IllegalStateException("Profile update failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.updateProfilePicture(
                    uid = "user1",
                    imageUri = imageUri
                )
            }
        }

        verify(exactly = 1) {
            avatarReference.putFile(imageUri)
        }

        verify(exactly = 1) {
            avatarReference.downloadUrl
        }

        verify(exactly = 1) {
            userReference.update(any<Map<String, Any>>())
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun successfulUploadTask(): UploadTask {
        val uploadTask = mockk<UploadTask>()
        val snapshot = mockk<UploadTask.TaskSnapshot>()

        every {
            uploadTask.isComplete
        } returns true

        every {
            uploadTask.isSuccessful
        } returns true

        every {
            uploadTask.isCanceled
        } returns false

        every {
            uploadTask.exception
        } returns null

        every {
            uploadTask.result
        } returns snapshot

        return uploadTask
    }

    private fun failedUploadTask(
        exception: Exception
    ): UploadTask {
        val uploadTask = mockk<UploadTask>()

        every {
            uploadTask.isComplete
        } returns true

        every {
            uploadTask.isSuccessful
        } returns false

        every {
            uploadTask.isCanceled
        } returns false

        every {
            uploadTask.exception
        } returns exception

        return uploadTask
    }
}