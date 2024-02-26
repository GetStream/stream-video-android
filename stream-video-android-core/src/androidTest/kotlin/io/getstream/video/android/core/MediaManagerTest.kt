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

package io.getstream.video.android.core

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class MediaManagerTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:AndroidDeviceTest")

    private lateinit var deviceTestCall: Call

    @get:Rule
    var runtimePermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.BLUETOOTH_CONNECT,
    )

    @Before
    fun createNewCall() {
        deviceTestCall = client.call("default", UUID.randomUUID().toString())
    }

    @Test
    fun camera() {
        val camera = deviceTestCall.mediaManager.camera
        assertThat(camera).isNotNull()
        if (camera.listDevices().isNotEmpty() && camera.availableResolutions.value.isNotEmpty()) {
            camera.enable()
            assertThat(camera.status.value).isEqualTo(DeviceStatus.Enabled)
            assertThat(camera.direction.value).isEqualTo(CameraDirection.Front)
            camera.flip()
            assertThat(camera.direction.value).isEqualTo(CameraDirection.Back)
            camera.disable()
            assertThat(camera.status.value).isEqualTo(DeviceStatus.Disabled)
        }
    }

    @Test
    fun cameraResume() {
        val camera = deviceTestCall.mediaManager.camera
        // test resume
        if (camera.listDevices().isNotEmpty() && camera.availableResolutions.value.isNotEmpty()) {
            camera.enable()
            assertThat(camera.status.value).isEqualTo(DeviceStatus.Enabled)
            camera.pause()
            assertThat(camera.status.value).isEqualTo(DeviceStatus.Disabled)
            camera.resume()
            assertThat(camera.status.value).isEqualTo(DeviceStatus.Enabled)
        }
    }

    @Test
    fun speaker() {
        val speaker = deviceTestCall.mediaManager.speaker

        assertThat(speaker).isNotNull()
        speaker.setVolume(100)
        assertThat(speaker.volume.value).isEqualTo(100)
        speaker.setVolume(0)
        assertThat(speaker.volume.value).isEqualTo(0)

        val oldDevice = speaker.selectedDevice.value
        speaker.enable()
        assertThat(speaker.speakerPhoneEnabled.value).isTrue()
        speaker.disable()
        assertThat(speaker.speakerPhoneEnabled.value).isFalse()
        assertThat(speaker.selectedDevice.value).isEqualTo(oldDevice)
    }

    @Test
    fun speakerResume() {
        val speaker = deviceTestCall.mediaManager.speaker
        // test resume
        speaker.enable()
        speaker.setVolume(54)
        speaker.pause()
        assertThat(speaker.volume.value).isEqualTo(0)
        speaker.resume()
        assertThat(speaker.volume.value).isEqualTo(54)
    }

    @Test
    fun microphone() = runTest {
        val microphone = deviceTestCall.mediaManager.microphone

        assertThat(microphone).isNotNull()
        microphone.enable()
        assertThat(microphone.status.value).isEqualTo(DeviceStatus.Enabled)
        microphone.disable()
        assertThat(microphone.status.value).isEqualTo(DeviceStatus.Disabled)
    }

    @Test
    fun microphoneResume() {
        val microphone = deviceTestCall.mediaManager.microphone
        microphone.enable()
        assertThat(microphone.status.value).isEqualTo(DeviceStatus.Enabled)
        microphone.pause()
        assertThat(microphone.status.value).isEqualTo(DeviceStatus.Disabled)
        microphone.resume()
        assertThat(microphone.status.value).isEqualTo(DeviceStatus.Enabled)
    }
}
