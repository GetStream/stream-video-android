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

package io.getstream.video.android.core.socket.common

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.socket.common.scope.UserScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val HEALTH_CHECK_INTERVAL = 1_000L
private const val MONITOR_INTERVAL = 10_000L
private const val NO_EVENT_INTERVAL_THRESHOLD = 30_000L

public object TimeProvider {
    private const val MILLIS_TO_SECONDS_FACTOR = 1_000L
    public fun provideCurrentTimeInSeconds(): Long = System.currentTimeMillis() / MILLIS_TO_SECONDS_FACTOR
    public fun provideCurrentTimeInMilliseconds(): Long = System.currentTimeMillis()
}

internal class HealthMonitor(
    private val healthCheckInterval: Long = HEALTH_CHECK_INTERVAL,
    private val monitorInterval: Long = MONITOR_INTERVAL,
    private val noEventIntervalThreshold: Long = NO_EVENT_INTERVAL_THRESHOLD,
    private val timeProvider: TimeProvider = TimeProvider,
    private val retryInterval: RetryInterval = ExponentialRetryInterval,
    private val userScope: UserScope,
    private val checkCallback: suspend () -> Unit,
    private val reconnectCallback: suspend () -> Unit,
) {

    private var consecutiveFailures = 0
    private var lastAck: Long = 0
    private var healthMonitorJob: Job? = null
    private var healthCheckJob: Job? = null
    private var reconnectJob: Job? = null

    private val logger by taggedLogger("Video:SocketMonitor")

    /**
     * Stop monitoring connection.
     */
    fun stop() {
        stopAllJobs()
    }

    /**
     * Notify that connection keeps alive.
     */
    fun ack() {
        resetHealthMonitor()
    }

    /**
     * Notify connection is disconnected.
     */
    fun onDisconnected() {
        stopAllJobs()
        lastAck = 0
        postponeReconnect()
    }

    /**
     * Reset health monitor process.
     */
    private fun resetHealthMonitor() {
        stopAllJobs()
        lastAck = TimeProvider.provideCurrentTimeInMilliseconds()
        consecutiveFailures = 0
        postponeHealthMonitor()
    }

    /**
     * Postpone the action to check if connection keeps alive.
     * If the connection is not alive anymore, an action to reconnect is postponed.
     * In another case the healthCheck is postponed.
     */
    private fun postponeHealthMonitor() {
        healthMonitorJob?.cancel()
        healthMonitorJob = userScope.launchDelayed(monitorInterval) {
            if (needToReconnect()) {
                postponeReconnect()
            } else {
                postponeHealthCheck()
            }
        }
    }

    /**
     * Postpone the action to send an "echo event" that will keep the connection alive.
     * Just after the event is sent, an action is postponed to verify the connection is alive.
     */
    private fun postponeHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = userScope.launchDelayed(healthCheckInterval) {
            checkCallback()
            postponeHealthMonitor()
        }
    }

    /**
     * Postpone the action to reconnect the socket.
     * Just after the reconnection of the socket is started, an action to monitor the connection is started.
     */
    private fun postponeReconnect() {
        reconnectJob?.cancel()
        val retryIntervalTime = retryInterval.nextInterval(consecutiveFailures++)
        logger.i { "Next connection attempt in $retryIntervalTime ms" }
        reconnectJob = userScope.launchDelayed(retryIntervalTime) {
            reconnectCallback()
            postponeHealthMonitor()
        }
    }

    /**
     * Stop all launched job on this health monitor.
     */
    private fun stopAllJobs() {
        reconnectJob?.cancel()
        healthCheckJob?.cancel()
        healthMonitorJob?.cancel()
    }

    /**
     * Check if time elapsed since the last received event is greater than [NO_EVENT_INTERVAL_THRESHOLD].
     *
     * @return True if time elapsed is bigger and we need to start reconnection process.
     */
    private fun needToReconnect(): Boolean =
        (TimeProvider.provideCurrentTimeInMilliseconds() - lastAck) >= noEventIntervalThreshold

    private fun CoroutineScope.launchDelayed(
        delayMilliseconds: Long,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = launch {
        delay(delayMilliseconds)
        block()
    }

    internal fun interface RetryInterval {
        fun nextInterval(consecutiveFailures: Int): Long
    }

    object ExponentialRetryInterval : RetryInterval {

        @Suppress("MagicNumber")
        override fun nextInterval(consecutiveFailures: Int): Long {
            val max = min(500 + consecutiveFailures * 2000, 25000)
            val min = min(
                max(250, (consecutiveFailures - 1) * 2000),
                25000,
            )
            return floor(Math.random() * (max - min) + min).toLong()
        }
    }
}
