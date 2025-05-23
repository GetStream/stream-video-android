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

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.CAMERA
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import io.getstream.chat.android.e2e.test.rules.RetryRule
import io.getstream.video.android.robots.ParticipantRobot
import io.getstream.video.android.robots.UserRobot
import io.getstream.video.android.robots.VideoView
import io.getstream.video.android.uiautomator.device
import io.getstream.video.android.uiautomator.grantPermission
import io.getstream.video.android.uiautomator.startApp
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import java.util.UUID

abstract class StreamTestCase {

    val userRobot = UserRobot()
    lateinit var participantRobot: ParticipantRobot
    val allViews: Array<VideoView> = arrayOf(VideoView.GRID, VideoView.DYNAMIC, VideoView.SPOTLIGHT)
    lateinit var callId: String
    private val headlessBrowser = true
    private val recordBrowser = true

    @get:Rule
    var testName: TestName = TestName()

    @get:Rule
    val retryRule = RetryRule(count = 3)

    @Before
    fun setUp() {
        participantRobot = ParticipantRobot(
            testName = testName.methodName,
            headless = headlessBrowser,
            record = recordBrowser,
        )
        generateCallId()
        device.startApp(callId)
        grantAppPermissions()
    }

    @SuppressLint("InlinedApi")
    private fun grantAppPermissions() {
        val permissions = arrayOf(
            POST_NOTIFICATIONS,
            READ_MEDIA_VIDEO,
            READ_MEDIA_IMAGES,
            READ_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE,
            CAMERA,
            RECORD_AUDIO,
            ACCESS_FINE_LOCATION,
            BLUETOOTH_ADVERTISE,
            BLUETOOTH_SCAN,
            BLUETOOTH_CONNECT,
        )
        for (permission in permissions) {
            device.grantPermission(permission)
        }
    }

    private fun generateCallId() {
        callId = UUID.randomUUID().toString().split("-").first()
    }
}
