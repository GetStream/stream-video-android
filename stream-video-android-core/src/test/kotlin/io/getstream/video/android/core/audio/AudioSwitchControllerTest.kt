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

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSwitchControllerTest {

    private val context = mockk<Context>(relaxed = true)
    private val listener = mockk<AudioDeviceChangeListener>(relaxed = true)

    private lateinit var controller: AudioSwitchController
    private lateinit var audioSwitch: AudioSwitch

    @Before
    fun setup() {
        audioSwitch = mockk(relaxed = true)

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
}
