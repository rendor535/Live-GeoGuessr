package com.example.livegeoguessr.acceptance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.livegeoguessr.MainActivity
import com.example.livegeoguessr.ui.testing.TestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AuthAcceptanceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun openRegistrationScreen_clickRegisterLink_displaysRegistrationScreen() {
        composeRule
            .onNodeWithTag(TestTags.REGISTER_LINK)
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNodeWithTag(TestTags.REGISTER_SCREEN)
            .assertIsDisplayed()
    }
}