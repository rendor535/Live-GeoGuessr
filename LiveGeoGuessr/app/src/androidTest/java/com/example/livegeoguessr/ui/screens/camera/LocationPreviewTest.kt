package com.example.livegeoguessr.ui.screens.camera

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.android.gms.maps.model.LatLng
import org.junit.Rule
import org.junit.Test

class LocationPreviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionDenied_showsLocationAccessMessage() {
        composeTestRule.setContent {
            LocationPreview(
                onLocationAcquired = {},
                useMockPermission = true,
                mockPermissionGranted = false
            )
        }

        composeTestRule.onNodeWithText("Location Access", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant Access", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun gpsDisabled_showsGpsRequiredMessage() {
        composeTestRule.setContent {
            LocationMapContent(
                onLocationAcquired = {},
                initialGpsEnabled = false
            )
        }

        composeTestRule.onNodeWithText("GPS is turned off", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Turn on GPS", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun locationAcquired_showsMap() {
        val testLocation = LatLng(52.0, 19.0)

        composeTestRule.setContent {
            LocationMapContent(
                onLocationAcquired = {},
                initialGpsEnabled = true,
                initialLocation = testLocation
            )
        }

        // MapView is an AndroidView, so we check if the node exists. 
        // We can't easily check osmdroid content, but we can check if the CircularProgressIndicator is gone.
        composeTestRule.onNodeWithText("Acquiring location", substring = true).assertDoesNotExist()
    }
}
