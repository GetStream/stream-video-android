/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.tests

import io.getstream.video.android.robots.Background
import io.getstream.video.android.robots.CameraPosition
import io.getstream.video.android.robots.ParticipantRobot.Options.WITH_MICROPHONE
import io.getstream.video.android.robots.UserControls.DISABLE
import io.getstream.video.android.robots.UserControls.ENABLE
import io.getstream.video.android.robots.assertBackground
import io.getstream.video.android.robots.assertClosedCaption
import io.getstream.video.android.robots.assertHand
import io.getstream.video.android.robots.assertParticipantMicrophone
import io.getstream.video.android.robots.assertTranscription
import io.getstream.video.android.robots.assertUserCamera
import io.getstream.video.android.robots.assertUserFrontCamera
import io.getstream.video.android.robots.assertUserMicrophone
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import org.junit.Ignore
import org.junit.Test

class UserActionsTests : StreamTestCase() {

    @AllureId("7535")
    @Test
    fun testUserMicrophone() {
        step("GIVEN user starts a call") {
            userRobot
                .joinCall()
                .microphone(DISABLE, hard = true)
        }
        step("AND participant joins the call") {
            participantRobot.joinCall(callId)
            userRobot.waitForParticipantsOnCall()
        }
        step("WHEN user enables microphone") {
            userRobot.microphone(ENABLE)
        }
        step("THEN user observes that user's microphone is enabled") {
            userRobot.assertUserMicrophone(isEnabled = true)
        }
        step("WHEN user disables microphone") {
            userRobot.microphone(DISABLE)
        }
        step("THEN user observes that user's microphone is enabled") {
            userRobot.assertUserMicrophone(isEnabled = false)
        }
    }

    @AllureId("7536")
    @Test
    fun testUserCamera() {
        step("GIVEN user starts a call") {
            userRobot
                .joinCall()
                .camera(DISABLE, hard = true)
        }
        step("AND participant joins the call") {
            participantRobot.joinCall(callId)
            userRobot.waitForParticipantsOnCall()
        }
        step("WHEN user enables camera") {
            userRobot.camera(ENABLE)
        }
        step("THEN user observes that user's camera is enabled") {
            userRobot.assertUserCamera(isEnabled = true)
        }
        step("WHEN user disables camera") {
            userRobot.camera(DISABLE)
        }
        step("THEN user observes that user's camera is disabled") {
            userRobot.assertUserCamera(isEnabled = false)
        }
    }

    @AllureId("7537")
    @Ignore("https://linear.app/stream/issue/AND-573")
    @Test
    fun testUserCameraPosition() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("WHEN user switches to the back camera") {
            userRobot
                .camera(ENABLE, hard = true)
                .camera(CameraPosition.BACK)
        }
        step("THEN user observes that the back camera is enabled") {
            userRobot.assertUserFrontCamera(isEnabled = false)
        }
        step("WHEN user switches to the front camera") {
            userRobot.camera(CameraPosition.FRONT)
        }
        step("THEN user observes that the back camera is enabled") {
            userRobot.assertUserFrontCamera(isEnabled = true)
        }
    }

    @AllureId("7538")
    @Ignore("https://linear.app/stream/issue/AND-562")
    @Test
    fun testUserRaisesHand() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("WHEN user raises a hand") {
            userRobot.raiseHand()
        }
        step("THEN user observes that the hand is raised") {
            userRobot.assertHand(isRaised = true)
        }
        step("WHEN user lowers a hand") {
            userRobot.lowerHand()
        }
        step("THEN user observes that the hand is lowered") {
            userRobot.assertHand(isRaised = false)
        }
    }

    @AllureId("7539")
    @Test
    fun testUserChangesBackground() {
        step("WHEN user starts a call") {
            userRobot.joinCall()
        }
        step("THEN user has a default background") {
            userRobot.assertBackground(Background.DEFAULT, isEnabled = true)
        }
        step("WHEN user changes background to image") {
            userRobot.setBackground(Background.IMAGE)
        }
        step("THEN user observes that the background was set to image") {
            userRobot
                .assertBackground(Background.IMAGE, isEnabled = true)
                .assertBackground(Background.DEFAULT, isEnabled = false)
        }
        step("WHEN user changes background to blur") {
            userRobot.setBackground(Background.BLUR)
        }
        step("THEN user observes that the background was set to blur") {
            userRobot
                .assertBackground(Background.BLUR, isEnabled = true)
                .assertBackground(Background.IMAGE, isEnabled = false)
        }
        step("WHEN user removes background") {
            userRobot.setBackground(Background.DEFAULT)
        }
        step("THEN user observes that the background was removed") {
            userRobot
                .assertBackground(Background.DEFAULT, isEnabled = true)
                .assertBackground(Background.BLUR, isEnabled = false)
        }
    }

    @AllureId("7540")
    @Test
    fun testUserEnablesNoiseCancellation() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call with enabled microphone") {
            participantRobot.joinCall(callId, arrayOf(WITH_MICROPHONE))
            userRobot.waitForParticipantsOnCall()
        }
        step("WHEN user enables noise cancellation") {
            userRobot.switchNoiseCancellationToggle()
        }
        step("THEN participant's microphone is enabled") {
            userRobot.assertParticipantMicrophone(isEnabled = true)
        }
        step("WHEN user disables noise cancellation") {
            userRobot.switchNoiseCancellationToggle()
        }
        step("THEN participant's microphone is enabled") {
            userRobot.assertParticipantMicrophone(isEnabled = true)
        }
    }

    @AllureId("7541")
    @Ignore("https://linear.app/stream/issue/AND-566")
    @Test
    fun testTranscription() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("WHEN user enables transcription") {
            userRobot.transcription(ENABLE)
        }
        step("THEN the transcription is enabled") {
            userRobot.assertTranscription(isEnabled = true)
        }
        step("WHEN user disables captions") {
            userRobot.transcription(DISABLE)
        }
        step("THEN the transcription is disabled") {
            userRobot.assertTranscription(isEnabled = false)
        }
    }

    @AllureId("7542")
    @Ignore("https://linear.app/stream/issue/AND-571")
    @Test
    fun testClosedCaption() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("WHEN user enables closed caption") {
            userRobot.closedCaption(ENABLE)
        }
        step("THEN the closed caption is enabled") {
            userRobot.assertClosedCaption(isEnabled = true)
        }
        step("WHEN user disables closed caption") {
            userRobot.closedCaption(DISABLE)
        }
        step("THEN the closed caption is disabled") {
            userRobot.assertClosedCaption(isEnabled = false)
        }
    }
}
