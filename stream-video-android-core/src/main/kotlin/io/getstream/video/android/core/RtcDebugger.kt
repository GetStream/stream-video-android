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
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection

/**
 * This class is experimental & only meant for debugging.
 * This class is not part of Offical Public Api
 * Changes to this class will not be announced publicly
 * Be careful to depend on this class as it can introduce future breaking-changes
 */
@InternalStreamVideoApi
public class RtcDebugger(val call: Call, val scope: CoroutineScope) {
    val logger by taggedLogger("RtcDebugger")

    private fun getPublisher(): Flow<Publisher> {
        return call.session
            .filterNotNull()
            .flatMapLatest { session -> session.publisher }
            .filterNotNull()
    }

    private fun getSubscriber(): Flow<Subscriber> {
        return call.session
            .filterNotNull()
            .flatMapLatest { session -> session.subscriber }
            .filterNotNull()
    }

    @InternalStreamVideoApi
    public fun getDebugPublisherConnectionState(): Flow<PeerConnection.PeerConnectionState?> {
        return getPublisher()
            .flatMapLatest { publisher -> publisher.state }
            .distinctUntilChanged()
    }

    @InternalStreamVideoApi
    public fun publisherIceConnectionTimeFlow(): Flow<Long> {
        return getPublisher()
            .flatMapLatest { publisher ->
                flow {
                    val startTime = System.currentTimeMillis()
                    var emitted = false

                    publisher.state.collect { state ->
                        if (!emitted && state == PeerConnection.PeerConnectionState.CONNECTED) {
                            emitted = true
                            val duration = System.currentTimeMillis() - startTime
                            emit(duration)
                        }
                    }
                }
            }
    }

    @InternalStreamVideoApi
    public fun getDebugSubscriberConnectionState(): Flow<PeerConnection.PeerConnectionState?> {
        return getSubscriber()
            .flatMapLatest { subscriber -> subscriber.state }
            .distinctUntilChanged()
    }

    @InternalStreamVideoApi
    fun firstPacketSentFlow(): Flow<Boolean> {
        return getPublisher()
            .flatMapLatest { publisher ->
                callbackFlow {
                    var firstEmitted = false

                    val job = scope.launch {
                        while (!firstEmitted) {
                            publisher.connection.getStats { report ->
                                report.statsMap.values.forEach { stat ->
                                    if (stat.type == "outbound-rtp") {
                                        val bytes = stat.members["bytesSent"] as? Number ?: return@forEach

                                        if (bytes.toLong() > 0 && !firstEmitted) {
                                            firstEmitted = true
                                            trySend(true)

                                            logger.d {
                                                "[firstPacketSentFlow] FIRST_OUTBOUND_PACKET, id:${stat.id}"
                                            }
                                        }
                                    }
                                }
                            }

                            delay(500) // polling interval
                        }
                    }

                    awaitClose {
                        job.cancel()
                    }
                }
            }
    }

    @InternalStreamVideoApi
    fun firstPacketReceivedFlow(): Flow<Boolean> {
        return getSubscriber()
            .flatMapLatest { subscriber ->
                callbackFlow {
                    var firstReceived = false

                    val job = scope.launch {
                        while (!firstReceived) {
                            subscriber.connection.getStats { report ->
                                report.statsMap.values.forEach { stat ->
                                    if (stat.type == "inbound-rtp") {
                                        val bytes = stat.members["bytesReceived"] as? Number ?: return@forEach

                                        if (bytes.toLong() > 0 && !firstReceived) {
                                            firstReceived = true
                                            trySend(true)

                                            logger.d {
                                                "[firstPacketReceivedFlow] FIRST_INBOUND_PACKET, id:${stat.id}"
                                            }
                                        }
                                    }
                                }
                            }

                            delay(500) // polling interval
                        }
                    }

                    awaitClose {
                        job.cancel()
                    }
                }
            }
    }

    fun subscriberTrackDiscoveredFlow(): Flow<Unit> {
        return getSubscriber()
            .flatMapLatest { subscriber -> subscriber.onSubscriberTrackAddedFlow }
            .filter { it }
            .map { Unit }
    }

    fun publisherTrackAttachedFlow(): Flow<Unit> {
        return getSubscriber()
            .flatMapLatest { subscriber -> subscriber.onSubscriberTrackAddedFlow }
            .filter { it }
            .map { Unit }
    }
}
