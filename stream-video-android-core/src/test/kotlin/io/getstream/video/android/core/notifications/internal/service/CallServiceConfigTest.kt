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

package io.getstream.video.android.core.notifications.internal.service

import android.media.AudioAttributes
import io.getstream.video.android.model.StreamCallId
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import kotlin.test.Test

class CallServiceConfigTest {

    @Test
    fun `callServiceConfig should return correct default configuration`() {
        // Given
        val config = callServiceConfig()

        // When
        val runInForeground = config.runCallServiceInForeground
        val servicePerTypeSize = config.callServicePerType.size
        val serviceClass = config.callServicePerType[ANY_MARKER]
        val audioUsage = config.audioUsage

        // Then
        assertEquals(true, runInForeground)
        assertEquals(1, servicePerTypeSize)
        assertEquals(CallService::class.java, serviceClass)
        assertEquals(AudioAttributes.USAGE_VOICE_COMMUNICATION, audioUsage)
    }

    @Test
    fun `livestreamCallServiceConfig should return correct default configuration`() {
        // Given
        val config = livestreamCallServiceConfig()

        // When
        val runInForeground = config.runCallServiceInForeground
        val servicePerTypeSize = config.callServicePerType.size
        val hostServiceClass = config.callServicePerType[ANY_MARKER]
        val livestreamServiceClass = config.callServicePerType["livestream"]
        val audioUsage = config.audioUsage

        // Then
        assertEquals(true, runInForeground)
        assertEquals(2, servicePerTypeSize)
        assertEquals(CallService::class.java, hostServiceClass)
        assertEquals(LivestreamCallService::class.java, livestreamServiceClass)
        assertEquals(AudioAttributes.USAGE_VOICE_COMMUNICATION, audioUsage)
    }

    @Test
    fun `resolveServiceClass should return correct service class for livestream type`() {
        // Given
        val streamCallId = mockk<StreamCallId>()
        every { streamCallId.type } returns "livestream"
        val config = livestreamCallServiceConfig()

        // When
        val resolvedClass = resolveServiceClass(streamCallId, config)

        // Then
        assertEquals(LivestreamCallService::class.java, resolvedClass)
    }

    @Test
    fun `resolveServiceClass should return default service class for unknown type`() {
        // Given
        val streamCallId = mockk<StreamCallId>()
        every { streamCallId.type } returns "unknown"
        val config = livestreamCallServiceConfig()

        // When
        val resolvedClass = resolveServiceClass(streamCallId, config)

        // Then
        assertEquals(CallService::class.java, resolvedClass)
    }

    @Test
    fun `resolveServiceClass should return default service class when no type is provided`() {
        // Given
        val streamCallId = mockk<StreamCallId>()
        every { streamCallId.type } returns ""
        val config = livestreamCallServiceConfig()

        // When
        val resolvedClass = resolveServiceClass(streamCallId, config)

        // Then
        assertEquals(CallService::class.java, resolvedClass)
    }

    @Test
    fun `livestreamGuestCallServiceConfig should return correct default configuration`() {
        // Given
        val config = livestreamGuestCallServiceConfig()

        // When
        val runInForeground = config.runCallServiceInForeground
        val servicePerTypeSize = config.callServicePerType.size
        val hostServiceClass = config.callServicePerType[ANY_MARKER]
        val livestreamServiceClass = config.callServicePerType["livestream"]

        // Then
        assertEquals(true, runInForeground)
        assertEquals(2, servicePerTypeSize)
        assertEquals(CallService::class.java, hostServiceClass)
        assertEquals(LivestreamViewerService::class.java, livestreamServiceClass)
    }

    @Test
    fun `livestreamAudioCallServiceConfig should return correct default configuration`() {
        // Given
        val config = livestreamAudioCallServiceConfig()

        // When
        val runInForeground = config.runCallServiceInForeground
        val servicePerTypeSize = config.callServicePerType.size
        val hostServiceClass = config.callServicePerType[ANY_MARKER]
        val livestreamServiceClass = config.callServicePerType["livestream"]
        val audioUsage = config.audioUsage

        // Then
        assertEquals(true, runInForeground)
        assertEquals(2, servicePerTypeSize)
        assertEquals(CallService::class.java, hostServiceClass)
        assertEquals(LivestreamAudioCallService::class.java, livestreamServiceClass)
        assertEquals(AudioAttributes.USAGE_VOICE_COMMUNICATION, audioUsage)
    }
}
