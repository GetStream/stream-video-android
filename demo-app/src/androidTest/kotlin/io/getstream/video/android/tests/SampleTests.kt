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

import io.getstream.video.android.robots.ParticipantRobot.Options.WITH_CAMERA
import io.getstream.video.android.uiautomator.seconds
import io.getstream.video.android.uiautomator.sleep
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import org.junit.Test

class SampleTests : StreamTestCase() {

    @AllureId("")
    @Test
    fun test_sample() {
        step("GIVEN user joins the call") {
            userRobot.joinCall(callId)
        }
        step("AND participant joins the call") {
            participantRobot.joinCall(callId, options = arrayOf(WITH_CAMERA))
        }
        step("THEN sleep") {
            userRobot.sleep(10.seconds)
        }
    }
}
