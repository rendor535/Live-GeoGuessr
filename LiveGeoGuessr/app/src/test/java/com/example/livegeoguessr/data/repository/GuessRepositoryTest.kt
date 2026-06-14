package com.example.livegeoguessr.data.repository

import com.example.livegeoguessr.data.remote.firebase.FirebaseModule
import com.example.livegeoguessr.domain.model.GameModeType
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GuessRepositoryTest {

    private lateinit var functions: FirebaseFunctions
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    private lateinit var repository: GuessRepository

    @BeforeEach
    fun setUp() {
        functions = mockk()
        firestore = mockk()
        auth = mockk()
        currentUser = mockk()

        /*
         * GuessRepository pobiera zależności bezpośrednio
         * z FirebaseModule, dlatego mockujemy moduł przed
         * utworzeniem repozytorium.
         */
        mockkObject(FirebaseModule)

        every {
            FirebaseModule.functions
        } returns functions

        every {
            FirebaseModule.firestore
        } returns firestore

        every {
            FirebaseModule.auth
        } returns auth

        repository = GuessRepository()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(FirebaseModule)
    }

    // -------------------------------------------------------------------------
    // submitGuess
    // -------------------------------------------------------------------------

    @Test
    fun `submitGuess sends correct data and maps complete response`() = runTest {
        val callable = mockk<HttpsCallableReference>()
        val capturedRequests = mutableListOf<Map<*, *>>()

        val responseData = mapOf(
            "guessId" to "guess1",
            "postId" to "post1",
            "gameMode" to GameModeType.CITY.name,
            "distanceMeters" to 123.45,
            "points" to 9_000L,
            "maxPoints" to 10_000,
            "maxScoringDistanceMeters" to 2_000L,
            "scoringVersion" to 1L,
            "guessedLatitude" to 50.123,
            "guessedLongitude" to 21.456,
            "realLatitude" to 50.500,
            "realLongitude" to 21.900
        )

        every {
            functions.getHttpsCallable("submitGuess")
        } returns callable

        every {
            callable.call(any())
        } answers {
            capturedRequests += firstArg<Any?>() as Map<*, *>

            Tasks.forResult(
                httpsCallableResult(responseData)
            )
        }

        val result = repository.submitGuess(
            postId = "post1",
            guessedLatitude = 50.123,
            guessedLongitude = 21.456
        )

        assertEquals("guess1", result.guessId)
        assertEquals("post1", result.postId)
        assertEquals(GameModeType.CITY, result.gameMode)
        assertEquals(123.45, result.distanceMeters)
        assertEquals(9_000, result.points)
        assertEquals(10_000, result.maxPoints)
        assertEquals(2_000.0, result.maxScoringDistanceMeters)
        assertEquals(1, result.scoringVersion)
        assertEquals(50.123, result.guessedLatitude)
        assertEquals(21.456, result.guessedLongitude)
        assertEquals(50.500, result.realLatitude)
        assertEquals(21.900, result.realLongitude)

        val request = capturedRequests.single()

        assertEquals("post1", request["postId"])
        assertEquals(50.123, request["guessedLatitude"])
        assertEquals(21.456, request["guessedLongitude"])
        assertEquals(GameModeType.CITY.name, request["gameMode"])

        verify(exactly = 1) {
            functions.getHttpsCallable("submitGuess")
        }

        verify(exactly = 1) {
            callable.call(any())
        }
    }

    @Test
    fun `submitGuess sends explicitly selected game mode`() = runTest {
        val callable = mockk<HttpsCallableReference>()
        val capturedRequests = mutableListOf<Map<*, *>>()

        val selectedMode = GameModeType.values().last()

        every {
            functions.getHttpsCallable("submitGuess")
        } returns callable

        every {
            callable.call(any())
        } answers {
            capturedRequests += firstArg<Any?>() as Map<*, *>

            Tasks.forResult(
                httpsCallableResult(
                    completeSubmitResponse(
                        postId = "post2",
                        gameMode = selectedMode
                    )
                )
            )
        }

        val result = repository.submitGuess(
            postId = "post2",
            guessedLatitude = 10.0,
            guessedLongitude = 20.0,
            gameMode = selectedMode
        )

        assertEquals(selectedMode, result.gameMode)

        val request = capturedRequests.single()

        assertEquals("post2", request["postId"])
        assertEquals(10.0, request["guessedLatitude"])
        assertEquals(20.0, request["guessedLongitude"])
        assertEquals(selectedMode.name, request["gameMode"])
    }

    @Test
    fun `submitGuess propagates cloud function error`() {
        val callable = mockk<HttpsCallableReference>()

        every {
            functions.getHttpsCallable("submitGuess")
        } returns callable

        every {
            callable.call(any())
        } returns Tasks.forException<HttpsCallableResult>(
            IllegalStateException("Submit failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.submitGuess(
                    postId = "post1",
                    guessedLatitude = 10.0,
                    guessedLongitude = 20.0
                )
            }
        }
    }

    @Test
    fun `submitGuess throws when response contains invalid field type`() {
        val callable = mockk<HttpsCallableReference>()

        val invalidResponse = mapOf(
            "guessId" to 123,
            "postId" to "post1",
            "gameMode" to GameModeType.CITY.name,
            "distanceMeters" to 10.0,
            "points" to 100,
            "maxPoints" to 10_000,
            "maxScoringDistanceMeters" to 2_000.0,
            "scoringVersion" to 1,
            "guessedLatitude" to 10.0,
            "guessedLongitude" to 20.0,
            "realLatitude" to 30.0,
            "realLongitude" to 40.0
        )

        every {
            functions.getHttpsCallable("submitGuess")
        } returns callable

        every {
            callable.call(any())
        } returns Tasks.forResult(
            httpsCallableResult(invalidResponse)
        )

        assertThrows(ClassCastException::class.java) {
            runTest {
                repository.submitGuess(
                    postId = "post1",
                    guessedLatitude = 10.0,
                    guessedLongitude = 20.0
                )
            }
        }
    }

    @Test
    fun `submitGuess throws when response contains unknown game mode`() {
        val callable = mockk<HttpsCallableReference>()

        every {
            functions.getHttpsCallable("submitGuess")
        } returns callable

        every {
            callable.call(any())
        } returns Tasks.forResult(
            httpsCallableResult(
                completeSubmitResponse(
                    postId = "post1",
                    gameModeName = "INVALID_MODE"
                )
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                repository.submitGuess(
                    postId = "post1",
                    guessedLatitude = 10.0,
                    guessedLongitude = 20.0
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // getMyGuessForPost
    // -------------------------------------------------------------------------

    @Test
    fun `getMyGuessForPost returns null when user is not logged in`() =
        runTest {
            every {
                auth.currentUser
            } returns null

            val result = repository.getMyGuessForPost("post1")

            assertNull(result)

            verify(exactly = 0) {
                firestore.collection(any())
            }
        }

    @Test
    fun `getMyGuessForPost returns null when document does not exist`() =
        runTest {
            val collection = mockk<CollectionReference>()
            val documentReference = mockk<DocumentReference>()
            val snapshot = mockk<DocumentSnapshot>()

            every {
                auth.currentUser
            } returns currentUser

            every {
                currentUser.uid
            } returns "user1"

            every {
                firestore.collection("guesses")
            } returns collection

            every {
                collection.document("user1_post1")
            } returns documentReference

            every {
                documentReference.get()
            } returns Tasks.forResult(snapshot)

            every {
                snapshot.exists()
            } returns false

            val result = repository.getMyGuessForPost("post1")

            assertNull(result)
        }

    @Test
    fun `getMyGuessForPost maps all stored values`() = runTest {
        val snapshot = prepareGuessSnapshot(
            requestedPostId = "requested-post",
            documentId = "document-id"
        )

        every {
            snapshot.getString("id")
        } returns "guess-id"

        every {
            snapshot.getString("postId")
        } returns "stored-post"

        every {
            snapshot.getString("gameMode")
        } returns GameModeType.CITY.name

        every {
            snapshot.getDouble("distanceMeters")
        } returns 150.5

        every {
            snapshot.getLong("points")
        } returns 8_000L

        every {
            snapshot.getLong("maxPoints")
        } returns 12_000L

        every {
            snapshot.getDouble("maxScoringDistanceMeters")
        } returns 5_000.0

        every {
            snapshot.getLong("scoringVersion")
        } returns 3L

        every {
            snapshot.getDouble("guessedLatitude")
        } returns 50.1

        every {
            snapshot.getDouble("guessedLongitude")
        } returns 21.1

        every {
            snapshot.getDouble("realLatitude")
        } returns 50.2

        every {
            snapshot.getDouble("realLongitude")
        } returns 21.2

        val result = repository.getMyGuessForPost(
            "requested-post"
        )

        requireNotNull(result)

        assertEquals("guess-id", result.guessId)
        assertEquals("stored-post", result.postId)
        assertEquals(GameModeType.CITY, result.gameMode)
        assertEquals(150.5, result.distanceMeters)
        assertEquals(8_000, result.points)
        assertEquals(12_000, result.maxPoints)
        assertEquals(5_000.0, result.maxScoringDistanceMeters)
        assertEquals(3, result.scoringVersion)
        assertEquals(50.1, result.guessedLatitude)
        assertEquals(21.1, result.guessedLongitude)
        assertEquals(50.2, result.realLatitude)
        assertEquals(21.2, result.realLongitude)
    }

    @Test
    fun `getMyGuessForPost uses every fallback when fields are missing`() =
        runTest {
            val snapshot = prepareGuessSnapshot(
                requestedPostId = "post1",
                documentId = "user1_post1"
            )

            every {
                snapshot.getString("id")
            } returns null

            every {
                snapshot.getString("postId")
            } returns null

            every {
                snapshot.getString("gameMode")
            } returns null

            every {
                snapshot.getDouble("distanceMeters")
            } returns null

            every {
                snapshot.getLong("points")
            } returns null

            every {
                snapshot.getLong("maxPoints")
            } returns null

            every {
                snapshot.getDouble("maxScoringDistanceMeters")
            } returns null

            every {
                snapshot.getLong("scoringVersion")
            } returns null

            every {
                snapshot.getDouble("guessedLatitude")
            } returns null

            every {
                snapshot.getDouble("guessedLongitude")
            } returns null

            every {
                snapshot.getDouble("realLatitude")
            } returns null

            every {
                snapshot.getDouble("realLongitude")
            } returns null

            val result = repository.getMyGuessForPost("post1")

            requireNotNull(result)

            assertEquals("user1_post1", result.guessId)
            assertEquals("post1", result.postId)
            assertEquals(GameModeType.CITY, result.gameMode)
            assertEquals(0.0, result.distanceMeters)
            assertEquals(0, result.points)
            assertEquals(10_000, result.maxPoints)
            assertEquals(2_000.0, result.maxScoringDistanceMeters)
            assertEquals(1, result.scoringVersion)
            assertEquals(0.0, result.guessedLatitude)
            assertEquals(0.0, result.guessedLongitude)
            assertEquals(0.0, result.realLatitude)
            assertEquals(0.0, result.realLongitude)
        }

    @Test
    fun `getMyGuessForPost throws for unknown stored game mode`() {
        val snapshot = prepareGuessSnapshot(
            requestedPostId = "post1",
            documentId = "user1_post1"
        )

        every {
            snapshot.getString("id")
        } returns "guess1"

        every {
            snapshot.getString("postId")
        } returns "post1"

        every {
            snapshot.getString("gameMode")
        } returns "UNKNOWN_MODE"

        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                repository.getMyGuessForPost("post1")
            }
        }
    }

    @Test
    fun `getMyGuessForPost propagates Firestore error`() {
        val collection = mockk<CollectionReference>()
        val documentReference = mockk<DocumentReference>()

        every {
            auth.currentUser
        } returns currentUser

        every {
            currentUser.uid
        } returns "user1"

        every {
            firestore.collection("guesses")
        } returns collection

        every {
            collection.document("user1_post1")
        } returns documentReference

        every {
            documentReference.get()
        } returns Tasks.forException<DocumentSnapshot>(
            IllegalStateException("Firestore failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.getMyGuessForPost("post1")
            }
        }
    }

    // -------------------------------------------------------------------------
    // getGuessMapPreview
    // -------------------------------------------------------------------------

    @Test
    fun `getGuessMapPreview sends request and maps response`() = runTest {
        val callable = mockk<HttpsCallableReference>()
        val capturedRequests = mutableListOf<Map<*, *>>()

        val selectedMode = GameModeType.values().first()

        val responseData = mapOf(
            "postId" to "post1",
            "gameMode" to selectedMode.name,
            "initialMapCenterLatitude" to 50.123,
            "initialMapCenterLongitude" to 21.456,
            "initialMapDiameterMeters" to 20_000L
        )

        every {
            functions.getHttpsCallable("getGuessMapPreview")
        } returns callable

        every {
            callable.call(any())
        } answers {
            capturedRequests += firstArg<Any?>() as Map<*, *>

            Tasks.forResult(
                httpsCallableResult(responseData)
            )
        }

        val result = repository.getGuessMapPreview(
            postId = "post1",
            gameMode = selectedMode
        )

        assertEquals("post1", result.postId)
        assertEquals(selectedMode, result.gameMode)
        assertEquals(50.123, result.initialMapCenterLatitude)
        assertEquals(21.456, result.initialMapCenterLongitude)
        assertEquals(20_000.0, result.initialMapDiameterMeters)

        val request = capturedRequests.single()

        assertEquals("post1", request["postId"])
        assertEquals(selectedMode.name, request["gameMode"])

        verify(exactly = 1) {
            functions.getHttpsCallable("getGuessMapPreview")
        }

        verify(exactly = 1) {
            callable.call(any())
        }
    }

    @Test
    fun `getGuessMapPreview propagates cloud function error`() {
        val callable = mockk<HttpsCallableReference>()

        every {
            functions.getHttpsCallable("getGuessMapPreview")
        } returns callable

        every {
            callable.call(any())
        } returns Tasks.forException<HttpsCallableResult>(
            IllegalStateException("Preview failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.getGuessMapPreview(
                    postId = "post1",
                    gameMode = GameModeType.CITY
                )
            }
        }
    }

    @Test
    fun `getGuessMapPreview throws for unknown game mode`() {
        val callable = mockk<HttpsCallableReference>()

        val responseData = mapOf(
            "postId" to "post1",
            "gameMode" to "INVALID_MODE",
            "initialMapCenterLatitude" to 50.0,
            "initialMapCenterLongitude" to 21.0,
            "initialMapDiameterMeters" to 10_000.0
        )

        every {
            functions.getHttpsCallable("getGuessMapPreview")
        } returns callable

        every {
            callable.call(any())
        } returns Tasks.forResult(
            httpsCallableResult(responseData)
        )

        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                repository.getGuessMapPreview(
                    postId = "post1",
                    gameMode = GameModeType.CITY
                )
            }
        }
    }

    @Test
    fun `getGuessMapPreview throws when numeric field has invalid type`() {
        val callable = mockk<HttpsCallableReference>()

        val responseData = mapOf(
            "postId" to "post1",
            "gameMode" to GameModeType.CITY.name,
            "initialMapCenterLatitude" to "not-a-number",
            "initialMapCenterLongitude" to 21.0,
            "initialMapDiameterMeters" to 10_000.0
        )

        every {
            functions.getHttpsCallable("getGuessMapPreview")
        } returns callable

        every {
            callable.call(any())
        } returns Tasks.forResult(
            httpsCallableResult(responseData)
        )

        assertThrows(ClassCastException::class.java) {
            runTest {
                repository.getGuessMapPreview(
                    postId = "post1",
                    gameMode = GameModeType.CITY
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun prepareGuessSnapshot(
        requestedPostId: String,
        documentId: String
    ): DocumentSnapshot {
        val collection = mockk<CollectionReference>()
        val documentReference = mockk<DocumentReference>()
        val snapshot = mockk<DocumentSnapshot>()

        every {
            auth.currentUser
        } returns currentUser

        every {
            currentUser.uid
        } returns "user1"

        every {
            firestore.collection("guesses")
        } returns collection

        every {
            collection.document("user1_$requestedPostId")
        } returns documentReference

        every {
            documentReference.get()
        } returns Tasks.forResult(snapshot)

        every {
            snapshot.exists()
        } returns true

        every {
            snapshot.id
        } returns documentId

        return snapshot
    }

    private fun completeSubmitResponse(
        postId: String,
        gameMode: GameModeType = GameModeType.CITY,
        gameModeName: String = gameMode.name
    ): Map<String, Any> {
        return mapOf(
            "guessId" to "guess-$postId",
            "postId" to postId,
            "gameMode" to gameModeName,
            "distanceMeters" to 100.0,
            "points" to 9_000,
            "maxPoints" to 10_000,
            "maxScoringDistanceMeters" to 2_000.0,
            "scoringVersion" to 1,
            "guessedLatitude" to 50.0,
            "guessedLongitude" to 21.0,
            "realLatitude" to 50.5,
            "realLongitude" to 21.5
        )
    }

    /*
     * HttpsCallableResult nie udostępnia publicznego konstruktora,
     * dlatego tworzymy prawdziwy wynik przez refleksję zamiast
     * mockować finalną klasę Firebase.
     */
    private fun httpsCallableResult(
        data: Any?
    ): HttpsCallableResult {
        val constructor =
            HttpsCallableResult::class.java.getDeclaredConstructor(
                Any::class.java
            )

        constructor.isAccessible = true

        return constructor.newInstance(data)
    }
}