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

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.coroutines.scopes.RestartableProducerScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection

internal class CallIceConnectionMonitor(
    private val scope: RestartableProducerScope,
    private val sessionProvider: () -> RtcSession?,
) {

    private val logger by taggedLogger("CallIceConnectionMonitor")

    private var publisherJob: Job? = null
    private var subscriberJob: Job? = null

    fun start() {
        stop()

        publisherJob = scope.launch {
            sessionProvider()?.publisher?.iceState?.collect { state ->
                when (state) {
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    -> {
                        sessionProvider()?.publisher?.connection?.restartIce()
                    }

                    else -> {
                        logger.d { "[publisher] ICE state = $state" }
                    }
                }
            }
        }

        subscriberJob = scope.launch {
            sessionProvider()?.subscriber?.iceState?.collect { state ->
                when (state) {
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    -> {
                        sessionProvider()?.requestSubscriberIceRestart()
                    }

                    else -> {
                        logger.d { "[subscriber] ICE state = $state" }
                    }
                }
            }
        }
    }

    fun stop() {
        publisherJob?.cancel()
        subscriberJob?.cancel()
        publisherJob = null
        subscriberJob = null
    }
}
