package com.example.livegeoguessr.ui.screens.settings

import android.net.Uri
import com.example.livegeoguessr.R
import com.example.livegeoguessr.data.repository.AuthRepository
import com.example.livegeoguessr.data.repository.SettingsRepository
import com.example.livegeoguessr.data.repository.UserRepository
import com.example.livegeoguessr.domain.model.UserProfile
import com.example.livegeoguessr.domain.model.UserStats
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val authRepository = mockk<AuthRepository>()
    private val userRepository = mockk<UserRepository>()
    private val settingsRepository = mockk<SettingsRepository>()

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun createProfile(
        nickname: String = "Test",
        photoUrl: String? = "url",
        stats: UserStats = UserStats(
            pointsTotal = 10,
            guessesCount = 2,
            postsCount = 1,
            friendsCount = 3,
            bestGuessMeters = null
        )
    ) = UserProfile(
        uid = "user123",
        email = "test@test.com",
        nickname = nickname,
        photoUrl = photoUrl,
        avatarPath = null,
        isBanned = false,
        stats = stats,
    )

    @Test
    fun `init loads user profile successfully`() = runTest {
        coEvery { authRepository.currentUid() } returns "user123"
        coEvery { userRepository.getUserProfile("user123") } returns createProfile()

        val vm = ProfileViewModel(authRepository, userRepository, settingsRepository)

        advanceUntilIdle()

        assertEquals("Test", vm.uiState.value.displayName)
    }

    @Test
    fun `init loads user profile when user is null`() = runTest {
        coEvery { authRepository.currentUid() } returns null

        val vm = ProfileViewModel(authRepository, userRepository, settingsRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(R.string.user_not_found, vm.uiState.value.errorResId)
    }

    @Test
    fun `updateProfilePicture success updates url`() = runTest {
        coEvery { authRepository.currentUid() } returns "user123"
        coEvery { userRepository.updateProfilePicture(any(), any()) } returns "newUrl"
        coEvery { userRepository.getUserProfile(any()) } returns createProfile()

        val vm = ProfileViewModel(authRepository, userRepository, settingsRepository)

        vm.updateProfilePicture(mockk<Uri>())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("newUrl", vm.uiState.value.profileImageUrl)
        assertTrue(vm.uiState.value.isSuccess)
    }

    @Test
    fun `updateProfilePicture failure sets error`() = runTest {
        coEvery { authRepository.currentUid() } returns "user123"
        coEvery { userRepository.updateProfilePicture(any(), any()) } throws RuntimeException()
        coEvery { userRepository.getUserProfile(any()) } returns createProfile()

        val vm = ProfileViewModel(authRepository, userRepository, settingsRepository)

        vm.updateProfilePicture(mockk<Uri>())
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorResId)
    }

    @Test
    fun `logout sets logged out state`() = runTest {
        coEvery { authRepository.logout() } just Runs
        coEvery { settingsRepository.setLoggedIn(false) } just Runs
        coEvery { authRepository.currentUid() } returns "user123"
        coEvery { userRepository.getUserProfile(any()) } returns createProfile()

        val vm = ProfileViewModel(authRepository, userRepository, settingsRepository)

        vm.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoggedOut)
    }
}