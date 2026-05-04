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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.PeerConnection.PeerConnectionState

private const val PEER_CONNECTION_OBSERVER_TIMEOUT = 5_000L
private const val INTERCEPTOR_TIMEOUT_MS = 5_000L

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
        interceptor: RingingCallJoinInterceptor,
        onReady: () -> Unit,
    ) {
        logger.d { "[awaitAndTransition], ringingState: $currentRingingState" }

        if (strategy == TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR) {
            onReady()
            return
        }

        val isRingingCall = previousRingingStates.any {
            it is RingingState.Incoming || it is RingingState.Outgoing
        }

        when {
            !isRingingCall -> onReady()
            currentRingingState !is RingingState.Active -> observePeerConnection(
                call,
                interceptor,
                onReady,
            )
        }
    }

    private fun observePeerConnection(
        call: Call,
        interceptor: RingingCallJoinInterceptor,
        onReady: () -> Unit,
    ) {
        if (peerConnectionObserverJob?.isActive == true) return

        peerConnectionObserverJob = coroutineScope.launch {
            val start = System.currentTimeMillis()
            val result = withTimeoutOrNull(timeoutMs) { buildConnectionFlow(call).first() }
            val duration = System.currentTimeMillis() - start

            logConnectionResult(result, duration)

            if (isActive) {
                invokeInterceptorSafely(call, interceptor)
                onReady()
                cleanup()
            }
        }
    }

    private fun buildConnectionFlow(call: Call): Flow<Unit> =
        call.session
            .filterNotNull()
            .flatMapLatest { session ->
                session.publisher
                    .filterNotNull()
                    .flatMapLatest { it.state }
                    .filter { it == PeerConnectionState.CONNECTED }
                    .map { }
            }

    private suspend fun invokeInterceptorSafely(call: Call, interceptor: RingingCallJoinInterceptor) {
        try {
            withTimeoutOrNull(INTERCEPTOR_TIMEOUT_MS) {
                interceptor.callReadyToJoinWithTimeout(call)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "[RingingCallJoinInterceptor] interceptor threw, proceeding to Active" }
        }
    }

    private fun logConnectionResult(result: Unit?, duration: Long) {
        if (result != null) {
            logger.d { "[observeConnection] Connected in ${duration}ms" }
        } else {
            logger.w { "[observeConnection] Timeout after ${duration}ms" }
        }
    }

    fun cleanup() {
        peerConnectionObserverJob?.cancel()
        peerConnectionObserverJob = null
    }
}
