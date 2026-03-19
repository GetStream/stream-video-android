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

package io.getstream.video.android.core

import io.getstream.log.taggedLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.PeerConnection

private const val PEER_CONNECTION_OBSERVER_TIMEOUT = 5_000L

internal class ActiveStateTransition(
    private val coroutineScope: CoroutineScope,
    private val previousRingingStates: Set<RingingState>,
) {
    private val logger by taggedLogger("ActiveStateTransition")
    private var peerConnectionObserverJob: Job? = null

    internal fun transitionToActiveState(currentRingingState: RingingState, call: Call, transitionToActiveState: () -> Unit) {
        logger.d { "[transitionToActiveState], ringingState: $currentRingingState" }
        val isIncomingOrOutgoingCall =
            previousRingingStates.any { it is RingingState.Incoming || it is RingingState.Outgoing }
        if (isIncomingOrOutgoingCall) {
            val oldState = currentRingingState
            if (oldState !is RingingState.Active) {
                observePeerConnection(
                    call,
                    transitionToActiveState,
                    TransitionToRingingStateStrategy.BOTH_PEER_CONNECTED,
                )
            }
        } else {
            transitionToActiveState()
        }
    }

    private enum class TransitionToRingingStateStrategy {
        ANY_PEER_CONNECTED,
        BOTH_PEER_CONNECTED,
    }

    private fun observePeerConnection(call: Call, transitionToActiveState: () -> Unit, strategy: TransitionToRingingStateStrategy) {
        if (peerConnectionObserverJob?.isActive == true) return

        peerConnectionObserverJob = coroutineScope.launch {
            val start = System.currentTimeMillis()

            val result = withTimeoutOrNull(PEER_CONNECTION_OBSERVER_TIMEOUT) {
                call.session
                    .filterNotNull()
                    .flatMapLatest { session ->

                        val publisherFlow = session.publisher
                            .filterNotNull()
                            .flatMapLatest { it.state }

                        val subscriberFlow = session.subscriber
                            .filterNotNull()
                            .flatMapLatest { it.state }

                        when (strategy) {
                            TransitionToRingingStateStrategy.ANY_PEER_CONNECTED -> {
                                merge(
                                    publisherFlow.map { "publisher" to it },
                                    subscriberFlow.map { "subscriber" to it },
                                ).filter { (_, state) ->
                                    state == PeerConnection.PeerConnectionState.CONNECTED
                                }
                            }

                            TransitionToRingingStateStrategy.BOTH_PEER_CONNECTED -> {
                                combine(publisherFlow, subscriberFlow) { pub, sub ->
                                    if (
                                        pub == PeerConnection.PeerConnectionState.CONNECTED &&
                                        sub == PeerConnection.PeerConnectionState.CONNECTED
                                    ) {
                                        "both" to PeerConnection.PeerConnectionState.CONNECTED
                                    } else {
                                        null
                                    }
                                }.filterNotNull()
                            }
                        }
                    }
                    .first()
            }

            val duration = System.currentTimeMillis() - start

            if (result != null) {
                val (source, state) = result
                logger.d {
                    "[observeConnection-$strategy] $source reached $state in ${duration}ms"
                }
            } else {
                logger.w {
                    "[observeConnection-$strategy] Timeout after ${duration}ms"
                }
            }
            transitionToActiveState()
        }
    }

    fun cleanup() {
        peerConnectionObserverJob?.cancel()
        peerConnectionObserverJob = null
    }
}
