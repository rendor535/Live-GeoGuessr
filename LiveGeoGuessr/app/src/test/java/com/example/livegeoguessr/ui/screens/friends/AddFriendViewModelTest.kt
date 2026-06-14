package com.example.livegeoguessr.ui.screens.friends

import com.example.livegeoguessr.data.repository.FriendRepository
import com.example.livegeoguessr.domain.model.UserProfile
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddFriendViewModelTest {

    private lateinit var repository: FriendRepository
    private lateinit var viewModel: AddFriendViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = AddFriendViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun fakeUser(uid: String, nickname: String = "nick"): UserProfile {
        return mockk {
            every { this@mockk.uid } returns uid
            every { displayName } returns "Display $uid"
            every { this@mockk.nickname } returns nickname
            every { photoUrl } returns null
            every { stats } returns mockk {
                every { pointsTotal } returns 100
            }
        }
    }

    @Test
    fun `init loads users successfully`() = runTest {
        coEvery { repository.getUsersForFriendSearch() } returns listOf(
            fakeUser("1"),
            fakeUser("2")
        )

        createViewModel()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(2, state.users.size)
    }

    @Test
    fun `init handles error`() = runTest {
        coEvery { repository.getUsersForFriendSearch() } throws RuntimeException("load error")

        createViewModel()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals("load error", state.errorMessage)
    }

    @Test
    fun `onQueryChange filters users`() = runTest {
        coEvery { repository.getUsersForFriendSearch() } returns listOf(
            fakeUser("1", "john"),
            fakeUser("2", "mike")
        )

        createViewModel()

        viewModel.onQueryChange("jo")

        val state = viewModel.uiState.value

        assertEquals("jo", state.query)
        assertEquals(1, state.filteredUsers.size)
    }

    @Test
    fun `sendFriendRequest success marks requestSent`() = runTest {
        val user = fakeUser("1")

        coEvery { repository.getUsersForFriendSearch() } returns listOf(user)
        coEvery { repository.sendFriendRequest("1") } returns Unit

        createViewModel()

        viewModel.sendFriendRequest("1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        val updatedUser = state.users.first()

        assertTrue(updatedUser.requestSent)
        assertFalse(updatedUser.isSending)

        coVerify { repository.sendFriendRequest("1") }
    }

    @Test
    fun `sendFriendRequest failure resets isSending and sets error`() = runTest {
        val user = fakeUser("1")

        coEvery { repository.getUsersForFriendSearch() } returns listOf(user)
        coEvery { repository.sendFriendRequest("1") } throws RuntimeException("fail")

        createViewModel()

        viewModel.sendFriendRequest("1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val updatedUser = state.users.first()

        assertFalse(updatedUser.isSending)
        assertEquals("fail", state.errorMessage)
    }

    @Test
    fun `init sets filteredUsers empty when query blank`() = runTest {
        coEvery { repository.getUsersForFriendSearch() } returns listOf(fakeUser("1"))

        createViewModel()

        val state = viewModel.uiState.value

        assertTrue(state.filteredUsers.isEmpty())
    }
}