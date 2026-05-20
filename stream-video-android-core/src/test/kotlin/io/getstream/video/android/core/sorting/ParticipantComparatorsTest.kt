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

package io.getstream.video.android.core.sorting

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.ReactionResponse
import io.getstream.video.android.core.events.PinUpdate
import io.getstream.video.android.core.model.Reaction
import io.getstream.video.android.core.pinning.PinType
import io.getstream.video.android.core.pinning.PinUpdateAtTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Test
import org.threeten.bp.OffsetDateTime
import stream.video.sfu.models.ParticipantSource

class ParticipantComparatorsTest {

    private val scope = CoroutineScope(StandardTestDispatcher(TestCoroutineScheduler()))
    private val callActions = stubCallActions()

    @Test
    fun `dominantSpeaker sorts dominant before non-dominant`() {
        val a = participant("a", scope, callActions, dominantSpeaker = true)
        val b = participant("b", scope, callActions, dominantSpeaker = false)
        assertThat(dominantSpeaker.compare(a, b)).isEqualTo(-1)
        assertThat(dominantSpeaker.compare(b, a)).isEqualTo(1)
        assertThat(dominantSpeaker.compare(a, a)).isEqualTo(0)
        assertThat(dominantSpeaker.compare(b, b)).isEqualTo(0)
    }

    @Test
    fun `speaking sorts speaking before silent`() {
        val a = participant("a", scope, callActions, speaking = true)
        val b = participant("b", scope, callActions, speaking = false)
        assertThat(speaking.compare(a, b)).isEqualTo(-1)
        assertThat(speaking.compare(b, a)).isEqualTo(1)
    }

    @Test
    fun `screenSharing sorts presenter before others`() {
        val a = participant("a", scope, callActions, screenSharingEnabled = true)
        val b = participant("b", scope, callActions, screenSharingEnabled = false)
        assertThat(screenSharing.compare(a, b)).isEqualTo(-1)
        assertThat(screenSharing.compare(b, a)).isEqualTo(1)
    }

    @Test
    fun `publishingVideo sorts camera-on first`() {
        val a = participant("a", scope, callActions, videoEnabled = true)
        val b = participant("b", scope, callActions, videoEnabled = false)
        assertThat(publishingVideo.compare(a, b)).isEqualTo(-1)
        assertThat(publishingVideo.compare(b, a)).isEqualTo(1)
    }

    @Test
    fun `publishingAudio sorts mic-on first`() {
        val a = participant("a", scope, callActions, audioEnabled = true)
        val b = participant("b", scope, callActions, audioEnabled = false)
        assertThat(publishingAudio.compare(a, b)).isEqualTo(-1)
        assertThat(publishingAudio.compare(b, a)).isEqualTo(1)
    }

    @Test
    fun `raisedHand sorts hand-raised before others`() {
        val a = participant("a", scope, callActions).apply {
            _reactions.value = listOf(reactionWithType(":raise-hand:"))
        }
        val b = participant("b", scope, callActions)
        assertThat(raisedHand.compare(a, b)).isEqualTo(-1)
        assertThat(raisedHand.compare(b, a)).isEqualTo(1)
    }

    @Test
    fun `byName sorts alphabetically`() {
        val a = participant("a", scope, callActions, name = "Alice")
        val b = participant("b", scope, callActions, name = "Bob")
        assertThat(byName.compare(a, b)).isLessThan(0)
        assertThat(byName.compare(b, a)).isGreaterThan(0)
    }

    @Test
    fun `byUserId sorts lexicographically by userId`() {
        val a = participant("alpha", scope, callActions, userId = "alice")
        val b = participant("beta", scope, callActions, userId = "bob")
        assertThat(byUserId.compare(a, b)).isLessThan(0)
        assertThat(byUserId.compare(b, a)).isGreaterThan(0)
        assertThat(byUserId.compare(a, a)).isEqualTo(0)
    }

    @Test
    fun `byUserId on equal ids returns 0`() {
        val a = participant("a-session", scope, callActions, userId = "u")
        val b = participant("b-session", scope, callActions, userId = "u")
        assertThat(byUserId.compare(a, b)).isEqualTo(0)
    }

    @Test
    fun `byJoinedAt sorts earliest first, nulls last`() {
        val t = OffsetDateTime.parse("2026-05-14T10:00:00Z")
        val a = participant("a", scope, callActions, joinedAt = t)
        val b = participant("b", scope, callActions, joinedAt = t.plusSeconds(10))
        val c = participant("c", scope, callActions, joinedAt = null)
        assertThat(byJoinedAt.compare(a, b)).isLessThan(0)
        assertThat(byJoinedAt.compare(b, a)).isGreaterThan(0)
        assertThat(byJoinedAt.compare(a, c)).isEqualTo(-1)
        assertThat(byJoinedAt.compare(c, a)).isEqualTo(1)
    }

    @Test
    fun `bySourcePriority ranks sources by argument order`() {
        // Ports React's withParticipantSource truth table from sorting.test.ts.
        val pSrt = participant(
            "srt",
            scope,
            callActions,
            source = ParticipantSource.PARTICIPANT_SOURCE_SRT,
        )
        val pRtmp = participant(
            "rtmp",
            scope,
            callActions,
            source = ParticipantSource.PARTICIPANT_SOURCE_RTMP,
        )
        val pSip = participant(
            "sip",
            scope,
            callActions,
            source = ParticipantSource.PARTICIPANT_SOURCE_SIP,
        )
        val pWebrtc = participant(
            "webrtc",
            scope,
            callActions,
            source = ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
        )
        val pWhip = participant(
            "whip",
            scope,
            callActions,
            source = ParticipantSource.PARTICIPANT_SOURCE_WHIP,
        )

        val srtOnly = bySourcePriority(ParticipantSource.PARTICIPANT_SOURCE_SRT)
        assertThat(srtOnly.compare(pWebrtc, pSrt)).isGreaterThan(0)
        assertThat(srtOnly.compare(pSrt, pWebrtc)).isLessThan(0)

        val rtmpOnly = bySourcePriority(ParticipantSource.PARTICIPANT_SOURCE_RTMP)
        // WEBRTC and SIP are both unranked → equal.
        assertThat(rtmpOnly.compare(pWebrtc, pSip)).isEqualTo(0)
        assertThat(rtmpOnly.compare(pRtmp, pSip)).isLessThan(0)
        assertThat(rtmpOnly.compare(pSip, pRtmp)).isGreaterThan(0)

        val rtmpSrt = bySourcePriority(
            ParticipantSource.PARTICIPANT_SOURCE_RTMP,
            ParticipantSource.PARTICIPANT_SOURCE_SRT,
        )
        assertThat(rtmpSrt.compare(pRtmp, pSrt)).isLessThan(0)
        assertThat(rtmpSrt.compare(pSrt, pRtmp)).isGreaterThan(0)
        assertThat(rtmpSrt.compare(pWhip, pSrt)).isGreaterThan(0)

        val whipRtmpSrt = bySourcePriority(
            ParticipantSource.PARTICIPANT_SOURCE_WHIP,
            ParticipantSource.PARTICIPANT_SOURCE_RTMP,
            ParticipantSource.PARTICIPANT_SOURCE_SRT,
        )
        assertThat(whipRtmpSrt.compare(pWhip, pSrt)).isLessThan(0)
    }

    @Test
    fun `bySourcePriority full sort in declared order`() {
        val sources = listOf(
            ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
            ParticipantSource.PARTICIPANT_SOURCE_SRT,
            ParticipantSource.PARTICIPANT_SOURCE_RTMP,
            ParticipantSource.PARTICIPANT_SOURCE_SIP,
            ParticipantSource.PARTICIPANT_SOURCE_RTSP,
            ParticipantSource.PARTICIPANT_SOURCE_WHIP,
        )
        val ps = sources.mapIndexed { i, s -> participant("p$i", scope, callActions, source = s) }
        val sorted = ps.sortedWith(
            bySourcePriority(
                ParticipantSource.PARTICIPANT_SOURCE_RTMP,
                ParticipantSource.PARTICIPANT_SOURCE_SRT,
                ParticipantSource.PARTICIPANT_SOURCE_WHIP,
                ParticipantSource.PARTICIPANT_SOURCE_RTSP,
                ParticipantSource.PARTICIPANT_SOURCE_SIP,
                ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
            ),
        )
        assertThat(sorted.map { it.source }).containsExactly(
            ParticipantSource.PARTICIPANT_SOURCE_RTMP,
            ParticipantSource.PARTICIPANT_SOURCE_SRT,
            ParticipantSource.PARTICIPANT_SOURCE_WHIP,
            ParticipantSource.PARTICIPANT_SOURCE_RTSP,
            ParticipantSource.PARTICIPANT_SOURCE_SIP,
            ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
        ).inOrder()
    }

    @Test
    fun `bySourcePriority does NOT have the SIP=RTSP collision the old code had`() {
        // The old participantSourceRank assigned SIP=2 and RTSP=2, breaking deterministic
        // ordering between the two. The new API assigns priority by argument position so
        // each source has a unique rank.
        val pSip = participant(
            "sip",
            scope,
            callActions,
            source = ParticipantSource.PARTICIPANT_SOURCE_SIP,
        )
        val pRtsp = participant(
            "rtsp",
            scope,
            callActions,
            source = ParticipantSource.PARTICIPANT_SOURCE_RTSP,
        )
        val cmp = bySourcePriority(
            ParticipantSource.PARTICIPANT_SOURCE_RTSP,
            ParticipantSource.PARTICIPANT_SOURCE_SIP,
        )
        assertThat(cmp.compare(pRtsp, pSip)).isLessThan(0)
        assertThat(cmp.compare(pSip, pRtsp)).isGreaterThan(0)
    }

    @Test
    fun `byRole prioritizes participants holding any listed role`() {
        val host = participant("h", scope, callActions, roles = listOf("host"))
        val guest = participant("g", scope, callActions, roles = listOf("guest"))
        val cmp = byRole("host", "admin")
        assertThat(cmp.compare(host, guest)).isEqualTo(-1)
        assertThat(cmp.compare(guest, host)).isEqualTo(1)
        assertThat(cmp.compare(host, host)).isEqualTo(0)
        assertThat(cmp.compare(guest, guest)).isEqualTo(0)
    }

    @Test
    fun `pinned sorts pinned before unpinned`() {
        val now = OffsetDateTime.parse("2026-05-14T10:00:00Z")
        val pin = pinUpdateAt("a", now, PinType.Server)
        val cmp = pinned(mapOf("a" to pin))
        val a = participant("a", scope, callActions)
        val b = participant("b", scope, callActions)
        assertThat(cmp.compare(a, b)).isEqualTo(-1)
        assertThat(cmp.compare(b, a)).isEqualTo(1)
    }

    @Test
    fun `pinned prefers local pin over server pin`() {
        val now = OffsetDateTime.parse("2026-05-14T10:00:00Z")
        val pins = mapOf(
            "a" to pinUpdateAt("a", now, PinType.Local),
            "b" to pinUpdateAt("b", now, PinType.Server),
        )
        val a = participant("a", scope, callActions)
        val b = participant("b", scope, callActions)
        val cmp = pinned(pins)
        assertThat(cmp.compare(a, b)).isEqualTo(-1)
        assertThat(cmp.compare(b, a)).isEqualTo(1)
    }

    @Test
    fun `pinned prefers more recently pinned within same type`() {
        val earlier = OffsetDateTime.parse("2026-05-14T10:00:00Z")
        val later = OffsetDateTime.parse("2026-05-14T11:00:00Z")
        val pins = mapOf(
            "a" to pinUpdateAt("a", earlier, PinType.Server),
            "b" to pinUpdateAt("b", later, PinType.Server),
        )
        val a = participant("a", scope, callActions)
        val b = participant("b", scope, callActions)
        val cmp = pinned(pins)
        assertThat(cmp.compare(a, b)).isGreaterThan(0)
        assertThat(cmp.compare(b, a)).isLessThan(0)
    }

    private fun pinUpdateAt(
        sessionId: String,
        at: OffsetDateTime,
        type: PinType,
    ): PinUpdateAtTime = PinUpdateAtTime(
        it = PinUpdate(sessionId = sessionId, userId = "user-$sessionId"),
        at = at,
        type = type,
    )

    private fun reactionWithType(type: String): Reaction = Reaction(
        id = "reaction-$type",
        response = ReactionResponse(
            type = type,
            user = USER_DUMMY,
        ),
        createdAt = System.currentTimeMillis(),
    )

    private companion object {
        private val USER_DUMMY: io.getstream.android.video.generated.models.UserResponse =
            io.getstream.android.video.generated.models.UserResponse(
                createdAt = OffsetDateTime.parse("2026-05-14T00:00:00Z"),
                id = "u",
                language = "en",
                role = "user",
                updatedAt = OffsetDateTime.parse("2026-05-14T00:00:00Z"),
            )
    }
}
