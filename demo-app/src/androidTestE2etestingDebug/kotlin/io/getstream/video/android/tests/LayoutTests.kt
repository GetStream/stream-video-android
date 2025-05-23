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

import androidx.test.uiautomator.Direction
import io.getstream.video.android.pages.CallPage
import io.getstream.video.android.robots.ParticipantRobot.Actions
import io.getstream.video.android.robots.ParticipantRobot.Options
import io.getstream.video.android.robots.UserControls.DISABLE
import io.getstream.video.android.robots.VideoView
import io.getstream.video.android.robots.assertGridView
import io.getstream.video.android.robots.assertMediaTracks
import io.getstream.video.android.robots.assertParticipantScreenSharingView
import io.getstream.video.android.robots.assertParticipantsCountOnCall
import io.getstream.video.android.robots.assertParticipantsViews
import io.getstream.video.android.robots.assertSpotlightView
import io.getstream.video.android.uiautomator.findObject
import io.getstream.video.android.uiautomator.seconds
import io.getstream.video.android.uiautomator.waitToAppear
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class LayoutTests : StreamTestCase() {

    @AllureId("7015")
    @Test
    fun testUserIsAloneOnTheCall() {
        step("WHEN user starts a new call") {
            userRobot.joinCall()
        }
        step("THEN user is alone on the call") {
            userRobot.assertParticipantsCountOnCall(0)
        }
    }

    @AllureId("6988")
    @Test
    fun testOneParticipantOnTheCall() {
        assertParticipantsOnTheCall(participants = 1, callDuration = 30)
    }

    @AllureId("6931")
    @Test
    fun testTwoParticipantsOnTheCall() {
        assertParticipantsOnTheCall(participants = 2, callDuration = 45)
    }

    @AllureId("7501")
    @Ignore("https://linear.app/stream/issue/AND-361")
    @Test
    fun testThreeParticipantsOnTheCall() {
        assertParticipantsOnTheCall(participants = 3, callDuration = 60)
    }

    @AllureId("7502")
    @Test
    @Ignore
    fun testSixParticipantsOnTheCall() {
        assertParticipantsOnTheCall(participants = 6, callDuration = 120)
    }

    private fun assertParticipantsOnTheCall(participants: Int, callDuration: Int) {
        val user = 1

        step("WHEN user starts a new call") {
            userRobot.joinCall()
        }
        step("WHEN participant joins the call") {
            participantRobot
                .setUserCount(participants)
                .setCallDuration(callDuration)
                .joinCall(callId, options = arrayOf(Options.WITH_CAMERA))
            userRobot.waitForParticipantsOnCall(participants, timeOutMillis = callDuration.seconds)
        }
        step("AND user enables grid view") {
            userRobot.setView(VideoView.GRID)
        }
        step("THEN there are $participants participants on the call") {
            userRobot
                .assertGridView(participants)
                .assertParticipantsViews(
                    count = if (participants > 2) participants + user else participants,
                    view = VideoView.GRID,
                )
        }
        step("WHEN user enables spotlight view") {
            userRobot.setView(VideoView.SPOTLIGHT)
        }
        step("THEN there are $participants participants on the call") {
            userRobot
                .assertSpotlightView()
                .assertParticipantsViews(
                    count = participants + user,
                    view = VideoView.SPOTLIGHT,
                )
        }
    }

    @AllureId("7011")
    @Ignore("https://linear.app/stream/issue/AND-542")
    @Test
    fun testScreenSharingLayout() {
        val participants = 2

        step("GIVEN user starts a call") {
            userRobot
                .joinCall()
                .microphone(DISABLE, hard = true)
                .camera(DISABLE, hard = true)
                .setView(VideoView.DYNAMIC)
        }
        step("WHEN participants join the call and share their screen") {
            participantRobot
                .setUserCount(participants)
                .setScreenSharingDuration(30)
                .joinCall(
                    callId,
                    options = arrayOf(Options.WITH_CAMERA),
                    actions = arrayOf(Actions.SHARE_SCREEN),
                )
            userRobot
                .waitForParticipantsOnCall(participants)
                .assertParticipantScreenSharingView(isDisplayed = true)
        }
        step("THEN there are $participants participants in Spotlight view") {
            userRobot
                .assertSpotlightView()
                .assertMediaTracks(
                    count = participants,
                    view = VideoView.SPOTLIGHT,
                )
        }
        step("WHEN user switches to Grid view") {
            userRobot.setView(VideoView.GRID)
        }
        step("THEN the layout stays in Spotlight view") {
            userRobot.assertSpotlightView()
        }
    }

    @AllureId("6990")
    @Test
    fun testUserMovesCornerDraggableView() {
        val participants = 1

        step("GIVEN user starts a call") {
            userRobot.joinCall()
        }
        step("AND participant joins the call") {
            participantRobot
                .setUserCount(participants)
                .joinCall(callId)
            userRobot.waitForParticipantsOnCall(participants)
        }
        step("WHEN user enables grid view") {
            userRobot.setView(VideoView.GRID)
        }

        val initialCoordinates = CallPage.cornerDraggableView.waitToAppear().visibleBounds
        step("AND user moves corner draggable view down") {
            userRobot.moveCornerDraggableView(Direction.DOWN)
        }

        val newCoordinates = CallPage.cornerDraggableView.findObject().visibleBounds
        step("THEN corner draggable has been moved down") {
            assertEquals(initialCoordinates.centerX(), newCoordinates.centerX())
            assertTrue(initialCoordinates.centerY() < newCoordinates.centerY())
        }
    }
}
