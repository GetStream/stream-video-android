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

package io.getstream.video.android.core.socket.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val HEALTH_CHECK_INTERVAL = 10 * 1000L
private const val MONITOR_INTERVAL = 1000L
private const val NO_EVENT_INTERVAL_THRESHOLD = 30 * 1000L
private const val MONITOR_START_DELAY = 1000L

/**
 * The health monitor sends a health check ping every HEALTH_CHECK_INTERVAL milliseconds.
 *
 * It tracks when we last received (any) event.
 * If we go more than NO_EVENT_INTERVAL_THRESHOLD milliseconds without receiving an event
 * We know the connection is broken and disconnect
 *
 * The healthCallback exposes 2 methods
 * - reconnect (tells the socket to reconnect)
 * - check (tells the socket to send a healthcheck ping)
 *
 * Whenever the socket receives an event, monitor.ack should be called
 */
internal class HealthMonitor(
    private val healthCallback: HealthCallback,
    private val scope: CoroutineScope,
) {

    private var reconnectInProgress: Boolean = false
    private var healthPingJob: Job? = null
    private var monitorJob: Job? = null

    private var consecutiveFailures = 0
    private var disconnected = false
    private var lastEventDate: Date = Date()

    fun start() {
        lastEventDate = Date()
        disconnected = false

        healthPingJob = scope.launch {
            while (true) {
                delay(HEALTH_CHECK_INTERVAL)
                // send the ping
                healthCallback.check()
            }
        }

        monitorJob = scope.launch {
            while (true) {
                delay(MONITOR_INTERVAL)
                if (needToReconnect()) {
                    reconnect()
                }
            }
        }
    }

    fun stop() {
        healthPingJob?.cancel()
        monitorJob?.cancel()
    }

    fun ack() {
        lastEventDate = Date()
        disconnected = false
        consecutiveFailures = 0
    }

    fun cleanup() {
        stop()
    }

    private suspend fun reconnect() {
        if (!reconnectInProgress) {
            reconnectInProgress = true
            val retryInterval = getRetryInterval(consecutiveFailures)
            consecutiveFailures++
            delay(retryInterval)
            healthCallback.reconnect()
            reconnectInProgress = false
        }
    }

    private fun needToReconnect() =
        disconnected || (Date().time - lastEventDate.time) >= NO_EVENT_INTERVAL_THRESHOLD

    @Suppress("MagicNumber")
    private fun getRetryInterval(consecutiveFailures: Int): Long {
        if (consecutiveFailures == 0) return 0
        val max = min(500 + consecutiveFailures * 2000, 25000)
        val min = min(
            max(250, (consecutiveFailures - 1) * 2000),
            25000,
        )
        return floor(Math.random() * (max - min) + min).toLong()
    }

    interface HealthCallback {
        fun check()
        suspend fun reconnect()
    }
}
