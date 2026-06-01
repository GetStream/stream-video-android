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

import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSwitchCoroutineDecoratorTest {

    private val controller = mockk<AudioSwitchController>(relaxed = true)

    private val audioExecutionContext = mockk<AudioExecutionContext>(relaxed = true)
    private val client = mockk<StreamVideoClient>(relaxed = true)

    private lateinit var handler: AudioSwitchDecorator

    @Before
    fun setup() {
        mockkObject(StreamVideo)

        every { StreamVideo.instanceOrNull() } returns client
        every { client.getAudioContext() } returns audioExecutionContext

        handler = AudioSwitchDecorator(controller)
    }

    @Test
    fun `start should delegate to controller`() = runTest {
        every { audioExecutionContext.createChildScope() } returns this

        handler.start()

        advanceUntilIdle()

        verify { controller.start() }
    }

    @Test
    fun `stop should delegate to controller`() = runTest {
        every { audioExecutionContext.createChildScope() } returns this

        handler.stop()

        advanceUntilIdle()

        verify { controller.stop() }
    }

    @Test
    fun `selectDevice should delegate to controller`() = runTest {
        every { audioExecutionContext.createChildScope() } returns this

        val device = StreamAudioDevice.Speakerphone()

        handler.selectDevice(device)

        advanceUntilIdle()

        verify { controller.selectDevice(device) }
    }

    @Test
    fun `should not call controller when scope is null`() = runTest {
        every { audioExecutionContext.createChildScope() } returns null

        handler.start()
        handler.stop()
        handler.selectDevice(null)

        advanceUntilIdle()

        verify(exactly = 0) { controller.start() }
        verify(exactly = 0) { controller.stop() }
        verify(exactly = 0) { controller.selectDevice(any()) }
    }

    @Test
    fun `should reuse existing active scope`() = runTest {
        val scope = TestScope()
        every { audioExecutionContext.createChildScope() } returns scope

        handler.start()
        handler.selectDevice(null)
        handler.stop()

        advanceUntilIdle()

        verify(exactly = 1) { audioExecutionContext.createChildScope() }
    }

    @Test
    fun `should recreate scope if previous is cancelled`() = runTest {
        val scope1 = TestScope()
        val scope2 = TestScope()

        every { audioExecutionContext.createChildScope() } returnsMany listOf(scope1, scope2)

        handler.start()
        scope1.cancel() // simulate lifecycle end

        handler.selectDevice(null)

        advanceUntilIdle()

        verify(exactly = 2) { audioExecutionContext.createChildScope() }
    }

    @Test
    fun `ensureScope should not create multiple scopes under concurrency`() = runTest {
        val scope = TestScope()
        every { audioExecutionContext.createChildScope() } returns scope

        coroutineScope {
            repeat(10) {
                launch {
                    handler.start()
                }
            }
        }

        advanceUntilIdle()

        verify(exactly = 1) { audioExecutionContext.createChildScope() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
}
