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

package io.getstream.video.android.robots

import io.getstream.video.android.pages.CallPage
import io.getstream.video.android.uiautomator.isDisplayed
import io.getstream.video.android.uiautomator.waitForText
import io.getstream.video.android.uiautomator.waitToAppear
import org.junit.Assert.assertTrue

fun UserRobot.assertCallControls(microphone: Boolean, camera: Boolean): UserRobot {
    assertTrue(CallPage.callSettingsClosedToggle.waitToAppear().isDisplayed())
    assertTrue(CallPage.hangUpButton.isDisplayed())
    assertTrue(CallPage.chatButton.isDisplayed())
    assertTrue(CallPage.cameraPositionToggleFront.isDisplayed())

    if (microphone) {
        assertTrue(CallPage.microphoneEnabledToggle.isDisplayed())
    } else {
        assertTrue(CallPage.microphoneDisabledToggle.isDisplayed())
    }

    if (camera) {
        assertTrue(CallPage.cameraEnabledToggle.isDisplayed())
    } else {
        assertTrue(CallPage.cameraDisabledToggle.isDisplayed())
    }

    return this
}

fun UserRobot.assertParticipantsCountOnCall(count: Int): UserRobot {
    val user = 1
    val participants = user + count
    CallPage.participantsCountBadge
        .waitToAppear()
        .waitForText(expectedText = participants.toString())
    return this
}
