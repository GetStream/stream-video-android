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

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MicrophoneManagerTest {

    private val audioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION

    @Test
    fun `Ensure setup is called prior to any action onto the microphone manager`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val actual = MicrophoneManager(mediaManager, audioUsage)
        val context = mockk<Context>(relaxed = true)
        val microphoneManager = spyk(actual)
        every { mediaManager.context } returns context
        every { context.getSystemService(any()) } returns mockk<AudioManager>(relaxed = true)

        // When
        microphoneManager.enable() // 1
        microphoneManager.select(null) // 2
        microphoneManager.resume() // 3, 4, Resume calls enable internally, thus two invocations
        microphoneManager.disable() // 5
        microphoneManager.pause() // 6
        microphoneManager.setEnabled(true) // 7, 8, calls enable internally
        microphoneManager.setEnabled(false) // 9, 10, calls disable internally

        // Then
        verify(exactly = 10) {
            // Setup will be called exactly 10 times
            microphoneManager.setup()
        }
        verify(exactly = 1) {
            // Even thou setup was invoked 10 times, actual initialization happened once
            // because context.getSystemService was called once only.
            context.getSystemService(any())
        }
    }

    @Test
    fun `Don't crash when accessing audioHandler prior to setup`() {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val actual =
            MicrophoneManager(
                mediaManager,
                audioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
            )
        val context = mockk<Context>(relaxed = true)
        val microphoneManager = spyk(actual)
        every { mediaManager.context } returns context
        every { context.getSystemService(any()) } returns mockk<AudioManager>(relaxed = true)

        // When
        microphoneManager.cleanup()

        // Then
        verify(exactly = 0) {
            // Setup was not called, but still there is no exception
            microphoneManager.setup()
        }
    }

    @Test
    fun `Ensure setup if ever the manager was cleaned`() {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val actual = MicrophoneManager(mediaManager, audioUsage)
        val context = mockk<Context>(relaxed = true)
        val microphoneManager = spyk(actual)
        every { mediaManager.context } returns context
        every { context.getSystemService(any()) } returns mockk<AudioManager>(relaxed = true)

        // When
        microphoneManager.setup()
        microphoneManager.cleanup() // Clean and then invoke again
        microphoneManager.resume() // Should call setup again

        // Then
        verify(exactly = 2) {
            // Setup was called twice
            microphoneManager.setup()
        }
        verifyOrder {
            microphoneManager.setup() // Manual call
            microphoneManager.cleanup() // Manual call
            microphoneManager.resume() // Manual call
            microphoneManager.setup() // Automatic as part of enforce setup strategy of resume()
        }
    }

    @Test
    fun `Resume will call enable only if prior status was DeviceStatus#enabled`() {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val actual = MicrophoneManager(mediaManager, audioUsage)
        val context = mockk<Context>(relaxed = true)
        val microphoneManager = spyk(actual)
        every { mediaManager.context } returns context
        every { context.getSystemService(any()) } returns mockk<AudioManager>(relaxed = true)

        // When
        microphoneManager.setup()
        microphoneManager.priorStatus = DeviceStatus.Enabled
        microphoneManager.resume() // Should call setup again

        // Then
        verify(exactly = 1) {
            // Setup was called twice
            microphoneManager.enable()
        }
    }
}
