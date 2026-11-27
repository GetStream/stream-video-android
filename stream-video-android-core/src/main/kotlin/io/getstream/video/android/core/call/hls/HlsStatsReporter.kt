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

package io.getstream.video.android.core.call.hls

import android.os.Build
import io.getstream.android.video.generated.models.HLSPlaybackStats
import io.getstream.android.video.generated.models.HLSPlayerEvent
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.ReportHLSStatsRequest
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/** What the reporter needs from the `join_call` response that sent the viewer to HLS. */
internal class HlsStatsConfig(
    val callSessionId: String,
    val statsToken: String,
    val unifiedSessionId: String,
    val intervalMs: Long,
) {
    companion object {
        const val DEFAULT_INTERVAL_MS = 10_000L

        /**
         * Returns null, so no reports are sent, when the app turned stats reporting off, the
         * backend sent `enable_hls_stats: false`, or the response has no stats token or call
         * session. A missing `enable_hls_stats` does not stop reporting.
         */
        fun from(
            response: JoinCallResponse,
            unifiedSessionId: String,
            statsReportingEnabled: Boolean = true,
        ): HlsStatsConfig? {
            if (!statsReportingEnabled) return null
            val options = response.statsOptions
            if (options.enableHlsStats == false) return null
            val statsToken = response.playback?.statsToken?.takeIf { it.isNotBlank() } ?: return null
            val callSessionId = response.call.currentSessionId.takeIf { it.isNotBlank() } ?: return null
            val intervalMs = options.reportingHlsIntervalMs?.takeIf { it > 0 }
                ?: options.reportingIntervalMs.takeIf { it > 0 }
            return HlsStatsConfig(
                callSessionId = callSessionId,
                statsToken = statsToken,
                unifiedSessionId = unifiedSessionId,
                intervalMs = intervalMs?.toLong() ?: DEFAULT_INTERVAL_MS,
            )
        }
    }
}

/** SDK and OS sent on every report. */
internal class HlsStatsClientInfo(
    val sdk: String,
    val sdkVersion: String,
    val os: String,
    val osVersion: String,
) {
    companion object {
        fun current(): HlsStatsClientInfo = HlsStatsClientInfo(
            sdk = "stream-android",
            sdkVersion = BuildConfig.STREAM_VIDEO_VERSION,
            os = "Android",
            osVersion = Build.VERSION.RELEASE.orEmpty(),
        )
    }
}

/**
 * Reports one HLS playback session of a call to `hls_stats`: `started` when the player first
 * plays, `stats` every [HlsStatsConfig.intervalMs], and `ended` from [stop] or when the stream
 * ends. A session that fails before its first frame cannot recover, so it sends a single `ended`
 * with the error instead.
 * Every report carries the same `user_session_id`.
 *
 * Reports are sent one at a time. Player events ride on the next report and are dropped only
 * once it is handled: a failed `stats` report keeps them for the next interval, and `started` and
 * `ended` are retried with backoff. A 403 means the stats token is no longer valid, so the
 * session stops reporting.
 */
internal class HlsStatsReporter(
    private val config: HlsStatsConfig,
    private val player: HlsPlayer,
    private val delivery: HlsStatsDelivery,
    private val scope: CoroutineScope,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val clock: HlsStatsClock = HlsStatsClock.System,
    private val clientInfo: HlsStatsClientInfo = HlsStatsClientInfo.current(),
    private val retryDelaysMs: List<Long> = DEFAULT_RETRY_DELAYS_MS,
) {
    companion object {
        const val TYPE_STARTED = "started"
        const val TYPE_STATS = "stats"
        const val TYPE_ENDED = "ended"
        const val END_REASON_LEFT = "left"
        const val END_REASON_ERROR = "error"
        const val MAX_EVENTS_PER_REPORT = 100
        val DEFAULT_RETRY_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L)

        private const val MAX_SDK_LENGTH = 50
        private const val MAX_CLIENT_FIELD_LENGTH = 64
        private const val MAX_ERROR_CODE_LENGTH = 128
    }

    private val logger by taggedLogger("Video:HlsStatsReporter")
    private val userSessionId = UUID.randomUUID().toString()
    private val collector = HlsStatsCollector(clock)
    private val sendMutex = Mutex()
    private val lock = Any()

    // Guarded by [lock].
    private var started = false
    private var stopped = false
    private var reportingJob: Job? = null

    @Volatile
    private var tokenRejected = false

    /** True once the backend has handled `started`, so `ended` never arrives on its own. */
    @Volatile
    private var startedSent = false

    /**
     * The player moved to [state], forwarded by [HlsPlaybackObserver]. Reporting begins when
     * playback first plays. [HlsPlaybackState.Ended] means the broadcast stopped and the viewer
     * has watched all of it, so the session ends there; a restart starts a new one.
     */
    fun onPlaybackStateChanged(state: HlsPlaybackState) {
        if (collector.onState(state)) startReporting()
        if (state == HlsPlaybackState.Ended) stop()
    }

    /**
     * Playback failed with [code], forwarded by [HlsPlaybackObserver]. Before the first frame this
     * ends the session with a single `ended` carrying the error.
     */
    fun onPlaybackError(code: String) {
        collector.onError(code.take(MAX_ERROR_CODE_LENGTH))
        synchronized(lock) {
            if (stopped || started) return
            stopped = true
        }
        collector.finish()
        scope.launch { send(TYPE_ENDED, withStats = false, retry = true) }
    }

    /**
     * Ends the session: stops periodic reports and ignores later player events, then sends
     * `ended` if playback played. Safe to call more than once, from any thread.
     */
    fun stop() {
        val job: Job?
        val sendEnded: Boolean
        synchronized(lock) {
            if (stopped) return
            stopped = true
            job = reportingJob
            sendEnded = started
        }
        collector.finish()
        job?.cancel()
        if (!sendEnded) return
        scope.launch {
            job?.join()
            // A stop right after the session began can cancel `started` before it went out.
            if (!startedSent) send(TYPE_STARTED, withStats = false, retry = true)
            send(TYPE_ENDED, withStats = true, retry = true)
        }
    }

    private fun startReporting() {
        synchronized(lock) {
            if (stopped || started) return
            started = true
            reportingJob = scope.launch {
                send(TYPE_STARTED, withStats = false, retry = true)
                while (isActive && !tokenRejected) {
                    delay(config.intervalMs)
                    send(TYPE_STATS, withStats = true, retry = false)
                }
            }
        }
    }

    private suspend fun send(type: String, withStats: Boolean, retry: Boolean) = sendMutex.withLock {
        var attempt = 0
        while (!tokenRejected) {
            val metrics = if (withStats) readMetrics() else null
            val events = collector.pendingEvents(MAX_EVENTS_PER_REPORT)
            val result = delivery.deliver(
                buildReport(
                    type = type,
                    stats = if (withStats) collector.stats(metrics) else null,
                    events = events,
                ),
            )
            when (result) {
                HlsStatsDeliveryResult.Delivered -> {
                    collector.acknowledgeEvents(events)
                    if (type == TYPE_STARTED) startedSent = true
                    return@withLock
                }
                HlsStatsDeliveryResult.Rejected -> {
                    logger.e { "[send] The backend rejected the $type report as invalid" }
                    collector.acknowledgeEvents(events)
                    if (type == TYPE_STARTED) startedSent = true
                    return@withLock
                }
                HlsStatsDeliveryResult.Forbidden -> {
                    logger.w { "[send] Stats token rejected, HLS stats reporting stopped" }
                    tokenRejected = true
                    return@withLock
                }
                HlsStatsDeliveryResult.Failed -> {
                    if (!retry || attempt >= retryDelaysMs.size) {
                        logger.w { "[send] Could not send the $type report" }
                        return@withLock
                    }
                    delay(retryDelaysMs[attempt++])
                }
            }
        }
    }

    private suspend fun readMetrics(): HlsPlaybackMetrics? = try {
        withContext(mainDispatcher) { player.readPlaybackMetrics() }
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        logger.w { "[readMetrics] Could not read player metrics: ${e.message}" }
        null
    }

    private fun buildReport(
        type: String,
        stats: HLSPlaybackStats?,
        events: List<HLSPlayerEvent>,
    ): ReportHLSStatsRequest {
        val errorCode = if (type == TYPE_ENDED) collector.errorCode else null
        return ReportHLSStatsRequest(
            statsToken = config.statsToken,
            callSessionId = config.callSessionId,
            userSessionId = userSessionId,
            unifiedSessionId = config.unifiedSessionId,
            type = type,
            timestamp = clock.wallMs(),
            sdk = clientInfo.sdk.take(MAX_SDK_LENGTH),
            sdkVersion = clientInfo.sdkVersion.take(MAX_SDK_LENGTH),
            os = clientInfo.os.take(MAX_CLIENT_FIELD_LENGTH),
            osVersion = clientInfo.osVersion.take(MAX_CLIENT_FIELD_LENGTH),
            endReason = when {
                type != TYPE_ENDED -> null
                errorCode != null -> END_REASON_ERROR
                else -> END_REASON_LEFT
            },
            errorCode = errorCode,
            stats = stats,
            events = events.takeIf { it.isNotEmpty() },
        )
    }
}
