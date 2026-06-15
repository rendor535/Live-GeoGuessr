package com.example.livegeoguessr.ui.screens.posts

import com.example.livegeoguessr.data.repository.PostRepository
import com.example.livegeoguessr.domain.model.Post
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
class PostsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var postRepository: PostRepository
    private lateinit var viewModel: PostsViewModel

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
    fun `init loads posts successfully`() = runTest {
        val fakePosts = listOf<Post>(mockk(), mockk())

        coEvery { postRepository.getMyPosts() } returns fakePosts

        viewModel = PostsViewModel(postRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(2, state.posts.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun `init sets error when repository fails`() = runTest {
        coEvery { postRepository.getMyPosts() } throws RuntimeException("network error")

        viewModel = PostsViewModel(postRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals("network error", state.errorMessage)
        assertTrue(state.posts.isEmpty())
    }

    @Test
    fun `loading state is set during fetch`() = runTest {
        coEvery {
            postRepository.getMyPosts()
        } coAnswers {
            delay(50)
            emptyList()
        }

        viewModel = PostsViewModel(postRepository)

        // allow coroutine to start
        testDispatcher.scheduler.advanceTimeBy(10)

        assertTrue(viewModel.uiState.value.isLoading)

        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
    }
}