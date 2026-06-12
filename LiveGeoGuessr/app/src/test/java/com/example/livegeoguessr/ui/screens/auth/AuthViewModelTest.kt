package com.example.livegeoguessr.ui.screens.auth

import com.example.livegeoguessr.data.repository.AuthRepository
import com.example.livegeoguessr.data.repository.SettingsRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val viewModel = AuthViewModel(authRepository, settingsRepository)

    @Test
    fun getUiState() {
        val viewModel = AuthViewModel(authRepository, settingsRepository)

        val state = viewModel.uiState.value

        assertEquals(true, state.isLoginMode)
        assertEquals(false, state.isLoading)
        assertNull(state.errorResId)
        assertFalse(state.isSuccess)
    }

    @Test
    fun toggleMode() {
        val initial = viewModel.uiState.value.isLoginMode

        viewModel.toggleMode()

        assertEquals(!initial, viewModel.uiState.value.isLoginMode)
        assertNull(viewModel.uiState.value.errorResId)
    }
}