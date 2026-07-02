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

package io.getstream.video.android

import io.getstream.log.taggedLogger
import io.getstream.video.android.CallActivity.Companion.USE_CALL_JOIN_INTERCEPTOR
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallJoinInterceptor
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.RingingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch

/**
 * Do the following changes before testing this flow
 * Set [io.getstream.video.android.core.INTERCEPTOR_TIMEOUT_MS] to 10_000L
 * Set [io.getstream.video.android.core.PEER_CONNECTION_OBSERVER_TIMEOUT] to 10_000L
 */
class DemoCallJoinInterceptor(
    private val previousRingingStates: Set<RingingState>,
    private val coroutineScope: CoroutineScope,
) : CallJoinInterceptor {
    private val logger by taggedLogger("DemoCallJoinInterceptor")

    companion object {
        const val CALLER_READY_TO_JOIN_EVENT_TYPE = "caller_ready_join"
        const val CALLEE_READY_TO_JOIN_EVENT_TYPE = "callee_ready_join"
    }
    private var observeJob: Job? = null
    override suspend fun callWillJoin(call: Call) {
        observeJob?.cancel()
        observeJob = coroutineScope.launch {
            val muteJob = launch {
                call.state.participants
                    .flatMapLatest { participants ->
                        participants
                            .filter { !it.isLocal }
                            .asFlow()
                            .flatMapMerge { participant ->
                                participant.audioTrack.filterNotNull()
                            }
                    }
                    .collect { track ->
                        track.audio.setEnabled(false)
                    }
            }
            // Stop muting once the call reaches a terminal state, even if callDidJoin
            // never runs (e.g. the interceptor aborts the join or it's left externally).
            call.state.connection.first {
                it is RealtimeConnection.Disconnected ||
                    it is RealtimeConnection.Failed ||
                    it is RealtimeConnection.ReconnectingFailed
            }
            muteJob.cancel()
        }
    }

    override suspend fun callReadyToJoin(call: Call) {
        if (USE_CALL_JOIN_INTERCEPTOR) {
            val isIncomingOrOutgoing = previousRingingStates.firstOrNull {
                it is RingingState.Incoming || it is RingingState.Outgoing
            }

            val isOutgoing = isIncomingOrOutgoing is RingingState.Outgoing
            val isIncoming = isIncomingOrOutgoing is RingingState.Incoming

            val currentUserId = call.user.id
            if (isIncoming) {
                val result = call.sendCustomEvent(
                    mapOf(
                        "type" to CALLEE_READY_TO_JOIN_EVENT_TYPE,
                        "user_id" to currentUserId,
                    ),
                )
                if (result.isSuccess) {
                    logger.d { "[callReadyToJoin] Successfully sent custom $CALLEE_READY_TO_JOIN_EVENT_TYPE event" }
                    App.demoApp.callerReadyToJoinFlow.filter { it != null && it.callCid == call.cid }.first()
                } else {
                    logger.d { "[callReadyToJoin] Failed to send custom $CALLEE_READY_TO_JOIN_EVENT_TYPE event" }
                }
                logger.d { "[callReadyToJoin] callerReadyToJoinFlow finish" }
            } else if (isOutgoing) {
                val result = call.sendCustomEvent(
                    mapOf(
                        "type" to CALLER_READY_TO_JOIN_EVENT_TYPE,
                        "user_id" to currentUserId,
                    ),
                )

                if (result.isSuccess) {
                    logger.d { "[callReadyToJoin] Successfully sent custom $CALLER_READY_TO_JOIN_EVENT_TYPE event" }
                    App.demoApp.calleeReadyToJoinFlow.filter { it != null && it.callCid == call.cid }.first()
                } else {
                    logger.d { "[callReadyToJoin] Failed to send custom $CALLER_READY_TO_JOIN_EVENT_TYPE event" }
                }
                logger.d { "[callReadyToJoin] calleeReadyToJoinFlow finish" }
            }
        }
    }

    override suspend fun callDidJoin(call: Call) {
        observeJob?.cancel()
        observeJob = null
        call.state.participants.value.filter { !it.isLocal }
            .forEach {
                val audioTrack = it.audioTrack.value
                audioTrack?.audio?.setEnabled(true)
            }
    }
}
