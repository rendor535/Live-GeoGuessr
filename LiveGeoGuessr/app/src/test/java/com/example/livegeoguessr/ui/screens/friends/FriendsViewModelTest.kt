package com.example.livegeoguessr.ui.screens.friends

import com.example.livegeoguessr.data.repository.FriendRepository
import com.example.livegeoguessr.ui.state.ScreenState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    private lateinit var repository: FriendRepository
    private lateinit var viewModel: FriendsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = FriendsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `init loads friends successfully`() = runTest {
        coEvery { repository.getFriends() } returns listOf(
            mockk {
                every { uid } returns "1"
                every { displayName } returns "John"
                every { nickname } returns "Johnny"
                every { photoUrl } returns null
                every { stats.pointsTotal } returns 100
            }
        )

        coEvery { repository.getIncomingRequests() } returns emptyList()
        coEvery { repository.getOutgoingRequests() } returns emptyList()

        createViewModel()

        val state = viewModel.uiState.value

        assertTrue(state is ScreenState.Content)
        assertEquals(1, (state as ScreenState.Content).data.friends.size)
    }

    @Test
    fun `init handles error`() = runTest {
        coEvery { repository.getFriends() } throws RuntimeException("error")

        createViewModel()

        val state = viewModel.uiState.value

        assertTrue(state is ScreenState.Error)
        assertEquals("error", (state as ScreenState.Error).message)
    }

    @Test
    fun `acceptFriendRequest success reloads data`() = runTest {
        coEvery { repository.getFriends() } returns emptyList()
        coEvery { repository.getIncomingRequests() } returns emptyList()
        coEvery { repository.getOutgoingRequests() } returns emptyList()

        createViewModel()

        coEvery { repository.acceptFriendRequest("req1") } returns Unit

        viewModel.acceptFriendRequest("req1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.acceptFriendRequest("req1") }
        coVerify(atLeast = 1) { repository.getFriends() }
    }

    @Test
    fun `acceptFriendRequest failure sets error`() = runTest {
        coEvery { repository.getFriends() } returns emptyList()
        coEvery { repository.getIncomingRequests() } returns emptyList()
        coEvery { repository.getOutgoingRequests() } returns emptyList()

        createViewModel()

        coEvery { repository.acceptFriendRequest("req1") } throws RuntimeException("fail")

        viewModel.acceptFriendRequest("req1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state is ScreenState.Error)
        assertEquals("fail", (state as ScreenState.Error).message)
    }

    @Test
    fun `rejectFriendRequest success calls repository`() = runTest {
        coEvery { repository.getFriends() } returns emptyList()
        coEvery { repository.getIncomingRequests() } returns emptyList()
        coEvery { repository.getOutgoingRequests() } returns emptyList()

        createViewModel()

        coEvery { repository.rejectFriendRequest("req1") } returns Unit

        viewModel.rejectFriendRequest("req1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.rejectFriendRequest("req1") }
    }

    @Test
    fun `removeFriend success calls repository`() = runTest {
        coEvery { repository.getFriends() } returns emptyList()
        coEvery { repository.getIncomingRequests() } returns emptyList()
        coEvery { repository.getOutgoingRequests() } returns emptyList()

        createViewModel()

        coEvery { repository.removeFriend("user1") } returns Unit

        viewModel.removeFriend("user1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.removeFriend("user1") }
    }
}
