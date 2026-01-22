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

import android.annotation.SuppressLint
import android.os.PowerManager
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.Call.Companion.testInstanceProvider
import io.getstream.video.android.core.CallStatsReport
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.toIceServer
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.StreamSingleFlightProcessorImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.webrtc.PeerConnection
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.UUID
import kotlin.collections.plus

private const val PERMISSION_ERROR = "\n[Call.join()] called without having the required permissions.\n" +
    "This will work only if you have [runForegroundServiceForCalls = false] in the StreamVideoBuilder.\n" +
    "The reason is that [Call.join()] will by default start an ongoing call foreground service,\n" +
    "To start this service and send the appropriate audio/video tracks the permissions are required,\n" +
    "otherwise the service will fail to start, resulting in a crash.\n" +
    "You can re-define your permissions and their expected state by overriding the [permissionCheck] in [StreamVideoBuilder]\n"

internal class CallSessionManager(
    private val call: Call,
    private val clientImpl: StreamVideoClient,
    private val powerManager: PowerManager?,

) {
    private val logger by taggedLogger("CallSessionManager")

    internal var callStatsReportingJob: Job? = null

    internal var session: RtcSession? = null
    internal var sessionId = UUID.randomUUID().toString()

    private var reconnectAttempts = 0
    internal var reconnectStartTime = 0L
    internal var connectStartTime = 0L
    private var lastDisconnect = 0L
    private var reconnectDeadlineMils: Int = 10_000
    private var leaveTimeoutAfterDisconnect: Job? = null

    internal var monitorPublisherPCStateJob: Job? = null
    internal var monitorSubscriberPCStateJob: Job? = null
    internal var sfuListener: Job? = null
    internal var sfuEvents: Job? = null

    internal val network by lazy { clientImpl.coordinatorConnectionModule.networkStateProvider }

    private val streamSingleFlightProcessorImpl = StreamSingleFlightProcessorImpl(call.scope)

    /**
     * Indicates whether this Call has been left at least once.
     * Used to determine if reinitialization is needed on next join().
     *
     * THREAD SAFETY: Access must be protected using cleanupMutex.withLock { }.
     * Reading/writing this field outside mutex is NOT safe.
     */
    internal var hasBeenLeft = false

    private val listener = object : NetworkStateProvider.NetworkStateListener {
        override suspend fun onConnected() {
            leaveTimeoutAfterDisconnect?.cancel()

            val elapsedTimeMils = System.currentTimeMillis() - lastDisconnect
            logger.d {
                "[NetworkStateListener#onConnected] #network; no args, elapsedTimeMils:$elapsedTimeMils, lastDisconnect:$lastDisconnect, reconnectDeadlineMils:$reconnectDeadlineMils"
            }
            if (lastDisconnect > 0 && elapsedTimeMils < reconnectDeadlineMils) {
                logger.d {
                    "[NetworkStateListener#onConnected] #network; Reconnecting (fast). Time since last disconnect is ${elapsedTimeMils / 1000} seconds. Deadline is ${reconnectDeadlineMils / 1000} seconds"
                }
                fastReconnect("NetworkStateListener#onConnected")
            } else {
                logger.d {
                    "[NetworkStateListener#onConnected] #network; Reconnecting (full). Time since last disconnect is ${elapsedTimeMils / 1000} seconds. Deadline is ${reconnectDeadlineMils / 1000} seconds"
                }
                rejoin("NetworkStateListener#onConnected")
            }
        }

        override suspend fun onDisconnected() {
            call.state._connection.value = RealtimeConnection.Reconnecting
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; old lastDisconnect:$lastDisconnect, clientImpl.leaveAfterDisconnectSeconds:${clientImpl.leaveAfterDisconnectSeconds}"
            }
            lastDisconnect = System.currentTimeMillis()
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; new lastDisconnect:$lastDisconnect"
            }
            leaveTimeoutAfterDisconnect = call.scope.launch {
                delay(clientImpl.leaveAfterDisconnectSeconds * 1000)
                logger.d {
                    "[NetworkStateListener#onDisconnected] #network; Leaving after being disconnected for ${clientImpl.leaveAfterDisconnectSeconds}"
                }
                call.leave()
            }
            logger.d { "[NetworkStateListener#onDisconnected] #network; at $lastDisconnect" }
        }
    }

    suspend fun join(
        create: Boolean,
        createOptions: CreateCallOptions?,
        ring: Boolean,
        notify: Boolean,
    ): Result<RtcSession> {
        logger.d {
            "[join] #ringing; #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }
        // Wait for cleanup
        val job = call.cleanupMutex.withLock { call.cleanupJob?.takeIf { it.isActive } }
        job?.let {
            logger.d { "[join] Waiting for cleanup job: $it" }
            try {
                withTimeout(5000) { it.join() }
                logger.d { "[join] Cleanup complete" }
            } catch (e: TimeoutCancellationException) {
                logger.w { "[join] Cleanup timeout, proceeding anyway" }
            }

            call.cleanupMutex.withLock {
                if (call.cleanupJob == it) {
                    call.cleanupJob = null
                }
            }
        }

        // Reinitialize if needed
        val needsReinit = call.cleanupMutex.withLock {
            if (hasBeenLeft) {
                hasBeenLeft = false
                true
            } else {
                false
            }
        }

        if (needsReinit) {
            logger.d { "[join] Reinitializing for rejoin" }
            call.reinitializeForRejoin()
        }

        // CRITICAL: Reset isDestroyed for new session
        call.isDestroyed = false
        logger.d { "[join] isDestroyed reset to false for new session" }

        val permissionPass =
            clientImpl.permissionCheck.checkAndroidPermissionsGroup(clientImpl.context, call)
        // Check android permissions and log a warning to make sure developers requested adequate permissions prior to using the call.
        if (!permissionPass.first) {
            logger.w { PERMISSION_ERROR }
        }
        // if we are a guest user, make sure we wait for the token before running the join flow
        clientImpl.guestUserJob?.await()

        // Ensure factory is created with the current audioBitrateProfile before joining
        call.ensureFactoryMatchesAudioProfile()

        // the join flow should retry up to 3 times
        // if the error is not permanent
        // and fail immediately on permanent errors
        call.state._connection.value = RealtimeConnection.InProgress
        var retryCount = 0

        var result: Result<RtcSession>

        call.atomicLeave = AtomicUnitCall()
        while (retryCount < 3) {
            result = _join(create, createOptions, ring, notify)
            if (result is Success) {
                // we initialise the camera, mic and other according to local + backend settings
                // only when the call is joined to make sure we don't switch and override
                // the settings during a call.
                val settings = call.state.settings.value
                if (settings != null) {
                    call.updateMediaManagerFromSettings(settings)
                } else {
                    logger.w {
                        "[join] Call settings were null - this should never happen after a call" +
                            "is joined. MediaManager will not be initialised with server settings."
                    }
                }
                return result
            }
            if (result is Failure) {
                session = null
                logger.e { "Join failed with error $result" }
                if (isPermanentError(result.value)) {
                    call.state._connection.value = RealtimeConnection.Failed(result.value)
                    return result
                } else {
                    retryCount += 1
                }
            }
            delay(retryCount - 1 * 1000L)
        }
        session = null
        val errorMessage = "Join failed after 3 retries"
        call.state._connection.value = RealtimeConnection.Failed(errorMessage)
        return Failure(value = Error.GenericError(errorMessage))
    }

    @SuppressLint("VisibleForTests")
    private suspend fun _join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<RtcSession> {
        reconnectAttempts = 0
        sfuEvents?.cancel()
        sfuListener?.cancel()

        if (session != null) {
            return Failure(Error.GenericError("Call $call.cid has already been joined"))
        }
        logger.d {
            "[joinInternal] #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }

        connectStartTime = System.currentTimeMillis()

        // step 1. call the join endpoint to get a list of SFUs
        val locationResult = clientImpl.getCachedLocation()
        if (locationResult !is Success) {
            return locationResult as Failure
        }
        call.location = locationResult.value

        val options = createOptions
            ?: if (create) {
                CreateCallOptions()
            } else {
                null
            }
        val result = call.joinRequest(options, locationResult.value, ring = ring, notify = notify)

        if (result !is Success) {
            return result as Failure
        }
        val sfuToken = result.value.credentials.token
        val sfuUrl = result.value.credentials.server.url
        val sfuWsUrl = result.value.credentials.server.wsEndpoint
        val iceServers = result.value.credentials.iceServers.map { it.toIceServer() }
        try {
            session = if (testInstanceProvider.rtcSessionCreator != null) {
                testInstanceProvider.rtcSessionCreator!!.invoke()
            } else {
                RtcSession(
                    sessionId = this.sessionId,
                    apiKey = clientImpl.apiKey,
                    lifecycle = clientImpl.coordinatorConnectionModule.lifecycle,
                    client = clientImpl,
                    call = call,
                    sfuUrl = sfuUrl,
                    sfuWsUrl = sfuWsUrl,
                    sfuToken = sfuToken,
                    remoteIceServers = iceServers,
                    powerManager = powerManager,
                )
            }

            session?.let {
                call.state._connection.value = RealtimeConnection.Joined(it)
            }

            session?.connect()
        } catch (e: Exception) {
            return Failure(Error.GenericError(e.message ?: "RtcSession error occurred."))
        }
        clientImpl.state.setActiveCall(call)
        monitorSession(result.value)
        return Success(value = session!!)
    }

    internal fun isPermanentError(error: Any): Boolean {
        if (error is Error.ThrowableError) {
            if (error.message.contains("Unable to resolve host")) {
                return false
            }
        }
        return true
    }

    suspend fun fastReconnect(reason: String) = schedule("fast") {
        logger.d {
            "[fastReconnect] Reconnecting, reconnectAttempts:$reconnectAttempts"
        }
        session?.prepareReconnect()
        call.state._connection.value = RealtimeConnection.Reconnecting
        if (session != null) {
            reconnectStartTime = System.currentTimeMillis()

            val session = session!!
            val (prevSessionId, subscriptionsInfo, publishingInfo) = session.currentSfuInfo()
            val reconnectDetails = ReconnectDetails(
                previous_session_id = prevSessionId,
                strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                announced_tracks = publishingInfo,
                subscriptions = subscriptionsInfo,
                reconnect_attempt = reconnectAttempts,
                reason = reason,
            )
            session.fastReconnect(reconnectDetails)
            val oldSessionStats = collectStats()
            session.sendCallStats(oldSessionStats)
        } else {
            logger.d { "[fastReconnect] [RealtimeConnection.Disconnected], call_id:${call.id}" }
            call.state._connection.value = RealtimeConnection.Disconnected
        }
    }

    @SuppressLint("VisibleForTests")
    internal suspend fun rejoin(reason: String) = schedule("rejoin") {
        logger.d { "[rejoin] Rejoining" }
        reconnectAttempts++
        call.state._connection.value = RealtimeConnection.Reconnecting
        call.location?.let {
            reconnectStartTime = System.currentTimeMillis()

            val joinResponse = call.joinRequest(location = it)
            if (joinResponse is Success) {
                // switch to the new SFU
                val cred = joinResponse.value.credentials
                val oldSession = this.session!!
                val oldSessionStats = collectStats()
                val currentOptions = this.session?.publisher?.currentOptions()
                logger.i { "Rejoin SFU ${oldSession?.sfuUrl} to ${cred.server.url}" }

                this.sessionId = UUID.randomUUID().toString()
                val (prevSessionId, subscriptionsInfo, publishingInfo) = oldSession.currentSfuInfo()
                val reconnectDetails = ReconnectDetails(
                    previous_session_id = prevSessionId,
                    strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
                    announced_tracks = publishingInfo,
                    subscriptions = subscriptionsInfo,
                    reconnect_attempt = reconnectAttempts,
                    reason = reason,
                )
                call.state.removeParticipant(prevSessionId)
                oldSession.prepareRejoin()
                try {
                    this.session = RtcSession(
                        clientImpl,
                        reconnectAttempts,
                        powerManager,
                        call,
                        sessionId,
                        clientImpl.apiKey,
                        clientImpl.coordinatorConnectionModule.lifecycle,
                        cred.server.url,
                        cred.server.wsEndpoint,
                        cred.token,
                        cred.iceServers.map { ice ->
                            ice.toIceServer()
                        },
                    )
                    this.session?.connect(reconnectDetails, currentOptions)
                    this.session?.sfuTracer?.trace("rejoin", reason)
                    oldSession.sendCallStats(oldSessionStats)
                    oldSession.leaveWithReason("Rejoin :: $reason")
                    oldSession.cleanup()
                    monitorSession(joinResponse.value)
                } catch (ex: Exception) {
                    logger.e(ex) {
                        "[rejoin] Failed to join response with ex: ${ex.message}"
                    }
                    call.state._connection.value = RealtimeConnection.Failed(ex)
                }
            } else {
                logger.e {
                    "[rejoin] Failed to get a join response ${joinResponse.errorOrNull()}"
                }
                call.state._connection.value = RealtimeConnection.Reconnecting
            }
        }
    }

    internal fun monitorSession(result: JoinCallResponse) {
        sfuEvents?.cancel()
        sfuListener?.cancel()
        startCallStatsReporting(result.statsOptions.reportingIntervalMs.toLong())
        // listen to Signal WS
        sfuEvents = call.scope.launch {
            session?.let {
                it.socket.events().collect { event ->
                    if (event is JoinCallResponseEvent) {
                        reconnectDeadlineMils = event.fastReconnectDeadlineSeconds * 1000
                        logger.d { "[join] #deadline for reconnect is ${reconnectDeadlineMils / 1000} seconds" }
                    }
                }
            }
        }
        monitorPublisherPCStateJob?.cancel()
        monitorPublisherPCStateJob = call.scope.launch {
            session?.publisher?.iceState?.collect {
                when (it) {
                    PeerConnection.IceConnectionState.FAILED, PeerConnection.IceConnectionState.DISCONNECTED -> {
                        session?.publisher?.connection?.restartIce()
                    }

                    else -> {
                        logger.d { "[monitorPubConnectionState] Ice connection state is $it" }
                    }
                }
            }
        }

        monitorSubscriberPCStateJob?.cancel()
        monitorSubscriberPCStateJob = call.scope.launch {
            session?.subscriber?.iceState?.collect {
                when (it) {
                    PeerConnection.IceConnectionState.FAILED, PeerConnection.IceConnectionState.DISCONNECTED -> {
                        session?.requestSubscriberIceRestart()
                    }

                    else -> {
                        logger.d { "[monitorSubConnectionState] Ice connection state is $it" }
                    }
                }
            }
        }
        network.subscribe(listener)
    }

    private fun startCallStatsReporting(reportingIntervalMs: Long = 10_000) {
        callStatsReportingJob?.cancel()
        callStatsReportingJob = call.scope.launch {
            // Wait a bit before we start capturing stats
            delay(reportingIntervalMs)

            while (isActive) {
                delay(reportingIntervalMs)
                session?.sendCallStats(
                    report = collectStats(),
                )
            }
        }
    }

    suspend fun migrate() = schedule("migrate") {
        logger.d { "[migrate] Migrating" }
        call.state._connection.value = RealtimeConnection.Migrating
        call.location?.let {
            reconnectStartTime = System.currentTimeMillis()

            val joinResponse = call.joinRequest(location = it)
            if (joinResponse is Success) {
                // switch to the new SFU
                val cred = joinResponse.value.credentials
                val session = this.session!!
                val currentOptions = this.session?.publisher?.currentOptions()
                val oldSfuUrl = session.sfuUrl
                logger.i { "Rejoin SFU $oldSfuUrl to ${cred.server.url}" }

                this.sessionId = UUID.randomUUID().toString()
                val (prevSessionId, subscriptionsInfo, publishingInfo) = session.currentSfuInfo()
                val reconnectDetails = ReconnectDetails(
                    previous_session_id = prevSessionId,
                    strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
                    announced_tracks = publishingInfo,
                    subscriptions = subscriptionsInfo,
                    from_sfu_id = oldSfuUrl,
                    reconnect_attempt = reconnectAttempts,
                )
                session.prepareRejoin()
                try {
                    val newSession = RtcSession(
                        clientImpl,
                        reconnectAttempts,
                        powerManager,
                        call,
                        sessionId,
                        clientImpl.apiKey,
                        clientImpl.coordinatorConnectionModule.lifecycle,
                        cred.server.url,
                        cred.server.wsEndpoint,
                        cred.token,
                        cred.iceServers.map { ice ->
                            ice.toIceServer()
                        },
                    )
                    val oldSession = this.session
                    this.session = newSession
                    this.session?.connect(reconnectDetails, currentOptions)
                    monitorSession(joinResponse.value)
                    oldSession?.leaveWithReason("migrating")
                    oldSession?.cleanup()
                } catch (ex: Exception) {
                    logger.e(ex) {
                        "[switchSfu] Failed to join during " +
                            "migration - Error ${ex.message}"
                    }
                    call.state._connection.value = RealtimeConnection.Failed(ex)
                }
            } else {
                logger.e {
                    "[switchSfu] Failed to get a join response during " +
                        "migration - falling back to reconnect. Error ${joinResponse.errorOrNull()}"
                }
                call.state._connection.value = RealtimeConnection.Reconnecting
            }
        }
    }

    internal suspend fun collectStats(): CallStatsReport {
        val publisherStats = session?.getPublisherStats()
        val subscriberStats = session?.getSubscriberStats()
        call.state.stats.updateFromRTCStats(publisherStats, isPublisher = true)
        call.state.stats.updateFromRTCStats(subscriberStats, isPublisher = false)
        call.state.stats.updateLocalStats()
        val local = call.state.stats._local.value

        val report = CallStatsReport(
            publisher = publisherStats,
            subscriber = subscriberStats,
            local = local,
            stateStats = call.state.stats,
        )

        call.statsReport.value = report
        call.statLatencyHistory.value += report.stateStats.publisher.latency.value
        if (call.statLatencyHistory.value.size > 20) {
            call.statLatencyHistory.value = call.statLatencyHistory.value.takeLast(20)
        }

        return report
    }

    private suspend fun schedule(key: String, block: suspend () -> Unit) {
        logger.d { "[schedule] #reconnect; no args" }
        streamSingleFlightProcessorImpl.run(key, block)
    }

    private fun unsubscribe() {
        network.unsubscribe(listener)
    }

    fun cleanup() {
        session?.cleanup()
        session = null
    }

    fun cleanupMonitor() {
        monitorSubscriberPCStateJob?.cancel()
        monitorPublisherPCStateJob?.cancel()
        monitorPublisherPCStateJob = null
        monitorSubscriberPCStateJob = null
    }

    fun cleanupNetworkMonitoring() {
        leaveTimeoutAfterDisconnect?.cancel()
        unsubscribe()
        sfuListener?.cancel()
        sfuEvents?.cancel()
    }
}
