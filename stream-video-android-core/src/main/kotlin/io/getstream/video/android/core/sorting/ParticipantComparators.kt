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
import io.getstream.video.android.core.pinning.PinType
import io.getstream.video.android.core.pinning.PinUpdateAtTime
import stream.video.sfu.models.ParticipantSource

/**
 * Sorts participants flagged as dominant speaker before the rest.
 */
public val dominantSpeaker: Comparator<ParticipantState> = Comparator { a, b ->
    when {
        a.dominantSpeaker.value && !b.dominantSpeaker.value -> -1
        !a.dominantSpeaker.value && b.dominantSpeaker.value -> 1
        else -> 0
    }
}

/**
 * Sorts participants currently speaking before silent ones.
 */
public val speaking: Comparator<ParticipantState> = Comparator { a, b ->
    when {
        a.speaking.value && !b.speaking.value -> -1
        !a.speaking.value && b.speaking.value -> 1
        else -> 0
    }
}

/**
 * Sorts screen-sharing participants first.
 */
public val screenSharing: Comparator<ParticipantState> = Comparator { a, b ->
    when {
        a.screenSharingEnabled.value && !b.screenSharingEnabled.value -> -1
        !a.screenSharingEnabled.value && b.screenSharingEnabled.value -> 1
        else -> 0
    }
}

/**
 * Sorts participants currently publishing video first.
 */
public val publishingVideo: Comparator<ParticipantState> = Comparator { a, b ->
    when {
        a.videoEnabled.value && !b.videoEnabled.value -> -1
        !a.videoEnabled.value && b.videoEnabled.value -> 1
        else -> 0
    }
}

/**
 * Sorts participants currently publishing audio first.
 */
public val publishingAudio: Comparator<ParticipantState> = Comparator { a, b ->
    when {
        a.audioEnabled.value && !b.audioEnabled.value -> -1
        !a.audioEnabled.value && b.audioEnabled.value -> 1
        else -> 0
    }
}

internal const val RAISE_HAND_REACTION_TYPE: String = ":raise-hand:"

/**
 * Sorts participants who have a reaction matching [type] first.
 */
public fun byReactionType(type: String): Comparator<ParticipantState> =
    Comparator { a, b ->
        val aHas = a.reactions.value.any { it.response.type == type }
        val bHas = b.reactions.value.any { it.response.type == type }
        when {
            aHas && !bHas -> -1
            !aHas && bHas -> 1
            else -> 0
        }
    }

/**
 * Sorts participants with a raised-hand reaction first.
 */
public val raisedHand: Comparator<ParticipantState> =
    byReactionType(RAISE_HAND_REACTION_TYPE)

/**
 * Sorts participants by name ascending.
 */
public val byName: Comparator<ParticipantState> = Comparator { a, b ->
    a.name.value.compareTo(b.name.value)
}

/**
 * Sorts participants by their [ParticipantState.userId] ascending (string compare).
 *
 * Intended as a deterministic last-resort tiebreaker so participants with otherwise
 * equal signals (no dominant-speaker, no pin, no raised hand, etc.) don't shuffle
 * around between sort passes. Mirrors iOS's `userId` comparator used in the trailing
 * `ifInvisibleBy(userId)` of every iOS preset.
 *
 * Note: this is lexicographic string compare — for production user IDs (UUIDs or
 * arbitrary strings) the resulting order is meaningless to humans but is stable
 * across re-sorts.
 */
public val byUserId: Comparator<ParticipantState> = Comparator { a, b ->
    a.userId.value.compareTo(b.userId.value)
}

/**
 * Sorts participants by their [ParticipantState.joinedAt], earliest first. Nulls last.
 */
public val byJoinedAt: Comparator<ParticipantState> = Comparator { a, b ->
    val aJoined = a.joinedAt.value
    val bJoined = b.joinedAt.value
    when {
        aJoined == null && bJoined == null -> 0
        aJoined == null -> 1
        bJoined == null -> -1
        else -> aJoined.compareTo(bJoined)
    }
}

/**
 * Returns a comparator that prioritizes participants whose [ParticipantState.source]
 * matches one of [sources], in the given order. Sources not in the list are placed last.
 *
 * Example: `bySourcePriority(RTMP, SRT, WHIP)` → RTMP first, then SRT, then WHIP, then
 * everything else.
 */
public fun bySourcePriority(
    vararg sources: ParticipantSource,
): Comparator<ParticipantState> {
    val priority: (ParticipantSource) -> Int = { source ->
        val index = sources.indexOf(source)
        if (index == -1) Int.MAX_VALUE else index
    }
    return Comparator { a, b -> priority(a.source).compareTo(priority(b.source)) }
}

/**
 * Returns a comparator that prioritizes participants holding any of [roles].
 */
public fun byRole(vararg roles: String): Comparator<ParticipantState> =
    Comparator { a, b ->
        val aHas = a.roles.value.any { it in roles }
        val bHas = b.roles.value.any { it in roles }
        when {
            aHas && !bHas -> -1
            !aHas && bHas -> 1
            else -> 0
        }
    }

/**
 * Returns a comparator that prioritizes pinned participants. Within pinned, local pins
 * win over server pins; among pins of the same type, more recently pinned wins.
 *
 * Internal: parameter type [PinUpdateAtTime] is an internal SDK type. Callers compose
 * presets via [SortPresets]; this is invoked by [SortedParticipantsState] with the
 * latest detailed pin map before each sort pass.
 *
 * @param pins the current snapshot of pin state, keyed by session id.
 */
internal fun pinned(
    pins: Map<String, PinUpdateAtTime>,
): Comparator<ParticipantState> = Comparator { a, b ->
    val aPin = pins[a.sessionId]
    val bPin = pins[b.sessionId]
    when {
        aPin != null && bPin == null -> -1
        aPin == null && bPin != null -> 1
        aPin == null && bPin == null -> 0
        else -> {
            // Both pinned: Local > Server, then more recent first.
            val aLocal = aPin!!.type == PinType.Local
            val bLocal = bPin!!.type == PinType.Local
            when {
                aLocal && !bLocal -> -1
                !aLocal && bLocal -> 1
                else -> bPin.at.compareTo(aPin.at) // newer first
            }
        }
    }
}
