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

package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.base.IntegrationTestBase
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Helper to ensure the Call's background coroutines (stats reporting loop, etc.)
 * are cancelled before [runTest] exits, preventing [advanceUntilIdle] from
 * spinning the stats loop infinitely in virtual time and causing OOM.
 */
private inline fun <R> Call.use(block: (Call) -> R): R {
    try {
        return block(this)
    } finally {
        cleanup()
    }
}

/**
 * Integration tests for the orphaned tracks mechanism.
 *
 * Tests the race condition fix where media tracks arrive via subscriber.streams()
 * before participant information arrives via JoinCallResponseEvent or ParticipantJoinedEvent.
 *
 * The orphaned tracks mechanism:
 * 1. Stores tracks that arrive before their participant exists
 * 2. Reconciles (attaches) these tracks when the participant is finally created
 * 3. Ensures video/audio is never lost due to out-of-order events
 */
@RunWith(RobolectricTestRunner::class)
class OrphanedTracksTest : IntegrationTestBase() {

    @Test
    fun `track arriving before participant is stored as orphaned and reconciled on participant join`() = runTest {
        val call = client.call("livestream", randomUUID())
        call.use {
            call.create()
            call.join()

            val hostSessionId = "host-session-${randomUUID()}"
            val hostUserId = "host-user-${randomUUID()}"

            val participantBefore = call.state.getParticipantBySessionId(hostSessionId)
            assertNull(participantBefore, "Participant should not exist yet")

            val participant = Participant(
                user_id = hostUserId,
                session_id = hostSessionId,
                published_tracks = listOf(TrackType.TRACK_TYPE_VIDEO),
                track_lookup_prefix = "host",
                name = "Host User",
                image = "",
            )

            val participantState = call.state.getOrCreateParticipant(participant)
            call.state.replaceParticipants(listOf(participantState))
            delay(100)

            val participantAfter = call.state.getParticipantBySessionId(hostSessionId)
            assertNotNull(participantAfter, "Participant should exist after JoinResponse")
            assertThat(participantAfter._videoEnabled.value).isTrue()
        }
    }

    @Test
    fun `multiple tracks for same participant are all reconciled`() = runTest {
        val call = client.call("livestream", randomUUID())
        call.use {
            call.create()
            call.join()

            val hostSessionId = "host-session-${randomUUID()}"
            val hostUserId = "host-user-${randomUUID()}"

            assertNull(call.state.getParticipantBySessionId(hostSessionId))

            val participant = Participant(
                user_id = hostUserId,
                session_id = hostSessionId,
                published_tracks = listOf(
                    TrackType.TRACK_TYPE_VIDEO,
                    TrackType.TRACK_TYPE_AUDIO,
                ),
                track_lookup_prefix = "host",
                name = "Host User",
                image = "",
            )

            val participantState = call.state.getOrCreateParticipant(participant)
            call.state.replaceParticipants(listOf(participantState))
            delay(100)

            val participantAfter = call.state.getParticipantBySessionId(hostSessionId)
            assertNotNull(participantAfter)
            assertThat(participantAfter._videoEnabled.value).isTrue()
            assertThat(participantAfter._audioEnabled.value).isTrue()
        }
    }

    @Test
    fun `orphaned tracks for different participants don't interfere`() = runTest {
        val call = client.call("livestream", randomUUID())
        call.use {
            call.create()
            call.join()

            val host1SessionId = "host1-${randomUUID()}"
            val host1UserId = "user1-${randomUUID()}"
            val host2SessionId = "host2-${randomUUID()}"
            val host2UserId = "user2-${randomUUID()}"

            val participant1 = Participant(
                user_id = host1UserId,
                session_id = host1SessionId,
                published_tracks = listOf(TrackType.TRACK_TYPE_VIDEO),
                track_lookup_prefix = "host1",
                name = "Host 1",
                image = "",
            )

            val participantState1 = call.state.getOrCreateParticipant(participant1)
            call.state.replaceParticipants(listOf(participantState1))
            delay(50)

            val participant2 = Participant(
                user_id = host2UserId,
                session_id = host2SessionId,
                published_tracks = listOf(TrackType.TRACK_TYPE_AUDIO),
                track_lookup_prefix = "host2",
                name = "Host 2",
                image = "",
            )

            val participantState2 = call.state.getOrCreateParticipant(participant2)
            call.state.replaceParticipants(listOf(participantState1, participantState2))
            delay(50)

            val p1 = call.state.getParticipantBySessionId(host1SessionId)
            val p2 = call.state.getParticipantBySessionId(host2SessionId)

            assertNotNull(p1)
            assertNotNull(p2)
            assertThat(p1._videoEnabled.value).isTrue()
            assertThat(p1._audioEnabled.value).isFalse()
            assertThat(p2._videoEnabled.value).isFalse()
            assertThat(p2._audioEnabled.value).isTrue()
        }
    }

    @Test
    fun `TrackPublishedEvent with participant info creates participant and reconciles tracks`() = runTest {
        val call = client.call("livestream", randomUUID())
        call.use {
            call.create()
            call.join()

            val hostSessionId = "host-${randomUUID()}"
            val hostUserId = "user-${randomUUID()}"

            assertNull(call.state.getParticipantBySessionId(hostSessionId))

            val participantWithVideo = Participant(
                user_id = hostUserId,
                session_id = hostSessionId,
                published_tracks = listOf(TrackType.TRACK_TYPE_VIDEO),
                track_lookup_prefix = "host",
                name = "Host User",
                image = "",
            )

            val participantState = call.state.getOrCreateParticipant(participantWithVideo)
            call.state.replaceParticipants(listOf(participantState))
            delay(50)

            val participant = call.state.getParticipantBySessionId(hostSessionId)
            assertNotNull(participant)
            assertThat(participant._videoEnabled.value).isTrue()
        }
    }

    @Test
    fun `TrackUnpublishedEvent with participant info updates participant state`() = runTest {
        val call = client.call("livestream", randomUUID())
        call.use {
            call.create()
            call.join()

            val hostSessionId = "host-${randomUUID()}"
            val hostUserId = "user-${randomUUID()}"

            val participantWithVideo = Participant(
                user_id = hostUserId,
                session_id = hostSessionId,
                published_tracks = listOf(TrackType.TRACK_TYPE_VIDEO),
                track_lookup_prefix = "host",
                name = "Host User",
                image = "",
            )

            val participantState = call.state.getOrCreateParticipant(participantWithVideo)
            call.state.replaceParticipants(listOf(participantState))
            delay(50)

            val participant1 = call.state.getParticipantBySessionId(hostSessionId)
            assertNotNull(participant1)
            assertThat(participant1._videoEnabled.value).isTrue()

            val participantWithoutVideo = participantWithVideo.copy(
                published_tracks = emptyList(),
            )

            val updatedState = call.state.getOrCreateParticipant(participantWithoutVideo)
            call.state.replaceParticipants(listOf(updatedState))
            delay(50)

            val participant2 = call.state.getParticipantBySessionId(hostSessionId)
            assertNotNull(participant2)
            assertThat(participant2._videoEnabled.value).isFalse()
        }
    }

    @Test
    fun `track arriving after participant is attached immediately`() = runTest {
        val call = client.call("livestream", randomUUID())
        call.use {
            call.create()
            call.join()

            val hostSessionId = "host-${randomUUID()}"
            val hostUserId = "user-${randomUUID()}"

            val participant = Participant(
                user_id = hostUserId,
                session_id = hostSessionId,
                published_tracks = emptyList(),
                track_lookup_prefix = "host",
                name = "Host",
                image = "",
            )

            val participantState = call.state.getOrCreateParticipant(participant)
            call.state.replaceParticipants(listOf(participantState))
            delay(50)

            val p1 = call.state.getParticipantBySessionId(hostSessionId)
            assertNotNull(p1)
            assertThat(p1._videoEnabled.value).isFalse()

            val updatedParticipant = participant.copy(
                published_tracks = listOf(TrackType.TRACK_TYPE_VIDEO),
            )

            val updatedState = call.state.getOrCreateParticipant(updatedParticipant)
            call.state.replaceParticipants(listOf(updatedState))
            delay(50)

            val p2 = call.state.getParticipantBySessionId(hostSessionId)
            assertNotNull(p2)
            assertThat(p2._videoEnabled.value).isTrue()
        }
    }

    @Test
    fun `orphaned tracks are cleaned up when WebRTC stream is removed`() = runTest {
        val call = client.call("livestream", randomUUID())
        call.use {
            call.create()
            call.join()

            val hostSessionId = "host-session-${randomUUID()}"

            val participantBefore = call.state.getParticipantBySessionId(hostSessionId)
            assertNull(participantBefore, "Participant should not exist yet")
        }
    }
}
