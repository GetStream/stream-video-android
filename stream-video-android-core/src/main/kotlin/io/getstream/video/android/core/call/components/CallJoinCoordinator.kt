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

import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.RingCallRequest
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.flatMap
import io.getstream.video.android.core.BackendCause
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallJoinInterceptor
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.model.AnalyticsCallAbortReason
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.model.toIceServer
import kotlinx.coroutines.delay

/**
 * Drives the join flow for a [Call]: permission checks, the bounded retry loop, the
 * underlying join request to the coordinator, and creation + connection of the [RtcSession].
 */
internal class CallJoinCoordinator(
    private val call: Call,
) {
    private val logger by taggedLogger("Call:JoinCoordinator:${call.type}:${call.id}")

    private val clientImpl get() = call.clientImpl
    private val state get() = call.state
    private val session get() = call.session
    private val callAnalytics get() = call.callAnalytics
    private val type get() = call.type
    private val id get() = call.id

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
        val permissionPass =
            clientImpl.permissionCheck.checkAndroidPermissionsGroup(clientImpl.context, call)
        // Check android permissions and log a warning to make sure developers requested adequate permissions prior to using the call.
        if (!permissionPass.first) {
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
        call.ensureFactoryMatchesAudioProfile()

        state.callJoinInterceptor = callJoinInterceptor

        // the join flow should retry up to 3 times
        // if the error is not permanent
        // and fail immediately on permanent errors
        state._connection.value = RealtimeConnection.InProgress
        var retryCount = 0

        var result: Result<RtcSession>

        call.resetLeaveGuard()
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
                session.value = null
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
        session.value = null
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
        video: Boolean = call.isVideoEnabled(),
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
            call.ring(RingCallRequest(call.isVideoEnabled(), members)).map {
                logger.d { "[joinAndRing] Ringed #ringing; #track; ring: $members" }
                clientImpl.state._ringingCall.value = call
                rtcSession
            }.onError {
                logger.e { "[joinAndRing] Ring failed #ringing; #track; error: $it" }
                state.toggleJoinAndRingProgress(false)
                call.leave(
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
        call.nonFastReconnectAttempts = 0
        call.cancelSfuObservers()

        if (session.value != null) {
            return Failure(Error.GenericError("Call ${call.cid} has already been joined"))
        }
        logger.d {
            "[joinInternal] #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }

        call.connectStartTime = System.currentTimeMillis()

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
        val result =
            joinRequest(
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
        val localSession = if (Call.testInstanceProvider.rtcSessionCreator != null) {
            Call.testInstanceProvider.rtcSessionCreator!!.invoke()
        } else {
            RtcSession(
                sessionId = call.sessionId,
                apiKey = clientImpl.apiKey,
                lifecycle = clientImpl.coordinatorConnectionModule.lifecycle,
                client = call.client,
                call = call,
                sfuUrl = sfuUrl,
                sfuWsUrl = sfuWsUrl,
                sfuToken = sfuToken,
                sfuName = sfuName,
                remoteIceServers = iceServers,
                powerManager = call.powerManager,
                sfuAnalytics = callAnalytics.sfuAnalytics.apply {
                    sfuAnalyticsStateHolder.updateSfuId(
                        sfuName,
                    )
                },
            )
        }
        session.value = localSession

        session.value?.let {
            state._connection.value = RealtimeConnection.Joined(it)
        }

        when (val result = session.value?.connectInternal()) {
            is SfuConnectionResult.Connected -> Unit
            is SfuConnectionResult.Failed ->
                return Failure(
                    Error.GenericError(result.error.message ?: "RtcSession error occurred."),
                )
            null ->
                return Failure(Error.GenericError("RtcSession was null during connect"))
        }
        call.client.state.setActiveCall(call)
        call.monitorSession(result.value)
        return Success(value = session.value!!)
    }

    suspend fun joinRequest(
        create: CreateCallOptions? = null,
        location: String,
        migratingFrom: String? = null,
        migratingFromList: List<String>? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        hintHighScaleLivestreamPublisher: Boolean? = null,
        joinAnalyticsModel: JoinAnalyticsModel,
    ): Result<JoinCallResponse> {
        val migratingFromList =
            migratingFromList ?: call.getFailedSfuIdsSnapshot().takeIf { it.isNotEmpty() }
        callAnalytics.joinAnalytics.onJoinRequestStart(joinAnalyticsModel.joinReason)
        val result = clientImpl.joinCall(
            type, id,
            create = create != null,
            members = create?.memberRequestsFromIds(),
            custom = create?.custom,
            settingsOverride = create?.settings,
            startsAt = create?.startsAt,
            team = create?.team,
            ring = ring,
            notify = notify,
            location = location,
            migratingFrom = migratingFrom,
            migratingFromList = migratingFromList,
            hintHighScaleLivestreamPublisher = hintHighScaleLivestreamPublisher,
        )
        result.onSuccess {
            callAnalytics.joinAnalytics.onJoinRequestSuccess(
                joinAnalyticsModel,
                it.call.currentSessionId,
            )
            state.updateFromResponse(it)
        }
        return result
    }
}
