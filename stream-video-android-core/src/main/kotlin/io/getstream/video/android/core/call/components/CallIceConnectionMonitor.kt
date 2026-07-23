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

package io.getstream.video.android.core.call.components

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection

/**
 * Watches the publisher and subscriber peer-connection ICE states for a [Call] and
 * triggers an ICE restart whenever a connection FAILS or becomes DISCONNECTED.
 */
internal class CallIceConnectionMonitor(
    private val call: Call,
) {
    private val logger by taggedLogger("Call:IceMonitor:${call.type}:${call.id}")

    private var monitorPublisherPCStateJob: Job? = null
    private var monitorSubscriberPCStateJob: Job? = null

    fun start() {
        startPublisherMonitor()
        startSubscriberMonitor()
    }

    private fun startPublisherMonitor() {
        monitorPublisherPCStateJob?.cancel()
        monitorPublisherPCStateJob = call.scope.launch {
            call.session
                .filterNotNull()
                .flatMapLatest { it.publisher.filterNotNull() }
                .flatMapLatest { publisher ->
                    publisher.iceState.map { publisher to it }
                }
                .collect { (publisher, state) ->
                    when (state) {
                        PeerConnection.IceConnectionState.FAILED,
                        PeerConnection.IceConnectionState.DISCONNECTED,
                        -> {
                            publisher.connection.restartIce()
                        }
                        else -> {
                            logger.d { "[monitorPubConnectionState] Ice connection state is $state" }
                        }
                    }
                }
        }
    }

    private fun startSubscriberMonitor() {
        monitorSubscriberPCStateJob?.cancel()
        monitorSubscriberPCStateJob = call.scope.launch {
            call.session.value?.subscriber?.value?.iceState?.collect {
                when (it) {
                    PeerConnection.IceConnectionState.FAILED, PeerConnection.IceConnectionState.DISCONNECTED -> {
                        call.session.value?.requestSubscriberIceRestart()
                    }

                    else -> {
                        logger.d { "[monitorSubConnectionState] Ice connection state is $it" }
                    }
                }
            }
        }
    }

    fun stop() {
        monitorSubscriberPCStateJob?.cancel()
        monitorPublisherPCStateJob?.cancel()
        monitorPublisherPCStateJob = null
        monitorSubscriberPCStateJob = null
    }
}
