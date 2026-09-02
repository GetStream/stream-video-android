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

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.robots.UserControls.DISABLE
import io.getstream.video.android.robots.assertAudioCallControls
import io.getstream.video.android.robots.assertConnectingView
import io.getstream.video.android.robots.assertIncomingCall
import io.getstream.video.android.robots.assertOutgoingCall
import io.getstream.video.android.robots.assertThatCallIsEnded
import io.getstream.video.android.robots.assertVideoCallControls
import io.getstream.video.android.uiautomator.seconds
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RingingTests : StreamTestCase() {

    @Test
    fun testJoinAndRingRemainsOutgoingAfterFastReconnectBeforeCalleeAccepts() {
        startUnansweredJoinAndRingCall()

        step("WHEN the caller completes a fast reconnect before the callee accepts") {
            reconnectOutgoingCall(expectSessionReplacement = false) {
                fastReconnect("E2E: outgoing join-and-ring")
            }
        }
        step("THEN the outgoing call screen is still displayed") {
            userRobot.assertOutgoingCall(audioOnly = false, isDisplayed = true)
        }

        userRobot.declineOutgoingCall()
    }

    @Test
    fun testJoinAndRingRemainsOutgoingAfterRejoinBeforeCalleeAccepts() {
        startUnansweredJoinAndRingCall()

        step("WHEN the caller completes a rejoin before the callee accepts") {
            reconnectOutgoingCall(expectSessionReplacement = true) {
                rejoin("E2E: outgoing join-and-ring")
            }
        }
        step("THEN the outgoing call screen is still displayed") {
            userRobot.assertOutgoingCall(audioOnly = false, isDisplayed = true)
        }

        userRobot.declineOutgoingCall()
    }

    private fun startUnansweredJoinAndRingCall() {
        step("GIVEN the caller joins first and rings a callee who has not accepted") {
            // Do not start the participant robot: the selected callee remains unanswered.
            userRobot
                .logout()
                .loginAsRandomUser()
                .directCall(audioOnly = false, joinAndRing = true)
                .assertOutgoingCall(audioOnly = false, isDisplayed = true)
        }
    }

    private fun reconnectOutgoingCall(
        expectSessionReplacement: Boolean,
        reconnect: suspend Call.() -> Unit,
    ) {
        val call = requireNotNull(StreamVideo.instance().state.ringingCall.value) {
            "Expected the join-and-ring call to be registered as the ringing call"
        }
        assertTrue(call.state.ringingState.value is RingingState.Outgoing)
        assertEquals(RealtimeConnection.Connected, call.state.connection.value)
        val sessionIdBeforeReconnect = call.sessionId

        runBlocking {
            // Subscribe before triggering the reconnect so a fast Reconnecting transition
            // cannot be missed by a polling assertion.
            val enteredReconnecting = async(start = CoroutineStart.UNDISPATCHED) {
                call.state.connection.first { it is RealtimeConnection.Reconnecting }
            }
            val reconnectCompleted = async {
                reconnect(call)
            }

            val reconnectingState = withTimeoutOrNull(30.seconds) {
                enteredReconnecting.await()
            }
            assertNotNull(
                "Expected the connection to enter Reconnecting within 30 seconds, " +
                    "but its current state is ${call.state.connection.value}",
                reconnectingState,
            )
            withTimeout(60.seconds) { reconnectCompleted.await() }
            val connectedState = withTimeoutOrNull(30.seconds) {
                call.state.connection.first { it is RealtimeConnection.Connected }
            }
            assertNotNull(
                "Expected the connection to recover to Connected within 30 seconds, " +
                    "but its current state is ${call.state.connection.value}",
                connectedState,
            )
        }

        if (expectSessionReplacement) {
            assertNotEquals(sessionIdBeforeReconnect, call.sessionId)
        } else {
            assertEquals(sessionIdBeforeReconnect, call.sessionId)
        }
        assertTrue(call.state.ringingState.value is RingingState.Outgoing)
    }

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
        step("THEN the user is connecting") {
            userRobot.assertConnectingView()
        }
        step("AND the call starts") {
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
        step("THEN the user is connecting") {
            userRobot.assertConnectingView()
        }
        step("AND the call starts") {
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
        step("THEN the user is connecting") {
            userRobot.assertConnectingView()
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
        step("THEN the user is connecting") {
            userRobot.assertConnectingView()
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
