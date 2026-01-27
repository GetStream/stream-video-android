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
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.model.toIceServer
import io.getstream.video.android.core.utils.StreamSingleFlightProcessorImpl
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.UUID

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
    private val testInstanceProvider: Call.Companion.TestInstanceProvider,

) {
    private val logger by taggedLogger("CallSessionManager")

    /** Session handles all real time communication for video and audio */
    internal var session: RtcSession? = null
    internal var sessionId = UUID.randomUUID().toString()

    private var reconnectAttempts = 0
    internal var reconnectStartTime = 0L
    internal var connectStartTime = 0L

    private val callConnectivityMonitorState = CallConnectivityMonitorState()
    internal val network by lazy { clientImpl.coordinatorConnectionModule.networkStateProvider }
    private val streamSingleFlightProcessorImpl = StreamSingleFlightProcessorImpl(call.scope)
    private val callStatsReporter = CallStatsReporter(call)
    private val callConnectivityMonitor = CallConnectivityMonitor(
        call.scope,
        callConnectivityMonitorState,
        clientImpl.leaveAfterDisconnectSeconds,
        {
            fastReconnect("NetworkStateListener#onConnected")
        },
        {
            rejoin("NetworkStateListener#onConnected")
        },
        {
            call.state._connection.value = RealtimeConnection.Reconnecting
        },
        { call.leave() },
    )

    val sfuEventMonitor = CallSfuEventMonitor(call.scope, { session }, callConnectivityMonitorState)
    val iceConnectionMonitor = CallIceConnectionMonitor(call.scope, { session })
    val networkSubscriptionController =
        CallNetworkSubscriptionController(network, callConnectivityMonitor.listener)

    @SuppressLint("VisibleForTests")
    internal suspend fun _join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<RtcSession> {
        reconnectAttempts = 0
        sfuEventMonitor.stop()

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

        val result = call.joinRequest(
            getOptions(create, createOptions),
            locationResult.value,
            ring = ring,
            notify = notify,
        )

        if (result !is Success) {
            return result as Failure
        }

        try {
            createJoinRtcSession(result.value)
        } catch (e: Exception) {
            return Failure(Error.GenericError(e.message ?: "RtcSession error occurred."))
        }

        clientImpl.state.setActiveCall(call)
        monitorSession(result.value)
        return Success(value = session!!)
    }

    internal fun getOptions(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
    ): CreateCallOptions? {
        return createOptions ?: if (create) {
            CreateCallOptions()
        } else {
            null
        }
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
            val oldSessionStats = callStatsReporter.collectStats(session)
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
                replaceSession(joinResponse.value, reason)
            } else {
                logger.e {
                    "[rejoin] Failed to get a join response ${joinResponse.errorOrNull()}"
                }
                call.state._connection.value = RealtimeConnection.Reconnecting
            }
        }
    }
    internal fun monitorSession(result: JoinCallResponse) {
        callStatsReporter.startCallStatsReporting(
            session,
            result.statsOptions.reportingIntervalMs.toLong(),
        )
        sfuEventMonitor.start()
        iceConnectionMonitor.start()
        networkSubscriptionController.start()
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

    suspend fun createJoinRtcSession(result: JoinCallResponse) {
        session = createJoinRtcSessionInner(result)
        session?.let { call.state._connection.value = RealtimeConnection.Joined(it) }
        session?.connect()
    }

    fun createJoinRtcSessionInner(result: JoinCallResponse): RtcSession {
        return if (testInstanceProvider.rtcSessionCreator != null) {
            testInstanceProvider.rtcSessionCreator!!.invoke()
        } else {
            RtcSession(
                sessionId = this.sessionId,
                apiKey = clientImpl.apiKey,
                lifecycle = clientImpl.coordinatorConnectionModule.lifecycle,
                client = clientImpl,
                call = call,
                sfuUrl = result.credentials.server.url,
                sfuWsUrl = result.credentials.server.wsEndpoint,
                sfuToken = result.credentials.token,
                remoteIceServers = result.credentials.iceServers.map { it.toIceServer() },
                powerManager = powerManager,
            )
        }
    }

    fun createRejoinSession(joinResponse: JoinCallResponse): RtcSession {
        val cred = joinResponse.credentials
        return RtcSession(
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
    }

    fun createReconnectDetails(session: RtcSession, reason: String): ReconnectDetails {
        val (prevSessionId, subscriptionsInfo, publishingInfo) = session.currentSfuInfo()
        return ReconnectDetails(
            previous_session_id = prevSessionId,
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            announced_tracks = publishingInfo,
            subscriptions = subscriptionsInfo,
            reconnect_attempt = reconnectAttempts,
            reason = reason,
        )
    }

    suspend fun replaceSession(joinResponse: JoinCallResponse, reason: String) {
        // switch to the new SFU
        val cred = joinResponse.credentials
        val oldSession = this.session!!
        val oldSessionStats = callStatsReporter.collectStats(session)
        val currentOptions = this.session?.publisher?.currentOptions()
        logger.i { "Rejoin SFU ${oldSession?.sfuUrl} to ${cred.server.url}" }

        this.sessionId = UUID.randomUUID().toString()
        val (prevSessionId, _, _) = oldSession.currentSfuInfo()
        val reconnectDetails = createReconnectDetails(oldSession, reason)
        call.state.removeParticipant(prevSessionId)
        oldSession.prepareRejoin()
        try {
            this.session = createRejoinSession(joinResponse)
            this.session?.connect(reconnectDetails, currentOptions)
            this.session?.sfuTracer?.trace("rejoin", reason)
            oldSession.sendCallStats(oldSessionStats)
            oldSession.leaveWithReason("Rejoin :: $reason")
            oldSession.cleanup()
            monitorSession(joinResponse)
        } catch (ex: Exception) {
            logger.e(ex) {
                "[rejoin] Failed to join response with ex: ${ex.message}"
            }
            call.state._connection.value = RealtimeConnection.Failed(ex)
        }
    }

    private suspend fun schedule(key: String, block: suspend () -> Unit) {
        logger.d { "[schedule] #reconnect; no args" }
        streamSingleFlightProcessorImpl.run(key, block)
    }

    fun cleanup() {
        session?.cleanup()
        session = null
    }

    fun cleanupMonitor() {
        iceConnectionMonitor.stop()
        sfuEventMonitor.stop()
    }

    fun cleanupNetworkMonitoring() {
        networkSubscriptionController.stop()
    }
}
