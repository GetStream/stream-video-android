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
import io.getstream.video.android.core.notifications.RingingCallActivationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.PeerConnection

internal class ActiveStateGate(
    private val coroutineScope: CoroutineScope,
    private val previousRingingStates: Set<RingingState>,
) {
    private val logger by taggedLogger("ActiveStateGate")
    private var peerConnectionObserverJob: Job? = null

    internal fun awaitAndTransition(currentRingingState: RingingState, call: Call, ringingCallActivationConfig: RingingCallActivationConfig, onReady: () -> Unit) {
        logger.d { "[awaitAndTransition], ringingState: $currentRingingState" }
        val isIncomingOrOutgoing =
            previousRingingStates.any { it is RingingState.Incoming || it is RingingState.Outgoing }
        if (isIncomingOrOutgoing && currentRingingState !is RingingState.Active) {
            observePeerConnection(
                call,
                onReady,
                ringingCallActivationConfig,
            )
        } else if (!isIncomingOrOutgoing) {
            onReady()
        }
    }

    private fun observePeerConnection(call: Call, onReady: () -> Unit, ringingCallActivationConfig: RingingCallActivationConfig) {
        if (peerConnectionObserverJob?.isActive == true) return

        peerConnectionObserverJob = coroutineScope.launch {
            val start = System.currentTimeMillis()

            val result = withTimeoutOrNull(ringingCallActivationConfig.timeoutMillis) {
                call.session
                    .filterNotNull()
                    .flatMapLatest { session ->

                        val publisherFlow = session.publisher
                            .filterNotNull()
                            .flatMapLatest { it.state }

                        val subscriberFlow = session.subscriber
                            .filterNotNull()
                            .flatMapLatest { it.state }

                        when (ringingCallActivationConfig.criteria) {
                            RingingCallActivationCriteria.LEGACY_BEHAVIOR -> {
                                emptyFlow<Int>()
                                    .map { "none" to it }
                            }
                            RingingCallActivationCriteria.PUBLISHER_CONNECTED -> {
                                publisherFlow.filter { it == PeerConnection.PeerConnectionState.CONNECTED }
                                    .map { "publisher" to it }
                            }
                            RingingCallActivationCriteria.SUBSCRIBER_CONNECTED -> {
                                subscriberFlow.filter { it == PeerConnection.PeerConnectionState.CONNECTED }
                                    .map { "subscriber" to it }
                            }

                            RingingCallActivationCriteria.FIRST_PACKET_RECEIVED ->
                                session.subscriber
                                    .filterNotNull()
                                    .flatMapLatest { it.debugFirstRtpPacketArrivedWithinTimeout }
                                    .filter { it }
                                    .map { "first_packet" to it }

                            RingingCallActivationCriteria.ANY_PEER_CONNECTED -> {
                                merge(
                                    publisherFlow.map { "publisher" to it },
                                    subscriberFlow.map { "subscriber" to it },
                                ).filter { (_, state) ->
                                    state == PeerConnection.PeerConnectionState.CONNECTED
                                }
                            }

                            RingingCallActivationCriteria.BOTH_PEER_CONNECTED -> {
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
                    "[observeConnection-${ringingCallActivationConfig.criteria}] $source reached $state in ${duration}ms"
                }
            } else {
                logger.w {
                    "[observeConnection-${ringingCallActivationConfig.criteria}] Timeout after ${duration}ms"
                }
            }
            if (isActive) {
                onReady()
            }
        }
    }

    fun cleanup() {
        peerConnectionObserverJob?.cancel()
        peerConnectionObserverJob = null
    }
}
