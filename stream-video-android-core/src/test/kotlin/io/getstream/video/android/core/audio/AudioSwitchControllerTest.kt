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

package io.getstream.video.android.core.audio

import android.content.Context
import android.media.AudioManager
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDeviceChangeListener
import com.twilio.audioswitch.AudioSwitch
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSwitchControllerTest {

    private val context = mockk<Context>(relaxed = true)
    private val listener = mockk<AudioDeviceChangeListener>(relaxed = true)
    private val audioManager = mockk<AudioManager>(relaxed = true)

    private lateinit var controller: AudioSwitchController
    private lateinit var audioSwitch: AudioSwitch

    @Before
    fun setup() {
        audioSwitch = mockk(relaxed = true)

        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        every { audioManager.mode } returns AudioManager.MODE_IN_COMMUNICATION

        controller = spyk(
            AudioSwitchController(context, emptyList(), listener),
        )

        every { controller.getAudioSwitch() } returns audioSwitch
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `start should create and start AudioSwitch`() {
        controller.start()

        verify { audioSwitch.start(listener) }
    }

    @Test
    fun `start should not create AudioSwitch twice`() {
        controller.start()
        controller.start()

        verify(exactly = 1) { controller.getAudioSwitch() }
        verify(exactly = 1) { audioSwitch.start(listener) }
    }

    @Test
    fun `stop should call stop on AudioSwitch`() {
        controller.start()

        controller.stop()

        verify { audioSwitch.stop() }
    }

    @Test
    fun `selectDevice should delegate to AudioSwitch`() {
        controller.start()

        val device = mockk<AudioDevice>()

        controller.selectDevice(device)

        verify { audioSwitch.selectDevice(device) }
    }

    @Test
    fun `selectDevice should activate only once`() {
        controller.start()

        val device = mockk<AudioDevice>()

        controller.selectDevice(device)
        controller.selectDevice(device)

        verify(exactly = 1) { audioSwitch.activate() }
    }

    @Test
    fun `selectDevice should do nothing if not started`() {
        val device = mockk<AudioDevice>()

        controller.selectDevice(device)

        verify(exactly = 0) { audioSwitch.selectDevice(any()) }
    }

    @Test
    fun `setCommunicationModeEnabled false should move the device to MODE_NORMAL`() {
        controller.start()

        assertTrue(controller.setCommunicationModeEnabled(false))

        verify { audioManager.mode = AudioManager.MODE_NORMAL }
    }

    @Test
    fun `setCommunicationModeEnabled true should move the device to MODE_IN_COMMUNICATION`() {
        controller.start()
        every { audioManager.mode } returns AudioManager.MODE_NORMAL

        assertTrue(controller.setCommunicationModeEnabled(true))

        verify { audioManager.mode = AudioManager.MODE_IN_COMMUNICATION }
    }

    @Test
    fun `setCommunicationModeEnabled should report failure with no AudioManager`() {
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns null
        controller.start()

        assertFalse(controller.setCommunicationModeEnabled(false))
    }

    @Test
    fun `selectDevice should reapply the requested mode over the one activate sets`() {
        controller.start()
        controller.setCommunicationModeEnabled(false)
        // activate() puts the device back in communication mode behind our back.
        every { audioManager.mode } returns AudioManager.MODE_IN_COMMUNICATION

        controller.selectDevice(mockk<AudioDevice>())

        verify(exactly = 2) { audioManager.mode = AudioManager.MODE_NORMAL }
    }

    @Test
    fun `selectDevice should leave the mode alone when none was requested`() {
        controller.start()

        controller.selectDevice(mockk<AudioDevice>())

        verify(exactly = 0) { audioManager.mode = any() }
    }

    @Test
    fun `stop should drop the request so the next session does not inherit it`() {
        controller.start()
        controller.setCommunicationModeEnabled(false)

        controller.stop()
        controller.start()
        controller.selectDevice(mockk<AudioDevice>())

        // Once from the explicit request, and not again for the session that followed it.
        verify(exactly = 1) { audioManager.mode = AudioManager.MODE_NORMAL }
    }
}
