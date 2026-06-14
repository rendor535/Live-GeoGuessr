package com.example.livegeoguessr.ui.screens.home

import com.example.livegeoguessr.data.repository.PostRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var postRepository: PostRepository
    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        postRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPosts success updates state`() = runTest {
        coEvery { postRepository.getPosts() } returns emptyList()

        viewModel = HomeViewModel(postRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.posts.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `loadPosts failure sets error`() = runTest {
        coEvery { postRepository.getPosts() } throws RuntimeException("network error")

        viewModel = HomeViewModel(postRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals("network error", state.error)
    }

    @Test
    fun `loadPosts sets loading true initially`() = runTest {
        coEvery {
            postRepository.getPosts()
        } coAnswers {
            delay(50)
            emptyList()
        }

        viewModel = HomeViewModel(postRepository)

        // let coroutine start but not finish
        testDispatcher.scheduler.advanceTimeBy(10)

        assertTrue(viewModel.uiState.value.isLoading)

        testDispatcher.scheduler.advanceUntilIdle()
    }
}