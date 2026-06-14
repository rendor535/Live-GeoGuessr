package com.example.livegeoguessr.data.repository

import com.example.livegeoguessr.data.remote.firebase.FirebaseModule
import com.example.livegeoguessr.domain.model.UserProfile
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FriendRepositoryTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var currentUser: FirebaseUser

    private lateinit var usersCollection: CollectionReference
    private lateinit var friendRequestsCollection: CollectionReference

    private lateinit var repository: FriendRepository

    @BeforeEach
    fun setUp() {
        auth = mockk()
        firestore = mockk()
        functions = mockk()
        currentUser = mockk()

        usersCollection = mockk()
        friendRequestsCollection = mockk()

        /*
         * FriendRepository pobiera zależności bezpośrednio
         * z FirebaseModule oraz tworzy referencje do kolekcji
         * podczas wykonywania konstruktora.
         */
        mockkObject(FirebaseModule)

        every {
            FirebaseModule.auth
        } returns auth

        every {
            FirebaseModule.firestore
        } returns firestore

        every {
            FirebaseModule.functions
        } returns functions

        every {
            firestore.collection("users")
        } returns usersCollection

        every {
            firestore.collection("friendRequests")
        } returns friendRequestsCollection

        repository = FriendRepository()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(FirebaseModule)
    }

    // -------------------------------------------------------------------------
    // getUsersForFriendSearch
    // -------------------------------------------------------------------------

    @Test
    fun `getUsersForFriendSearch excludes current user friends and pending requests`() =
        runTest {
            prepareCurrentUser("current-user")

            prepareFriendsSnapshot(
                currentUid = "current-user",
                friendUids = listOf("friend-user")
            )

            val incomingRequest = friendRequestDocument(
                id = "incoming-request",
                fromUid = "incoming-user",
                toUid = "current-user"
            )

            val outgoingRequest = friendRequestDocument(
                id = "outgoing-request",
                fromUid = "current-user",
                toUid = "outgoing-user"
            )

            prepareIncomingRequests(
                currentUid = "current-user",
                documents = listOf(incomingRequest)
            )

            prepareOutgoingRequests(
                currentUid = "current-user",
                documents = listOf(outgoingRequest)
            )

            val currentProfile = userProfile("current-user")
            val friendProfile = userProfile("friend-user")
            val incomingProfile = userProfile("incoming-user")
            val outgoingProfile = userProfile("outgoing-user")
            val availableProfile = userProfile("available-user")

            val currentDocument = profileDocument(currentProfile)
            val friendDocument = profileDocument(friendProfile)
            val incomingDocument = profileDocument(incomingProfile)
            val outgoingDocument = profileDocument(outgoingProfile)
            val availableDocument = profileDocument(availableProfile)

            /*
             * Ten dokument pokrywa mapNotNull, gdy Firestore
             * nie potrafi utworzyć obiektu UserProfile.
             */
            val invalidDocument = mockk<DocumentSnapshot>()

            every {
                invalidDocument.toObject(UserProfile::class.java)
            } returns null

            val usersQuery = mockk<Query>()
            val usersSnapshot = mockk<QuerySnapshot>()

            every {
                usersCollection.limit(200)
            } returns usersQuery

            every {
                usersQuery.get()
            } returns Tasks.forResult(usersSnapshot)

            every {
                usersSnapshot.documents
            } returns listOf(
                currentDocument,
                friendDocument,
                incomingDocument,
                outgoingDocument,
                availableDocument,
                invalidDocument
            )

            val result = repository.getUsersForFriendSearch()

            assertEquals(1, result.size)
            assertEquals("available-user", result.single().uid)

            verify(exactly = 1) {
                usersCollection.limit(200)
            }
        }

    @Test
    fun `getUsersForFriendSearch returns all valid users when nothing except current user is excluded`() =
        runTest {
            prepareCurrentUser("current-user")

            prepareFriendsSnapshot(
                currentUid = "current-user",
                friendUids = emptyList()
            )

            prepareIncomingRequests(
                currentUid = "current-user",
                documents = emptyList()
            )

            prepareOutgoingRequests(
                currentUid = "current-user",
                documents = emptyList()
            )

            val currentProfile = userProfile("current-user")
            val firstAvailableProfile = userProfile("user-1")
            val secondAvailableProfile = userProfile("user-2")

            val usersQuery = mockk<Query>()
            val usersSnapshot = mockk<QuerySnapshot>()

            every {
                usersCollection.limit(200)
            } returns usersQuery

            every {
                usersQuery.get()
            } returns Tasks.forResult(usersSnapshot)

            every {
                usersSnapshot.documents
            } returns listOf(
                profileDocument(currentProfile),
                profileDocument(firstAvailableProfile),
                profileDocument(secondAvailableProfile)
            )

            val result = repository.getUsersForFriendSearch()

            assertEquals(
                listOf("user-1", "user-2"),
                result.map { it.uid }
            )
        }

    @Test
    fun `getUsersForFriendSearch throws when user is not logged in`() {
        every {
            auth.currentUser
        } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.getUsersForFriendSearch()
            }
        }

        verify(exactly = 0) {
            usersCollection.limit(any())
        }
    }

    // -------------------------------------------------------------------------
    // getFriends
    // -------------------------------------------------------------------------

    @Test
    fun `getFriends returns existing profiles and skips missing profiles`() =
        runTest {
            prepareCurrentUser("current-user")

            val currentUserReference = mockk<DocumentReference>()
            val friendsCollection = mockk<CollectionReference>()
            val friendsSnapshot = mockk<QuerySnapshot>()

            val firstFriendIdDocument = idDocument("friend-1")
            val secondFriendIdDocument = idDocument("friend-2")

            every {
                usersCollection.document("current-user")
            } returns currentUserReference

            every {
                currentUserReference.collection("friends")
            } returns friendsCollection

            every {
                friendsCollection.get()
            } returns Tasks.forResult(friendsSnapshot)

            every {
                friendsSnapshot.documents
            } returns listOf(
                firstFriendIdDocument,
                secondFriendIdDocument
            )

            val firstFriendReference = mockk<DocumentReference>()
            val secondFriendReference = mockk<DocumentReference>()

            val firstFriendSnapshot = mockk<DocumentSnapshot>()
            val secondFriendSnapshot = mockk<DocumentSnapshot>()

            val firstFriendProfile = userProfile("friend-1")

            every {
                usersCollection.document("friend-1")
            } returns firstFriendReference

            every {
                firstFriendReference.get()
            } returns Tasks.forResult(firstFriendSnapshot)

            every {
                firstFriendSnapshot.toObject(UserProfile::class.java)
            } returns firstFriendProfile

            every {
                usersCollection.document("friend-2")
            } returns secondFriendReference

            every {
                secondFriendReference.get()
            } returns Tasks.forResult(secondFriendSnapshot)

            /*
             * Pokrycie mapNotNull: profil użytkownika nie istnieje.
             */
            every {
                secondFriendSnapshot.toObject(UserProfile::class.java)
            } returns null

            val result = repository.getFriends()

            assertEquals(1, result.size)
            assertEquals("friend-1", result.single().uid)
        }

    @Test
    fun `getFriends returns empty list when user has no friends`() =
        runTest {
            prepareCurrentUser("current-user")

            prepareFriendsSnapshot(
                currentUid = "current-user",
                friendUids = emptyList()
            )

            val result = repository.getFriends()

            assertTrue(result.isEmpty())
        }

    @Test
    fun `getFriends propagates Firestore error`() {
        prepareCurrentUser("current-user")

        val currentUserReference = mockk<DocumentReference>()
        val friendsCollection = mockk<CollectionReference>()

        every {
            usersCollection.document("current-user")
        } returns currentUserReference

        every {
            currentUserReference.collection("friends")
        } returns friendsCollection

        every {
            friendsCollection.get()
        } returns Tasks.forException(
            IllegalStateException("Firestore failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.getFriends()
            }
        }
    }

    // -------------------------------------------------------------------------
    // getIncomingRequests
    // -------------------------------------------------------------------------

    @Test
    fun `getIncomingRequests maps all stored values`() = runTest {
        prepareCurrentUser("current-user")

        val requestDocument = friendRequestDocument(
            id = "request-1",
            fromUid = "sender",
            toUid = "current-user",
            fromNickname = "SenderNick",
            fromDisplayName = "Sender Name",
            fromPhotoUrl = "sender-photo",
            toNickname = "ReceiverNick",
            toDisplayName = "Receiver Name",
            toPhotoUrl = "receiver-photo",
            status = "pending"
        )

        prepareIncomingRequests(
            currentUid = "current-user",
            documents = listOf(requestDocument)
        )

        val result = repository.getIncomingRequests()

        assertEquals(1, result.size)

        val request = result.single()

        assertEquals("request-1", request.id)
        assertEquals("sender", request.fromUid)
        assertEquals("current-user", request.toUid)
        assertEquals("SenderNick", request.fromNickname)
        assertEquals("Sender Name", request.fromDisplayName)
        assertEquals("sender-photo", request.fromPhotoUrl)
        assertEquals("ReceiverNick", request.toNickname)
        assertEquals("Receiver Name", request.toDisplayName)
        assertEquals("receiver-photo", request.toPhotoUrl)
        assertEquals("pending", request.status)
    }

    @Test
    fun `getIncomingRequests uses fallback values for missing fields`() =
        runTest {
            prepareCurrentUser("current-user")

            val requestDocument = friendRequestDocument(
                id = "request-1",
                fromUid = null,
                toUid = null,
                fromNickname = null,
                fromDisplayName = null,
                fromPhotoUrl = null,
                toNickname = null,
                toDisplayName = null,
                toPhotoUrl = null,
                status = null
            )

            prepareIncomingRequests(
                currentUid = "current-user",
                documents = listOf(requestDocument)
            )

            val request = repository
                .getIncomingRequests()
                .single()

            assertEquals("request-1", request.id)
            assertEquals("", request.fromUid)
            assertEquals("", request.toUid)
            assertEquals("Player", request.fromNickname)
            assertEquals("", request.fromDisplayName)
            assertNull(request.fromPhotoUrl)
            assertEquals("Player", request.toNickname)
            assertEquals("", request.toDisplayName)
            assertNull(request.toPhotoUrl)
            assertEquals("pending", request.status)
        }

    @Test
    fun `getIncomingRequests returns empty list when there are no requests`() =
        runTest {
            prepareCurrentUser("current-user")

            prepareIncomingRequests(
                currentUid = "current-user",
                documents = emptyList()
            )

            val result = repository.getIncomingRequests()

            assertTrue(result.isEmpty())
        }

    // -------------------------------------------------------------------------
    // getOutgoingRequests
    // -------------------------------------------------------------------------

    @Test
    fun `getOutgoingRequests maps all stored values`() = runTest {
        prepareCurrentUser("current-user")

        val requestDocument = friendRequestDocument(
            id = "request-2",
            fromUid = "current-user",
            toUid = "receiver",
            fromNickname = "SenderNick",
            fromDisplayName = "Sender Name",
            fromPhotoUrl = "sender-photo",
            toNickname = "ReceiverNick",
            toDisplayName = "Receiver Name",
            toPhotoUrl = "receiver-photo",
            status = "pending"
        )

        prepareOutgoingRequests(
            currentUid = "current-user",
            documents = listOf(requestDocument)
        )

        val request = repository
            .getOutgoingRequests()
            .single()

        assertEquals("request-2", request.id)
        assertEquals("current-user", request.fromUid)
        assertEquals("receiver", request.toUid)
        assertEquals("SenderNick", request.fromNickname)
        assertEquals("Sender Name", request.fromDisplayName)
        assertEquals("sender-photo", request.fromPhotoUrl)
        assertEquals("ReceiverNick", request.toNickname)
        assertEquals("Receiver Name", request.toDisplayName)
        assertEquals("receiver-photo", request.toPhotoUrl)
        assertEquals("pending", request.status)
    }

    @Test
    fun `getOutgoingRequests uses fallback values for missing fields`() =
        runTest {
            prepareCurrentUser("current-user")

            val requestDocument = friendRequestDocument(
                id = "request-2",
                fromUid = null,
                toUid = null,
                fromNickname = null,
                fromDisplayName = null,
                fromPhotoUrl = null,
                toNickname = null,
                toDisplayName = null,
                toPhotoUrl = null,
                status = null
            )

            prepareOutgoingRequests(
                currentUid = "current-user",
                documents = listOf(requestDocument)
            )

            val request = repository
                .getOutgoingRequests()
                .single()

            assertEquals("request-2", request.id)
            assertEquals("", request.fromUid)
            assertEquals("", request.toUid)
            assertEquals("Player", request.fromNickname)
            assertEquals("", request.fromDisplayName)
            assertNull(request.fromPhotoUrl)
            assertEquals("Player", request.toNickname)
            assertEquals("", request.toDisplayName)
            assertNull(request.toPhotoUrl)
            assertEquals("pending", request.status)
        }

    @Test
    fun `getOutgoingRequests returns empty list when there are no requests`() =
        runTest {
            prepareCurrentUser("current-user")

            prepareOutgoingRequests(
                currentUid = "current-user",
                documents = emptyList()
            )

            val result = repository.getOutgoingRequests()

            assertTrue(result.isEmpty())
        }

    // -------------------------------------------------------------------------
    // Cloud Functions
    // -------------------------------------------------------------------------

    @Test
    fun `sendFriendRequest calls function with recipient uid`() = runTest {
        val callable = successfulCallable(
            functionName = "sendFriendRequest",
            expectedData = mapOf(
                "toUid" to "target-user"
            )
        )

        repository.sendFriendRequest("target-user")

        verify(exactly = 1) {
            callable.call(
                mapOf(
                    "toUid" to "target-user"
                )
            )
        }
    }

    @Test
    fun `acceptFriendRequest calls function with request id`() = runTest {
        val callable = successfulCallable(
            functionName = "acceptFriendRequest",
            expectedData = mapOf(
                "requestId" to "request-1"
            )
        )

        repository.acceptFriendRequest("request-1")

        verify(exactly = 1) {
            callable.call(
                mapOf(
                    "requestId" to "request-1"
                )
            )
        }
    }

    @Test
    fun `rejectFriendRequest calls function with request id`() = runTest {
        val callable = successfulCallable(
            functionName = "rejectFriendRequest",
            expectedData = mapOf(
                "requestId" to "request-1"
            )
        )

        repository.rejectFriendRequest("request-1")

        verify(exactly = 1) {
            callable.call(
                mapOf(
                    "requestId" to "request-1"
                )
            )
        }
    }

    @Test
    fun `removeFriend calls function with friend uid`() = runTest {
        val callable = successfulCallable(
            functionName = "removeFriend",
            expectedData = mapOf(
                "friendUid" to "friend-1"
            )
        )

        repository.removeFriend("friend-1")

        verify(exactly = 1) {
            callable.call(
                mapOf(
                    "friendUid" to "friend-1"
                )
            )
        }
    }

    @Test
    fun `sendFriendRequest propagates cloud function error`() {
        val callable = mockk<HttpsCallableReference>()

        every {
            functions.getHttpsCallable("sendFriendRequest")
        } returns callable

        every {
            callable.call(
                mapOf(
                    "toUid" to "target-user"
                )
            )
        } returns Tasks.forException(
            IllegalStateException("Function failed")
        )

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.sendFriendRequest("target-user")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun prepareCurrentUser(uid: String) {
        every {
            auth.currentUser
        } returns currentUser

        every {
            currentUser.uid
        } returns uid
    }

    private fun prepareFriendsSnapshot(
        currentUid: String,
        friendUids: List<String>
    ) {
        val currentUserReference = mockk<DocumentReference>()
        val friendsCollection = mockk<CollectionReference>()
        val friendsSnapshot = mockk<QuerySnapshot>()

        every {
            usersCollection.document(currentUid)
        } returns currentUserReference

        every {
            currentUserReference.collection("friends")
        } returns friendsCollection

        every {
            friendsCollection.get()
        } returns Tasks.forResult(friendsSnapshot)

        every {
            friendsSnapshot.documents
        } returns friendUids.map { friendUid ->
            idDocument(friendUid)
        }
    }

    private fun prepareIncomingRequests(
        currentUid: String,
        documents: List<DocumentSnapshot>
    ) {
        prepareRequestQuery(
            uidField = "toUid",
            currentUid = currentUid,
            documents = documents
        )
    }

    private fun prepareOutgoingRequests(
        currentUid: String,
        documents: List<DocumentSnapshot>
    ) {
        prepareRequestQuery(
            uidField = "fromUid",
            currentUid = currentUid,
            documents = documents
        )
    }

    private fun prepareRequestQuery(
        uidField: String,
        currentUid: String,
        documents: List<DocumentSnapshot>
    ) {
        val uidQuery = mockk<Query>()
        val statusQuery = mockk<Query>()
        val snapshot = mockk<QuerySnapshot>()

        every {
            friendRequestsCollection.whereEqualTo(
                uidField,
                currentUid
            )
        } returns uidQuery

        every {
            uidQuery.whereEqualTo(
                "status",
                "pending"
            )
        } returns statusQuery

        every {
            statusQuery.get()
        } returns Tasks.forResult(snapshot)

        every {
            snapshot.documents
        } returns documents
    }

    private fun idDocument(id: String): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()

        every {
            document.id
        } returns id

        return document
    }

    private fun userProfile(uid: String): UserProfile {
        val profile = mockk<UserProfile>()

        every {
            profile.uid
        } returns uid

        return profile
    }

    private fun profileDocument(
        profile: UserProfile
    ): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()

        every {
            document.toObject(UserProfile::class.java)
        } returns profile

        return document
    }

    private fun friendRequestDocument(
        id: String,
        fromUid: String?,
        toUid: String?,
        fromNickname: String? = "FromNick",
        fromDisplayName: String? = "From Name",
        fromPhotoUrl: String? = null,
        toNickname: String? = "ToNick",
        toDisplayName: String? = "To Name",
        toPhotoUrl: String? = null,
        status: String? = "pending"
    ): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()

        every {
            document.id
        } returns id

        every {
            document.getString("fromUid")
        } returns fromUid

        every {
            document.getString("toUid")
        } returns toUid

        every {
            document.getString("fromNickname")
        } returns fromNickname

        every {
            document.getString("fromDisplayName")
        } returns fromDisplayName

        every {
            document.getString("fromPhotoUrl")
        } returns fromPhotoUrl

        every {
            document.getString("toNickname")
        } returns toNickname

        every {
            document.getString("toDisplayName")
        } returns toDisplayName

        every {
            document.getString("toPhotoUrl")
        } returns toPhotoUrl

        every {
            document.getString("status")
        } returns status

        return document
    }

    private fun successfulCallable(
        functionName: String,
        expectedData: Map<String, String>
    ): HttpsCallableReference {
        val callable = mockk<HttpsCallableReference>()

        every {
            functions.getHttpsCallable(functionName)
        } returns callable

        every {
            callable.call(expectedData)
        } returns Tasks.forResult(
            httpsCallableResult(emptyMap<String, Any>())
        )

        return callable
    }

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