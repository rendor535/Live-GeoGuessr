package com.example.livegeoguessr.ui.screens.camera

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class CameraScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel = mockk<CameraViewModel>(relaxed = true)
    private val uploadStateFlow = MutableStateFlow(CameraUploadState())

    init {
        every { mockViewModel.uploadState } returns uploadStateFlow
    }

    @Test
    fun cameraContent_initiallyShowsCameraPreview() {
        composeTestRule.setContent {
            CameraContent(viewModel = mockViewModel)
        }

        composeTestRule.onNodeWithContentDescription("Take Photo", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun photoCaptured_showsConfirmButton() {
        val mockBitmap = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
        
        composeTestRule.setContent {
            CameraContent(
                viewModel = mockViewModel,
                initialBitmap = mockBitmap
            )
        }

        composeTestRule.onNodeWithContentDescription("Confirm photo", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Cancel and retake", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Captured Photo", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun photoConfirmed_showsPostButton() {
        val mockBitmap = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)

        composeTestRule.setContent {
            CameraContent(
                viewModel = mockViewModel,
                initialBitmap = mockBitmap,
                initialConfirmed = true
            )
        }

        // When confirmed, it shows LocationPreview which is supposed to acquire location.
        // Once location is acquired (simulated here if possible, but let's check UI), 
        // the Post button should be enabled.
        
        // The Post button has content description "Post"
        composeTestRule.onNodeWithContentDescription("Post", ignoreCase = true).assertExists()
    }

    @Test
    fun uploadingState_showsCircularProgressIndicator() {
        val mockBitmap = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
        uploadStateFlow.value = CameraUploadState(isUploading = true)

        composeTestRule.setContent {
            CameraContent(
                viewModel = mockViewModel,
                initialBitmap = mockBitmap,
                initialConfirmed = true
            )
        }

        composeTestRule.onNode(hasProgressBarRangeInfo(androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun errorState_showsErrorMessage() {
        val errorMessage = "Upload failed"
        val mockBitmap = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
        uploadStateFlow.value = CameraUploadState(errorMessage = errorMessage)

        composeTestRule.setContent {
            CameraContent(
                viewModel = mockViewModel,
                initialBitmap = mockBitmap,
                initialConfirmed = true
            )
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }
}
