package com.example.livegeoguessr.ui.screens.guessedposts

import com.example.livegeoguessr.data.repository.PostRepository
import com.example.livegeoguessr.domain.model.GuessedPost
import com.example.livegeoguessr.ui.state.ScreenState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuessedPostsViewModelTest {

    private lateinit var postRepository: PostRepository
    private lateinit var viewModel: GuessedPostsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        postRepository = mockk(relaxed = true)

        viewModel = GuessedPostsViewModel(postRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): GuessedPostsViewModel {
        return GuessedPostsViewModel(postRepository)
    }

    @Test
    fun `init loads guessed posts successfully`() = runTest {
        val posts = listOf(
            mockk<GuessedPost>(),
            mockk<GuessedPost>()
        )

        coEvery {
            postRepository.getMyGuessedPosts()
        } returns posts

        viewModel = createViewModel()

        runCurrent()

        val state = viewModel.uiState.value

        assertTrue(state is ScreenState.Content)
        assertEquals(posts, (state as ScreenState.Content).data)
    }

    @Test
    fun `loadGuessedPosts sets loading state then success`() = runTest {
        val posts = listOf(mockk<GuessedPost>())

        coEvery {
            postRepository.getMyGuessedPosts()
        } returns posts

        viewModel = createViewModel()

        viewModel.loadGuessedPosts()

        runCurrent()

        val state = viewModel.uiState.value

        assertTrue(state is ScreenState.Content)
        assertEquals(posts, (state as ScreenState.Content).data)
    }

    @Test
    fun `loadGuessedPosts failure sets error state`() = runTest {

        coEvery {
            postRepository.getMyGuessedPosts()
        } throws RuntimeException("network error")

        viewModel = createViewModel()

        viewModel.loadGuessedPosts()

        runCurrent()

        val state = viewModel.uiState.value

        assertTrue(state is ScreenState.Error)
        assertEquals("network error", (state as ScreenState.Error).message)
    }

    @Test
    fun `loadGuessedPosts sets loading true initially`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        Dispatchers.setMain(testDispatcher)

        coEvery {
            postRepository.getMyGuessedPosts()
        } coAnswers {
            delay(50)
            emptyList()
        }

        viewModel = createViewModel()
        viewModel.loadGuessedPosts()

        runCurrent()

        assertTrue(viewModel.uiState.value is ScreenState.Loading)
        advanceUntilIdle()
    }
}