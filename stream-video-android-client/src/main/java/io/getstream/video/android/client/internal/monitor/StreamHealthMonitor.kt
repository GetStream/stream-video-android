package io.getstream.video.android.client.internal.monitor

import io.getstream.log.TaggedLogger
import io.getstream.video.android.client.internal.log.provideLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock



internal class StreamHealthMonitor(
    private val logger: TaggedLogger = provideLogger(tag = "HealthMonitor"),
    private val scope: CoroutineScope,
    private val interval: Long = INTERVAL,
    private val livenessThreshold: Long = ALIVE_THRESHOLD
) {

    companion object {
        const val INTERVAL = 25_000L
        const val ALIVE_THRESHOLD = 60_000L
    }

    private var monitorJob: Job? = null
    private var lastAck: Long = Clock.System.now().toEpochMilliseconds()

    // callbacks default to no-op
    private var onIntervalCallback: () -> Unit = {}
    private var onLivenessThresholdCallback: () -> Unit = {}

    /**
     * Register a callback to run every [interval] ms *if* the socket is still considered alive.
     */
    fun onInterval(callback: () -> Unit) {
        onIntervalCallback = callback
    }

    /**
     * Register a callback to run when no ack has been received for more than [livenessThreshold] ms.
     */
    fun onLivenessThreshold(callback: () -> Unit) {
        onLivenessThresholdCallback = callback
    }

    /** Call this whenever you get a “pong” or other liveness signal over the socket */
    fun ack() {
        lastAck = Clock.System.now().toEpochMilliseconds()
    }

    /** Starts (or restarts) the periodic health-check loop */
    fun start() {
        if (monitorJob?.isActive == true) {
            logger.d { "Health monitor already running" }
            return
        }
        monitorJob = scope.launch {
            while (isActive) {
                delay(interval)

                val now = Clock.System.now().toEpochMilliseconds()
                if (now - lastAck > livenessThreshold) {
                    logger.d { "Liveness threshold reached" }
                    onLivenessThresholdCallback()
                } else {
                    logger.d { "Running health check" }
                    onIntervalCallback()
                }
            }
        }
    }

    /** Stops the health-check loop */
    fun stop() {
        monitorJob?.cancel()
    }
}
