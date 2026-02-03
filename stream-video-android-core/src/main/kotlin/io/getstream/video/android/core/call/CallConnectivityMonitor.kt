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
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class CallConnectivityMonitor(
    val callScope: RestartableProducerScope,
    val state: CallConnectivityMonitorState,
    val leaveAfterDisconnectSeconds: Long,
    onFastReconnect: suspend () -> Unit,
    onRejoin: suspend () -> Unit,
    onDisconnected: suspend () -> Unit,
    onLeaveTimeout: suspend () -> Unit,
) {
    private val logger by taggedLogger("CallConnectivityMonitor")
    private var leaveTimeoutAfterDisconnect: Job? = null

    internal val listener = object : NetworkStateProvider.NetworkStateListener {
        override suspend fun onConnected() {
            leaveTimeoutAfterDisconnect?.cancel()
            val elapsedTimeMils = System.currentTimeMillis() - state.lastDisconnect
            logger.d {
                "[NetworkStateListener#onConnected] #network; no args, elapsedTimeMils:$elapsedTimeMils, lastDisconnect:${state.lastDisconnect}, reconnectDeadlineMils:${state.reconnectDeadlineMils}"
            }
            if (state.lastDisconnect > 0 && elapsedTimeMils < state.reconnectDeadlineMils) {
                logger.d {
                    "[NetworkStateListener#onConnected] #network; Reconnecting (fast). Time since last disconnect is ${elapsedTimeMils / 1000} seconds. Deadline is ${state.reconnectDeadlineMils / 1000} seconds"
                }
                onFastReconnect()
            } else {
                logger.d {
                    "[NetworkStateListener#onConnected] #network; Reconnecting (full). Time since last disconnect is ${elapsedTimeMils / 1000} seconds. Deadline is ${state.reconnectDeadlineMils / 1000} seconds"
                }
                onRejoin()
            }
        }

        override suspend fun onDisconnected() {
            onDisconnected()
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; old lastDisconnect:${state.lastDisconnect}, clientImpl.leaveAfterDisconnectSeconds:$leaveAfterDisconnectSeconds"
            }
            state.lastDisconnect = System.currentTimeMillis()
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; new lastDisconnect:${state.lastDisconnect}"
            }
            leaveTimeoutAfterDisconnect = callScope.launch {
                delay(leaveAfterDisconnectSeconds * 1000)
                logger.d {
                    "[NetworkStateListener#onDisconnected] #network; Leaving after being disconnected for $leaveAfterDisconnectSeconds"
                }
                onLeaveTimeout()
            }
            logger.d { "[NetworkStateListener#onDisconnected] #network; at ${state.lastDisconnect}" }
        }
    }

    fun reset() {
        leaveTimeoutAfterDisconnect?.cancel()
    }
}

internal data class CallConnectivityMonitorState(var lastDisconnect: Long = 0L, var reconnectDeadlineMils: Int = 10_000)
