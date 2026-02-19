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

import io.getstream.video.android.robots.UserControls.DISABLE
import io.getstream.video.android.robots.assertAudioCallControls
import io.getstream.video.android.robots.assertIncomingCall
import io.getstream.video.android.robots.assertOutgoingCall
import io.getstream.video.android.robots.assertThatCallIsEnded
import io.getstream.video.android.robots.assertVideoCallControls
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
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

    @AllureId("7775")
    @Test
    fun testUserRejectsTheOutgoingVideoCall() {
        step("GIVEN user rings to participant by video") {
            userRobot
                .logout()
                .loginAsRandomUser()
                .directCall(audioOnly = false)
        }
        step("THEN the outgoing call starts") {
            userRobot.assertOutgoingCall(audioOnly = false, isDisplayed = true)
        }
        step("WHEN user rejects the outgoing call") {
            userRobot.declineOutgoingCall()
        }
        step("THEN the outgoing call ends") {
            userRobot.assertOutgoingCall(isDisplayed = false)
        }
    }

    @AllureId("7912")
    @Test
    fun testUserRejectsTheOutgoingAudioCall() {
        step("GIVEN user rings to participant by audio") {
            userRobot
                .logout()
                .loginAsRandomUser()
                .directCall(audioOnly = true)
        }
        step("THEN the outgoing call starts") {
            userRobot.assertOutgoingCall(audioOnly = true, isDisplayed = true)
        }
        step("WHEN user rejects the outgoing call") {
            userRobot.declineOutgoingCall()
        }
        step("THEN the outgoing call ends") {
            userRobot.assertOutgoingCall(isDisplayed = false)
        }
    }

    @AllureId("7776")
    @Test
    fun testUserRejectsTheIncomingVideoCall() {
        step("GIVEN participant rings to user") {
            participantRobot.ringUser(userRobot.getUsername())
        }
        step("WHEN user declines the incoming call") {
            userRobot
                .waitForIncomingCall()
                .declineIncomingCall()
        }
        step("THEN the incoming call ends") {
            userRobot.assertIncomingCall(isDisplayed = false)
        }
    }

    @AllureId("7845")
    @Test
    fun testUserRejectsTheIncomingAudioCall() {
        step("GIVEN participant rings to user") {
            participantRobot.ringUser(userRobot.getUsername(), audioOnly = true)
        }
        step("WHEN user declines the incoming call") {
            userRobot
                .waitForIncomingCall()
                .declineIncomingCall()
        }
        step("THEN the incoming call ends") {
            userRobot.assertIncomingCall(isDisplayed = false)
        }
    }

    @AllureId("7777")
    @Test
    fun testUserAcceptsTheIncomingAudioCallWithMicrophoneEnabled() {
        step("Precondition to avoid flakiness on CI") {
            userRobot
                .declineIncomingCallIfExists()
                .logout()
                .loginAsRandomUser()
        }
        step("GIVEN participant rings to user by audio") {
            participantRobot.ringUser(userRobot.getUsername(), audioOnly = true)
        }
        step("WHEN user accepts the incoming audio call") {
            userRobot
                .waitForIncomingCall()
                .assertIncomingCall(isDisplayed = true)
                .acceptIncomingCall()
        }
        step("THEN the call starts") {
            userRobot
                .assertAudioCallControls(microphone = true)
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

    @AllureId("7879")
    @Test
    fun testUserAcceptsTheIncomingAudioCallWithMicrophoneDisabled() {
        step("Precondition to avoid flakiness on CI") {
            userRobot
                .declineIncomingCallIfExists()
                .logout()
                .loginAsRandomUser()
        }
        step("GIVEN participant rings to user by audio") {
            participantRobot.ringUser(userRobot.getUsername(), audioOnly = true)
        }
        step("WHEN user accepts the incoming audio call with mic disabled") {
            userRobot
                .waitForIncomingCall()
                .assertIncomingCall(isDisplayed = true)
                .microphone(DISABLE)
                .acceptIncomingCall()
        }
        step("THEN the call starts") {
            userRobot
                .assertAudioCallControls(microphone = false)
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

    @AllureId("7778")
    @Test
    fun testUserAcceptsTheIncomingVideoCallWithCameraAndMicrophoneEnabled() {
        step("Precondition to avoid flakiness on CI") {
            userRobot
                .declineIncomingCallIfExists()
                .logout()
                .loginAsRandomUser()
        }
        step("GIVEN participant rings to user") {
            participantRobot.ringUser(userRobot.getUsername())
        }
        step("WHEN user accepts the incoming video call with camera and mic") {
            userRobot.acceptIncomingCall()
        }
        step("THEN the call starts and user has camera and mic enabled") {
            userRobot
                .assertVideoCallControls(camera = true, microphone = true)
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
                .assertVideoCallControls(camera = false, microphone = false)
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
