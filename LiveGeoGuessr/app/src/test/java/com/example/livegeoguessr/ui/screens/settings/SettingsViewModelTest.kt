package com.example.livegeoguessr.ui.screens.settings

import com.example.livegeoguessr.data.repository.AuthRepository
import com.example.livegeoguessr.data.repository.SettingsRepository
import com.example.livegeoguessr.data.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val settingsRepository = mockk<SettingsRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val userRepository = mockk<UserRepository>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { settingsRepository.darkModeFlow } returns MutableStateFlow(false)
        every { settingsRepository.isLoggedInFlow } returns MutableStateFlow(true)
        every { settingsRepository.useMilesFlow } returns MutableStateFlow(false)

        // safe default to prevent init crash
        every { authRepository.currentUid() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            settingsRepository,
            authRepository,
            userRepository
        )
    }

    @Test
    fun `init loads profile image when user exists`() = runTest {
        every { authRepository.currentUid() } returns "uid-1"

        coEvery { userRepository.getUserProfile("uid-1") } returns
                mockk { every { photoUrl } returns "image-url" }

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("image-url", vm.profileImageUrl.value)
    }

    @Test
    fun `init sets null when user is null`() = runTest {
        every { authRepository.currentUid() } returns null

        val vm = createViewModel()
        advanceUntilIdle()

        assertNull(vm.profileImageUrl.value)
    }

    @Test
    fun `updateDarkMode calls repository`() = runTest {
        coEvery { settingsRepository.setDarkMode(true) } just Runs

        val vm = createViewModel()
        vm.updateDarkMode(true)

        advanceUntilIdle()

        coVerify { settingsRepository.setDarkMode(true) }
    }

    @Test
    fun `updateUseMiles calls repository`() = runTest {
        coEvery { settingsRepository.setUseMiles(true) } just Runs

        val vm = createViewModel()
        vm.updateUseMiles(true)

        advanceUntilIdle()

        coVerify { settingsRepository.setUseMiles(true) }
    }

    @Test
    fun `logout sets logged out state and calls dependencies`() = runTest {
        coEvery { authRepository.logout() } just Runs
        coEvery { settingsRepository.setLoggedIn(false) } just Runs

        val vm = createViewModel()
        vm.logout()

        advanceUntilIdle()

        assertTrue(vm.isLoggedOut.value)

        coVerify { authRepository.logout() }
        coVerify { settingsRepository.setLoggedIn(false) }
    }

    @Test
    fun `deleteAccount triggers logout flow`() = runTest {
        coEvery { authRepository.logout() } just Runs
        coEvery { settingsRepository.setLoggedIn(false) } just Runs

        val vm = createViewModel()
        vm.deleteAccount()

        advanceUntilIdle()

        assertTrue(vm.isLoggedOut.value)

        coVerify { authRepository.logout() }
        coVerify { settingsRepository.setLoggedIn(false) }
    }
}