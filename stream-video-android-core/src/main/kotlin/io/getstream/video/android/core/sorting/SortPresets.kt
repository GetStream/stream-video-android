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

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.pinning.PinUpdateAtTime
import stream.video.sfu.models.ParticipantSource

/**
 * Named sort presets controlling participant ordering.
 *
 * Selected via `CallState.setSortPreset(...)`. For ad-hoc comparators that don't fit a
 * preset, use the existing `CallState.updateParticipantSortingOrder(Comparator)` escape
 * hatch.
 */
public sealed interface SortPreset {
    /**
     * The default grid/continuous-scroll preset.
     *
     * Order: screen-sharing → pinned (local > server > recency) →
     *        `ifInvisibleOrUnknown(dominantSpeaker → speaking → raised-hand →
     *        ingress-source priority → publishing video → publishing audio)`.
     *
     * UNKNOWN-as-invisible matters for Android continuous scroll: off-screen tiles that
     * haven't rendered yet still get ordered by activity, so they're in the right place
     * when they roll into view.
     */
    public data object Default : SortPreset

    /**
     * Speaker-focused layout preset.
     *
     * Like [Default], but dominant speaker sits outside the visibility guard so the
     * spotlight always reflects who's talking, regardless of viewport state.
     *
     * Order: screen-sharing → pinned → dominant speaker →
     *        `ifInvisible(speaking → raised-hand → ingress-source → publishing video →
     *        publishing audio)`.
     */
    public data object SpeakerLayout : SortPreset

    /**
     * Audio room / livestream-style room preset. Role-aware: admins, hosts and speakers
     * sort ahead of regular participants among those with the same activity profile.
     *
     * Order: `ifInvisibleOrUnknown(dominantSpeaker → speaking → raised-hand →
     *        ingress-source → publishing video → publishing audio)` → role priority.
     */
    public data object AudioRoom : SortPreset
}

/**
 * Default ingress-source priority. Real-time WebRTC sources fall through to
 * [Int.MAX_VALUE] (unranked), so they're effectively ordered by other criteria.
 */
internal val ingressSourcePriority: Comparator<ParticipantState> = bySourcePriority(
    ParticipantSource.PARTICIPANT_SOURCE_RTMP,
    ParticipantSource.PARTICIPANT_SOURCE_WHIP,
    ParticipantSource.PARTICIPANT_SOURCE_SRT,
    ParticipantSource.PARTICIPANT_SOURCE_RTSP,
)

/**
 * Resolves a [SortPreset] into a concrete [Comparator] using the latest pin snapshot.
 * Re-invoked from [SortedParticipantsState] before each sort pass so [pinned] sees the
 * current local/server pin map.
 */
internal fun SortPreset.build(
    pins: Map<String, PinUpdateAtTime>,
): Comparator<ParticipantState> = when (this) {
    SortPreset.Default -> combineComparators(
        screenSharing,
        pinned(pins),
        ifInvisibleOrUnknown(
            combineComparators(
                dominantSpeaker,
                speaking,
                raisedHand,
                ingressSourcePriority,
                publishingVideo,
                publishingAudio,
            ),
        ),
        // Deterministic tiebreaker (mirrors iOS's trailing `ifInvisibleBy(userId)`).
        // Runs only when at least one side is off-screen so the visible block keeps
        // its internal order.
        ifInvisibleOrUnknown(byUserId),
    )

    SortPreset.SpeakerLayout -> combineComparators(
        screenSharing,
        pinned(pins),
        dominantSpeaker,
        ifInvisible(
            combineComparators(
                speaking,
                raisedHand,
                ingressSourcePriority,
                publishingVideo,
                publishingAudio,
            ),
        ),
        ifInvisible(byUserId),
    )

    SortPreset.AudioRoom -> combineComparators(
        ifInvisibleOrUnknown(
            combineComparators(
                dominantSpeaker,
                speaking,
                raisedHand,
                ingressSourcePriority,
                publishingVideo,
                publishingAudio,
            ),
        ),
        byRole(
            AUDIO_ROOM_PRIORITY_ROLE_ADMIN,
            AUDIO_ROOM_PRIORITY_ROLE_HOST,
            AUDIO_ROOM_PRIORITY_ROLE_SPEAKER,
        ),
        ifInvisibleOrUnknown(byUserId),
    )
}

internal const val AUDIO_ROOM_PRIORITY_ROLE_ADMIN: String = "admin"
internal const val AUDIO_ROOM_PRIORITY_ROLE_HOST: String = "host"
internal const val AUDIO_ROOM_PRIORITY_ROLE_SPEAKER: String = "speaker"
