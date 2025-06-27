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

import io.getstream.video.android.robots.UserControls.DISABLE
import io.getstream.video.android.robots.assertCallControls
import io.getstream.video.android.robots.assertIncomingCall
import io.getstream.video.android.robots.assertThatCallIsEnded
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import org.junit.Ignore
import org.junit.Test

class RingingTests : StreamTestCase() {

    @AllureId("7774")
    @Test
    fun testParticipantRejectsTheOutgoingCall() {
        step("GIVEN participant rings to user") {
            participantRobot
                .setCallDuration(5)
                .ringUser(userRobot.getUsername())
        }
        step("THEN user receives an incoming call") {
            userRobot.assertIncomingCall(isDisplayed = true)
        }
        step("WHEN participant rejects the outgoing call") {
            // simulated by call duration timeout
        }
        step("THEN user misses the incoming call") {
            userRobot.assertIncomingCall(isDisplayed = false)
        }
    }

    @Ignore("https://linear.app/stream/issue/AND-568")
    @AllureId("7775")
    @Test
    fun testUserRejectsTheOutgoingCall() {
        step("GIVEN user rings to participant") {
        }
        step("THEN the outgoing call starts") {
        }
        step("WHEN user rejects the outgoing call") {
        }
        step("THEN the outgoing call ends") {
        }
    }

    @AllureId("7776")
    @Test
    fun testUserRejectsTheIncomingCall() {
        step("GIVEN participant rings to user") {
            participantRobot.ringUser(userRobot.getUsername())
        }
        step("WHEN user declines the incoming call") {
            userRobot.declineIncomingCall()
        }
        step("THEN the incoming call ends") {
            userRobot.assertIncomingCall(isDisplayed = false)
        }
    }

    @Ignore("TODO: Wait for implementation on React")
    @AllureId("7777")
    @Test
    fun testUserAcceptsTheIncomingAudioCall() {
        step("GIVEN participant rings to user") {
            participantRobot.ringUser(userRobot.getUsername())
        }
        step("WHEN user accepts the incoming audio call") {
            userRobot.acceptIncomingCall()
        }
        step("THEN the call starts") {
        }
        step("WHEN user ends the call") {
            userRobot.endCall()
        }
        step("THEN the call ends") {
        }
    }

    @AllureId("7778")
    @Test
    fun testUserAcceptsTheIncomingVideoCallWithCameraAndMicrophoneEnabled() {
        step("GIVEN participant rings to user") {
            participantRobot.ringUser(userRobot.getUsername())
        }
        step("WHEN user accepts the incoming video call with camera and mic") {
            userRobot.acceptIncomingCall()
        }
        step("THEN the call starts and user has camera and mic enabled") {
            userRobot
                .assertCallControls(camera = true, microphone = true)
                .assertIncomingCall(isDisplayed = false)
        }
        step("WHEN user ends the call") {
            userRobot.endCall()
        }
        step("THEN the call ends") {
            userRobot
                .assertThatCallIsEnded()
                .assertIncomingCall(isDisplayed = false)
        }
    }

    @AllureId("7779")
    @Test
    fun testUserAcceptsTheIncomingVideoCallWithCameraAndMicrophoneDisabled() {
        step("GIVEN participant rings to user") {
            participantRobot.ringUser(userRobot.getUsername())
        }
        step("WHEN user accepts the incoming video call without camera and mic") {
            userRobot
                .waitForIncomingCall()
                .camera(DISABLE)
                .microphone(DISABLE)
                .acceptIncomingCall()
        }
        step("THEN the call starts and user has camera and mic disabled") {
            userRobot
                .assertCallControls(camera = false, microphone = false)
                .assertIncomingCall(isDisplayed = false)
        }
        step("WHEN user ends the call") {
            userRobot.endCall()
        }
        step("THEN the call ends") {
            userRobot
                .assertThatCallIsEnded()
                .assertIncomingCall(isDisplayed = false)
        }
    }
}
