package com.example.livegeoguessr.ui.screens.camera

import android.graphics.Bitmap
import com.example.livegeoguessr.data.repository.PostRepository
import com.example.livegeoguessr.domain.model.Post
import com.google.android.gms.maps.model.LatLng
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private lateinit var repository: PostRepository
    private lateinit var viewModel: CameraViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = CameraViewModel(repository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uploadPost success updates state to uploaded`() = runTest {
        val bitmap = mockk<Bitmap>(relaxed = true)
        val location = LatLng(1.0, 2.0)

        val fakePost = mockk<Post>(relaxed = true)

        coEvery {
            repository.addPost(any(), any(), any())
        } returns fakePost

        viewModel.uploadPost(bitmap, location)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uploadState.value

        assertFalse(state.isUploading)
        assertTrue(state.isUploaded)
        assertNull(state.errorMessage)

        coVerify {
            repository.addPost(bitmap, location.latitude, location.longitude)
        }
    }

    @Test
    fun `uploadPost failure sets error state`() = runTest {
        val bitmap = mockk<Bitmap>(relaxed = true)
        val location = LatLng(1.0, 2.0)

        coEvery {
            repository.addPost(any(), any(), any())
        } throws RuntimeException("Network error")

        viewModel.uploadPost(bitmap, location)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uploadState.value

        assertFalse(state.isUploading)
        assertFalse(state.isUploaded)
        assertEquals("Network error", state.errorMessage)
    }

    @Test
    fun `resetUploadState clears state`() {
        viewModel.resetUploadState()

        val state = viewModel.uploadState.value

        assertFalse(state.isUploading)
        assertFalse(state.isUploaded)
        assertNull(state.errorMessage)
    }
}