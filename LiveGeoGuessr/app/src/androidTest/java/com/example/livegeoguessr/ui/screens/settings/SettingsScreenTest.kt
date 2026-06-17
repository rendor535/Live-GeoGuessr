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

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockNavController = mockk<NavController>(relaxed = true)
    private val mockViewModel = mockk<SettingsViewModel>(relaxed = true)

    private val darkModeFlow = MutableStateFlow(false)
    private val useMilesFlow = MutableStateFlow(false)
    private val isLoggedOutFlow = MutableStateFlow(false)

    init {
        every { mockViewModel.darkMode } returns darkModeFlow
        every { mockViewModel.useMiles } returns useMilesFlow
        every { mockViewModel.isLoggedOut } returns isLoggedOutFlow
    }

    @Test
    fun settingsItems_areDisplayed() {
        composeTestRule.setContent {
            SettingsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Dark Mode", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Use Miles", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Log out", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete Account", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun toggleDarkMode_callsViewModel() {
        composeTestRule.setContent {
            SettingsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // Find the switch for Dark Mode. 
        // SettingsItem uses Row with semantics mergeDescendants = true, so we can find it by text or use a more specific matcher.
        composeTestRule.onNodeWithText("Dark Mode", ignoreCase = true).performClick()

        verify { mockViewModel.updateDarkMode(any()) }
    }

    @Test
    fun logoutButton_callsViewModelLogout() {
        composeTestRule.setContent {
            SettingsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Log out", ignoreCase = true).performClick()

        verify { mockViewModel.logout() }
    }

    @Test
    fun deleteAccount_showsConfirmationDialog() {
        composeTestRule.setContent {
            SettingsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Delete Account", ignoreCase = true).performClick()

        // Check if dialog is shown
        composeTestRule.onNodeWithText("Delete account", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete your account?", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun deleteAccountDialog_confirmEnabledOnlyAfterTypingDELETE() {
        composeTestRule.setContent {
            SettingsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Delete Account", ignoreCase = true).performClick()

        // The "Delete" button in the dialog should be disabled initially
        // There are two "Delete" texts: one on the button that opens the dialog, and one in the dialog title/button.
        // We use useUnmergedTree = true or check for the specific button.
        composeTestRule.onNode(hasText("Delete", ignoreCase = true) and hasClickAction()).assertIsNotEnabled()

        // Type something else
        composeTestRule.onNodeWithText("Type DELETE to confirm", ignoreCase = true).performTextInput("DELET")
        composeTestRule.onNode(hasText("Delete", ignoreCase = true) and hasClickAction()).assertIsNotEnabled()

        // Type DELETE
        composeTestRule.onNodeWithText("Type DELETE to confirm", ignoreCase = true).performTextReplacement("DELETE")
        composeTestRule.onNode(hasText("Delete", ignoreCase = true) and hasClickAction()).assertIsEnabled()
        
        composeTestRule.onNode(hasText("Delete", ignoreCase = true) and hasClickAction()).performClick()
        verify { mockViewModel.deleteAccount() }
    }
}
