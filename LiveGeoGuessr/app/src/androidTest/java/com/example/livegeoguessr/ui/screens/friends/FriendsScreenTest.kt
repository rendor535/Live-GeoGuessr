package com.example.livegeoguessr.ui.screens.friends

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavController
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class FriendsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockNavController = mockk<NavController>(relaxed = true)
    private val mockViewModel = mockk<FriendsViewModel>(relaxed = true)
    private val uiStateFlow = MutableStateFlow(FriendsUiState())

    init {
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @Test
    fun loadingState_showsCircularProgressIndicator() {
        uiStateFlow.value = FriendsUiState(isLoading = true)

        composeTestRule.setContent {
            FriendsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // CircularProgressIndicator has Indeterminate range info
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun emptyState_showsNoFriendsYetMessage() {
        uiStateFlow.value = FriendsUiState(friends = emptyList(), isLoading = false)

        composeTestRule.setContent {
            FriendsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // Searching for "No friends yet"
        composeTestRule.onNodeWithText("No friends yet", ignoreCase = true).assertExists()
    }

    @Test
    fun errorState_showsErrorMessage() {
        val errorMessage = "Failed to load friends"
        uiStateFlow.value = FriendsUiState(errorMessage = errorMessage)

        composeTestRule.setContent {
            FriendsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun successState_showsFriendsAndPoints() {
        val friends = listOf(
            FriendUI(
                id = "1",
                displayName = "Alice",
                profileImageUrl = null,
                isOnline = true,
                points = 1234
            )
        )
        uiStateFlow.value = FriendsUiState(friends = friends)

        composeTestRule.setContent {
            FriendsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Alice").assertExists()
        composeTestRule.onNodeWithText("1234", substring = true).assertExists()
        composeTestRule.onNodeWithText("Online", ignoreCase = true).assertExists()
    }

    @Test
    fun incomingRequests_showsAcceptAndRejectButtons() {
        val requests = listOf(
            FriendRequestUI(
                id = "req1",
                userUid = "u1",
                displayName = "Requester",
                nickname = "req_nick",
                profileImageUrl = null
            )
        )
        uiStateFlow.value = FriendsUiState(incomingRequests = requests)

        composeTestRule.setContent {
            FriendsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Requester").assertExists()
        composeTestRule.onNodeWithText("Accept", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Reject", ignoreCase = true).assertExists()
    }
    
    @Test
    fun outgoingRequests_showsPendingStatus() {
        val requests = listOf(
            FriendRequestUI(
                id = "req2",
                userUid = "u2",
                displayName = "SentTo",
                nickname = "sent_nick",
                profileImageUrl = null
            )
        )
        uiStateFlow.value = FriendsUiState(outgoingRequests = requests)

        composeTestRule.setContent {
            FriendsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("SentTo").assertExists()
        composeTestRule.onNodeWithText("Pending", ignoreCase = true).assertExists()
    }
}
