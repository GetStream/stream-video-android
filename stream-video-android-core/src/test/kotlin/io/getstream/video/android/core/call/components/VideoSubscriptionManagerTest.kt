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

package io.getstream.video.android.core.call.components

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.utils.SerialProcessor
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension

@OptIn(ExperimentalCoroutinesApi::class)
internal class VideoSubscriptionManagerTest {

    private val peerConnections = PeerConnections()
    private val sessionManager = CallSessionManager().apply { sessionId = "local-session" }

    private fun manager(
        state: CallState = mockk(relaxed = true),
        scope: kotlinx.coroutines.CoroutineScope,
    ): VideoSubscriptionManager {
        every { state.participants } returns MutableStateFlow(emptyList())
        every { state.remoteParticipants } returns MutableStateFlow(emptyList())
        every { state._participantVideoEnabledOverrides } returns MutableStateFlow(emptyMap())
        return VideoSubscriptionManager(
            state = state,
            peerConnections = peerConnections,
            sessionManager = sessionManager,
            coroutineScope = scope,
            serialProcessor = SerialProcessor(scope),
            sfuConnectionModule = { mockk(relaxed = true) },
        )
    }

    @Test
    fun `setVideoSubscriptions forwards participants and overrides to the subscriber`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val subscriber = mockk<Subscriber>(relaxed = true)
        peerConnections.setSubscriber(subscriber)
        val participants = listOf(mockk<ParticipantState>(relaxed = true))
        val remote = listOf(mockk<ParticipantState>(relaxed = true))
        val state = mockk<CallState>(relaxed = true)
        every { state.participants } returns MutableStateFlow(participants)
        every { state.remoteParticipants } returns MutableStateFlow(remote)
        every { state._participantVideoEnabledOverrides } returns MutableStateFlow(emptyMap())

        val subscriptions = VideoSubscriptionManager(
            state = state,
            peerConnections = peerConnections,
            sessionManager = sessionManager,
            coroutineScope = backgroundScope,
            serialProcessor = SerialProcessor(backgroundScope),
            sfuConnectionModule = { mockk(relaxed = true) },
        )

        subscriptions.setVideoSubscriptions(useDefaults = true)
        advanceUntilIdle()

        coVerify {
            subscriber.setVideoSubscriptions(
                subscriptions.trackOverridesHandler,
                participants,
                remote,
                true,
            )
        }
    }

    @Test
    fun `updateTrackDimensions records the viewport and refreshes remote subscriptions`() =
        runTest(UnconfinedTestDispatcher()) {
            val subscriber = mockk<Subscriber>(relaxed = true)
            peerConnections.setSubscriber(subscriber)
            val subscriptions = manager(scope = backgroundScope)
            val dimensions = VideoDimension(640, 480)

            subscriptions.updateTrackDimensions(
                sessionId = "remote-session",
                trackType = TrackType.TRACK_TYPE_VIDEO,
                visible = true,
                dimensions = dimensions,
                viewportId = "viewport-1",
            )
            advanceUntilIdle()

            verify {
                subscriber.setTrackDimension(
                    "viewport-1",
                    "remote-session",
                    TrackType.TRACK_TYPE_VIDEO,
                    true,
                    dimensions,
                )
            }
            coVerify {
                subscriber.setVideoSubscriptions(any(), any(), any(), any())
            }
        }

    @Test
    fun `updateTrackDimensions for the local session does not refresh subscriptions`() =
        runTest(UnconfinedTestDispatcher()) {
            val subscriber = mockk<Subscriber>(relaxed = true)
            peerConnections.setSubscriber(subscriber)
            val subscriptions = manager(scope = backgroundScope)

            subscriptions.updateTrackDimensions(
                sessionId = "local-session",
                trackType = TrackType.TRACK_TYPE_VIDEO,
                visible = false,
            )
            advanceUntilIdle()

            verify {
                subscriber.setTrackDimension(
                    "local-session",
                    "local-session",
                    TrackType.TRACK_TYPE_VIDEO,
                    false,
                    Subscriber.defaultVideoDimension,
                )
            }
            coVerify(exactly = 0) {
                subscriber.setVideoSubscriptions(any(), any(), any(), any())
            }
        }

    @Test
    fun `applySubscriptionsNow updates the subscriber immediately`() = runTest {
        val subscriber = mockk<Subscriber>(relaxed = true)
        peerConnections.setSubscriber(subscriber)
        val subscriptions = manager(scope = backgroundScope)

        subscriptions.applySubscriptionsNow()

        coVerify {
            subscriber.setVideoSubscriptions(any(), any(), any(), any())
        }
        assertThat(subscriptions.trackOverridesHandler).isNotNull()
    }
}
