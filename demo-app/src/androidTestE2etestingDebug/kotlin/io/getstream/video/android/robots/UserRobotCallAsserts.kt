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

import androidx.test.uiautomator.BySelector
import io.getstream.video.android.pages.CallPage
import io.getstream.video.android.pages.CallPage.SettingsMenu
import io.getstream.video.android.pages.RingPage
import io.getstream.video.android.robots.UserControls.DISABLE
import io.getstream.video.android.robots.UserControls.ENABLE
import io.getstream.video.android.uiautomator.defaultTimeout
import io.getstream.video.android.uiautomator.device
import io.getstream.video.android.uiautomator.findObject
import io.getstream.video.android.uiautomator.findObjects
import io.getstream.video.android.uiautomator.isDisplayed
import io.getstream.video.android.uiautomator.retryOnStaleObjectException
import io.getstream.video.android.uiautomator.seconds
import io.getstream.video.android.uiautomator.waitForCount
import io.getstream.video.android.uiautomator.waitForText
import io.getstream.video.android.uiautomator.waitToAppear
import io.getstream.video.android.uiautomator.waitToDisappear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue

fun UserRobot.assertVideoCallControls(microphone: Boolean, camera: Boolean): UserRobot {
    device.retryOnStaleObjectException {
        assertTrue(CallPage.callSettingsClosedToggle.waitToAppear().isDisplayed())
    }
    assertTrue(CallPage.hangUpButton.isDisplayed())
    assertTrue(CallPage.chatButton.isDisplayed())
    assertTrue(CallPage.connectionQualityIndicator.waitToAppear().isDisplayed())
    assertUserFrontCamera(isEnabled = true)
    assertUserMicrophone(isEnabled = microphone)
    assertUserCamera(isEnabled = camera)
    return this
}

fun UserRobot.assertAudioCallControls(microphone: Boolean): UserRobot {
    assertTrue(CallPage.hangUpButton.waitToAppear().isDisplayed())
    assertTrue(CallPage.AudioCall.durationLabel.waitToAppear().isDisplayed())
    assertTrue(CallPage.AudioCall.participantName.isDisplayed())
    assertTrue(CallPage.AudioCall.participantAvatar.isDisplayed())
    assertUserMicrophone(isEnabled = microphone, videoCall = false)
    assertFalse(CallPage.chatButton.isDisplayed())
    assertFalse(CallPage.connectionQualityIndicator.isDisplayed())
    assertFalse(CallPage.cameraEnabledToggle.isDisplayed())
    assertFalse(CallPage.cameraDisabledToggle.isDisplayed())
    return this
}

fun UserRobot.assertThatCallIsEnded(): UserRobot {
    assertFalse(CallPage.hangUpButton.waitToDisappear().isDisplayed())
    return this
}

fun UserRobot.assertUserMicrophone(isEnabled: Boolean, videoCall: Boolean = true): UserRobot {
    if (isEnabled) {
        assertTrue(CallPage.microphoneEnabledToggle.waitToAppear().isDisplayed())
        if (videoCall) {
            assertTrue(CallPage.ParticipantView.microphoneEnabledIcon.isDisplayed())
        }
    } else {
        assertTrue(CallPage.microphoneDisabledToggle.waitToAppear().isDisplayed())
        if (videoCall) {
            assertTrue(CallPage.ParticipantView.microphoneDisabledIcon.isDisplayed())
        }
    }
    return this
}

fun UserRobot.assertUserCamera(isEnabled: Boolean): UserRobot {
    if (isEnabled) {
        assertTrue(CallPage.cameraEnabledToggle.waitToAppear().isDisplayed())
    } else {
        assertTrue(CallPage.cameraDisabledToggle.waitToAppear().isDisplayed())
    }
    return this
}

fun UserRobot.assertUserFrontCamera(isEnabled: Boolean): UserRobot {
    if (isEnabled) {
        assertTrue(CallPage.cameraPositionToggleFront.waitToAppear().isDisplayed())
    } else {
        assertTrue(CallPage.cameraPositionToggleBack.waitToDisappear().isDisplayed())
    }
    return this
}

fun UserRobot.assertHand(isRaised: Boolean): UserRobot {
    if (isRaised) {
        assertTrue(CallPage.raisedHand.waitToAppear().isDisplayed())
    } else {
        assertTrue(CallPage.raisedHand.waitToDisappear().isDisplayed())
    }
    return this
}

fun UserRobot.assertBackground(background: Background, isEnabled: Boolean): UserRobot {
    settings(ENABLE)
    val locator = when (background) {
        Background.DEFAULT -> SettingsMenu.defaultBackgroundEnabledToggle
        Background.IMAGE -> SettingsMenu.imageBackgroundEnabledToggle
        Background.BLUR -> SettingsMenu.blurBackgroundEnabledToggle
    }
    val expectedCount = if (isEnabled) 1 else 0
    assertEquals(expectedCount, locator.findObjects().size)
    return this
}

fun UserRobot.assertTranscription(isEnabled: Boolean): UserRobot {
    settings(ENABLE)
    val locator = if (isEnabled) SettingsMenu.stopTranscriptionButton else SettingsMenu.startTranscriptionButton
    assertEquals(1, locator.waitForCount(1).size)
    settings(DISABLE)
    return this
}

fun UserRobot.assertClosedCaption(isEnabled: Boolean): UserRobot {
    settings(ENABLE)
    val locator = if (isEnabled) SettingsMenu.stopClosedCaptionButton else SettingsMenu.startClosedCaptionButton
    assertEquals(1, locator.waitForCount(1).size)
    settings(DISABLE)
    return this
}

fun UserRobot.assertParticipantsCountOnCall(
    count: Int,
    timeOutMillis: Long = defaultTimeout,
): UserRobot {
    val user = 1
    val participants = (user + count).toString()
    val actualCount = device.retryOnStaleObjectException {
        CallPage.participantsCountBadge
            .waitToAppear()
            .waitForText(expectedText = participants, timeOutMillis = timeOutMillis)
            .text
    }
    assertEquals(participants, actualCount)
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

fun UserRobot.assertMediaTracks(count: Int, view: VideoView): UserRobot {
    val locator = CallPage.ParticipantView.videoViewWithMediaTrack
    return assertViews(count, view, locator)
}

fun UserRobot.assertParticipantsViews(count: Int, view: VideoView): UserRobot {
    val locator = CallPage.ParticipantView.videoView
    return assertViews(count, view, locator)
}

private fun UserRobot.assertViews(count: Int, view: VideoView, locator: BySelector): UserRobot {
    if (count > 0) {
        val expectedCount = when {
            count > 6 && view == VideoView.GRID -> 6
            count > 4 && view == VideoView.SPOTLIGHT -> 4
            else -> count
        }
        val viewsCount = locator.waitForCount(expectedCount).size
        assertEquals(expectedCount, viewsCount)
    } else {
        assertEquals(count, locator.waitToDisappear().findObjects().size)
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
        assertTrue(screenSharingView.waitToAppear(timeOutMillis = 15.seconds).isDisplayed())
        assertTrue(CallPage.ParticipantView.screenSharingLabel.waitToAppear().isDisplayed())
    } else {
        assertFalse(screenSharingView.waitToDisappear(timeOutMillis = 10.seconds).isDisplayed())
        assertFalse(CallPage.ParticipantView.screenSharingLabel.waitToDisappear().isDisplayed())
    }
    return this
}

fun UserRobot.assertGridView(participants: Int): UserRobot {
    if (participants >= 3) {
        assertFalse(CallPage.cornerDraggableView.waitToDisappear().isDisplayed())
    } else {
        assertTrue(CallPage.cornerDraggableView.waitToAppear().isDisplayed())
    }
    assertTrue(CallPage.ParticipantView.gridView.waitToAppear().isDisplayed())
    return this
}

fun UserRobot.assertSpotlightView(): UserRobot {
    assertTrue(CallPage.ParticipantView.spotlightView.waitToAppear().isDisplayed())
    assertFalse(CallPage.cornerDraggableView.waitToDisappear().isDisplayed())
    return this
}

fun UserRobot.assertIncomingCall(isDisplayed: Boolean): UserRobot {
    if (isDisplayed) {
        assertTrue("Accept call button", RingPage.acceptCallButton.waitToAppear().isDisplayed())
        assertTrue("Decline call button", RingPage.declineCallButton.isDisplayed())
        assertTrue("Call label", RingPage.incomingCallLabel.isDisplayed())
        assertTrue("Avatar", RingPage.callParticipantAvatar.isDisplayed())
    } else {
        assertFalse("Accept call button", RingPage.acceptCallButton.waitToDisappear().isDisplayed())
    }
    return this
}

fun UserRobot.assertOutgoingCall(audioOnly: Boolean = true, isDisplayed: Boolean): UserRobot {
    if (isDisplayed) {
        assertTrue(
            "Decline call button",
            RingPage.declineCallButton.waitToAppear(timeOutMillis = 10.seconds).isDisplayed(),
        )
        assertTrue("Call label", RingPage.outgoingCallLabel.isDisplayed())
        assertTrue("Avatar", RingPage.callParticipantAvatar.isDisplayed())
        assertTrue("Microphone", RingPage.microphoneEnabledToggle.isDisplayed())
        assertEquals(
            "Camera should be displayed: ${!audioOnly}",
            !audioOnly,
            RingPage.cameraEnabledToggle.isDisplayed(),
        )
    } else {
        assertFalse(
            "Decline call button",
            RingPage.declineCallButton.waitToDisappear().isDisplayed(),
        )
    }
    return this
}
