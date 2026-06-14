package com.example.livegeoguessr.acceptance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.livegeoguessr.MainActivity
import com.example.livegeoguessr.data.repository.SettingsRepository
import com.example.livegeoguessr.ui.testing.TestTags
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AuthAcceptanceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var auth: FirebaseAuth

    @Before
    fun setUp() {
        hiltRule.inject()

        auth.signOut()

        runBlocking {
            settingsRepository.setLoggedIn(false)
        }

        waitForTag(TestTags.LOGIN_SCREEN)
    }

    private fun waitForTag(
        tag: String,
        timeoutMillis: Long = 10_000
    ) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule
                .onAllNodesWithTag(
                    testTag = tag,
                    useUnmergedTree = true
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun openRegistrationScreen() {
        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_LINK,
                useUnmergedTree = true
            )
            .assertIsDisplayed()
            .performClick()

        waitForTag(TestTags.REGISTER_SCREEN)

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_SCREEN,
                useUnmergedTree = true
            )
            .assertIsDisplayed()
    }

    @Test
    fun openRegistrationScreen_clickRegisterLink_displaysRegistrationForm() {
        openRegistrationScreen()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_EMAIL,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_PASSWORD,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_BUTTON,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(
                TestTags.LOGIN_LINK,
                useUnmergedTree = true
            )
            .assertIsDisplayed()
    }

    @Test
    fun registerWithEmail_invalidData_showsErrorAndStaysOnRegistrationScreen() {
        openRegistrationScreen()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_EMAIL,
                useUnmergedTree = true
            )
            .performTextInput("incorrect-email")

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_PASSWORD,
                useUnmergedTree = true
            )
            .performTextInput("Password123!")

        closeSoftKeyboard()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_BUTTON,
                useUnmergedTree = true
            )
            .performClick()

        waitForTag(
            tag = TestTags.AUTH_ERROR,
            timeoutMillis = 15_000
        )

        composeRule
            .onNodeWithTag(
                TestTags.AUTH_ERROR,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_SCREEN,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_EMAIL,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_BUTTON,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        assertNull(auth.currentUser)
    }

    @Test
    fun registerWithEmail_validData_createsAccountAndDisplaysHomeScreen() {
        val email =
            "acceptance-${System.currentTimeMillis()}@example.com"

        val password = "Password123!"

        openRegistrationScreen()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_EMAIL,
                useUnmergedTree = true
            )
            .performTextInput(email)

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_PASSWORD,
                useUnmergedTree = true
            )
            .performTextInput(password)

        closeSoftKeyboard()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(
                TestTags.REGISTER_BUTTON,
                useUnmergedTree = true
            )
            .performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            auth.currentUser?.email == email
        }

        waitForTag(
            tag = TestTags.HOME_SCREEN,
            timeoutMillis = 15_000
        )

        composeRule
            .onNodeWithTag(
                TestTags.HOME_SCREEN,
                useUnmergedTree = true
            )
            .assertIsDisplayed()

        val currentUser = auth.currentUser

        assertNotNull(currentUser)
        assertEquals(email, currentUser?.email)
    }
}