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
import io.getstream.video.android.core.BackendCause
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import stream.video.sfu.models.WebsocketReconnectStrategy

/**
 * Observes device network connectivity for a call and drives the call's response:
 * triggering a fast/rejoin reconnect when connectivity returns, and leaving the call if
 * the device stays offline past the configured `leaveAfterDisconnectSeconds`.
 *
 * Also owns the call's subscription to the underlying [NetworkStateProvider].
 *
 * @param reconnectDeadlineMillis a provider for the (mutable, runtime-updated) fast-reconnect
 * deadline; read on each connectivity change rather than captured as a snapshot.
 */
internal class CallConnectivityMonitor(
    private val type: String,
    private val id: String,
    private val clientImpl: StreamVideoClient,
    private val scope: CoroutineScope,
    private val state: CallState,
    private val reconnector: CallReconnector,
    private val lifecycle: CallLifecycleManager,
    private val reconnectDeadlineMillis: () -> Int,
) {
    private val logger by taggedLogger("Call:ConnectivityMonitor:$type:$id")

    private val network by lazy { clientImpl.coordinatorConnectionModule.networkStateProvider }

    private var leaveTimeoutAfterDisconnect: Job? = null
    private var lastDisconnect = 0L

    private val listener = object : NetworkStateProvider.NetworkStateListener {
        override suspend fun onConnected() {
            leaveTimeoutAfterDisconnect?.cancel()

            val elapsedTimeMils = System.currentTimeMillis() - lastDisconnect
            val deadlineMillis = reconnectDeadlineMillis()
            logger.d {
                "[NetworkStateListener#onConnected] #network; no args, elapsedTimeMils:$elapsedTimeMils, lastDisconnect:$lastDisconnect, reconnectDeadlineMils:$deadlineMillis"
            }
            val strategy = if (lastDisconnect > 0 && elapsedTimeMils < deadlineMillis) {
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST
            } else {
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
            }
            reconnector.reconnect(strategy, "NetworkStateListener#onConnected")
        }

        override suspend fun onDisconnected() {
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; old lastDisconnect:$lastDisconnect, clientImpl.leaveAfterDisconnectSeconds:${clientImpl.leaveAfterDisconnectSeconds}"
            }
            lastDisconnect = System.currentTimeMillis()
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; new lastDisconnect:$lastDisconnect"
            }
            leaveTimeoutAfterDisconnect = scope.launch {
                delay(clientImpl.leaveAfterDisconnectSeconds * 1000)
                val conn = state.connection.value
                if (conn is RealtimeConnection.Connected) {
                    logger.d {
                        "[NetworkStateListener#onDisconnected] #network; Already reconnected ($conn) — not leaving"
                    }
                    return@launch
                }
                val message = "Leaving after being disconnected for ${clientImpl.leaveAfterDisconnectSeconds}"
                logger.d {
                    "[NetworkStateListener#onDisconnected] #network; Leaving after being disconnected for ${clientImpl.leaveAfterDisconnectSeconds} (connection=$conn)"
                }
                lifecycle.leave(
                    CallLeaveReason.Backend(
                        cause = BackendCause.LEAVE_TIMEOUT_AFTER_DISCONNECT,
                        message = message,
                    ),
                )
            }
            logger.d { "[NetworkStateListener#onDisconnected] #network; at $lastDisconnect" }
        }
    }

    fun subscribe() = network.subscribe(listener)

    fun unsubscribe() = network.unsubscribe(listener)

    fun isConnected(): Boolean = network.isConnected()

    fun cancelLeaveTimeout() {
        leaveTimeoutAfterDisconnect?.cancel()
    }
}
