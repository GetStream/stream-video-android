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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

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
 * connection settles and a genuine last-participant state still emits, while an unchanged
 * roster is not emitted twice across connection transitions.
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
        .filter { (roster, connectionState) ->
            roster.size <= 1 && connectionState is RealtimeConnection.Connected
        }
        .map { (roster, _) -> roster }
        .distinctUntilChanged()
