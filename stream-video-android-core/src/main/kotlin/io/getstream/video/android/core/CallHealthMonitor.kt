/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.getstream.android.video.generated.models.OwnCapability
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit
import org.webrtc.PeerConnection

/**
 * Monitors
 * - Publisher and subscriber Peer connection states -> immediately reconnect
 * - Network up/down -> mark down instantly when down. reconnect when up
 * - Interval every 2 seconds. check and decide what to do
 *
 * Calls call.reconnectOrSwitchSfu() when needed
 *
 * Notes
 * - There is a delay after a restart till connections show healthy again
 * - So we shouldn't immediately try to reconnect if we're already reconnecting
 *
 */
public class CallHealthMonitor(
    val call: Call,
    val callScope: CoroutineScope,
    val onIceRecoveryFailed: () -> Unit,
) {
    private val logger by taggedLogger("Call:HealthMonitor")

    private val network by lazy { call.clientImpl.coordinatorConnectionModule.networkStateProvider }

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(callScope.coroutineContext + supervisorJob)

    // ensures we don't attempt to reconnect, if we attempted to reconnect less than 700ms ago
    private var reconnectInProgress: Boolean = false
    private val checkInterval = 5000L
    private var lastReconnectAt: OffsetDateTime? = null
    private val reconnectDebounceMs = 700L
    private val iceRestartTimeout = 6000L
    private var isRunning = false
    private var timeoutJob: Job? = null

    val badStates = listOf(
        PeerConnection.PeerConnectionState.DISCONNECTED,
        PeerConnection.PeerConnectionState.FAILED,
        PeerConnection.PeerConnectionState.CLOSED,
    )

    val badStatesExcludingClosed = listOf(
        PeerConnection.PeerConnectionState.DISCONNECTED,
        PeerConnection.PeerConnectionState.FAILED,
    )

    fun start() {
        // Don't start multiple instances of call health monitor for one call.
        // We keep it running until the call is left and then we stop (it can't
        // be restarted again)
        if (isRunning) {
            return
        }
        isRunning = true
        supervisorJob.start()
        logger.i { "starting call health monitor" }
        network.subscribe(networkStateListener)
        monitorPeerConnection()
        monitorInterval()
    }

    fun stop() {
        isRunning = false
        supervisorJob.cancel()
        network.unsubscribe(networkStateListener)
    }

    fun stopTimer() {
        timeoutJob?.cancel()
    }

    val goodStates = listOf(
        PeerConnection.PeerConnectionState.NEW, // New is good, means we're not using it yet
        PeerConnection.PeerConnectionState.CONNECTED,
        PeerConnection.PeerConnectionState.CONNECTING,
    )

    /**
     * Checks the peer connection states.
     * Launches reconnect() if not healthy
     */
    @Synchronized
    fun check() {
        // skip health checks if we are migrating
        if (call.state._connection.value == RealtimeConnection.Migrating) {
            logger.d { "Skipping health-check - we are migrating" }
            return
        }

        val subscriberState = call.session?.subscriber?.state?.value
        val publisherState = call.session?.publisher?.state?.value
        val canPublish = call.state.ownCapabilities.value.any {
            it == OwnCapability.SendAudio || it == OwnCapability.SendVideo
        }

        val isSubConnectionHealthy = subscriberState in goodStates
        val isPubConnectionHealthyOrNotNeeded = publisherState in goodStates || (publisherState == null && !canPublish)
        val arePeerConnectionsHealthy = isSubConnectionHealthy && isPubConnectionHealthyOrNotNeeded

        logger.d {
            "checking call health: peers are healthy: $arePeerConnectionsHealthy publisher $publisherState subscriber $subscriberState"
        }

        if (arePeerConnectionsHealthy) {
            // don't reconnect if things are healthy
            timeoutJob?.cancel()
            timeoutJob = null
            lastReconnectAt = null

            if (call.state._connection.value != RealtimeConnection.Connected) {
                logger.i { "call health check passed, marking connection as healthy" }
                call.state._connection.value = RealtimeConnection.Connected
            }
        } else {
            logger.w {
                "call health check failed, reconnecting. publisher $publisherState subscriber $subscriberState"
            }

            // We start a timer in DISCONNECTED or FAILED state (not in CLOSED - because that's usually closed
            // by us during reconnection)
            if (timeoutJob == null &&
                (subscriberState in badStatesExcludingClosed || publisherState in badStatesExcludingClosed)
            ) {
                timeoutJob = scope.launch {
                    delay(iceRestartTimeout)
                    onIceRecoveryFailed.invoke()
                    return@launch
                }
            }
        }
    }

    /**
     * Only 1 reconnect attempt runs at the same time
     * Will skip if we already tried to reconnect less than reconnectDebounceMs ms ago
     */
    suspend fun reconnect(forceRestart: Boolean) {
        if (reconnectInProgress && !forceRestart) {
            logger.d { "[reconnect] Reconnect already in progress - skipping" }
            return
        }

        if (call.state._connection.value == RealtimeConnection.Migrating) {
            logger.d { "[reconnect] Skipping reconnect - already migrating" }
            return
        }

        logger.i { "attempting to reconnect" }

        reconnectInProgress = true

        val now = OffsetDateTime.now()

        val timeDifference = if (lastReconnectAt != null) {
            ChronoUnit.MILLIS.between(lastReconnectAt?.toInstant(), now.toInstant())
        } else {
            null
        }

        logger.i {
            "reconnect called, time since last reconnect $timeDifference"
        }

        // ensure we don't run the reconnect too often
        if (timeDifference != null && timeDifference < reconnectDebounceMs && !forceRestart) {
            logger.d { "[reconnect] skipping reconnect - too often" }
        } else {
            lastReconnectAt = now
            call.rejoin()
        }

        reconnectInProgress = false
    }

    // monitor the network state since it's faster to detect recovered network sometimes
    internal val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override suspend fun onConnected() {
            logger.i { "network connected, running check to see if we should reconnect" }
            scope.launch {
                check()
            }
        }

        override suspend fun onDisconnected() {
            val connectionState = call.state._connection.value
            logger.i {
                "network disconnected. connection is $connectionState marking the connection as reconnecting"
            }
            if (connectionState is RealtimeConnection.Joined || connectionState == RealtimeConnection.Connected) {
                call.state._connection.value = RealtimeConnection.Reconnecting
            }
        }
    }

    // monitor the peer connection since it's most likely to break
    private fun monitorPeerConnection() {
        val session = call.session

        scope.launch {
            session?.let {
                // failed and closed indicate we should retry connecting to this or another SFU
                // disconnected is temporary, only if it lasts for a certain duration we should reconnect or switch
                it.subscriber?.state?.collect {
                    logger.d { "subscriber ice connection state changed to $it" }
                    check()
                }
            }
        }

        scope.launch {
            session?.let {
                // failed and closed indicate we should retry connecting to this or another SFU
                // disconnected is temporary, only if it lasts for a certain duration we should reconnect or switch
                it.publisher?.state?.collect {
                    logger.d { "publisher ice connection state changed to $it " }
                    check()
                }
            }
        }
    }

    // and for all other scenarios recheck every checkInterval ms
    private fun monitorInterval() {
        scope.launch {
            while (true) {
                delay(checkInterval)
                check()
            }
        }
    }
}
