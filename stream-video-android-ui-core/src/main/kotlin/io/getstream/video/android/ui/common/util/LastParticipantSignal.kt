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

package io.getstream.video.android.ui.common.util

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold

/**
 * Emits the participant roster whenever the local user has become the last participant in the
 * call while the connection is [RealtimeConnection.Connected].
 *
 * Roster changes are debounced by [debounceMs] to absorb quick disconnect/reconnect flaps.
 * Emissions require a connected state because the roster is unreliable at any other point:
 * during the initial join it is still being populated, and during a reconnect the rejoin
 * removes the previous local participant record and remote participants of the failing SFU
 * may not have rejoined yet. Acting on the roster then would leave the call and cancel the
 * join or reconnect that is still running in the call scope. Terminal states need no signal
 * from here: the reconnector leaves the call itself when retries are exhausted. The
 * connection state is part of the combined stream, so the roster is re-evaluated once the
 * connection settles and a genuine last-participant state still emits.
 *
 * The signal is a rising edge of the roster, not of the combined condition: becoming the last
 * participant arms it, and it fires on the first connected evaluation after that. A connection
 * transition with an unchanged roster therefore does not repeat the signal, while a remote
 * participant joining and leaving again re-arms it and does signal a second time.
 *
 * @param participants the participant roster of the call.
 * @param connection the realtime connection state of the call.
 * @param debounceMs debounce applied to the combined stream before evaluation.
 * @param onEvaluated invoked for every debounced evaluation, regardless of the outcome.
 */
@OptIn(FlowPreview::class)
internal fun lastParticipantSignal(
    participants: Flow<List<ParticipantState>>,
    connection: Flow<RealtimeConnection>,
    debounceMs: Long,
    onEvaluated: suspend (List<ParticipantState>, RealtimeConnection) -> Unit = { _, _ -> },
): Flow<List<ParticipantState>> =
    combine(participants, connection) { roster, connectionState -> roster to connectionState }
        .debounce(debounceMs)
        .onEach { (roster, connectionState) -> onEvaluated(roster, connectionState) }
        .runningFold(LastParticipantState()) { previous, (roster, connectionState) ->
            val isLast = roster.size <= 1
            // Arm on the rising edge of the roster and stay armed until a connected
            // evaluation consumes it, so a signal found mid-reconnect is not lost.
            val armed = when {
                !isLast -> false
                !previous.wasLast -> true
                else -> previous.armed
            }
            val connected = connectionState is RealtimeConnection.Connected
            LastParticipantState(
                wasLast = isLast,
                armed = armed && !connected,
                signal = roster.takeIf { armed && connected },
            )
        }
        .mapNotNull { it.signal }

/**
 * Fold state of [lastParticipantSignal].
 *
 * @param wasLast whether the previous evaluation saw a last-participant roster.
 * @param armed whether a last-participant roster is waiting for a connected evaluation.
 * @param signal the roster to emit for this evaluation, or null when there is nothing to emit.
 */
private data class LastParticipantState(
    val wasLast: Boolean = false,
    val armed: Boolean = false,
    val signal: List<ParticipantState>? = null,
)
