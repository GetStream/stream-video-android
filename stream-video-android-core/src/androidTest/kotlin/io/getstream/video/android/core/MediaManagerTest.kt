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

package io.getstream.video.android.core

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.StreamAudioDevice
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

    @Test
    fun setSpeakerPhone() {
        val speaker = deviceTestCall.mediaManager.speaker
        val microphone = deviceTestCall.mediaManager.microphone

        // Initialize devices first
        microphone.setup()

        // Wait for devices to be populated - we need to wait for the audio devices callback
        val devices = microphone.devices.value

        // Only run test if we have speaker device
        if (devices.any { it is StreamAudioDevice.Speakerphone }) {
            // Get initial state
            val initialDevice = speaker.selectedDevice.value

            // Test enabling speaker phone
            speaker.setSpeakerPhone(true)
            assertThat(speaker.speakerPhoneEnabled.value).isTrue()
            assertThat(
                speaker.selectedDevice.value,
            ).isInstanceOf(StreamAudioDevice.Speakerphone::class.java)

            // Should store the previous device
            assertThat(speaker.selectedBeforeSpeaker).isEqualTo(
                initialDevice?.takeUnless { it is StreamAudioDevice.Speakerphone },
            )

            // Test disabling speaker phone - should revert to saved device
            val savedDevice = speaker.selectedBeforeSpeaker
            speaker.setSpeakerPhone(false)
            assertThat(speaker.speakerPhoneEnabled.value).isFalse()

            // If there was a saved device, check we're using it
            if (savedDevice != null) {
                // Should select the previously saved device
                assertThat(speaker.selectedDevice.value).isEqualTo(savedDevice)
            }

            // Test with specific fallback device
            speaker.setSpeakerPhone(true)

            // Find a non-speaker device to use as fallback
            val nonSpeakerDevice = microphone.devices.value
                .firstOrNull { it !is StreamAudioDevice.Speakerphone }

            if (nonSpeakerDevice != null) {
                speaker.setSpeakerPhone(false, nonSpeakerDevice)
                assertThat(speaker.speakerPhoneEnabled.value).isFalse()
                assertThat(speaker.selectedDevice.value).isEqualTo(nonSpeakerDevice)
            }
        } else {
            // Skip test if no speaker device is available
            println("Skipping test - no speaker device available")
        }
    }
}
