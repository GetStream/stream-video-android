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

package io.getstream.video.android.core.pinning

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.SessionId
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.PinUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime

internal class PinManager(
    private val localPins: MutableStateFlow<Map<SessionId, PinEntry>>,
    private val serverPins: MutableStateFlow<Map<SessionId, PinEntry>>,
    private val participants: () -> Map<SessionId, ParticipantState>,
) {

    fun pin(userId: String, sessionId: String) {
        localPins.value = localPins.value + (
            sessionId to PinEntry(
                PinUpdate(userId, sessionId),
                OffsetDateTime.now(Clock.systemUTC()),
                PinType.Local,
            )
            )
    }

    fun unpin(sessionId: String) {
        localPins.value = localPins.value - sessionId
    }

    fun setServerPins(pins: List<PinUpdate>) {
        serverPins.value = resolveServerPins(pins, buildPinnedAtLookup(pins))
    }

    fun onParticipantJoined(internalParticipants: Map<String, ParticipantState>, event: ParticipantJoinedEvent) {
        val sessionId = event.participant.session_id
        if (internalParticipants.containsKey(sessionId) && event.isPinned) {
            if (!serverPins.value.containsKey(sessionId)) {
                serverPins.value = serverPins.value + (
                    sessionId to PinEntry(
                        PinUpdate(event.participant.user_id, sessionId),
                        getCurrentTime(),
                        PinType.Server,
                    )
                    )
            }
        }
    }

    // The server delivers pins in priority order (index 0 = highest priority).
    // To preserve that order as timestamps, each pin is assigned
    //   pinnedAt = now + (pins.size - index)
    // so the first pin gets the largest value and the last gets the smallest.
    // Sorting participants by pinnedAt descending then naturally reflects
    // the server's intended priority order.
    //
    // Note: byUserId maps a userId to null when two pins share the same userId,
    // disabling the userId fallback for that user to avoid ambiguous matches
    // during reconnection (see resolveServerPins).
    private fun buildPinnedAtLookup(pins: List<PinUpdate>): PinLookup {
        val now = System.currentTimeMillis()
        val bySessionId = mutableMapOf<String, Long>()
        val byUserId = mutableMapOf<String, Long?>()
        pins.forEachIndexed { index, pin ->
            val pinnedAt = now + (pins.size - index)
            byUserId[pin.userId] = if (byUserId.containsKey(pin.userId)) null else pinnedAt
            bySessionId.putIfAbsent(pin.sessionId, pinnedAt)
        }
        return PinLookup(bySessionId, byUserId)
    }

    // Resolves which participants are pinned server-side.
    // Matches by sessionId first (exact); falls back to userId for reconnection scenarios.
    private fun resolveServerPins(
        pins: List<PinUpdate>,
        lookup: PinLookup,
    ): Map<String, PinEntry> = participants()
        .mapNotNull { (sessionId, participant) ->
            val pinnedAtTime = lookup.pinnedAtFor(sessionId, participant.userId.value)
            pinnedAtTime?.let {
                val pinTarget = pins.find { it.sessionId == sessionId }
                    ?: pins.find { it.userId == participant.userId.value }
                pinTarget?.let {
                    val at = serverPins.value[sessionId]?.at ?: getCurrentTime()
                    sessionId to PinEntry(
                        it,
                        at,
                        PinType.Server,
                    )
                }
            }
        }
        .toMap()

    private fun getCurrentTime() = OffsetDateTime.now(Clock.systemUTC())
}

// Holds pin timestamps split by lookup axis so sessionId and userId keys never collide.
// byUserId is null for a given userId when multiple pins share it (conflict),
// which disables the userId fallback for that user.
private data class PinLookup(
    val bySessionId: Map<String, Long>,
    val byUserId: Map<String, Long?>,
) {
    fun pinnedAtFor(sessionId: String, userId: String): Long? =
        bySessionId[sessionId] ?: byUserId[userId]
}
