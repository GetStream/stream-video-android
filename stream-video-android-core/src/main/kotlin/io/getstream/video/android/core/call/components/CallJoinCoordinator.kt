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

package io.getstream.video.android.core.call.components

import io.getstream.android.video.generated.models.RingCallRequest
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.flatMap
import io.getstream.video.android.core.BackendCause
import io.getstream.video.android.core.CallJoinInterceptor
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.CallAnalytics
import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.model.AnalyticsCallAbortReason
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectFailureCause
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.model.toIceServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import stream.video.sfu.models.WebsocketReconnectStrategy

/**
 * Drives the join flow for a call: permission checks, the bounded retry loop, the
 * underlying join request to the coordinator, and creation + connection of the [RtcSession].
 */
internal class CallJoinCoordinator(
    private val clientImpl: StreamVideoClient,
    private val state: CallState,
    private val callAnalytics: CallAnalytics,
    private val type: String,
    private val id: String,
    private val scope: CoroutineScope,
    private val sessionManager: CallSessionManager,
    private val sessionFactory: RtcSessionFactory,
    private val media: CallMediaManager,
    private val lifecycle: CallLifecycleManager,
    private val apiClient: CallApiClient,
    private val reconnector: CallReconnector,
    private val sessionMonitor: SessionMonitor,
    private val callRegistry: ClientCallRegistry,
    private val hasRequiredPermissions: () -> Boolean,
) {
    private val logger by taggedLogger("Call:JoinCoordinator:$type:$id")

    private fun isVideoEnabled(): Boolean = state.settings.value?.video?.enabled ?: false

    /**
     * Runs the public `Call.join()` flow and emits analytics in the following order:
     * 1. `JOIN_INITIATED`: `[io.getstream.video.android.core.analytics.call.observer.JoinAnalytics.onJoinFunctionStart]` reports that the public SDK join method was
     *    invoked.
     * 2. `MEDIA_DEVICE_PERMISSION`: [io.getstream.video.android.core.analytics.call.observer.MediaPermissionObserver.mediaPermissionStatus] reports the current camera and
     *    microphone permission state.
     * 3. `COORDINATOR_JOIN`: [joinInternal] calls [CallApiClient.joinRequest], which reports the
     *    stage as initiated. A successful response completes it successfully and sends `[io.getstream.video.android.core.analytics.call.observer.JoinAnalytics.onJoinSuccess]`. A permanent error or
     *    exhausted retry budget completes the active stage as failed through
     *    [io.getstream.video.android.core.analytics.call.observer.JoinAnalytics.onJoinRequestPermanentError] or [io.getstream.video.android.core.analytics.call.observer.JoinAnalytics.onJoinRequestRetryExhausted] respectively.
     *
     */
    suspend fun join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        hintHighScaleLivestreamPublisher: Boolean? = null,
        callJoinInterceptor: CallJoinInterceptor? = null,
    ): Result<RtcSession> {
        callAnalytics.joinAnalytics.onJoinFunctionStart()
        callAnalytics.mediaPermissionObserver.mediaPermissionStatus()
        logger.d {
            "[join] #ringing; #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }
        // Check android permissions and log a warning to make sure developers requested adequate permissions prior to using the call.
        if (!hasRequiredPermissions()) {
            logger.w {
                "\n[Call.join()] called without having the required permissions.\n" +
                    "This will work only if you have [runForegroundServiceForCalls = false] in the StreamVideoBuilder.\n" +
                    "The reason is that [Call.join()] will by default start an ongoing call foreground service,\n" +
                    "To start this service and send the appropriate audio/video tracks the permissions are required,\n" +
                    "otherwise the service will fail to start, resulting in a crash.\n" +
                    "You can re-define your permissions and their expected state by overriding the [permissionCheck] in [StreamVideoBuilder]\n"
            }
        }
        // if we are a guest user, make sure we wait for the token before running the join flow
        clientImpl.guestUserJob?.await()

        // Ensure factory is created with the current audioBitrateProfile before joining
        media.ensureFactoryMatchesAudioProfile()

        state.callJoinInterceptor = callJoinInterceptor

        // the join flow should retry up to 3 times
        // if the error is not permanent
        // and fail immediately on permanent errors
        state._connection.value = RealtimeConnection.InProgress
        var retryCount = 0

        var result: Result<RtcSession>

        lifecycle.resetLeaveGuard()
        while (retryCount < 3) {
            result = joinInternal(
                create,
                createOptions,
                ring,
                notify,
                hintHighScaleLivestreamPublisher,
                JoinAnalyticsModel(retryCount, JoinReason.FirstAttempt),
            )
            if (result is Success) {
                // we initialise the camera, mic and other according to local + backend settings
                // only when the call is joined to make sure we don't switch and override
                // the settings during a call.
                val settings = state.settings.value
                if (settings != null) {
                    media.updateMediaManagerFromSettings(settings)
                } else {
                    logger.w {
                        "[join] Call settings were null - this should never happen after a call" +
                            "is joined. MediaManager will not be initialised with server settings."
                    }
                }
                return result
            }
            if (result is Failure) {
                sessionManager.setActiveSession(null)
                logger.e { "Join failed with error $result" }
                if (isPermanentError(result.value)) {
                    state._connection.value = RealtimeConnection.Failed(result.value)
                    callAnalytics.joinAnalytics.onJoinRequestPermanentError(
                        retryCount,
                        AnalyticsCallAbortReason.SERVER_ERROR.name,
                        result.value.message,
                    )
                    return result
                } else {
                    retryCount += 1
                }
            }
            delay((retryCount - 1) * 1000L)
        }
        sessionManager.setActiveSession(null)
        val errorMessage = "Join failed after 3 retries"
        state._connection.value = RealtimeConnection.Failed(errorMessage)
        callAnalytics.joinAnalytics.onJoinRequestRetryExhausted(
            retryCount,
            AnalyticsCallAbortReason.RETRY_EXHAUSTED.name,
            errorMessage,
        )
        return Failure(value = Error.GenericError(errorMessage))
    }

    suspend fun joinAndRing(
        members: List<String>,
        createOptions: CreateCallOptions? = CreateCallOptions(members),
        video: Boolean = isVideoEnabled(),
        callJoinInterceptor: CallJoinInterceptor? = null,
    ): Result<RtcSession> {
        logger.d { "[joinAndRing] #ringing; #track; members: $members, video: $video" }
        state.toggleJoinAndRingProgress(true)
        return join(
            ring = false,
            createOptions = createOptions,
            callJoinInterceptor = callJoinInterceptor,
        ).flatMap { rtcSession ->
            logger.d { "[joinAndRing] Joined #ringing; #track; ring: $members" }
            apiClient.ring(RingCallRequest(isVideoEnabled(), members)).map {
                logger.d { "[joinAndRing] Ringed #ringing; #track; ring: $members" }
                callRegistry.markRinging()
                rtcSession
            }.onError {
                logger.e { "[joinAndRing] Ring failed #ringing; #track; error: $it" }
                state.toggleJoinAndRingProgress(false)
                lifecycle.leave(
                    CallLeaveReason.Backend(
                        BackendCause.RING_FAILED,
                        message = "ring-failed (${it.message})",
                    ),
                )
            }
        }
    }

    fun isPermanentError(error: Any): Boolean {
        if (error is Error.ThrowableError) {
            if (error.message.contains("Unable to resolve host")) {
                return false
            }
        }
        return true
    }

    suspend fun joinInternal(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        hintHighScaleLivestreamPublisher: Boolean? = null,
        joinAnalyticsModel: JoinAnalyticsModel,
    ): Result<RtcSession> {
        sessionManager.nonFastReconnectAttempts = 0
        sessionMonitor.cancelSfuObservers()

        if (sessionManager.session.value != null) {
            return Failure(Error.GenericError("Call $type:$id has already been joined"))
        }
        logger.d {
            "[joinInternal] #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }

        sessionManager.connectStartTime = System.currentTimeMillis()

        // step 1. call the join endpoint to get a list of SFUs
        val locationResult = clientImpl.getCachedLocation()
        if (locationResult !is Success) {
            return locationResult as Failure
        }
        sessionManager.location = locationResult.value

        val options = createOptions
            ?: if (create) {
                CreateCallOptions()
            } else {
                null
            }
        val result =
            apiClient.joinRequest(
                options,
                locationResult.value,
                ring = ring,
                notify = notify,
                hintHighScaleLivestreamPublisher = hintHighScaleLivestreamPublisher,
                joinAnalyticsModel = joinAnalyticsModel,
            )

        if (result !is Success) {
            return result as Failure
        }
        val sfuToken = result.value.credentials.token
        val sfuUrl = result.value.credentials.server.url
        val sfuWsUrl = result.value.credentials.server.wsEndpoint
        val sfuName = result.value.credentials.server.edgeName
        val iceServers = result.value.credentials.iceServers.map { it.toIceServer() }
        val localSession = sessionFactory.create(
            sessionId = sessionManager.sessionId,
            sessionCounter = 0,
            sfuUrl = sfuUrl,
            sfuWsUrl = sfuWsUrl,
            sfuToken = sfuToken,
            sfuName = sfuName,
            iceServers = iceServers,
        )
        sessionManager.setActiveSession(localSession)

        state._connection.value = RealtimeConnection.Joined(localSession)

        // This is the SFU ws connection
        val sfuConnectionResult = localSession.connectInternal()

        when (sfuConnectionResult) {
            is SfuConnectionResult.Success -> Unit
            is SfuConnectionResult.Failure -> {
                when (sfuConnectionResult.cause) {
                    SfuConnectFailureCause.SocketStateObservationTimeout -> {
                        // REJOIN (not FAST) on purpose: a connect timeout means the initial
                        // join never completed. There is no established SFU session or
                        // negotiated media path to resume, so a FAST resume would likely hit
                        // PARTICIPANT_NOT_FOUND and burn attempts before the loop escalates.
                        // A full REJOIN re-fetches credentials and starts a clean join, which
                        // is the only thing that can actually succeed here.
                        logger.w {
                            "[_join] SFU socket state observation timed out with no recovery started — triggering REJOIN"
                        }
                        scope.launch {
                            reconnector.reconnect(
                                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
                                "join-recoverable-connect-failure",
                            )
                        }
                    }

                    SfuConnectFailureCause.RecoverableSocketFailure -> {
                        logger.w { "[_join] Recoverable SFU socket failure — awaiting recovery outcome" }
                    }

                    SfuConnectFailureCause.TerminalSocketFailure -> {
                        logger.e {
                            "[_join] Got terminal error while connecting to SFU. Error : $sfuConnectionResult"
                        }
                        sendJoinErrorAnalytics(sfuConnectionResult)
                        return Failure(
                            Error.GenericError(
                                sfuConnectionResult.error.message ?: "RtcSession error occurred.",
                            ),
                        )
                    }
                }

                if (sfuConnectionResult.cause != SfuConnectFailureCause.TerminalSocketFailure) {
                    if (!didReconnectSucceed()) {
                        logger.e { "[_join] Could not recover. Error : $sfuConnectionResult" }
                        sendJoinErrorAnalytics(sfuConnectionResult)
                        return Failure(
                            Error.GenericError(
                                sfuConnectionResult.error.message ?: "SFU connection failed",
                            ),
                        )
                    }
                }
            }
        }
        val connectedSession = sessionManager.session.value
            ?: return Failure(Error.GenericError("RtcSession was cleared during connection to sfu"))
        callRegistry.markActive()
        // rejoin/migrate recovery swaps in a NEW session and already calls monitorSession()
        // with the recovered join response. fastReconnect recovery — and the normal success
        // path — keep the original session, which is not monitored anywhere else. Only
        // (re)establish monitoring when the session is unchanged, using the response that
        // still matches it, so we neither double-register nor monitor with a stale response.
        if (connectedSession === localSession) {
            sessionMonitor.monitorSession(result.value)
        }
        return Success(value = connectedSession)
    }

    /**
     * Reports the SFU WebSocket join failure to analytics. Only called from the join
     * flow ([joinInternal]) so that reconnect-driven [RtcSession.connectInternal] failures
     * are not counted as join errors. The retry count comes from the session's
     * [RtcSession.sfuWsRetryCount]; the failure reason and abort code come straight from
     * the [SfuConnectionResult.Failure] the connect attempt produced.
     */
    private fun sendJoinErrorAnalytics(failure: SfuConnectionResult.Failure) {
        callAnalytics.sfuAnalytics.onSfuWsCompleted(
            success = false,
            retryCount = sessionManager.session.value?.sfuWsRetryCount?.get() ?: 0,
            failureReason = failure.error.message,
            failureCode = (failure.abortReason ?: AnalyticsCallAbortReason.SFU_ERROR).name,
        )
    }

    /**
     * Suspends until the reconnect loop triggered by a recoverable connection failure
     * reaches a terminal state, returning `true` if the call recovered (became
     * [RealtimeConnection.Connected]) and `false` otherwise
     * ([RealtimeConnection.ReconnectingFailed] / [RealtimeConnection.Disconnected]).
     */
    private suspend fun didReconnectSucceed(): Boolean {
        val terminal = state.connection.first {
            it is RealtimeConnection.Connected ||
                it is RealtimeConnection.ReconnectingFailed ||
                it is RealtimeConnection.Disconnected
        }
        logger.d { "[_join] Reconnect after recoverable connection failure settled on $terminal" }
        return terminal is RealtimeConnection.Connected
    }
}
