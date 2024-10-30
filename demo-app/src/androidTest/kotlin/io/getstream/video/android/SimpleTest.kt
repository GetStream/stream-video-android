package io.getstream.video.android

import android.Manifest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.platform.ServiceLoaderWrapper
import androidx.test.internal.platform.content.PermissionGranter
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.permission.PermissionRequester
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimpleTest {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    @get:Rule
    val testRule = launchAppTestRule()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val automatorTestRule = AutomatorTestRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun test() {
        testRule.activity.launchActivity()

        composeTestRule.waitUntilAtLeastOneExists(hasText("Stream", substring = true))
        composeTestRule.waitUntilAtLeastOneExists(hasText("[Video Calling]", substring = true))
        composeTestRule.waitUntilAtLeastOneExists(hasText("Join Call", substring = true))

        composeTestRule.onNodeWithText("Join Call", substring = true)
            .performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasText("Set up your test call", substring = true))

        composeTestRule.onNodeWithText("Start a test call", substring = true)
            .performClick()

        // Wait to see what happens after click
        composeTestRule.waitUntil(timeoutMillis = 20000) { false }
    }


}