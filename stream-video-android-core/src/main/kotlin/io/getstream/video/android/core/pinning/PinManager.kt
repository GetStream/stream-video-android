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

import androidx.annotation.VisibleForTesting
import io.getstream.video.android.core.IsPinned
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.SessionId
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.PinUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import stream.video.sfu.models.Participant

internal class PinManager(
    private val timeProvider: TimeProvider = DefaultTimeProvider(),
    private val participants: () -> Map<SessionId, ParticipantState>,
) {
    private val _localPins: MutableStateFlow<Map<SessionId, PinEntry>> =
        MutableStateFlow(emptyMap())
    internal val localPins: StateFlow<Map<SessionId, PinEntry>> = _localPins
    private val _serverPins: MutableStateFlow<Map<SessionId, PinEntry>> =
        MutableStateFlow(emptyMap())
    internal val serverPins: StateFlow<Map<SessionId, PinEntry>> = _serverPins

    @VisibleForTesting
    fun updateLocalPins(pins: Map<SessionId, PinEntry>) {
        _localPins.value = pins
    }

    @VisibleForTesting
    fun updateServerPins(pins: Map<SessionId, PinEntry>) {
        _serverPins.value = pins
    }

    fun onParticipantLeft(sessionId: SessionId) {
        if (localPins.value.containsKey(sessionId)) {
            // Remove any pins for the participant
            unpin(sessionId)
        }

        if (serverPins.value.containsKey(sessionId)) {
            _serverPins.value = serverPins.value.filter { it.key != sessionId }
        }
    }

    fun pin(userId: String, sessionId: String) {
        _localPins.value = localPins.value + (
            sessionId to PinEntry(
                PinUpdate(userId, sessionId),
                timeProvider.now(),
                PinType.Local,
            )
            )
    }

    fun unpin(sessionId: String) {
        _localPins.value = localPins.value - sessionId
    }

    fun setServerPins(pins: List<PinUpdate>) {
        _serverPins.value = resolveServerPins(pins, buildPinnedAtLookup(pins))
    }

    fun onParticipantJoined(event: ParticipantJoinedEvent) {
        val updatedPins = serverPins.value.toMutableMap()

        val changed =
            updatedPins.addServerPinIfEligible(
                participant = event.participant,
                isPinned = event.isPinned,
                connectedParticipants = participants(),
                timeProvider = timeProvider,
            )

        if (changed) {
            _serverPins.value = updatedPins
        }
    }

    fun onParticipantsJoined(list: List<Pair<Participant, IsPinned>>) {
        val updatedPins = serverPins.value.toMutableMap()
        val connectedParticipants = participants()

        var changed = false

        for ((participant, isPinned) in list) {
            changed =
                updatedPins.addServerPinIfEligible(
                    participant = participant,
                    isPinned = isPinned,
                    connectedParticipants = connectedParticipants,
                    timeProvider = timeProvider,
                ) || changed
        }

        if (changed) {
            _serverPins.value = updatedPins
        }
    }

    private fun MutableMap<SessionId, PinEntry>.addServerPinIfEligible(
        participant: Participant,
        isPinned: Boolean,
        connectedParticipants: Map<SessionId, ParticipantState>,
        timeProvider: TimeProvider,
    ): Boolean {
        if (!isPinned) return false

        val sessionId = participant.session_id

        if (!connectedParticipants.containsKey(sessionId)) {
            return false
        }

        if (containsKey(sessionId)) {
            return false
        }

        this[sessionId] = PinEntry(
            pinTarget = PinUpdate(
                userId = participant.user_id,
                sessionId = sessionId,
            ),
            at = timeProvider.now(),
            type = PinType.Server,
        )

        return true
    }

    /**
     * The server delivers pins in priority order (index 0 = highest priority).
     * To preserve that order as timestamps, each pin is assigned
     * pinnedAt = now + (pins.size - index)
     * so the first pin gets the largest value and the last gets the smallest.
     * Sorting participants by pinnedAt descending then naturally reflects
     * the server's intended priority order.
     * Duplicate userIds disable reconnect fallback matching to avoid ambiguity.
     */
    private fun buildPinnedAtLookup(pins: List<PinUpdate>): PinLookup {
        val now = timeProvider.currentTimeMillis()
        val bySessionId = mutableMapOf<String, Long>()
        val byUserId = mutableMapOf<String, Long?>()
        pins.forEachIndexed { index, pin ->
            val pinnedAt = now + (pins.size - index)
            byUserId[pin.userId] = if (byUserId.containsKey(pin.userId)) null else pinnedAt
            bySessionId.putIfAbsent(pin.sessionId, pinnedAt)
        }
        return PinLookup(bySessionId, byUserId)
    }

    /**
     * Resolves which participants are pinned server-side.
     * Matches by sessionId first (exact); falls back to userId for reconnection scenarios.
     */
    private fun resolveServerPins(
        pins: List<PinUpdate>,
        lookup: PinLookup,
    ): Map<String, PinEntry> = participants()
        .mapNotNull { (sessionId, participant) ->
            val pinnedAtMillis = lookup.pinnedAtFor(
                sessionId = sessionId,
                userId = participant.userId.value,
            ) ?: return@mapNotNull null

            val pinTarget =
                pins.find { it.sessionId == sessionId }
                    ?: pins.find { it.userId == participant.userId.value }
                    ?: return@mapNotNull null

            val existingTimestamp = serverPins.value[sessionId]?.at
            val timestamp = existingTimestamp ?: timeProvider.fromMillis(pinnedAtMillis)
            sessionId to PinEntry(
                pinTarget,
                timestamp,
                PinType.Server,
            )
        }
        .toMap()
}

/**
 * Separate lookup maps for sessionId and reconnect-by-userId matching.
 * Duplicate userIds disable the fallback match to avoid ambiguity.
 */
private data class PinLookup(
    val bySessionId: Map<String, Long>,
    val byUserId: Map<String, Long?>,
) {
    fun pinnedAtFor(sessionId: String, userId: String): Long? =
        bySessionId[sessionId] ?: byUserId[userId]
}

internal interface TimeProvider {
    fun now(): OffsetDateTime
    fun currentTimeMillis(): Long
    fun fromMillis(millis: Long): OffsetDateTime
}

internal class DefaultTimeProvider : TimeProvider {
    override fun now(): OffsetDateTime {
        return OffsetDateTime.now(Clock.systemUTC())
    }

    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun fromMillis(millis: Long): OffsetDateTime {
        return OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneOffset.UTC,
        )
    }
}
