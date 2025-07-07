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

package io.getstream.video.android.core.notifications.handlers

import android.app.Application
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import io.getstream.video.android.core.Call
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class StreamNotificationUpdateInterceptorsTest {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockNotificationBuilder: NotificationCompat.Builder

    @MockK
    lateinit var mockCall: Call

    @MockK
    lateinit var mockMediaMetadataBuilder: MediaMetadataCompat.Builder

    @MockK
    lateinit var mockPlaybackStateBuilder: PlaybackStateCompat.Builder

    private lateinit var interceptors: StreamNotificationUpdateInterceptors

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        interceptors = StreamNotificationUpdateInterceptors()
    }

    @Test
    fun `onUpdateOngoingCallNotification returns builder unchanged by default`() = runTest {
        // Given
        val callDisplayName = "John Doe"

        // When
        val result = interceptors.onUpdateOngoingCallNotification(
            builder = mockNotificationBuilder,
            callDisplayName = callDisplayName,
            call = mockCall,
        )

        // Then
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun `onUpdateOutgoingCallNotification returns builder unchanged by default`() = runTest {
        // Given
        val callDisplayName = "John Doe"

        // When
        val result = interceptors.onUpdateOutgoingCallNotification(
            builder = mockNotificationBuilder,
            callDisplayName = callDisplayName,
            call = mockCall,
        )

        // Then
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun `onUpdateIncomingCallNotification returns builder unchanged by default`() = runTest {
        // Given
        val callDisplayName = "John Doe"

        // When
        val result = interceptors.onUpdateIncomingCallNotification(
            builder = mockNotificationBuilder,
            callDisplayName = callDisplayName,
            call = mockCall,
        )

        // Then
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun `onUpdateMediaNotificationMetadata returns builder unchanged by default`() = runTest {
        // Given
        val callDisplayName = "John Doe"

        // When
        val result = interceptors.onUpdateMediaNotificationMetadata(
            builder = mockMediaMetadataBuilder,
            call = mockCall,
            callDisplayName = callDisplayName,
        )

        // Then
        assertEquals(mockMediaMetadataBuilder, result)
    }

    @Test
    fun `onUpdateMediaNotificationPlaybackState returns builder unchanged by default`() = runTest {
        // Given
        val callDisplayName = "John Doe"

        // When
        val result = interceptors.onUpdateMediaNotificationPlaybackState(
            builder = mockPlaybackStateBuilder,
            call = mockCall,
            callDisplayName = callDisplayName,
        )

        // Then
        assertEquals(mockPlaybackStateBuilder, result)
    }

    @Test
    fun `onUpdateMediaSessionCompat returns null by default`() = runTest {
        // Given
        val channelId = "test-channel"

        // When
        val result = interceptors.onUpdateMediaSessionCompat(
            application = mockApplication,
            channelId = channelId,
        )

        // Then
        assertNull(result)
    }

    @Test
    fun `custom interceptor can modify ongoing call notification`() = runTest {
        // Given
        val customInterceptors = object : StreamNotificationUpdateInterceptors() {
            override suspend fun onUpdateOngoingCallNotification(
                builder: NotificationCompat.Builder,
                callDisplayName: String?,
                call: Call,
            ): NotificationCompat.Builder {
                return builder.setContentTitle("Updated: $callDisplayName")
            }
        }

        val spyBuilder = spyk(mockNotificationBuilder)
        every { spyBuilder.setContentTitle(any()) } returns spyBuilder

        // When
        val result = customInterceptors.onUpdateOngoingCallNotification(
            builder = spyBuilder,
            callDisplayName = "John Doe",
            call = mockCall,
        )

        // Then
        verify { spyBuilder.setContentTitle("Updated: John Doe") }
        assertEquals(spyBuilder, result)
    }

    @Test
    fun `custom interceptor can modify outgoing call notification`() = runTest {
        // Given
        val customInterceptors = object : StreamNotificationUpdateInterceptors() {
            override suspend fun onUpdateOutgoingCallNotification(
                builder: NotificationCompat.Builder,
                callDisplayName: String?,
                call: Call,
            ): NotificationCompat.Builder {
                return builder.setContentText("Calling $callDisplayName...")
            }
        }

        val spyBuilder = spyk(mockNotificationBuilder)
        every { spyBuilder.setContentText(any()) } returns spyBuilder

        // When
        val result = customInterceptors.onUpdateOutgoingCallNotification(
            builder = spyBuilder,
            callDisplayName = "Jane Smith",
            call = mockCall,
        )

        // Then
        verify { spyBuilder.setContentText("Calling Jane Smith...") }
        assertEquals(spyBuilder, result)
    }

    @Test
    fun `custom interceptor can modify incoming call notification`() = runTest {
        // Given
        val customInterceptors = object : StreamNotificationUpdateInterceptors() {
            override suspend fun onUpdateIncomingCallNotification(
                builder: NotificationCompat.Builder,
                callDisplayName: String?,
                call: Call,
            ): NotificationCompat.Builder {
                return builder.setSubText("Incoming from $callDisplayName")
            }
        }

        val spyBuilder = spyk(mockNotificationBuilder)
        every { spyBuilder.setSubText(any()) } returns spyBuilder

        // When
        val result = customInterceptors.onUpdateIncomingCallNotification(
            builder = spyBuilder,
            callDisplayName = "Alice Johnson",
            call = mockCall,
        )

        // Then
        verify { spyBuilder.setSubText("Incoming from Alice Johnson") }
        assertEquals(spyBuilder, result)
    }

    @Test
    fun `custom interceptor can modify media metadata during update`() = runTest {
        // Given
        val customInterceptors = object : StreamNotificationUpdateInterceptors() {
            override suspend fun onUpdateMediaNotificationMetadata(
                builder: MediaMetadataCompat.Builder,
                call: Call,
                callDisplayName: String?,
            ): MediaMetadataCompat.Builder {
                return builder.putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    "Updated Artist: $callDisplayName",
                )
            }
        }

        val spyBuilder = spyk(mockMediaMetadataBuilder)
        every { spyBuilder.putString(any(), any()) } returns spyBuilder

        // When
        val result = customInterceptors.onUpdateMediaNotificationMetadata(
            builder = spyBuilder,
            call = mockCall,
            callDisplayName = "Bob Wilson",
        )

        // Then
        verify {
            spyBuilder.putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                "Updated Artist: Bob Wilson",
            )
        }
        assertEquals(spyBuilder, result)
    }

    @Test
    fun `custom interceptor can modify playback state during update`() = runTest {
        // Given
        val customInterceptors = object : StreamNotificationUpdateInterceptors() {
            override suspend fun onUpdateMediaNotificationPlaybackState(
                builder: PlaybackStateCompat.Builder,
                call: Call,
                callDisplayName: String?,
            ): PlaybackStateCompat.Builder {
                return builder.setState(PlaybackStateCompat.STATE_PAUSED, 100, 0.5f)
            }
        }

        val spyBuilder = spyk(mockPlaybackStateBuilder)
        every { spyBuilder.setState(any(), any(), any()) } returns spyBuilder

        // When
        val result = customInterceptors.onUpdateMediaNotificationPlaybackState(
            builder = spyBuilder,
            call = mockCall,
            callDisplayName = "Carol Davis",
        )

        // Then
        verify { spyBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 100, 0.5f) }
        assertEquals(spyBuilder, result)
    }

    @Test
    fun `custom interceptor can provide updated media session`() = runTest {
        // Given
        val mockMediaSession = mockk<MediaSessionCompat>()
        val customInterceptors = object : StreamNotificationUpdateInterceptors() {
            override suspend fun onUpdateMediaSessionCompat(
                application: Application,
                channelId: String,
            ): MediaSessionCompat? {
                return mockMediaSession
            }
        }

        // When
        val result = customInterceptors.onUpdateMediaSessionCompat(
            application = mockApplication,
            channelId = "updated-channel",
        )

        // Then
        assertNotNull(result)
        assertEquals(mockMediaSession, result)
    }

    @Test
    fun `interceptors handle null callDisplayName gracefully`() = runTest {
        // Given
        val customInterceptors = object : StreamNotificationUpdateInterceptors() {
            override suspend fun onUpdateOngoingCallNotification(
                builder: NotificationCompat.Builder,
                callDisplayName: String?,
                call: Call,
            ): NotificationCompat.Builder {
                val displayName = callDisplayName ?: "Unknown"
                return builder.setContentTitle("Call with $displayName")
            }
        }

        val spyBuilder = spyk(mockNotificationBuilder)
        every { spyBuilder.setContentTitle(any()) } returns spyBuilder

        // When
        val result = customInterceptors.onUpdateOngoingCallNotification(
            builder = spyBuilder,
            callDisplayName = null,
            call = mockCall,
        )

        // Then
        verify { spyBuilder.setContentTitle("Call with Unknown") }
        assertEquals(spyBuilder, result)
    }
}
