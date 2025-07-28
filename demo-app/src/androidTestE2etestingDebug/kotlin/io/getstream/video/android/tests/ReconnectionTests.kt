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

package io.getstream.video.android.tests

import io.getstream.video.android.robots.ParticipantRobot.Actions
import io.getstream.video.android.robots.ParticipantRobot.Options
import io.getstream.video.android.robots.VideoView
import io.getstream.video.android.robots.assertGridView
import io.getstream.video.android.robots.assertParticipantScreenSharingView
import io.getstream.video.android.robots.assertParticipantsCountOnCall
import io.getstream.video.android.robots.assertParticipantsViews
import io.getstream.video.android.robots.assertRecordingView
import io.getstream.video.android.uiautomator.device
import io.getstream.video.android.uiautomator.disableInternetConnection
import io.getstream.video.android.uiautomator.enableInternetConnection
import io.getstream.video.android.uiautomator.seconds
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import org.junit.Test

class ReconnectionTests : StreamTestCase() {

    @AllureId("7498")
    @Test
    fun testReconnection() {
        val user = 1
        val participants = 4

        step("GIVEN user starts a new call") {
            userRobot
                .joinCall()
                .setView(VideoView.GRID)
        }
        step("AND participants join the call") {
            participantRobot
                .setCallDuration(120)
                .setUserCount(participants)
                .joinCall(callId, options = arrayOf(Options.WITH_CAMERA, Options.WITH_MICROPHONE))
        }
        step("AND user waits for the first participant to join the call") {
            userRobot.waitForParticipantsOnCall(1)
        }
        step("WHEN user loses the internet connection") {
            device.disableInternetConnection()
        }
        step("AND user restores the connection after 5 seconds") {
            userRobot.sleep(5.seconds)
            device.enableInternetConnection()
        }
        step("THEN there are $participants participants on the call") {
            userRobot
                .assertParticipantsCountOnCall(participants, timeOutMillis = 100.seconds)
                .assertGridView(participants)
                .assertParticipantsViews(
                    count = participants + user,
                    view = VideoView.GRID,
                )
        }
    }

    @AllureId("7499")
    @Test
    fun testReconnectionDuringScreenSharing() {
        val participants = 1

        step("GIVEN user starts a new call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call") {
            participantRobot
                .setUserCount(participants)
                .setScreenSharingDuration(30)
                .joinCall(callId, actions = arrayOf(Actions.SHARE_SCREEN))
            userRobot.waitForParticipantsOnCall(participants)
        }
        step("AND participant starts sharing a screen") {
            userRobot.assertParticipantScreenSharingView(isDisplayed = true)
        }
        step("WHEN user loses the internet connection") {
            device.disableInternetConnection()
        }
        step("AND user restores the connection after 5 seconds") {
            userRobot.sleep(5.seconds)
            device.enableInternetConnection()
        }
        step("THEN user still can see that participant is sharing the screen") {
            userRobot.assertParticipantScreenSharingView(isDisplayed = true)
        }
    }

    @AllureId("7500")
    @Test
    fun testReconnectionDuringCallRecording() {
        val participants = 1

        step("GIVEN user starts a new call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call") {
            participantRobot
                .setUserCount(participants)
                .setCallRecordingDuration(30)
                .joinCall(callId, actions = arrayOf(Actions.RECORD_CALL))
        }
        step("AND participant starts recording a call") {
            userRobot
                .waitForParticipantsOnCall(participants)
                .acceptCallRecording()
                .assertRecordingView(isDisplayed = true)
        }
        step("WHEN user loses the internet connection") {
            device.disableInternetConnection()
        }
        step("AND user restores the connection after 5 seconds") {
            userRobot.sleep(5.seconds)
            device.enableInternetConnection()
        }
        step("THEN user still can see that participant is recording the call") {
            userRobot.assertRecordingView(isDisplayed = true)
        }
    }
}
