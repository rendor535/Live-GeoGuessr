package com.example.livegeoguessr.ui.screens.guess

import com.example.livegeoguessr.data.repository.GuessRepository
import com.example.livegeoguessr.data.repository.SettingsRepository
import com.example.livegeoguessr.domain.model.SubmitGuessResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuessViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var guessRepository: GuessRepository
    private lateinit var viewModel: GuessViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        settingsRepository = mockk {
            every { useMilesFlow } returns MutableStateFlow(false)
        }

        guessRepository = mockk(relaxed = true)

        viewModel = GuessViewModel(
            repository = settingsRepository,
            guessRepository = guessRepository
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectLocation updates state`() {
        viewModel.selectLocation(10.0, 20.0)

        val state = viewModel.guessUiState.value

        assertEquals(10.0, state.selectedLatitude)
        assertEquals(20.0, state.selectedLongitude)
    }

    @Test
    fun `loadGuessStatus success updates state`() = runTest {
        coEvery {
            guessRepository.getGuessMapPreview(any(), any())
        } returns mockk {
            every { initialMapCenterLatitude } returns 1.0
            every { initialMapCenterLongitude } returns 2.0
            every { initialMapDiameterMeters } returns 1000.0
        }

        coEvery {
            guessRepository.getMyGuessForPost(any())
        } returns null

        viewModel.loadGuessStatus("post1")

        runCurrent()

        val state = viewModel.guessUiState.value

        assertEquals(1.0, state.initialMapCenterLatitude)
        assertEquals(2.0, state.initialMapCenterLongitude)
        assertEquals(1000.0, state.initialMapDiameterMeters)
    }

    @Test
    fun `loadGuessStatus failure sets error`() = runTest {
        coEvery {
            guessRepository.getGuessMapPreview(any(), any())
        } throws RuntimeException("load error")

        viewModel.loadGuessStatus("post1")

        runCurrent()

        assertEquals(
            "load error",
            viewModel.guessUiState.value.errorMessage
        )
    }

    @Test
    fun `submitGuess without selection shows error`() {
        viewModel.submitGuess("post1")

        assertEquals(
            "Please select a location on the map first.",
            viewModel.guessUiState.value.errorMessage
        )
    }

    @Test
    fun `submitGuess success updates result`() = runTest {
        viewModel.selectLocation(10.0, 20.0)

        val result = mockk<SubmitGuessResult>(relaxed = true)

        coEvery {
            guessRepository.submitGuess(any(), any(), any(), any())
        } returns result

        viewModel.submitGuess("post1")

        runCurrent()

        val state = viewModel.guessUiState.value

        assertNotNull(state.result)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `submitGuess Firebase ALREADY_EXISTS maps message`() = runTest {
        viewModel.selectLocation(10.0, 20.0)

        coEvery {
            guessRepository.submitGuess(any(), any(), any(), any())
        } throws RuntimeException("exists")

        viewModel.submitGuess("post1")

        runCurrent()

        assertEquals(
            "exists",
            viewModel.guessUiState.value.errorMessage
        )
    }

    @Test
    fun `submitGuess generic exception sets error message`() = runTest {
        viewModel.selectLocation(10.0, 20.0)

        coEvery {
            guessRepository.submitGuess(any(), any(), any(), any())
        } throws RuntimeException("network error")

        viewModel.submitGuess("post1")

        runCurrent()

        assertEquals(
            "network error",
            viewModel.guessUiState.value.errorMessage
        )
    }
}