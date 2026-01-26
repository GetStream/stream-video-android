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

package io.getstream.video.android.core.notifications.handlers

import android.app.Application
import android.app.PendingIntent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.model.StreamCallId
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

class StreamNotificationBuilderInterceptorsTest {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockNotificationBuilder: NotificationCompat.Builder

    @MockK
    lateinit var mockPendingIntent: PendingIntent

    @MockK
    lateinit var mockStreamCallId: StreamCallId

    @MockK
    lateinit var mockMediaMetadataBuilder: MediaMetadataCompat.Builder

    @MockK
    lateinit var mockPlaybackStateBuilder: PlaybackStateCompat.Builder

    private lateinit var interceptors: StreamNotificationBuilderInterceptors

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        interceptors = StreamNotificationBuilderInterceptors()
    }

    @Test
    fun `onBuildIncomingCallNotification returns builder unchanged by default`() {
        // Given
        val callerName = "John Doe"
        val shouldHaveContentIntent = true

        // When
        val result = interceptors.onBuildIncomingCallNotification(
            builder = mockNotificationBuilder,
            fullScreenPendingIntent = mockPendingIntent,
            acceptCallPendingIntent = mockPendingIntent,
            rejectCallPendingIntent = mockPendingIntent,
            callerName = callerName,
            shouldHaveContentIntent = shouldHaveContentIntent,
        )

        // Then
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun `onBuildOutgoingCallNotification returns builder unchanged by default`() = runTest {
        // Given
        val ringingState = RingingState.Outgoing()
        val callDisplayName = "John Doe"

        // When
        val result = interceptors.onBuildOutgoingCallNotification(
            builder = mockNotificationBuilder,
            ringingState = ringingState,
            callId = mockStreamCallId,
            callDisplayName = callDisplayName,
            shouldHaveContentIntent = true,
        )

        // Then
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun `onBuildOngoingCallNotification returns builder unchanged by default`() {
        // Given
        val callDisplayName = "John Doe"

        // When
        val result = interceptors.onBuildOngoingCallNotification(
            builder = mockNotificationBuilder,
            callId = mockStreamCallId,
            callDisplayName = callDisplayName,
            isOutgoingCall = false,
            remoteParticipantCount = 1,
        )

        // Then
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun `onBuildMissedCallNotification returns builder unchanged by default`() {
        // Given
        val callDisplayName = "John Doe"

        // When
        val result = interceptors.onBuildMissedCallNotification(
            builder = mockNotificationBuilder,
            callDisplayName = callDisplayName,
        )

        // Then
        assertEquals(mockNotificationBuilder, result)
    }

    @Test
    fun `onCreateMediaSessionCompat returns null by default`() {
        // Given
        val channelId = "test-channel"

        // When
        val result = interceptors.onCreateMediaSessionCompat(
            application = mockApplication,
            channelId = channelId,
        )

        // Then
        assertNull(result)
    }

    @Test
    fun `onBuildMediaNotificationMetadata returns builder unchanged by default`() {
        // When
        val result = interceptors.onBuildMediaNotificationMetadata(
            builder = mockMediaMetadataBuilder,
            callId = mockStreamCallId,
        )

        // Then
        assertEquals(mockMediaMetadataBuilder, result)
    }

    @Test
    fun `onBuildMediaNotificationPlaybackState returns builder unchanged by default`() {
        // When
        val result = interceptors.onBuildMediaNotificationPlaybackState(
            builder = mockPlaybackStateBuilder,
            callId = mockStreamCallId,
        )

        // Then
        assertEquals(mockPlaybackStateBuilder, result)
    }

    @Test
    fun `custom interceptor can modify incoming call notification`() {
        // Given
        val customInterceptors = object : StreamNotificationBuilderInterceptors() {
            override fun onBuildIncomingCallNotification(
                builder: NotificationCompat.Builder,
                fullScreenPendingIntent: PendingIntent,
                acceptCallPendingIntent: PendingIntent,
                rejectCallPendingIntent: PendingIntent,
                callerName: String?,
                shouldHaveContentIntent: Boolean,
            ): NotificationCompat.Builder {
                return builder.setContentTitle("Custom Title: $callerName")
            }
        }

        val spyBuilder = spyk(mockNotificationBuilder)
        every { spyBuilder.setContentTitle(any()) } returns spyBuilder

        // When
        val result = customInterceptors.onBuildIncomingCallNotification(
            builder = spyBuilder,
            fullScreenPendingIntent = mockPendingIntent,
            acceptCallPendingIntent = mockPendingIntent,
            rejectCallPendingIntent = mockPendingIntent,
            callerName = "John Doe",
            shouldHaveContentIntent = true,
        )

        // Then
        verify { spyBuilder.setContentTitle("Custom Title: John Doe") }
        assertEquals(spyBuilder, result)
    }

    @Test
    fun `custom interceptor can modify media metadata`() {
        // Given
        val customInterceptors = object : StreamNotificationBuilderInterceptors() {
            override fun onBuildMediaNotificationMetadata(
                builder: MediaMetadataCompat.Builder,
                callId: StreamCallId,
            ): MediaMetadataCompat.Builder {
                return builder.putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    "Custom Media Title",
                )
            }
        }

        val spyBuilder = spyk(mockMediaMetadataBuilder)
        every { spyBuilder.putString(any(), any()) } returns spyBuilder

        // When
        val result = customInterceptors.onBuildMediaNotificationMetadata(
            builder = spyBuilder,
            callId = mockStreamCallId,
        )

        // Then
        verify {
            spyBuilder.putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                "Custom Media Title",
            )
        }
        assertEquals(spyBuilder, result)
    }

    @Test
    fun `custom interceptor can provide media session`() {
        // Given
        val mockMediaSession = mockk<MediaSessionCompat>()
        val customInterceptors = object : StreamNotificationBuilderInterceptors() {
            override fun onCreateMediaSessionCompat(
                application: Application,
                channelId: String,
            ): MediaSessionCompat? {
                return mockMediaSession
            }
        }

        // When
        val result = customInterceptors.onCreateMediaSessionCompat(
            application = mockApplication,
            channelId = "test-channel",
        )

        // Then
        assertNotNull(result)
        assertEquals(mockMediaSession, result)
    }

    @Test
    fun `custom interceptor can modify playback state`() {
        // Given
        val customInterceptors = object : StreamNotificationBuilderInterceptors() {
            override fun onBuildMediaNotificationPlaybackState(
                builder: PlaybackStateCompat.Builder,
                callId: StreamCallId,
            ): PlaybackStateCompat.Builder {
                return builder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
            }
        }

        val spyBuilder = spyk(mockPlaybackStateBuilder)
        every { spyBuilder.setState(any(), any(), any()) } returns spyBuilder

        // When
        val result = customInterceptors.onBuildMediaNotificationPlaybackState(
            builder = spyBuilder,
            callId = mockStreamCallId,
        )

        // Then
        verify { spyBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f) }
        assertEquals(spyBuilder, result)
    }
}
