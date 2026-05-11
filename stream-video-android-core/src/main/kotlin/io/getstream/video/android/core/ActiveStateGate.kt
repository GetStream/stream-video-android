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
    private var interceptorJob: Job? = null

    internal fun awaitAndTransition(
        currentRingingState: RingingState,
        call: Call,
        interceptor: CallJoinInterceptor?,
        onReady: () -> Unit,
    ) {
        logger.d { "[awaitAndTransition], ringingState: $currentRingingState" }

        if (strategy == TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR) {
            handleLegacyBehaviour(call, onReady, interceptor)
            return
        }

        val isRingingCall = previousRingingStates.any {
            it is RingingState.Incoming || it is RingingState.Outgoing
        }

        launchGate(call, interceptor, waitForPeerConnection = isRingingCall, onReady)
    }

    private fun handleLegacyBehaviour(call: Call, onReady: () -> Unit, interceptor: CallJoinInterceptor?) {
        if (interceptorJob?.isActive == true) return

        if (interceptor == null) {
            onReady()
            return
        }

        interceptorJob = coroutineScope.launch {
            val shouldProceed = invokeInterceptor(call, interceptor)
            if (!isActive) return@launch

            if (shouldProceed) onReady()
            cleanupInterceptorJob()
        }
    }

    private fun launchGate(
        call: Call,
        interceptor: CallJoinInterceptor?,
        waitForPeerConnection: Boolean,
        onReady: () -> Unit,
    ) {
        logger.d {
            "[launchGate] peerConnectionObserverJob?.isActive: ${peerConnectionObserverJob?.isActive}"
        }
        if (peerConnectionObserverJob?.isActive == true) return

        peerConnectionObserverJob = coroutineScope.launch {
            if (waitForPeerConnection) {
                awaitPeerConnection(call)
                if (!isActive) return@launch
            }

            val shouldProceed = invokeInterceptor(call, interceptor)
            if (!isActive) return@launch

            if (shouldProceed) onReady()
            cleanup()
        }
    }

    private suspend fun awaitPeerConnection(call: Call) {
        val start = System.currentTimeMillis()
        val result = withTimeoutOrNull(timeoutMs) { buildConnectionFlow(call).first() }
        logConnectionResult(result, System.currentTimeMillis() - start)
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

    private suspend fun invokeInterceptor(
        call: Call,
        interceptor: CallJoinInterceptor?,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        logger.d { "[invokeInterceptor] start at $startTime" }
        if (interceptor == null) return true
        return try {
            withTimeoutOrNull(INTERCEPTOR_TIMEOUT_MS) {
                interceptor.callReadyToJoin(call)
            }
            logger.d { "[invokeInterceptor] finish at ${(System.currentTimeMillis() - startTime) / 1000}s " }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: CallJoinInterceptionException) {
            val message = "[CallJoinInterceptor] aborted with reason: ${e.reason}"
            logger.e(e) { message }
            call.leave(reason = message)
            false
        } catch (e: Exception) {
            logger.e(e) { "[CallJoinInterceptor] interceptor threw, proceeding" }
            true
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
        cleanupInterceptorJob()
    }

    fun cleanupInterceptorJob() {
        interceptorJob?.cancel()
        interceptorJob = null
    }
}
