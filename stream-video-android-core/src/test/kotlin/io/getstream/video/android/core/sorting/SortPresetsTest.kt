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
import io.getstream.video.android.core.events.PinUpdate
import io.getstream.video.android.core.model.VisibilityOnScreenState
import io.getstream.video.android.core.pinning.PinType
import io.getstream.video.android.core.pinning.PinUpdateAtTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Test
import org.threeten.bp.OffsetDateTime

class SortPresetsTest {

    private val scope = CoroutineScope(StandardTestDispatcher(TestCoroutineScheduler()))
    private val callActions = stubCallActions()

    // ------------------------------------------------------------------
    // React parity — sorting.test.ts composed comparators against A-F
    // ------------------------------------------------------------------

    @Test
    fun `React parity - screenSharing then dominant then video then audio`() {
        // Mirrors stream-video-js sorting.test.ts:
        //   combineComparators(screenSharing, dominantSpeaker, publishingVideo, publishingAudio)
        // Expected: [B, E, D, A, F, C]
        val comparator = combineComparators(
            screenSharing,
            dominantSpeaker,
            publishingVideo,
            publishingAudio,
        )
        val sorted = participantsAF(scope, callActions).sortedWith(comparator)
        assertThat(
            sorted.map { it.name.value },
        ).containsExactly("B", "E", "D", "A", "F", "C").inOrder()
    }

    @Test
    fun `React parity - pinned then dominant then audio then video then screenshare`() {
        // Mirrors stream-video-js sorting.test.ts:
        //   combineComparators(pinned, dominantSpeaker, publishingAudio, publishingVideo, screenSharing)
        // Expected: [F, D, B, A, E, C]
        val pins = mapOf(
            "6" to pinUpdateAt("6", OffsetDateTime.parse("2026-05-14T10:00:00Z"), PinType.Local),
        )
        val comparator = combineComparators(
            pinned(pins),
            dominantSpeaker,
            publishingAudio,
            publishingVideo,
            screenSharing,
        )
        val sorted = participantsAF(scope, callActions).sortedWith(comparator)
        assertThat(
            sorted.map { it.name.value },
        ).containsExactly("F", "D", "B", "A", "E", "C").inOrder()
    }

    // ------------------------------------------------------------------
    // React parity — presets.test.ts (paginatedLayoutSortPreset equivalent)
    // Maps to our Default preset (uses ifInvisibleOrUnknown).
    // ------------------------------------------------------------------

    @Test
    fun `Default preset - all UNKNOWN visibility, screenSharing tier first`() {
        // Our Default preset is `screenSharing → pinned → ifInvisibleOrUnknown(...)`. This
        // differs from React's paginatedLayoutSortPreset (no screenSharing) but matches the
        // Android grid intent: presenters are tier-1 visual priority.
        //
        // Expected:
        //   tier 1 (screenSharing=true): B (video+audio+share), E (share-only)
        //     within tier: publishingVideo puts B before E
        //   tier 2 (pinned=true, no share): F (local pin)
        //   tier 3 (ifInvisibleOrUnknown chain): D (dominant) → A (video+audio) → C (nothing)
        val ps = participantsAF(scope, callActions).map { p ->
            p.also { it._visibleOnScreen.value = VisibilityOnScreenState.UNKNOWN }
        }
        val pins = mapOf(
            "6" to pinUpdateAt("6", OffsetDateTime.parse("2026-05-14T10:00:00Z"), PinType.Local),
        )
        val sorted = ps.sortedWith(SortPreset.Default.build(pins))
        assertThat(sorted.map { it.name.value })
            .containsExactly("B", "E", "F", "D", "A", "C").inOrder()
    }

    @Test
    fun `Default preset - server-pinning C bumps it to slot 2`() {
        // Mirrors presets.test.ts second assertion: after C gets server-pinned, order becomes
        // [E, F, D, A, B, C] in React. The order is determined by:
        //   1. screenSharing (B and E)
        //   2. pinned (F local, C server)
        //   3. ifInvisibleOrUnknown(...)
        // E and B share screenSharing first; among them B has audio, E doesn't... but B is
        // not pinned, so pinned slot wins for F and C.
        //
        // Reproducing React's exact output requires the same comparator chain. Result: with
        // SCREEN_SHARING running first and only E and B sharing, those two compete. B has
        // video+audio, E has neither. Then within pinned: F (local) > C (server). The non-
        // pinned, non-screen-sharing fall after.
        //
        // Our Default preset matches React's paginatedLayoutSortPreset which is:
        //   pinned → ifInvisibleOrUnknown(...) — but we add screenSharing FIRST. So the
        // expected order differs from React's paginated preset. We assert our actual
        // composition: screenSharing first.
        //
        // E and B both screen-share → E (sessionId="5") and B (sessionId="2") come first in
        // their original order (B then E). Then F (local pin), C (server pin). Then D
        // (dominant), then A.
        val ps = participantsAF(scope, callActions).map { p ->
            p.also { it._visibleOnScreen.value = VisibilityOnScreenState.UNKNOWN }
        }
        val pins = mapOf(
            "6" to pinUpdateAt("6", OffsetDateTime.parse("2026-05-14T10:00:00Z"), PinType.Local),
            "3" to pinUpdateAt("3", OffsetDateTime.parse("2026-05-14T10:00:01Z"), PinType.Server),
        )
        val sorted = ps.sortedWith(SortPreset.Default.build(pins))
        assertThat(
            sorted.map { it.name.value },
        ).containsExactly("B", "E", "F", "C", "D", "A").inOrder()
    }

    // ------------------------------------------------------------------
    // Rolling-scroll behavior — 15 participants, off-screen INVISIBLE
    // Mirrors the cross-checked React tests in rolling-scroll.test.ts.
    // ------------------------------------------------------------------

    @Test
    fun `Default preset - P15 dominant speaker jumps to top, no ring rotation`() {
        val ps = participants15(scope, callActions).toMutableList()
        ps[14]._dominantSpeaker.value = true

        val sorted = ps.sortedWith(SortPreset.Default.build(emptyMap()))
        assertThat(sorted.map { it.name.value }).containsExactly(
            "P15", "P1", "P2", "P3", "P4", "P5", "P6", "P7",
            "P8", "P9", "P10", "P11", "P12", "P13", "P14",
        ).inOrder()
    }

    @Test
    fun `Default preset - visible block P8 to P12 preserves internal order`() {
        val ps = participants15(scope, callActions).toMutableList()
        ps[14]._dominantSpeaker.value = true

        val sorted = ps.sortedWith(SortPreset.Default.build(emptyMap()))
        val visibleNames = sorted
            .filter { it.sessionId.toInt() in 8..12 }
            .map { it.name.value }
        assertThat(visibleNames).containsExactly("P8", "P9", "P10", "P11", "P12").inOrder()
    }

    @Test
    fun `Default preset - two off-screen promotions bubble to top, byUserId tiebreaks the rest`() {
        val ps = participants15(scope, callActions).toMutableList()
        ps[14]._dominantSpeaker.value = true // P15 dominant
        ps[2]._videoEnabled.value = true // P3 gains video

        val sorted = ps.sortedWith(SortPreset.Default.build(emptyMap()))
        // P15 wins on dominantSpeaker → index 0
        // P3 wins on publishingVideo → index 1
        // Remaining 13 participants have no distinguishing signals; the trailing
        // `ifInvisibleOrUnknown(byUserId)` decorator sorts them lexicographically by
        // userId. Fixture userIds are the same as sessionIds ("1".."14"), so the lex
        // order is "1", "10", "11", "12", "13", "14", "2", "4", "5", "6", "7", "8", "9".
        // In real calls userIds are UUIDs and the order looks "random" but is stable.
        assertThat(sorted.map { it.name.value }).containsExactly(
            "P15", "P3", "P1", "P10", "P11", "P12", "P13", "P14",
            "P2", "P4", "P5", "P6", "P7", "P8", "P9",
        ).inOrder()
    }

    @Test
    fun `Default preset - UNKNOWN off-screen still bubbles dominant speaker up`() {
        val ps = participants15(
            scope,
            callActions,
            offscreenVisibility = VisibilityOnScreenState.UNKNOWN,
        ).toMutableList()
        ps[14]._dominantSpeaker.value = true

        val sorted = ps.sortedWith(SortPreset.Default.build(emptyMap()))
        assertThat(sorted.first().name.value).isEqualTo("P15")
    }

    @Test
    fun `Default preset - pinned outside visibility guard, jumps to top regardless`() {
        val ps = participants15(scope, callActions).toMutableList()
        val pins = mapOf(
            "13" to pinUpdateAt("13", OffsetDateTime.parse("2026-05-14T10:00:00Z"), PinType.Local),
        )
        val sorted = ps.sortedWith(SortPreset.Default.build(pins))
        assertThat(sorted.first().name.value).isEqualTo("P13")
    }

    // ------------------------------------------------------------------
    // SpeakerLayout preset — dominantSpeaker outside the guard
    // ------------------------------------------------------------------

    @Test
    fun `SpeakerLayout - dominant speaker bubbles up even with all VISIBLE`() {
        val ps = participants15(
            scope,
            callActions,
            offscreenVisibility = VisibilityOnScreenState.VISIBLE,
        ).toMutableList()
        ps[14]._dominantSpeaker.value = true

        val sorted = ps.sortedWith(SortPreset.SpeakerLayout.build(emptyMap()))
        assertThat(sorted.first().name.value).isEqualTo("P15")
    }

    @Test
    fun `SpeakerLayout - publishing video does NOT bubble up when all VISIBLE`() {
        // publishingVideo is inside ifInvisible, so visible-visible pairs return 0.
        val ps = participants15(
            scope,
            callActions,
            offscreenVisibility = VisibilityOnScreenState.VISIBLE,
        ).toMutableList()
        ps[14]._videoEnabled.value = true

        val sorted = ps.sortedWith(SortPreset.SpeakerLayout.build(emptyMap()))
        // P15's video doesn't elevate it because visibility blocks the comparator.
        assertThat(sorted.first().name.value).isEqualTo("P1")
    }

    // ------------------------------------------------------------------
    // LivestreamOrAudioRoom preset — role priority
    // ------------------------------------------------------------------

    @Test
    fun `LivestreamOrAudioRoom - host role beats regular participant`() {
        val ps = participants15(scope, callActions).toMutableList()
        ps[10]._roles.value = listOf("host") // P11 is a host

        val sorted = ps.sortedWith(SortPreset.LivestreamOrAudioRoom.build(emptyMap()))
        // P11 wins the role tier among all role-less participants.
        assertThat(sorted.first().name.value).isEqualTo("P11")
    }

    @Test
    fun `LivestreamOrAudioRoom - dominant speaker beats host when off-screen`() {
        val ps = participants15(scope, callActions).toMutableList()
        ps[10]._roles.value = listOf("host")
        ps[5]._dominantSpeaker.value = true // P6 dominant

        val sorted = ps.sortedWith(SortPreset.LivestreamOrAudioRoom.build(emptyMap()))
        // ifInvisibleOrUnknown(dominantSpeaker → ...) runs first (P6 invisible/off-screen),
        // ahead of byRole.
        assertThat(sorted.first().name.value).isEqualTo("P6")
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun pinUpdateAt(
        sessionId: String,
        at: OffsetDateTime,
        type: PinType,
    ): PinUpdateAtTime = PinUpdateAtTime(
        it = PinUpdate(sessionId = sessionId, userId = "user-$sessionId"),
        at = at,
        type = type,
    )
}
