package io.getstream.video.android.core

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MediaManagerTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:AndroidDeviceTest")

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule
        .grant(Manifest.permission.BLUETOOTH_CONNECT)

    @Test
    fun camera() = runTest {

        val camera = call.mediaManager.camera
        assertThat(camera).isNotNull()
        camera.enable()
        assertThat(camera.status.value).isEqualTo(DeviceStatus.Enabled)
        assertThat(camera.direction.value).isEqualTo(CameraDirection.Front)
        camera.flip()
        assertThat(camera.direction.value).isEqualTo(CameraDirection.Back)
        camera.disable()
        assertThat(camera.status.value).isEqualTo(DeviceStatus.Disabled)

    }

    @Test
    fun speaker() = runTest {

        val speaker = call.mediaManager.speaker

        assertThat(speaker).isNotNull()
        speaker.setVolume(100)
        assertThat(speaker.volume.value).isEqualTo(100)
        speaker.setVolume(0)
        assertThat(speaker.volume.value).isEqualTo(0)

        val oldDevice = speaker.selectedDevice.value
        speaker.enableSpeakerPhone()
        assertThat(speaker.speakerPhoneEnabled.value).isTrue()
        speaker.disableSpeakerPhone()
        assertThat(speaker.speakerPhoneEnabled.value).isFalse()
        assertThat(speaker.selectedDevice.value).isEqualTo(oldDevice)

    }

    @Test
    fun microphone() = runTest {

        val microphone = call.mediaManager.microphone

        assertThat(microphone).isNotNull()

        microphone.enable()
        assertThat(microphone.status.value).isEqualTo(DeviceStatus.Enabled)
        microphone.disable()
        assertThat(microphone.status.value).isEqualTo(DeviceStatus.Disabled)
    }
}