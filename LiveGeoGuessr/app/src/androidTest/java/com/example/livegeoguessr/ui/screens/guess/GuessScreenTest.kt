package com.example.livegeoguessr.ui.screens.guess

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import com.example.livegeoguessr.domain.model.SubmitGuessResult

class GuessScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel = mockk<GuessViewModel>(relaxed = true)
    private val uiStateFlow = MutableStateFlow(GuessUiState())
    private val useMilesFlow = MutableStateFlow(false)

    init {
        every { mockViewModel.guessUiState } returns uiStateFlow
        every { mockViewModel.useMiles } returns useMilesFlow
    }

    @Test
    fun initiallyShowsFullScreenImage() {
        composeTestRule.setContent {
            GuessScreen(
                postId = "post1",
                imageUrl = "http://example.com/image.jpg",
                viewModel = mockViewModel
            )
        }

        // Full screen image description should be present
        composeTestRule.onNodeWithContentDescription("Full Screen Image", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun clickingFullScreenImage_minimizesIt() {
        composeTestRule.setContent {
            GuessScreen(
                postId = "post1",
                imageUrl = "http://example.com/image.jpg",
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Full Screen Image", ignoreCase = true).performClick()

        // Now it should be a preview image
        composeTestRule.onNodeWithContentDescription("Preview", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun confirmButton_appearsWhenLocationSelected() {
        uiStateFlow.value = GuessUiState(selectedLatitude = 1.0, selectedLongitude = 2.0)

        composeTestRule.setContent {
            GuessScreen(
                postId = "post1",
                imageUrl = "http://example.com/image.jpg",
                viewModel = mockViewModel
            )
        }

        // Make sure image is not full screen so buttons are visible
        composeTestRule.onNodeWithContentDescription("Full Screen Image", ignoreCase = true).performClick()

        composeTestRule.onNodeWithText("Confirm", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun clickingConfirm_callsViewModelSubmit() {
        uiStateFlow.value = GuessUiState(selectedLatitude = 1.0, selectedLongitude = 2.0)

        composeTestRule.setContent {
            GuessScreen(
                postId = "post1",
                imageUrl = "http://example.com/image.jpg",
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Full Screen Image", ignoreCase = true).performClick()
        composeTestRule.onNodeWithText("Confirm", ignoreCase = true).performClick()

        verify { mockViewModel.submitGuess("post1") }
    }

    @Test
    fun showingResult_displaysPointsAndDistance() {
        val mockResult = mockk<SubmitGuessResult>(relaxed = true)
        every { mockResult.points } returns 4500
        every { mockResult.distanceMeters } returns 1200.0

        uiStateFlow.value = GuessUiState(
            result = mockResult
        )

        composeTestRule.setContent {
            GuessScreen(
                postId = "post1",
                imageUrl = "http://example.com/image.jpg",
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Full Screen Image", ignoreCase = true).performClick()

        // Use substring match for points to be more robust
        composeTestRule.onNodeWithText("4500", substring = true).assertIsDisplayed()
        // Match integer part and unit to avoid decimal separator issues (1.20 vs 1,20)
        composeTestRule.onNodeWithText("1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("km", substring = true).assertIsDisplayed()
    }

    @Test
    fun showingResultInMiles_displaysDistanceInMiles() {
        useMilesFlow.value = true
        val mockResult = mockk<SubmitGuessResult>(relaxed = true)
        every { mockResult.points } returns 4500
        every { mockResult.distanceMeters } returns 1609.34 // 1 mile

        uiStateFlow.value = GuessUiState(
            result = mockResult
        )

        composeTestRule.setContent {
            GuessScreen(
                postId = "post1",
                imageUrl = "http://example.com/image.jpg",
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Full Screen Image", ignoreCase = true).performClick()

        // Match integer part and unit
        composeTestRule.onNodeWithText("1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("mi", substring = true).assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        val errorMessage = "Failed to load guess"
        uiStateFlow.value = GuessUiState(errorMessage = errorMessage)

        composeTestRule.setContent {
            GuessScreen(
                postId = "post1",
                imageUrl = "http://example.com/image.jpg",
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Full Screen Image", ignoreCase = true).performClick()

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun submittingState_showsLoadingIndicatorOnButton() {
        uiStateFlow.value = GuessUiState(
            selectedLatitude = 1.0,
            selectedLongitude = 2.0,
            isSubmitting = true
        )

        composeTestRule.setContent {
            GuessScreen(
                postId = "post1",
                imageUrl = "http://example.com/image.jpg",
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Full Screen Image", ignoreCase = true).performClick()

        // The button should show "Submitting..." (note the ellipsis character \u2026)
        composeTestRule.onNodeWithText("Submitting", substring = true, ignoreCase = true).assertIsDisplayed()
        // And an indeterminate progress indicator
        composeTestRule.onNode(hasProgressBarRangeInfo(androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate)).assertExists()
    }
}
