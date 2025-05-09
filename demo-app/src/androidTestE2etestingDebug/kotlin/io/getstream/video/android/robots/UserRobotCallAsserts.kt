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
import io.getstream.video.android.uiautomator.findObject
import io.getstream.video.android.uiautomator.findObjects
import io.getstream.video.android.uiautomator.isDisplayed
import io.getstream.video.android.uiautomator.seconds
import io.getstream.video.android.uiautomator.waitForCount
import io.getstream.video.android.uiautomator.waitForText
import io.getstream.video.android.uiautomator.waitToAppear
import io.getstream.video.android.uiautomator.waitToDisappear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

fun UserRobot.assertThatCallIsEnded(): UserRobot {
    assertFalse(CallPage.hangUpButton.waitToDisappear().isDisplayed())
    return this
}

fun UserRobot.assertParticipantsCountOnCall(count: Int): UserRobot {
    val user = 1
    val participants = (user + count).toString()
    CallPage.participantsCountBadge
        .waitToAppear()
        .waitForText(expectedText = participants)
    assertEquals(participants, CallPage.participantsCountBadge.findObject().text)
    return this
}

fun UserRobot.assertParticipantMicrophone(isEnabled: Boolean): UserRobot {
    if (isEnabled) {
        assertTrue(CallPage.ParticipantView.microphoneEnabledIcon.waitToAppear().isDisplayed())
    } else {
        assertTrue(CallPage.ParticipantView.microphoneDisabledIcon.waitToAppear().isDisplayed())
    }
    return this
}

fun UserRobot.assertMediaTracks(count: Int): UserRobot {
    if (count > 0) {
        val mediaTracks = CallPage.ParticipantView.videoViewWithMediaTrack.waitForCount(count)
        assertEquals(count, mediaTracks.size)
    } else {
        CallPage.ParticipantView.videoViewWithMediaTrack.waitToDisappear()
        assertEquals(count, CallPage.ParticipantView.videoViewWithMediaTrack.findObjects().size)
    }
    return this
}

fun UserRobot.assertConnectionQualityIndicator(): UserRobot {
    assertTrue(CallPage.ParticipantView.networkQualityIndicator.waitToAppear().isDisplayed())
    return this
}

fun UserRobot.assertRecordingView(isDisplayed: Boolean): UserRobot {
    val label = "Recording"
    if (isDisplayed) {
        assertTrue(CallPage.recordingIcon.waitToAppear().isDisplayed())
        assertEquals(label, CallPage.callInfoView.findObject().text)
    } else {
        assertFalse(CallPage.recordingIcon.waitToDisappear().isDisplayed())
        assertNotEquals(label, CallPage.callInfoView.findObject().text)
    }
    return this
}

fun UserRobot.assertCallDurationView(isDisplayed: Boolean): UserRobot {
    val callDurationViewText = CallPage.callInfoView.waitToAppear().text
    assertEquals(isDisplayed, Regex("\\d+s").containsMatchIn(callDurationViewText))
    return this
}

fun UserRobot.assertParticipantScreenSharingView(isDisplayed: Boolean): UserRobot {
    val screenSharingView = CallPage.ParticipantView.screenSharingView
    if (isDisplayed) {
        assertTrue(screenSharingView.waitToAppear(timeOutMillis = 10.seconds).isDisplayed())
        assertTrue(CallPage.ParticipantView.screenSharingLabel.waitToAppear().isDisplayed())
    } else {
        assertFalse(screenSharingView.waitToDisappear(timeOutMillis = 10.seconds).isDisplayed())
        assertFalse(CallPage.ParticipantView.screenSharingLabel.waitToDisappear().isDisplayed())
    }
    return this
}
