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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.PeerConnection

private const val PEER_CONNECTION_OBSERVER_TIMEOUT = 5_000L

internal class ActiveStateGate(
    private val coroutineScope: CoroutineScope,
    private val previousRingingStates: Set<RingingState>,
    private val strategy: TransitionToRingingStateStrategy = TransitionToRingingStateStrategy.PUBLISHER_CONNECTED,
    private val timeoutMs: Long = PEER_CONNECTION_OBSERVER_TIMEOUT,
) {
    private val logger by taggedLogger("ActiveStateGate")
    private var peerConnectionObserverJob: Job? = null

    internal fun awaitAndTransition(
        currentRingingState: RingingState,
        call: Call,
        onReady: () -> Unit,
    ) {
        logger.d { "[awaitAndTransition], ringingState: $currentRingingState" }
        when (strategy) {
            TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR -> {
                onReady()
            }

            else -> {
                val isIncomingOrOutgoing =
                    previousRingingStates.any { it is RingingState.Incoming || it is RingingState.Outgoing }

                if (isIncomingOrOutgoing && currentRingingState !is RingingState.Active) {
                    observePeerConnection(
                        call,
                        onReady,
                        strategy,
                    )
                } else if (!isIncomingOrOutgoing) {
                    onReady()
                }
            }
        }
    }

    private fun observePeerConnection(call: Call, onReady: () -> Unit, strategy: TransitionToRingingStateStrategy) {
        if (peerConnectionObserverJob?.isActive == true) return

        peerConnectionObserverJob = coroutineScope.launch {
            val start = System.currentTimeMillis()

            val result = withTimeoutOrNull(timeoutMs) {
                call.session
                    .filterNotNull()
                    .flatMapLatest { session ->

                        val publisherFlow = session.publisher
                            .filterNotNull()
                            .flatMapLatest { it.state }

                        when (strategy) {
                            TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR -> {
                                emptyFlow<Int>()
                                    .map { "none" to it }
                            }
                            TransitionToRingingStateStrategy.PUBLISHER_CONNECTED -> {
                                publisherFlow.filter { it == PeerConnection.PeerConnectionState.CONNECTED }
                                    .map { "publisher" to it }
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
            if (isActive) {
                onReady()
                cleanup()
            }
        }
    }

    fun cleanup() {
        peerConnectionObserverJob?.cancel()
        peerConnectionObserverJob = null
    }
}
