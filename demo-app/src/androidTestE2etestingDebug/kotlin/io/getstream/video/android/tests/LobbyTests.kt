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

import io.getstream.video.android.robots.UserControls
import io.getstream.video.android.robots.assertCallControls
import io.getstream.video.android.robots.assertLobby
import io.getstream.video.android.robots.assertParticipantsCountInLobby
import io.getstream.video.android.robots.assertParticipantsCountOnCall
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import org.junit.Test

class LobbyTests : StreamTestCase() {

    @AllureId("6992")
    @Test
    fun testLobbyWithTwoParticipants() {
        val participants = 2

        step("GIVEN $participants participants are on call") {
            userRobot.joinCall()

            participantRobot
                .setUserCount(participants)
                .joinCall(callId)

            userRobot
                .waitForParticipantsOnCall(participants)
                .endCall()
        }
        step("WHEN user enters lobby") {
            userRobot
                .enterLobby(callId)
                .camera(UserControls.ENABLE, hard = true)
                .microphone(UserControls.ENABLE, hard = true)
        }
        step("THEN all required elements are on the screen") {
            userRobot.assertLobby(microphone = true, camera = true)
        }
        step("AND user observes the number of participants on call") {
            userRobot.assertParticipantsCountInLobby(participants)
        }
        step("WHEN user joins the call") {
            userRobot.joinCallFromLobby()
        }
        step("THEN there are $participants participants on the call") {
            userRobot.assertParticipantsCountOnCall(participants)
        }
    }

    @AllureId("6950")
    @Test
    fun testLobbyWithZeroParticipants() {
        step("WHEN user enters lobby") {
            userRobot
                .enterLobby()
                .camera(UserControls.ENABLE, hard = true)
                .microphone(UserControls.ENABLE, hard = true)
        }
        step("THEN all required elements are on the screen") {
            userRobot.assertLobby(microphone = true, camera = true)
        }
        step("AND there are no participants on call") {
            userRobot.assertParticipantsCountInLobby(0)
        }
        step("WHEN user joins the call") {
            userRobot.joinCallFromLobby()
        }
        step("THEN there are no participants on the call") {
            userRobot.assertParticipantsCountOnCall(0)
        }
    }

    @AllureId("7254")
    @Test
    fun testCameraAndMicrophoneConfigurationInLobby() {
        step("WHEN user enters lobby w/o camera and mic") {
            userRobot
                .enterLobby()
                .camera(UserControls.DISABLE, hard = true)
                .microphone(UserControls.DISABLE, hard = true)
        }
        step("THEN camera and mic are disabled in lobby") {
            userRobot.assertLobby(microphone = false, camera = false)
        }
        step("WHEN user joins the call") {
            userRobot.joinCallFromLobby()
        }
        step("THEN camera and mic are disabled on call") {
            userRobot.assertCallControls(microphone = false, camera = false)
        }
        step("WHEN user enters lobby with camera and mic") {
            userRobot
                .endCall()
                .enterLobby(callId)
                .camera(UserControls.ENABLE, hard = true)
                .microphone(UserControls.ENABLE, hard = true)
        }
        step("THEN camera and mic are enabled in lobby") {
            userRobot.assertLobby(microphone = true, camera = true)
        }
        step("WHEN user joins the call") {
            userRobot.joinCallFromLobby()
        }
        step("THEN camera and mic are enabled on call") {
            userRobot.assertCallControls(microphone = true, camera = true)
        }
    }
}
