package com.example.livegeoguessr.ui.screens.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockNavController = mockk<NavController>(relaxed = true)
    private val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    private val uiStateFlow = MutableStateFlow(ProfileUiState())

    init {
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @Test
    fun profileInfo_isDisplayed() {
        uiStateFlow.value = ProfileUiState(
            displayName = "Explorer",
            pointsTotal = 1234,
            friendsCount = 5,
            postsCount = 10,
            guessesCount = 15
        )

        composeTestRule.setContent {
            ProfileScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Explorer").assertIsDisplayed()
        composeTestRule.onNodeWithText("1234").assertIsDisplayed()
        
        // Stats cards
        composeTestRule.onNodeWithText("Friends", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Posts", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Guesses", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun clickingSettings_navigatesToSettings() {
        composeTestRule.setContent {
            ProfileScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // The settings icon has contentDescription "Settings"
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        verify { mockNavController.navigate("settings") }
    }

    @Test
    fun clickingDisplayName_showsChangeNameDialog() {
        uiStateFlow.value = ProfileUiState(displayName = "Explorer")

        composeTestRule.setContent {
            ProfileScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Explorer").performClick()

        composeTestRule.onNodeWithText("Change Name", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Display Name", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun changingDisplayName_callsViewModelSave() {
        uiStateFlow.value = ProfileUiState(displayName = "Explorer")

        composeTestRule.setContent {
            ProfileScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Explorer").performClick()

        // Change text
        composeTestRule.onNodeWithText("Display Name", ignoreCase = true).performTextReplacement("New Explorer")
        
        // Click Save
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()

        verify { mockViewModel.updateDisplayName("New Explorer") }
        verify { mockViewModel.saveProfile() }
    }

    @Test
    fun clickingAddFriend_navigatesToAddFriend() {
        composeTestRule.setContent {
            ProfileScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Add a Friend", ignoreCase = true).performClick()

        verify { mockNavController.navigate("add_friend") }
    }
}
