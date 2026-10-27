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
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.robots.assertCallUiIsRendered
import io.getstream.video.android.robots.assertParticipantsCountOnCall
import io.getstream.video.android.robots.assertThatCallIsEnded
import io.getstream.video.android.uiautomator.seconds
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CallLifecycleTests : StreamTestCase() {

    @AllureId("7003")
    @Test
    fun testUserLeavesTheCallOnConnection() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("WHEN user leaves the call as soon as possible") {
            userRobot
                .endCall()
                .sleep(3.seconds)
        }
        step("THEN call is ended for the user") {
            userRobot.assertThatCallIsEnded()
        }
    }

    @AllureId("6942")
    @Test
    fun testUserLeavesTheCallAfterConnection() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("WHEN participant joins the call") {
            participantRobot.joinCall(callId)
            userRobot.waitForParticipantsOnCall(1)
        }
        step("AND user leaves the call") {
            userRobot.endCall()
        }
        step("THEN call is ended for the user") {
            userRobot.assertThatCallIsEnded()
        }
    }

    @AllureId("6943")
    @Test
    fun testParticipantReentersTheCall() {
        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call") {
            participantRobot
                .setCallDuration(5)
                .joinCall(callId)
        }
        step("THEN user observes the alert that participant joined") {
            // TODO: There is no alert at the moment, see: linear.app/stream/issue/AND-522
        }
        step("AND there is one participant on the call") {
            userRobot
                .waitForParticipantsOnCall(1)
                .assertParticipantsCountOnCall(1)
        }
        step("WHEN participant leaves the call") {
            // simulated by call duration timeout
        }
        step("THEN user observes the alert that participant left") {
            // TODO: There is no alert at the moment, see: linear.app/stream/issue/AND-522
        }
        step("AND there are no participants on the call") {
            userRobot
                .waitForParticipantsOnCall(0)
                .assertParticipantsCountOnCall(0)
        }
        step("WHEN participant re-enters the call") {
            participantRobot.joinCall(callId)
        }
        step("THEN user observes the alert that participant joined") {
            // TODO: There is no alert at the moment, see: linear.app/stream/issue/AND-522
        }
        step("AND there is one participant on the call") {
            userRobot
                .waitForParticipantsOnCall(1)
                .assertParticipantsCountOnCall(1)
        }
    }

    @AllureId("7012")
    @Test
    fun testUserReusesTheSameCallObjectAfterLeaving() {
        val participants = 1
        lateinit var reusableCall: Call

        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call") {
            participantRobot.joinCall(callId)
            userRobot.waitForParticipantsOnCall(participants)
        }
        step("AND the call UI is rendered") {
            userRobot.assertCallUiIsRendered()
            reusableCall = requireNotNull(StreamVideo.instance().state.activeCall.value) {
                "Expected the rendered call to be registered as the active call"
            }
        }
        step("WHEN user leaves the call") {
            userRobot
                .endCall()
                .assertThatCallIsEnded()
        }
        step("AND the same Call object joins again") {
            val rejoinResult = runBlocking {
                withTimeout(60_000) {
                    reusableCall.join()
                }
            }

            assertTrue(
                "Expected the same Call object to rejoin successfully",
                rejoinResult.isSuccess,
            )
            assertSame(reusableCall, StreamVideo.instance().state.activeCall.value)
        }
        step("AND the call screen is opened for the rejoined call") {
            userRobot.joinCall(callId)
        }
        step("THEN the call UI is rendered with the participant") {
            userRobot
                .waitForParticipantsOnCall(participants)
                .assertParticipantsCountOnCall(participants)
                .assertCallUiIsRendered()
            assertSame(reusableCall, StreamVideo.instance().state.activeCall.value)
        }
        step("AND user can leave the reused call") {
            userRobot
                .endCall()
                .assertThatCallIsEnded()
        }
    }

    @AllureId("7010")
    @Test
    fun testUserReentersTheCallAsTheSameUserAfterLoggingOut() {
        val testEmail = "a@b.cd"
        val participants = 1

        step("GIVEN user starts a call") {
            userRobot
                .logout()
                .loginWithEmail(testEmail)
                .joinCall(callId)
        }
        step("AND participant joins the call") {
            participantRobot.joinCall(callId)
            userRobot.waitForParticipantsOnCall(participants)
        }
        step("WHEN user re-enters the call as the same user") {
            userRobot
                .endCall()
                .logout()
                .loginWithEmail(testEmail)
                .joinCall(callId)
        }
        step("THEN there is one participant on the call") {
            userRobot.assertParticipantsCountOnCall(participants)
        }
    }

    @AllureId("6946")
    @Test
    fun testUserReentersTheCallAsAnotherUser() {
        val participants = 1

        step("GIVEN user starts a call") {
            userRobot.joinCall(callId)
        }
        step("AND participant joins the call") {
            participantRobot.joinCall(callId)
            userRobot.waitForParticipantsOnCall(participants)
        }
        step("WHEN user re-enters the call as another user") {
            userRobot
                .endCall()
                .logout()
                .loginAsRandomUser()
                .joinCall(callId)
        }
        step("THEN there is one participant on the call") {
            userRobot.assertParticipantsCountOnCall(participants)
        }
    }

    @AllureId("6991")
    @Test
    fun testUserSwitchesCalls() {
        val anotherCallId = UUID.randomUUID().toString().split("-").first()
        val firstCallParticipants = 1
        val secondCallParticipants = 1

        step("GIVEN there is a call with $secondCallParticipants participants") {
            participantRobot
                .setUserCount(secondCallParticipants)
                .joinCall(anotherCallId)
        }
        step("AND user starts a new call") {
            userRobot.joinCall(callId)
        }
        step("AND participant joins the call with the user") {
            participantRobot
                .setUserCount(firstCallParticipants)
                .joinCall(callId)
            userRobot
                .waitForParticipantsOnCall(firstCallParticipants)
        }
        step("WHEN user leaves the call") {
            userRobot.endCall()
        }
        step("AND user joins another call") {
            userRobot.joinCall(anotherCallId)
        }
        step("THEN there are $secondCallParticipants participants on the call") {
            userRobot.assertParticipantsCountOnCall(secondCallParticipants)
        }
    }
}
