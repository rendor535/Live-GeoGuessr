package com.example.livegeoguessr.data.repository

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.livegeoguessr.domain.model.Post
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.OutputStream
import java.util.Date

class PostRepositoryTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var currentUser: FirebaseUser
    private lateinit var repository: PostRepository

    @BeforeEach
    fun setUp() {
        auth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        currentUser = mockk(relaxed = true)

        mockkStatic(Log::class)

        every {
            Log.d(any<String>(), any<String>())
        } returns 0

        every {
            Log.e(any<String>(), any<String>())
        } returns 0

        every {
            Log.e(
                any<String>(),
                any<String>(),
                any<Throwable>()
            )
        } returns 0

        every { currentUser.uid } returns "user1"
        every { currentUser.displayName } returns "Current User"
        every { currentUser.email } returns "user@test.com"
        every { currentUser.photoUrl } returns null
        every { auth.currentUser } returns currentUser

        repository = PostRepository(
            auth = auth,
            firestore = firestore,
            storage = storage
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // -------------------------------------------------------------------------
    // getMyPosts
    // -------------------------------------------------------------------------

    @Test
    fun `getMyPosts returns posts and uses author nickname`() = runTest {
        val postsCollection = mockk<CollectionReference>()
        val postsQuery = mockk<Query>()
        val postsSnapshot = mockk<QuerySnapshot>()

        val usersCollection = mockk<CollectionReference>()
        val usersQuery = mockk<Query>()
        val usersSnapshot = mockk<QuerySnapshot>()

        val postDocument = postDocument(
            id = "post1",
            userId = "user1",
            user = "Old name",
            imageUrl = "https://example.com/post1.jpg",
            latitude = 50.0,
            longitude = 21.0,
            authorPhotoUrl = "old-photo"
        )

        val userDocument = userDocument(
            id = "user1",
            nickname = "Nickname",
            displayName = "Display name",
            photoUrl = "https://example.com/profile.jpg"
        )

        every {
            firestore.collection("posts")
        } returns postsCollection

        every {
            firestore.collection("users")
        } returns usersCollection

        every {
            postsCollection.whereEqualTo("userId", "user1")
        } returns postsQuery

        every {
            postsQuery.orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
        } returns postsQuery

        every {
            postsQuery.get()
        } returns Tasks.forResult(postsSnapshot)

        every {
            postsSnapshot.documents
        } returns listOf(postDocument)

        every {
            usersCollection.whereIn(
                any<FieldPath>(),
                listOf("user1")
            )
        } returns usersQuery

        every {
            usersQuery.get()
        } returns Tasks.forResult(usersSnapshot)

        every {
            usersSnapshot.documents
        } returns listOf(userDocument)

        val result = repository.getMyPosts()

        assertEquals(1, result.size)
        assertEquals("post1", result[0].id)
        assertEquals("user1", result[0].authorUid)
        assertEquals("Nickname", result[0].user)
        assertEquals(
            "https://example.com/profile.jpg",
            result[0].authorPhotoUrl
        )
    }

    @Test
    fun `getMyPosts uses display name when nickname is blank`() = runTest {
        val postsCollection = mockk<CollectionReference>()
        val postsQuery = mockk<Query>()
        val postsSnapshot = mockk<QuerySnapshot>()

        val usersCollection = mockk<CollectionReference>()
        val usersQuery = mockk<Query>()
        val usersSnapshot = mockk<QuerySnapshot>()

        val postDocument = postDocument(
            id = "post1",
            userId = "user1",
            user = "Old name",
            imageUrl = "image",
            latitude = 10.0,
            longitude = 20.0,
            authorPhotoUrl = "stored-photo"
        )

        val userDocument = userDocument(
            id = "user1",
            nickname = " ",
            displayName = "Display name",
            photoUrl = null
        )

        every { firestore.collection("posts") } returns postsCollection
        every { firestore.collection("users") } returns usersCollection

        every {
            postsCollection.whereEqualTo("userId", "user1")
        } returns postsQuery

        every {
            postsQuery.orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
        } returns postsQuery

        every {
            postsQuery.get()
        } returns Tasks.forResult(postsSnapshot)

        every {
            postsSnapshot.documents
        } returns listOf(postDocument)

        every {
            usersCollection.whereIn(
                any<FieldPath>(),
                listOf("user1")
            )
        } returns usersQuery

        every {
            usersQuery.get()
        } returns Tasks.forResult(usersSnapshot)

        every {
            usersSnapshot.documents
        } returns listOf(userDocument)

        val result = repository.getMyPosts()

        assertEquals("Display name", result.single().user)
        assertEquals("stored-photo", result.single().authorPhotoUrl)
    }

    @Test
    fun `getMyPosts leaves original author when author uid is blank`() = runTest {
        val postsCollection = mockk<CollectionReference>()
        val postsQuery = mockk<Query>()
        val postsSnapshot = mockk<QuerySnapshot>()

        val validDocument = postDocument(
            id = "post1",
            userId = null,
            user = "Original author",
            imageUrl = "image",
            latitude = 10.0,
            longitude = 20.0,
            authorPhotoUrl = "original-photo"
        )

        val invalidDocument = postDocument(
            id = "invalid",
            userId = "user1",
            user = "Invalid",
            imageUrl = null,
            latitude = 10.0,
            longitude = 20.0
        )

        every { firestore.collection("posts") } returns postsCollection

        every {
            postsCollection.whereEqualTo("userId", "user1")
        } returns postsQuery

        every {
            postsQuery.orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
        } returns postsQuery

        every {
            postsQuery.get()
        } returns Tasks.forResult(postsSnapshot)

        every {
            postsSnapshot.documents
        } returns listOf(validDocument, invalidDocument)

        val result = repository.getMyPosts()

        assertEquals(1, result.size)
        assertEquals("", result[0].authorUid)
        assertEquals("Original author", result[0].user)
        assertEquals("original-photo", result[0].authorPhotoUrl)
    }

    @Test
    fun `getMyPosts returns empty list when no posts exist`() = runTest {
        val postsCollection = mockk<CollectionReference>()
        val postsQuery = mockk<Query>()
        val postsSnapshot = mockk<QuerySnapshot>()

        every { firestore.collection("posts") } returns postsCollection

        every {
            postsCollection.whereEqualTo("userId", "user1")
        } returns postsQuery

        every {
            postsQuery.orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
        } returns postsQuery

        every {
            postsQuery.get()
        } returns Tasks.forResult(postsSnapshot)

        every {
            postsSnapshot.documents
        } returns emptyList()

        val result = repository.getMyPosts()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMyPosts throws when user is not logged in`() {
        every { auth.currentUser } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.getMyPosts()
            }
        }
    }

    // -------------------------------------------------------------------------
    // getPosts
    // -------------------------------------------------------------------------

    @Test
    fun `getPosts filters guessed and invalid posts sorts posts and applies profile fallbacks`() =
        runTest {
            val usersCollection = mockk<CollectionReference>()
            val currentUserDocument = mockk<DocumentReference>()
            val friendsCollection = mockk<CollectionReference>()
            val friendsSnapshot = mockk<QuerySnapshot>()

            val guessesCollection = mockk<CollectionReference>()
            val guessesQuery = mockk<Query>()
            val guessesSnapshot = mockk<QuerySnapshot>()

            val postsCollection = mockk<CollectionReference>()
            val postsQuery = mockk<Query>()
            val postsSnapshot = mockk<QuerySnapshot>()

            val profilesQuery = mockk<Query>()
            val profilesSnapshot = mockk<QuerySnapshot>()

            val friendDocuments = listOf(
                idDocument("friend1"),
                idDocument("friend2"),
                idDocument("friend3"),
                idDocument("friend4")
            )

            val guessedDocument = guessDocument(
                postId = "guessed-post",
                distanceMeters = 10.0,
                points = 10L,
                createdAtSeconds = 10L
            )

            val guessWithoutPostId = guessDocument(
                postId = null,
                distanceMeters = null,
                points = null,
                createdAtSeconds = null
            )

            val newestPost = postDocument(
                id = "newest",
                userId = "friend1",
                user = "Original one",
                imageUrl = "newest-image",
                latitude = 1.0,
                longitude = 1.0,
                authorPhotoUrl = "old-photo-1",
                createdAtSeconds = 300L
            )

            val olderPost = postDocument(
                id = "older",
                userId = "friend2",
                user = "Original two",
                imageUrl = "older-image",
                latitude = 2.0,
                longitude = 2.0,
                authorPhotoUrl = "stored-photo-2",
                createdAtSeconds = 200L
            )

            val noTimestampPost = postDocument(
                id = "no-time",
                userId = "friend3",
                user = "Original three",
                imageUrl = "no-time-image",
                latitude = 3.0,
                longitude = 3.0,
                authorPhotoUrl = "stored-photo-3",
                createdAtSeconds = null
            )

            val missingProfilePost = postDocument(
                id = "missing-profile",
                userId = "friend4",
                user = "Original four",
                imageUrl = "missing-profile-image",
                latitude = 4.0,
                longitude = 4.0,
                authorPhotoUrl = "stored-photo-4",
                createdAtSeconds = 100L
            )

            val alreadyGuessedPost = postDocument(
                id = "guessed-post",
                userId = "friend1",
                user = "Guessed",
                imageUrl = "guessed-image",
                latitude = 5.0,
                longitude = 5.0,
                createdAtSeconds = 400L
            )

            val invalidPost = postDocument(
                id = "invalid-post",
                userId = "friend1",
                user = "Invalid",
                imageUrl = null,
                latitude = 6.0,
                longitude = 6.0,
                createdAtSeconds = 500L
            )

            val friend1Profile = userDocument(
                id = "friend1",
                nickname = "Nickname one",
                displayName = "Display one",
                photoUrl = "profile-photo-1"
            )

            val friend2Profile = userDocument(
                id = "friend2",
                nickname = "",
                displayName = "Display two",
                photoUrl = null
            )

            val friend3Profile = userDocument(
                id = "friend3",
                nickname = "",
                displayName = "",
                photoUrl = "profile-photo-3"
            )

            every { firestore.collection("users") } returns usersCollection
            every { firestore.collection("guesses") } returns guessesCollection
            every { firestore.collection("posts") } returns postsCollection

            every {
                usersCollection.document("user1")
            } returns currentUserDocument

            every {
                currentUserDocument.collection("friends")
            } returns friendsCollection

            every {
                friendsCollection.get()
            } returns Tasks.forResult(friendsSnapshot)

            every {
                friendsSnapshot.documents
            } returns friendDocuments

            every {
                guessesCollection.whereEqualTo("userUid", "user1")
            } returns guessesQuery

            every {
                guessesQuery.get()
            } returns Tasks.forResult(guessesSnapshot)

            every {
                guessesSnapshot.documents
            } returns listOf(
                guessedDocument,
                guessWithoutPostId
            )

            every {
                postsCollection.whereIn(
                    "userId",
                    listOf(
                        "friend1",
                        "friend2",
                        "friend3",
                        "friend4"
                    )
                )
            } returns postsQuery

            every {
                postsQuery.get()
            } returns Tasks.forResult(postsSnapshot)

            every {
                postsSnapshot.size()
            } returns 6

            every {
                postsSnapshot.documents
            } returns listOf(
                newestPost,
                olderPost,
                noTimestampPost,
                missingProfilePost,
                alreadyGuessedPost,
                invalidPost
            )

            every {
                usersCollection.whereIn(
                    any<FieldPath>(),
                    listOf(
                        "friend1",
                        "friend2",
                        "friend4",
                        "friend3"
                    )
                )
            } returns profilesQuery

            every {
                profilesQuery.get()
            } returns Tasks.forResult(profilesSnapshot)

            every {
                profilesSnapshot.documents
            } returns listOf(
                friend1Profile,
                friend2Profile,
                friend3Profile
            )

            val result = repository.getPosts()

            assertEquals(4, result.size)

            assertEquals(
                listOf(
                    "newest",
                    "older",
                    "missing-profile",
                    "no-time"
                ),
                result.map { it.id }
            )

            // nickname ma pierwszeństwo
            assertEquals("Nickname one", result[0].user)
            assertEquals("profile-photo-1", result[0].authorPhotoUrl)

            // pusty nickname -> displayName
            assertEquals("Display two", result[1].user)

            // brak profilu -> dane zapisane w poście
            assertEquals("Original four", result[2].user)
            assertEquals("stored-photo-4", result[2].authorPhotoUrl)

            // pusty nickname i displayName -> nazwa z posta
            assertEquals("Original three", result[3].user)
            assertEquals("profile-photo-3", result[3].authorPhotoUrl)
        }

    @Test
    fun `getPosts returns empty list when user has no friends`() = runTest {
        val usersCollection = mockk<CollectionReference>()
        val currentUserDocument = mockk<DocumentReference>()
        val friendsCollection = mockk<CollectionReference>()
        val friendsSnapshot = mockk<QuerySnapshot>()

        val guessesCollection = mockk<CollectionReference>()
        val guessesQuery = mockk<Query>()
        val guessesSnapshot = mockk<QuerySnapshot>()

        every { firestore.collection("users") } returns usersCollection
        every { firestore.collection("guesses") } returns guessesCollection

        every {
            usersCollection.document("user1")
        } returns currentUserDocument

        every {
            currentUserDocument.collection("friends")
        } returns friendsCollection

        every {
            friendsCollection.get()
        } returns Tasks.forResult(friendsSnapshot)

        every {
            friendsSnapshot.documents
        } returns emptyList()

        every {
            guessesCollection.whereEqualTo("userUid", "user1")
        } returns guessesQuery

        every {
            guessesQuery.get()
        } returns Tasks.forResult(guessesSnapshot)

        every {
            guessesSnapshot.documents
        } returns emptyList()

        val result = repository.getPosts()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPosts splits more than thirty friends into chunks`() = runTest {
        val usersCollection = mockk<CollectionReference>()
        val currentUserDocument = mockk<DocumentReference>()
        val friendsCollection = mockk<CollectionReference>()
        val friendsSnapshot = mockk<QuerySnapshot>()

        val guessesCollection = mockk<CollectionReference>()
        val guessesQuery = mockk<Query>()
        val guessesSnapshot = mockk<QuerySnapshot>()

        val postsCollection = mockk<CollectionReference>()
        val firstPostsQuery = mockk<Query>()
        val secondPostsQuery = mockk<Query>()
        val firstPostsSnapshot = mockk<QuerySnapshot>()
        val secondPostsSnapshot = mockk<QuerySnapshot>()

        val friendIds = (1..31).map { "friend$it" }

        val friendDocuments = friendIds.map { id ->
            idDocument(id)
        }

        val firstChunk = friendIds.take(30)
        val secondChunk = friendIds.drop(30)

        every { firestore.collection("users") } returns usersCollection
        every { firestore.collection("guesses") } returns guessesCollection
        every { firestore.collection("posts") } returns postsCollection

        every {
            usersCollection.document("user1")
        } returns currentUserDocument

        every {
            currentUserDocument.collection("friends")
        } returns friendsCollection

        every {
            friendsCollection.get()
        } returns Tasks.forResult(friendsSnapshot)

        every {
            friendsSnapshot.documents
        } returns friendDocuments

        every {
            guessesCollection.whereEqualTo("userUid", "user1")
        } returns guessesQuery

        every {
            guessesQuery.get()
        } returns Tasks.forResult(guessesSnapshot)

        every {
            guessesSnapshot.documents
        } returns emptyList()

        every {
            postsCollection.whereIn("userId", firstChunk)
        } returns firstPostsQuery

        every {
            postsCollection.whereIn("userId", secondChunk)
        } returns secondPostsQuery

        every {
            firstPostsQuery.get()
        } returns Tasks.forResult(firstPostsSnapshot)

        every {
            secondPostsQuery.get()
        } returns Tasks.forResult(secondPostsSnapshot)

        every { firstPostsSnapshot.size() } returns 0
        every { secondPostsSnapshot.size() } returns 0

        every {
            firstPostsSnapshot.documents
        } returns emptyList()

        every {
            secondPostsSnapshot.documents
        } returns emptyList()

        val result = repository.getPosts()

        assertTrue(result.isEmpty())

        verify(exactly = 1) {
            postsCollection.whereIn("userId", firstChunk)
        }

        verify(exactly = 1) {
            postsCollection.whereIn("userId", secondChunk)
        }
    }

    @Test
    fun `getPosts throws when user is not logged in`() {
        every { auth.currentUser } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.getPosts()
            }
        }
    }

    // -------------------------------------------------------------------------
    // getMyGuessedPosts
    // -------------------------------------------------------------------------

    @Test
    fun `getMyGuessedPosts handles missing values duplicates invalid posts and missing posts`() =
        runTest {
            val guessesCollection = mockk<CollectionReference>()
            val guessesQuery = mockk<Query>()
            val guessesSnapshot = mockk<QuerySnapshot>()

            val postsCollection = mockk<CollectionReference>()

            val post1Reference = mockk<DocumentReference>()
            val missingPostReference = mockk<DocumentReference>()
            val brokenPostReference = mockk<DocumentReference>()

            val usersCollection = mockk<CollectionReference>()
            val usersQuery = mockk<Query>()
            val usersSnapshot = mockk<QuerySnapshot>()

            val missingPostIdGuess = guessDocument(
                postId = null,
                distanceMeters = 100.0,
                points = 100L,
                createdAtSeconds = 400L
            )

            val defaultValuesGuess = guessDocument(
                postId = "post1",
                distanceMeters = null,
                points = null,
                createdAtSeconds = 300L
            )

            val missingPostGuess = guessDocument(
                postId = "missing",
                distanceMeters = 50.0,
                points = 20L,
                createdAtSeconds = 200L
            )

            val invalidPostGuess = guessDocument(
                postId = "broken",
                distanceMeters = 25.0,
                points = 10L,
                createdAtSeconds = 100L
            )

            val duplicateGuess = guessDocument(
                postId = "post1",
                distanceMeters = 5.0,
                points = 2L,
                createdAtSeconds = null
            )

            val validPost = postDocument(
                id = "post1",
                userId = "friend1",
                user = "Original",
                imageUrl = "post-image",
                latitude = 10.0,
                longitude = 20.0
            )

            val missingPost = mockk<DocumentSnapshot>()

            val invalidPost = postDocument(
                id = "broken",
                userId = "friend2",
                user = "Broken",
                imageUrl = null,
                latitude = 10.0,
                longitude = 20.0
            )

            val userProfile = userDocument(
                id = "friend1",
                nickname = "",
                displayName = "Friend display",
                photoUrl = null
            )

            every {
                firestore.collection("guesses")
            } returns guessesCollection

            every {
                firestore.collection("posts")
            } returns postsCollection

            every {
                firestore.collection("users")
            } returns usersCollection

            every {
                guessesCollection.whereEqualTo("userUid", "user1")
            } returns guessesQuery

            every {
                guessesQuery.get()
            } returns Tasks.forResult(guessesSnapshot)

            every {
                guessesSnapshot.documents
            } returns listOf(
                missingPostIdGuess,
                defaultValuesGuess,
                missingPostGuess,
                invalidPostGuess,
                duplicateGuess
            )

            every {
                postsCollection.document("post1")
            } returns post1Reference

            every {
                postsCollection.document("missing")
            } returns missingPostReference

            every {
                postsCollection.document("broken")
            } returns brokenPostReference

            every {
                post1Reference.get()
            } returns Tasks.forResult(validPost)

            every {
                missingPostReference.get()
            } returns Tasks.forResult(missingPost)

            every {
                brokenPostReference.get()
            } returns Tasks.forResult(invalidPost)

            every {
                validPost.exists()
            } returns true

            every {
                missingPost.exists()
            } returns false

            every {
                invalidPost.exists()
            } returns true

            every {
                usersCollection.whereIn(
                    any<FieldPath>(),
                    listOf("friend1")
                )
            } returns usersQuery

            every {
                usersQuery.get()
            } returns Tasks.forResult(usersSnapshot)

            every {
                usersSnapshot.documents
            } returns listOf(userProfile)

            val result = repository.getMyGuessedPosts()

            assertEquals(2, result.size)

            assertEquals("post1", result[0].post.id)
            assertEquals("Friend display", result[0].post.user)
            assertEquals(0.0, result[0].distanceMeters)
            assertEquals(0, result[0].points)

            assertEquals("post1", result[1].post.id)
            assertEquals(5.0, result[1].distanceMeters)
            assertEquals(2, result[1].points)

            verify(exactly = 1) {
                postsCollection.document("post1")
            }

            verify(exactly = 1) {
                postsCollection.document("missing")
            }

            verify(exactly = 1) {
                postsCollection.document("broken")
            }
        }

    @Test
    fun `getMyGuessedPosts returns empty list when user has no guesses`() =
        runTest {
            val guessesCollection = mockk<CollectionReference>()
            val guessesQuery = mockk<Query>()
            val guessesSnapshot = mockk<QuerySnapshot>()

            every {
                firestore.collection("guesses")
            } returns guessesCollection

            every {
                guessesCollection.whereEqualTo("userUid", "user1")
            } returns guessesQuery

            every {
                guessesQuery.get()
            } returns Tasks.forResult(guessesSnapshot)

            every {
                guessesSnapshot.documents
            } returns emptyList()

            val result = repository.getMyGuessedPosts()

            assertTrue(result.isEmpty())
        }

    @Test
    fun `getMyGuessedPosts throws when user is not logged in`() {
        every { auth.currentUser } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.getMyGuessedPosts()
            }
        }
    }

    // -------------------------------------------------------------------------
    // addPost
    // -------------------------------------------------------------------------

    @Test
    fun `addPost throws when user is not logged in`() {
        every { auth.currentUser } returns null

        val bitmap = mockk<Bitmap>()

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.addPost(
                    bitmap = bitmap,
                    latitude = 50.0,
                    longitude = 21.0
                )
            }
        }
    }

    @Test
    fun `addPost covers all username and photo fallbacks`() = runTest {
        val nicknameResult = executeAddPostScenario(
            postId = "post-nickname",
            nickname = "Nickname",
            firestoreDisplayName = "Firestore display",
            authDisplayName = "Auth display",
            email = "user@test.com",
            firestorePhotoUrl = "firestore-photo",
            authPhotoUrl = "auth-photo"
        )

        assertEquals("Nickname", nicknameResult.user)
        assertEquals("firestore-photo", nicknameResult.authorPhotoUrl)

        val firestoreDisplayResult = executeAddPostScenario(
            postId = "post-firestore-display",
            nickname = " ",
            firestoreDisplayName = "Firestore display",
            authDisplayName = "Auth display",
            email = "user@test.com",
            firestorePhotoUrl = null,
            authPhotoUrl = "auth-photo"
        )

        assertEquals(
            "Firestore display",
            firestoreDisplayResult.user
        )
        assertEquals(
            "auth-photo",
            firestoreDisplayResult.authorPhotoUrl
        )

        val authDisplayResult = executeAddPostScenario(
            postId = "post-auth-display",
            nickname = null,
            firestoreDisplayName = "",
            authDisplayName = "Auth display",
            email = "user@test.com",
            firestorePhotoUrl = null,
            authPhotoUrl = null
        )

        assertEquals("Auth display", authDisplayResult.user)
        assertEquals(null, authDisplayResult.authorPhotoUrl)

        val emailResult = executeAddPostScenario(
            postId = "post-email",
            nickname = null,
            firestoreDisplayName = null,
            authDisplayName = null,
            email = "fallback@test.com",
            firestorePhotoUrl = null,
            authPhotoUrl = null
        )

        assertEquals("fallback@test.com", emailResult.user)

        val unknownResult = executeAddPostScenario(
            postId = "post-unknown",
            nickname = null,
            firestoreDisplayName = null,
            authDisplayName = null,
            email = null,
            firestorePhotoUrl = null,
            authPhotoUrl = null
        )

        assertEquals("Unknown user", unknownResult.user)
    }

    // -------------------------------------------------------------------------
    // deleteMyPost
    // -------------------------------------------------------------------------

    @Test
    fun `deleteMyPost throws when user is not logged in`() {
        every { auth.currentUser } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.deleteMyPost("post1")
            }
        }
    }

    @Test
    fun `deleteMyPost throws when post does not exist`() {
        prepareDeleteFixture(
            postId = "post1",
            postExists = false,
            ownerUid = null
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.deleteMyPost("post1")
            }
        }
    }

    @Test
    fun `deleteMyPost throws when post belongs to another user`() {
        prepareDeleteFixture(
            postId = "post1",
            postExists = true,
            ownerUid = "another-user"
        )

        assertThrows(SecurityException::class.java) {
            runTest {
                repository.deleteMyPost("post1")
            }
        }
    }

    @Test
    fun `deleteMyPost deletes own post updates counter and deletes image`() =
        runTest {
            val fixture = prepareDeleteFixture(
                postId = "post1",
                postExists = true,
                ownerUid = "user1"
            )

            every {
                fixture.imageReference.delete()
            } returns Tasks.forResult(null)

            repository.deleteMyPost("post1")

            verify(exactly = 1) {
                fixture.transaction.delete(fixture.postReference)
            }

            verify(exactly = 1) {
                fixture.transaction.update(
                    fixture.userReference,
                    any<Map<String, Any>>()
                )
            }

            verify(exactly = 1) {
                fixture.imageReference.delete()
            }
        }

    @Test
    fun `deleteMyPost ignores storage object not found error`() = runTest {
        val fixture = prepareDeleteFixture(
            postId = "post1",
            postExists = true,
            ownerUid = "user1"
        )

        val storageException = mockk<StorageException>()

        every {
            storageException.errorCode
        } returns StorageException.ERROR_OBJECT_NOT_FOUND

        every {
            fixture.imageReference.delete()
        } returns Tasks.forException(storageException)

        // Test przejdzie, jeżeli wyjątek nie zostanie przekazany dalej.
        repository.deleteMyPost("post1")

        verify(exactly = 1) {
            fixture.transaction.delete(fixture.postReference)
        }
    }

    @Test
    fun `deleteMyPost logs other storage errors but does not throw`() = runTest {
        val fixture = prepareDeleteFixture(
            postId = "post1",
            postExists = true,
            ownerUid = "user1"
        )

        val storageException = mockk<StorageException>()

        every {
            storageException.errorCode
        } returns StorageException.ERROR_UNKNOWN

        every {
            fixture.imageReference.delete()
        } returns Tasks.forException(storageException)

        repository.deleteMyPost("post1")

        verify(exactly = 1) {
            Log.e(
                "PostRepositoryDebug",
                "Post deleted, but image deletion failed",
                storageException
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private suspend fun executeAddPostScenario(
        postId: String,
        nickname: String?,
        firestoreDisplayName: String?,
        authDisplayName: String?,
        email: String?,
        firestorePhotoUrl: String?,
        authPhotoUrl: String?
    ): Post {
        val localAuth = mockk<FirebaseAuth>()
        val localFirestore = mockk<FirebaseFirestore>()
        val localStorage = mockk<FirebaseStorage>()
        val localUser = mockk<FirebaseUser>()

        val usersCollection = mockk<CollectionReference>()
        val userReference = mockk<DocumentReference>()
        val userSnapshot = mockk<DocumentSnapshot>()

        val postsCollection = mockk<CollectionReference>()
        val postReference = mockk<DocumentReference>()

        val storageRoot = mockk<StorageReference>()
        val imageReference = mockk<StorageReference>()

        val uploadTask = mockk<UploadTask>()
        val uploadSnapshot = mockk<UploadTask.TaskSnapshot>()

        val downloadUri = mockk<Uri>()
        val bitmap = mockk<Bitmap>()

        val authPhotoUri = authPhotoUrl?.let {
            mockk<Uri>().also { uri ->
                every { uri.toString() } returns it
            }
        }

        every { localAuth.currentUser } returns localUser
        every { localUser.uid } returns "user1"
        every { localUser.displayName } returns authDisplayName
        every { localUser.email } returns email
        every { localUser.photoUrl } returns authPhotoUri

        every {
            localFirestore.collection("users")
        } returns usersCollection

        every {
            localFirestore.collection("posts")
        } returns postsCollection

        every {
            usersCollection.document("user1")
        } returns userReference

        every {
            userReference.get()
        } returns Tasks.forResult(userSnapshot)

        every {
            userSnapshot.getString("nickname")
        } returns nickname

        every {
            userSnapshot.getString("displayName")
        } returns firestoreDisplayName

        every {
            userSnapshot.getString("photoUrl")
        } returns firestorePhotoUrl

        every {
            postsCollection.document()
        } returns postReference

        every {
            postReference.id
        } returns postId

        every {
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                85,
                any<OutputStream>()
            )
        } answers {
            thirdArg<OutputStream>().write(
                byteArrayOf(1, 2, 3)
            )
            true
        }

        every {
            localStorage.reference
        } returns storageRoot

        every {
            storageRoot.child(
                "posts/user1/$postId.jpg"
            )
        } returns imageReference

        every {
            imageReference.putBytes(any<ByteArray>())
        } returns uploadTask

        every { uploadTask.isComplete } returns true
        every { uploadTask.isSuccessful } returns true
        every { uploadTask.isCanceled } returns false
        every { uploadTask.exception } returns null
        every { uploadTask.result } returns uploadSnapshot

        every {
            downloadUri.toString()
        } returns "https://example.com/$postId.jpg"

        every {
            imageReference.downloadUrl
        } returns Tasks.forResult(downloadUri)

        every {
            postReference.set(any())
        } returns Tasks.forResult(null)

        every {
            userReference.update(any<Map<String, Any>>())
        } returns Tasks.forResult(null)

        val localRepository = PostRepository(
            auth = localAuth,
            firestore = localFirestore,
            storage = localStorage
        )

        val result = localRepository.addPost(
            bitmap = bitmap,
            latitude = 50.0,
            longitude = 21.0
        )

        verify(exactly = 1) {
            imageReference.putBytes(any<ByteArray>())
        }

        verify(exactly = 1) {
            postReference.set(any())
        }

        verify(exactly = 1) {
            userReference.update(any<Map<String, Any>>())
        }

        return result
    }

    private fun prepareDeleteFixture(
        postId: String,
        postExists: Boolean,
        ownerUid: String?
    ): DeleteFixture {
        val postsCollection = mockk<CollectionReference>()
        val postReference = mockk<DocumentReference>()

        val usersCollection = mockk<CollectionReference>()
        val userReference = mockk<DocumentReference>()

        val transaction = mockk<Transaction>()
        val postSnapshot = mockk<DocumentSnapshot>()

        val storageRoot = mockk<StorageReference>()
        val imageReference = mockk<StorageReference>()

        every {
            firestore.collection("posts")
        } returns postsCollection

        every {
            firestore.collection("users")
        } returns usersCollection

        every {
            postsCollection.document(postId)
        } returns postReference

        every {
            usersCollection.document("user1")
        } returns userReference

        every {
            transaction.get(postReference)
        } returns postSnapshot

        every {
            postSnapshot.exists()
        } returns postExists

        every {
            postSnapshot.getString("userId")
        } returns ownerUid

        every {
            transaction.delete(postReference)
        } returns transaction

        every {
            transaction.update(
                userReference,
                any<Map<String, Any>>()
            )
        } returns transaction

        every {
            firestore.runTransaction<Transaction>(any())
        } answers {
            val function =
                firstArg<Transaction.Function<Transaction>>()

            val result = function.apply(transaction)

            Tasks.forResult(result)
        }

        every {
            storage.reference
        } returns storageRoot

        every {
            storageRoot.child(
                "posts/user1/$postId.jpg"
            )
        } returns imageReference

        return DeleteFixture(
            transaction = transaction,
            postReference = postReference,
            userReference = userReference,
            imageReference = imageReference
        )
    }

    private fun postDocument(
        id: String,
        userId: String?,
        user: String,
        imageUrl: String?,
        latitude: Double?,
        longitude: Double?,
        authorPhotoUrl: String? = null,
        createdAtSeconds: Long? = null
    ): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()

        every { document.id } returns id
        every { document.getString("userId") } returns userId
        every { document.getString("user") } returns user
        every { document.getString("imageUrl") } returns imageUrl
        every { document.getDouble("latitude") } returns latitude
        every { document.getDouble("longitude") } returns longitude

        every {
            document.getString("authorPhotoUrl")
        } returns authorPhotoUrl

        every {
            document.getTimestamp("createdAt")
        } returns createdAtSeconds?.let {
            timestamp(it)
        }

        return document
    }

    private fun userDocument(
        id: String,
        nickname: String?,
        displayName: String?,
        photoUrl: String?
    ): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()

        every { document.id } returns id

        every {
            document.getString("nickname")
        } returns nickname

        every {
            document.getString("displayName")
        } returns displayName

        every {
            document.getString("photoUrl")
        } returns photoUrl

        return document
    }

    private fun guessDocument(
        postId: String?,
        distanceMeters: Double?,
        points: Long?,
        createdAtSeconds: Long?
    ): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()

        every {
            document.getString("postId")
        } returns postId

        every {
            document.getDouble("distanceMeters")
        } returns distanceMeters

        every {
            document.getLong("points")
        } returns points

        every {
            document.getTimestamp("createdAt")
        } returns createdAtSeconds?.let {
            timestamp(it)
        }

        return document
    }

    private fun idDocument(id: String): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()
        every { document.id } returns id
        return document
    }

    private fun timestamp(seconds: Long): Timestamp {
        val timestamp = mockk<Timestamp>()

        every {
            timestamp.seconds
        } returns seconds

        every {
            timestamp.toDate()
        } returns Date(seconds * 1_000)

        return timestamp
    }

    private data class DeleteFixture(
        val transaction: Transaction,
        val postReference: DocumentReference,
        val userReference: DocumentReference,
        val imageReference: StorageReference
    )
}