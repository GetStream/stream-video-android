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
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class CallConnectivityMonitor(
    val callScope: CoroutineScope,
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
            if (state.lastDisconnect > 0 && elapsedTimeMils < state.reconnectDeadlineMils) {
                onFastReconnect()
            } else {
                onRejoin()
            }
        }

        override suspend fun onDisconnected() {
            onDisconnected()
            state.lastDisconnect = System.currentTimeMillis()
            leaveTimeoutAfterDisconnect = callScope.launch {
                delay(leaveAfterDisconnectSeconds * 1000)
                onLeaveTimeout()
            }
        }
    }
}

internal data class CallConnectivityMonitorState(var lastDisconnect: Long = 0L, var reconnectDeadlineMils: Int = 10_000)
