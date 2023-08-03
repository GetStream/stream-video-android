/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
public class CallHealthMonitor(val call: Call, val callScope: CoroutineScope) {
    private val logger by taggedLogger("Call:HealthMonitor")

    private val network by lazy { call.clientImpl.connectionModule.networkStateProvider }

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(callScope.coroutineContext + supervisorJob)

    // ensures we don't attempt to reconnect, if we attempted to reconnect less than 700ms ago
    var reconnectInProgress: Boolean = false
    var reconnectionAttempts = 0
    val checkInterval = 5000L
    var lastReconnectAt: OffsetDateTime? = null
    val reconnectDebounceMs = 700L

    val badStates = listOf(
        PeerConnection.PeerConnectionState.DISCONNECTED,
        PeerConnection.PeerConnectionState.FAILED,
        PeerConnection.PeerConnectionState.CLOSED
    )

    fun start() {
        logger.i { "starting call health monitor" }
        network.subscribe(networkStateListener)
        monitorPeerConnection()
        monitorInterval()
    }

    fun stop() {
        supervisorJob.cancel()
        network.unsubscribe(networkStateListener)
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
        val subscriberState = call.session?.subscriber?.state?.value
        val publisherState = call.session?.publisher?.state?.value
        val healthyPeerConnections = subscriberState in goodStates && publisherState in goodStates

        logger.d { "checking call health: peers are healthy: $healthyPeerConnections publisher $publisherState subscriber $subscriberState" }

        if (healthyPeerConnections) {
            // don't reconnect if things are healthy
            reconnectionAttempts = 0
            lastReconnectAt = null
            if (call.state._connection.value != RealtimeConnection.Connected) {
                logger.i { "call health check passed, marking connection as healthy" }
                call.state._connection.value = RealtimeConnection.Connected
            }
        } else {
            logger.w { "call health check failed, reconnecting. publisher $publisherState subscriber $subscriberState" }
            scope.launch { reconnect() }
        }
    }

    /**
     * Only 1 reconnect attempt runs at the same time
     * Will skip if we already tried to reconnect less than reconnectDebounceMs ms ago
     */
    suspend fun reconnect() {
        if (reconnectInProgress) return

        logger.i { "attempting to reconnect" }

        reconnectInProgress = true
        reconnectionAttempts++

        val now = OffsetDateTime.now()

        val timeDifference = if (lastReconnectAt != null) {
            ChronoUnit.MILLIS.between(lastReconnectAt?.toInstant(), now.toInstant())
        } else {
            10000L
        }

        logger.i { "reconnect called, reconnect attempt: $reconnectionAttempts, time since last reconnect $timeDifference" }

        // ensure we don't run the reconnect too often
        if (timeDifference < reconnectDebounceMs) {
            logger.d { "reconnect skip" }
        } else {
            lastReconnectAt = now

            call.reconnectOrSwitchSfu()
        }

        reconnectInProgress = false
    }

    // monitor the network state since it's faster to detect recovered network sometimes
    internal val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override fun onConnected() {
            logger.i { "network connected, running check to see if we should reconnect" }
            scope.launch {
                check()
            }
        }

        override fun onDisconnected() {
            val connectionState = call.state._connection.value
            logger.i { "network disconnected. connection is $connectionState marking the connection as reconnecting" }
            if (connectionState is RealtimeConnection.Joined || connectionState == RealtimeConnection.Connected) {
                call.state._connection.value = RealtimeConnection.Reconnecting
            }
        }
    }

    // monitor the peer connection since it's most likely to break
    fun monitorPeerConnection() {
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
    fun monitorInterval() {
        scope.launch {
            while (true) {
                delay(checkInterval)
                check()
            }
        }
    }
}
