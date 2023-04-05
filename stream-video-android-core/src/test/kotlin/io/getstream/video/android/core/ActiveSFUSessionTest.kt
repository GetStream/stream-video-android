package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveSFUSessionTest : IntegrationTestBase() {

    @Test
    fun `Camera API`() = runTest {
        call.camera.devices
        val cameraId = call.camera.devices.value.first()
        call.camera.flip()
        call.camera.disable()
        call.camera.enable()
        call.camera.status
        call.camera.select(cameraId)
        // TODO: how to connect the buildCameraCapturer?
        // TODO: how to send a new track when the camera changes?
    }

    @Test
    fun `Microphone API`() = runTest {
        // TODO: Maybe audio in/out is better?
        call.microphone.devices
        call.microphone.disable()
        call.microphone.enable()
        call.microphone.status
    }

    @Test
    fun `Speaker API`() = runTest {
        call.speaker.devices
        call.speaker.disable()
        call.speaker.enable()
        call.speaker.setVolume(100L)
        call.speaker.setSpeakerPhone(true)
        call.speaker.status
    }

}