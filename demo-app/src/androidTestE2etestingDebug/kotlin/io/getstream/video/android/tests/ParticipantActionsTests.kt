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

import io.getstream.video.android.robots.ParticipantRobot.Actions.RECORD_CALL
import io.getstream.video.android.robots.ParticipantRobot.Actions.SHARE_SCREEN
import io.getstream.video.android.robots.ParticipantRobot.Options.WITH_CAMERA
import io.getstream.video.android.robots.ParticipantRobot.Options.WITH_MICROPHONE
import io.getstream.video.android.robots.UserControls.DISABLE
import io.getstream.video.android.robots.VideoView
import io.getstream.video.android.robots.assertCallDurationView
import io.getstream.video.android.robots.assertConnectionQualityIndicator
import io.getstream.video.android.robots.assertMediaTracks
import io.getstream.video.android.robots.assertParticipantMicrophone
import io.getstream.video.android.robots.assertParticipantScreenSharingView
import io.getstream.video.android.robots.assertRecordingView
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import org.junit.Test

class ParticipantActionsTests : StreamTestCase() {

    @AllureId("6985")
    @Test
    fun testParticipantEnablesMicrophone() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call and turns on mic") {
            participantRobot.joinCall(callId, arrayOf(WITH_MICROPHONE))
            userRobot.waitForParticipantsOnCall()
        }
        for (view in allViews) {
            step("WHEN user turns on ${view.name} view") {
                userRobot.setView(view)
            }
            step("THEN user observes that participant's microphone is enabled") {
                userRobot.assertParticipantMicrophone(isEnabled = true)
            }
        }
    }

    @AllureId("6987")
    @Test
    fun testParticipantDisablesMicrophone() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call and turns off mic") {
            participantRobot.joinCall(callId)
            userRobot.waitForParticipantsOnCall()
        }
        for (view in allViews) {
            step("WHEN user turns on ${view.name} view") {
                userRobot.setView(view)
            }
            step("THEN user observes that participant's microphone is disabled") {
                userRobot.assertParticipantMicrophone(isEnabled = false)
            }
        }
    }

    @AllureId("7007")
    @Test
    fun testParticipantEnablesCamera() {
        step("GIVEN user starts a call") {
            userRobot.joinCall(camera = DISABLE)
        }
        step("AND participant joins the call and turns camera on") {
            participantRobot.joinCall(callId, arrayOf(WITH_CAMERA))
            userRobot.waitForParticipantsOnCall()
        }
        for (view in allViews) {
            step("WHEN user turns on ${view.name} view") {
                userRobot.setView(view)
            }
            step("THEN user observes that participant's camera is enabled") {
                userRobot.assertMediaTracks(count = 1, view = VideoView.GRID)
            }
        }
    }

    @AllureId("7006")
    @Test
    fun testParticipantDisablesCamera() {
        step("GIVEN user starts a call") {
            userRobot.joinCall(camera = DISABLE)
        }
        step("AND participant joins the call and turns camera off") {
            participantRobot.joinCall(callId)
            userRobot.waitForParticipantsOnCall()
        }
        for (view in allViews) {
            step("WHEN user turns on ${view.name} view") {
                userRobot.setView(view)
            }
            step("THEN user observes that participant's camera is disabled") {
                userRobot.assertMediaTracks(count = 0, view = VideoView.GRID)
            }
        }
    }

    @AllureId("6927")
    @Test
    fun testParticipantConnectionQualityIndicator() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call") {
            participantRobot.joinCall(callId, arrayOf(WITH_CAMERA))
            userRobot.waitForParticipantsOnCall()
        }
        for (view in allViews) {
            step("WHEN user turns on ${view.name} view") {
                userRobot.setView(view)
            }
            step("THEN user observers participant's connection indicator icon") {
                userRobot.assertConnectionQualityIndicator()
            }
        }
    }

    @AllureId("6933")
    @Test
    fun testParticipantRecordsCall() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call and starts recording the call for 10 seconds") {
            participantRobot
                .setCallRecordingDuration(15)
                .joinCall(callId, actions = arrayOf(RECORD_CALL))
        }
        step("WHEN participants join the call and one of them starts recording") {
            userRobot
                .waitForParticipantsOnCall()
                .acceptCallRecording()
        }
        for (view in allViews) {
            step("THEN user turns on ${view.name} view and observes the recording icon appeared") {
                userRobot
                    .setView(view)
                    .assertRecordingView(isDisplayed = true)
                    .assertCallDurationView(isDisplayed = false)
            }
        }
        step("WHEN participant stops recording") {
            // presumably some stopRecording() call goes here if applicable
        }
        for (view in allViews) {
            step(
                "THEN user turns on ${view.name} view and observes the recording icon disappeared",
            ) {
                userRobot
                    .setView(view)
                    .assertRecordingView(isDisplayed = false)
                    .assertCallDurationView(isDisplayed = true)
            }
        }
    }

    @AllureId("6993")
    @Test
    fun testParticipantSharesScreen() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("WHEN participant joins the call and shares the screen for 3 seconds") {
            participantRobot
                .setScreenSharingDuration(10)
                .joinCall(callId, actions = arrayOf(SHARE_SCREEN))
            userRobot.waitForParticipantsOnCall()
        }
        step("THEN user observers participant's screen") {
            userRobot.assertParticipantScreenSharingView(isDisplayed = true)
        }
        step("AND participant stops sharing screen") {
            userRobot.assertParticipantScreenSharingView(isDisplayed = false)
        }
    }
}
