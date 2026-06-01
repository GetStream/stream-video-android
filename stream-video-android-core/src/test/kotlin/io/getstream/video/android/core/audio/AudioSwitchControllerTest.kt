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
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AudioSwitchControllerTest {

    private lateinit var context: Context
    private lateinit var listener: StreamAudioDeviceChangeListener
    private lateinit var controller: AudioSwitchController

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockkConstructor(StreamAudioSwitch::class)
        every { anyConstructed<StreamAudioSwitch>().start(any()) } just runs
        every { anyConstructed<StreamAudioSwitch>().stop() } just runs
        every { anyConstructed<StreamAudioSwitch>().selectDevice(any()) } just runs

        listener = { _, _ -> }
        controller = AudioSwitchController(context, emptyList(), listener)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `start should create and start StreamAudioSwitch`() {
        controller.start()

        verify { anyConstructed<StreamAudioSwitch>().start(listener) }
    }

    @Test
    fun `start should not create StreamAudioSwitch twice`() {
        controller.start()
        controller.start()

        verify(exactly = 1) { anyConstructed<StreamAudioSwitch>().start(any()) }
    }

    @Test
    fun `stop should call stop on StreamAudioSwitch`() {
        controller.start()

        controller.stop()

        verify { anyConstructed<StreamAudioSwitch>().stop() }
    }

    @Test
    fun `selectDevice should delegate to StreamAudioSwitch`() {
        controller.start()

        val device = StreamAudioDevice.Speakerphone()

        controller.selectDevice(device)

        verify { anyConstructed<StreamAudioSwitch>().selectDevice(device) }
    }

    @Test
    fun `selectDevice should do nothing if not started`() {
        val device = StreamAudioDevice.Speakerphone()

        controller.selectDevice(device)

        verify(exactly = 0) { anyConstructed<StreamAudioSwitch>().selectDevice(any()) }
    }
}
