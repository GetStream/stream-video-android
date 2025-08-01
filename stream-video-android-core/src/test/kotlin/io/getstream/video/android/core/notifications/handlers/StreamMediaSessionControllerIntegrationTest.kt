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
import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.model.StreamCallId
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.time.Duration

@OptIn(ExperimentalStreamVideoApi::class)
class StreamMediaSessionControllerIntegrationTest {
    private lateinit var controller: DefaultStreamMediaSessionController
    private lateinit var interceptors: StreamNotificationBuilderInterceptors
    private lateinit var updateInterceptors: StreamNotificationUpdateInterceptors
    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var call: Call
    private val callId = StreamCallId.fromCallCid("default:123")
    private val channelId = "test_channel"
    private val callDisplayName = "Test Call"

    @Before
    fun setup() {
        interceptors = mockk(relaxed = true)
        updateInterceptors = mockk(relaxed = true)
        application = mockk(relaxed = true)
        context = mockk(relaxed = true)
        call = mockk(relaxed = true)
        // Create two different mock MediaSessionCompat instances
        val mediaSession1 = mockk<MediaSessionCompat>(relaxed = true)
        val mediaSession2 = mockk<MediaSessionCompat>(relaxed = true)
        var callCount = 0
        every { interceptors.onCreateMediaSessionCompat(any(), any()) } answers {
            callCount++
            if (callCount == 1) mediaSession1 else mediaSession2
        }
        controller = DefaultStreamMediaSessionController(interceptors, updateInterceptors)

        val mockSubscriber = mockk<Subscriber>(relaxed = true)
        val mockSession = mockk<RtcSession>(relaxed = true)
        val mockState = mockk<CallState>(relaxed = true)
        every { mockState.duration } returns MutableStateFlow(Duration.ZERO)
        every { call.state } returns mockState
        every { mockSubscriber.isEnabled() } returns MutableStateFlow(true)
        every { mockSession.subscriber } returns mockSubscriber
        every { call.session } returns mockSession
        every { call.cid } returns "default:123"
    }

    @Test
    fun `provideMediaSession returns same instance for same callId and does not create duplicate`() {
        val session1 = controller.provideMediaSession(application, callId, channelId, null)
        val session2 = controller.provideMediaSession(application, callId, channelId, null)
        assertSame(session1, session2)
        verify(exactly = 1) { interceptors.onCreateMediaSessionCompat(application, channelId) }
    }

    @Test
    fun `provideMediaSession creates new instance after clear`() {
        val session1 = controller.provideMediaSession(application, callId, channelId, null)
        controller.clear(callId)
        val session2 = controller.provideMediaSession(application, callId, channelId, null)
        assertNotSame(session1, session2)
        verify(exactly = 2) { interceptors.onCreateMediaSessionCompat(application, channelId) }
    }

    @Test
    fun `updateMetadata calls setMetadata and interceptors`() = runTest {
        val session = controller.provideMediaSession(application, callId, channelId, null)
        val builder = MediaMetadataCompat.Builder()
        controller.updateMetadata(context, session, call, callDisplayName, builder)
        verify { session.setMetadata(any()) }
        verify { interceptors.onBuildMediaNotificationMetadata(any(), callId) }
        coVerify {
            updateInterceptors.onUpdateMediaNotificationMetadata(
                any(),
                call,
                callDisplayName,
            )
        }
    }

    @Test
    fun `updatePlaybackState calls setPlaybackState and interceptors`() = runTest {
        val session = controller.provideMediaSession(application, callId, channelId, null)
        val builder = PlaybackStateCompat.Builder()
        controller.updatePlaybackState(context, session, call, callDisplayName, builder)
        verify { session.setPlaybackState(any()) }
        verify { interceptors.onBuildMediaNotificationPlaybackState(any(), callId) }
        coVerify {
            updateInterceptors.onUpdateMediaNotificationPlaybackState(
                any(),
                call,
                callDisplayName,
            )
        }
    }

    @Test
    fun `initialMetadata calls setMetadata and builder interceptor`() {
        val session = controller.provideMediaSession(application, callId, channelId, null)
        val builder = MediaMetadataCompat.Builder()
        controller.initialMetadata(context, session, callId, builder)
        verify { session.setMetadata(any()) }
        verify { interceptors.onBuildMediaNotificationMetadata(any(), callId) }
    }

    @Test
    fun `initialPlaybackState calls setPlaybackState and builder interceptor`() {
        val session = controller.provideMediaSession(application, callId, channelId, null)
        val builder = PlaybackStateCompat.Builder()
        controller.initialPlaybackState(context, session, callId, builder)
        verify { session.setPlaybackState(any()) }
        verify { interceptors.onBuildMediaNotificationPlaybackState(any(), callId) }
    }
}
