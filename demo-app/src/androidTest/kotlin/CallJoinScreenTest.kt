/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android

import android.content.Intent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CallJoinScreenTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val testCallId = "123-456"
    private val callIdTextFieldTag = "call_id_text_field"

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun callIdTextField_appLaunchIntentWithCallIdExtra_containsLaunchIntentCallId() {
        // Launch with a call ID
        val intent = Intent(composeTestRule.activity, MainActivity::class.java).apply {
            putExtra(EXTRA_CALL_ID, testCallId)
        }

        ActivityScenario.launch<MainActivity>(intent)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(callIdTextFieldTag).assertTextEquals(testCallId)
    }

    @Test
    fun callIdTextField_appLaunchIntentWithoutCallIdExtra_containsDefaultCallId() {
        // Launch without a call ID
        val intent = Intent(composeTestRule.activity, MainActivity::class.java)

        ActivityScenario.launch<MainActivity>(intent)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(callIdTextFieldTag).assertTextEquals(defaultCallId)
    }
}
