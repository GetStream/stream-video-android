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

package io.getstream.video.android.core.call

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.PinUpdate
import io.getstream.video.android.core.pinning.PinType
import io.getstream.video.android.core.pinning.PinUpdateAtTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime

private typealias SessionId = String

internal class Pins(scope: CoroutineScope, private val clock: Clock = Clock.systemUTC()) {
    private val _localPins: MutableStateFlow<Map<SessionId, PinUpdateAtTime>> =
        MutableStateFlow(emptyMap())
    internal val localPins: StateFlow<Map<SessionId, PinUpdateAtTime>> = _localPins
    private val _serverPins: MutableStateFlow<Map<SessionId, PinUpdateAtTime>> =
        MutableStateFlow(emptyMap())

    internal val serverPins: StateFlow<Map<SessionId, PinUpdateAtTime>> = _serverPins

    internal val pinnedParticipants: StateFlow<Map<SessionId, OffsetDateTime>> =
        combine(localPins, serverPins) { local, server ->
            val combined = mutableMapOf<String, PinUpdateAtTime>()
            combined.putAll(local)
            combined.putAll(server)
            combined.toMap().asIterable().associate {
                Pair(it.key, it.value.at)
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    internal fun updateLocalPins(pins: Map<SessionId, PinUpdateAtTime>) {
        _localPins.value = pins
    }

    fun pin(userId: String, sessionId: String) {
        val pins = localPins.value.toMutableMap()
        pins[sessionId] = PinUpdateAtTime(
            PinUpdate(userId, sessionId),
            OffsetDateTime.now(clock),
            PinType.Local,
        )
        updateLocalPins(pins)
    }

    fun unpin(sessionId: String) {
        val pins = localPins.value.toMutableMap()
        pins.remove(sessionId)
        updateLocalPins(pins)
    }

    internal fun updateServerPins(pins: Map<SessionId, PinUpdateAtTime>) {
        _serverPins.value = pins
    }

    internal fun updateServerPins(internalParticipants: Map<SessionId, ParticipantState>, event: ParticipantJoinedEvent) {
        val participantSessionId = event.participant.session_id
        if (internalParticipants.containsKey(participantSessionId)) {
            val pinUpdate =
                PinUpdate(event.participant.user_id, participantSessionId)
            val tempPinUpdateList = serverPins.value.map { it.value.it }
            val participantIsNotPresent =
                tempPinUpdateList.none { it.sessionId == participantSessionId }
            if (participantIsNotPresent) {
                val updatedList = tempPinUpdateList.toMutableList().apply {
                    add(pinUpdate)
                }
                updateServerPins(internalParticipants, updatedList)
            }
        }
    }

    internal fun updateServerPins(internalParticipants: Map<SessionId, ParticipantState>, pins: List<PinUpdate>) {
        // Update participants that are still in the call
        val pinnedInCall = pins.filter {
            internalParticipants.containsKey(it.sessionId)
        }

        val serverPins = pinnedInCall.associate {
            Pair(
                it.sessionId,
                PinUpdateAtTime(it, OffsetDateTime.now(clock), PinType.Server),
            )
        }
        updateServerPins(serverPins)
    }
}
