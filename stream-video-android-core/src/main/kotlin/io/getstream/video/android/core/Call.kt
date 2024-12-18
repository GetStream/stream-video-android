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

import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Stable
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.audio.InputAudioFilter
import io.getstream.video.android.core.call.utils.SoundInputProcessor
import io.getstream.video.android.core.call.video.VideoFilter
import io.getstream.video.android.core.call.video.YuvFrame
import io.getstream.video.android.core.events.GoAwayEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.model.toIceServer
import io.getstream.video.android.core.utils.RampValueUpAndDownHelper
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.toQueriedMembers
import io.getstream.video.android.model.User
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.openapitools.client.models.AcceptCallResponse
import org.openapitools.client.models.AudioSettingsResponse
import org.openapitools.client.models.BlockUserResponse
import org.openapitools.client.models.CallSettingsRequest
import org.openapitools.client.models.CallSettingsResponse
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.ListRecordingsResponse
import org.openapitools.client.models.ListTranscriptionsResponse
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.OwnCapability
import org.openapitools.client.models.PinResponse
import org.openapitools.client.models.RejectCallResponse
import org.openapitools.client.models.SendCallEventResponse
import org.openapitools.client.models.SendReactionResponse
import org.openapitools.client.models.StartTranscriptionResponse
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.StopTranscriptionResponse
import org.openapitools.client.models.TranscriptionSettingsResponse
import org.openapitools.client.models.UnpinResponse
import org.openapitools.client.models.UpdateCallMembersRequest
import org.openapitools.client.models.UpdateCallMembersResponse
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse
import org.openapitools.client.models.UpdateUserPermissionsResponse
import org.openapitools.client.models.VideoEvent
import org.openapitools.client.models.VideoSettingsResponse
import org.threeten.bp.OffsetDateTime
import org.webrtc.PeerConnection
import org.webrtc.RendererCommon
import org.webrtc.VideoSink
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.Collections
import java.util.UUID
import kotlin.coroutines.resume

/**
 * How long do we keep trying to make a full-reconnect (once the SFU signalling WS went down)
 */
const val sfuReconnectTimeoutMillis = 30_000

/**
 * The call class gives you access to all call level API calls
 *
 * @sample
 *
 * val call = client.call("default", "123")
 * val result = call.create() // update, get etc.
 * // join the call and get audio/video
 * val result = call.join()
 *
 */
@Stable
public class Call(
    internal val client: StreamVideo,
    val type: String,
    val id: String,
    val user: User,
) {
    internal var location: String? = null
    private var subscriptions = Collections.synchronizedSet(mutableSetOf<EventSubscription>())

    internal var reconnectAttepmts = 0
    internal val clientImpl = client as StreamVideoClient

    private val logger by taggedLogger("Call:$type:$id")
    private val supervisorJob = SupervisorJob()
    private var callStatsReportingJob: Job? = null

    private val scope = CoroutineScope(clientImpl.scope.coroutineContext + supervisorJob)

    /** The call state contains all state such as the participant list, reactions etc */
    val state = CallState(client, this, user, scope)

    private val network by lazy { clientImpl.coordinatorConnectionModule.networkStateProvider }

    /** Camera gives you access to the local camera */
    val camera by lazy(LazyThreadSafetyMode.PUBLICATION) { mediaManager.camera }
    val microphone by lazy(LazyThreadSafetyMode.PUBLICATION) { mediaManager.microphone }
    val speaker by lazy(LazyThreadSafetyMode.PUBLICATION) { mediaManager.speaker }
    val screenShare by lazy(LazyThreadSafetyMode.PUBLICATION) { mediaManager.screenShare }

    /** The cid is type:id */
    val cid = "$type:$id"

    /**
     * Set a custom [VideoFilter] that will be applied to the video stream coming from your device.
     */
    var videoFilter: VideoFilter? = null

    /**
     * Set a custom [InputAudioFilter] that will be applied to the audio stream recorded on your device.
     */
    var audioFilter: InputAudioFilter? = null

    // val monitor = CallHealthMonitor(this, scope, onIceRecoveryFailed)

    private val soundInputProcessor = SoundInputProcessor(thresholdCrossedCallback = {
        if (!microphone.isEnabled.value) {
            state.markSpeakingAsMuted()
        }
    })
    private val audioLevelOutputHelper = RampValueUpAndDownHelper()

    /**
     * This returns the local microphone volume level. The audio volume is a linear
     * value between 0 (no sound) and 1 (maximum volume). This is not a raw output -
     * it is a smoothed-out volume level that gradually goes to the highest measured level
     * and will then gradually over 250ms return back to 0 or next measured value. This value
     * can be used directly in your UI for displaying a volume/speaking indicator for the local
     * participant.
     * Note: Doesn't return any values until the session is established!
     */
    val localMicrophoneAudioLevel: StateFlow<Float> = audioLevelOutputHelper.currentLevel

    /**
     * Contains stats events for observation.
     */
    @InternalStreamVideoApi
    val statsReport: MutableStateFlow<CallStatsReport?> = MutableStateFlow(null)

    /**
     * Contains stats history.
     */
    val statLatencyHistory: MutableStateFlow<List<Int>> = MutableStateFlow(listOf(0, 0, 0))

    /**
     * Time (in millis) when the full reconnection flow started. Will be null again once
     * the reconnection flow ends (success or failure)
     */
    private var sfuSocketReconnectionTime: Long? = null

    /**
     * Call has been left and the object is cleaned up and destroyed.
     */
    private var isDestroyed = false

    /** Session handles all real time communication for video and audio */
    internal var session: RtcSession? = null
    var sessionId = UUID.randomUUID().toString()

    internal val mediaManager by lazy {
        if (testInstanceProvider.mediaManagerCreator != null) {
            testInstanceProvider.mediaManagerCreator!!.invoke()
        } else {
            MediaManagerImpl(
                clientImpl.context,
                this,
                scope,
                clientImpl.peerConnectionFactory.eglBase.eglBaseContext,
                clientImpl.callServiceConfig.audioUsage,
            )
        }
    }

    private val listener = object : NetworkStateProvider.NetworkStateListener {
        override suspend fun onConnected() {
            leaveTimeoutAfterDisconnect?.cancel()
            logger.d { "[onConnected] no args" }
            val elapsedTimeMils = System.currentTimeMillis() - lastDisconnect
            if (lastDisconnect > 0 && elapsedTimeMils < reconnectDeadlineMils) {
                logger.d {
                    "[onConnected] Reconnecting (fast) time since last disconnect is ${elapsedTimeMils / 1000} seconds. Deadline is ${reconnectDeadlineMils / 1000} seconds"
                }
                fastReconnect()
            } else {
                logger.d {
                    "[onConnected] Reconnecting (full) time since last disconnect is ${elapsedTimeMils / 1000} seconds. Deadline is ${reconnectDeadlineMils / 1000} seconds"
                }
                rejoin()
            }
        }

        override suspend fun onDisconnected() {
            state._connection.value = RealtimeConnection.Reconnecting
            lastDisconnect = System.currentTimeMillis()
            leaveTimeoutAfterDisconnect = scope.launch {
                delay(clientImpl.leaveAfterDisconnectSeconds * 1000)
                logger.d {
                    "[onDisconnected] Leaving after being disconnected for ${clientImpl.leaveAfterDisconnectSeconds}"
                }
                leave()
            }
            logger.d { "[onDisconnected] at $lastDisconnect" }
        }
    }

    private var leaveTimeoutAfterDisconnect: Job? = null
    private var lastDisconnect = 0L
    private var reconnectDeadlineMils: Int = 10_000

    private var monitorPublisherPCStateJob: Job? = null
    private var monitorSubscriberPCStateJob: Job? = null
    private var monitorPublisherStateJob: Job? = null
    private var monitorSubscriberStateJob: Job? = null
    private var sfuListener: Job? = null
    private var sfuEvents: Job? = null

    init {
        scope.launch {
            soundInputProcessor.currentAudioLevel.collect {
                audioLevelOutputHelper.rampToValue(it)
            }
        }
    }

    /** Basic crud operations */
    suspend fun get(): Result<GetCallResponse> {
        val response = clientImpl.getCall(type, id)
        response.onSuccess {
            state.updateFromResponse(it)
        }
        return response
    }

    /** Create a call. You can create a call client side, many apps prefer to do this server side though */
    suspend fun create(
        memberIds: List<String>? = null,
        members: List<MemberRequest>? = null,
        custom: Map<String, Any>? = null,
        settings: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<GetOrCreateCallResponse> {
        val response = if (members != null) {
            clientImpl.getOrCreateCallFullMembers(
                type = type,
                id = id,
                members = members,
                custom = custom,
                settingsOverride = settings,
                startsAt = startsAt,
                team = team,
                ring = ring,
                notify = notify,
            )
        } else {
            clientImpl.getOrCreateCall(
                type = type,
                id = id,
                memberIds = memberIds,
                custom = custom,
                settingsOverride = settings,
                startsAt = startsAt,
                team = team,
                ring = ring,
                notify = notify,
            )
        }

        response.onSuccess {
            state.updateFromResponse(it)
            if (ring) {
                client.state.addRingingCall(this, RingingState.Outgoing())
            }
        }
        return response
    }

    /** Update a call */
    suspend fun update(
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
    ): Result<UpdateCallResponse> {
        val request = UpdateCallRequest(
            custom = custom,
            settingsOverride = settingsOverride,
            startsAt = startsAt,
        )
        val response = clientImpl.updateCall(type, id, request)
        response.onSuccess {
            state.updateFromResponse(it)
        }
        return response
    }

    suspend fun join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<RtcSession> {
        logger.d {
            "[join] #ringing; #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }
        val permissionPass =
            clientImpl.permissionCheck.checkAndroidPermissions(clientImpl.context, this)
        // Check android permissions and log a warning to make sure developers requested adequate permissions prior to using the call.
        if (!permissionPass) {
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
        // the join flow should retry up to 3 times
        // if the error is not permanent
        // and fail immediately on permanent errors
        state._connection.value = RealtimeConnection.InProgress
        var retryCount = 0

        var result: Result<RtcSession>

        while (retryCount < 3) {
            result = _join(create, createOptions, ring, notify)
            if (result is Success) {
                // we initialise the camera, mic and other according to local + backend settings
                // only when the call is joined to make sure we don't switch and override
                // the settings during a call.
                val settings = state.settings.value
                if (settings != null) {
                    updateMediaManagerFromSettings(settings)
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
                    state._connection.value = RealtimeConnection.Failed(result.value)
                    return result
                } else {
                    retryCount += 1
                }
            }
            delay(retryCount - 1 * 1000L)
        }
        return Failure(value = Error.GenericError("Join failed after 3 retries"))
    }

    internal fun isPermanentError(error: Any): Boolean {
        return true
    }

    internal suspend fun _join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<RtcSession> {
        reconnectAttepmts = 0
        sfuEvents?.cancel()
        sfuListener?.cancel()

        if (session != null) {
            throw IllegalStateException(
                "Call $cid has already been joined. Please use call.leave before joining it again",
            )
        }
        logger.d {
            "[joinInternal] #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }

        // step 1. call the join endpoint to get a list of SFUs

        val locationResult = clientImpl.getCachedLocation()
        if (locationResult !is Success) {
            return locationResult as Failure
        }
        location = locationResult.value

        val options = createOptions
            ?: if (create) {
                CreateCallOptions()
            } else {
                null
            }
        val result = joinRequest(options, locationResult.value, ring = ring, notify = notify)

        if (result !is Success) {
            return result as Failure
        }
        val sfuToken = result.value.credentials.token
        val sfuUrl = result.value.credentials.server.url
        val sfuWsUrl = result.value.credentials.server.wsEndpoint
        val iceServers = result.value.credentials.iceServers.map { it.toIceServer() }

        session = if (testInstanceProvider.rtcSessionCreator != null) {
            testInstanceProvider.rtcSessionCreator!!.invoke()
        } else {
            RtcSession(
                sessionId = this.sessionId,
                apiKey = clientImpl.apiKey,
                lifecycle = clientImpl.coordinatorConnectionModule.lifecycle,
                client = client,
                call = this,
                sfuUrl = sfuUrl,
                sfuWsUrl = sfuWsUrl,
                sfuToken = sfuToken,
                remoteIceServers = iceServers,
            )
        }

        session?.let {
            state._connection.value = RealtimeConnection.Joined(it)
        }

        try {
            session?.connect()
        } catch (e: Exception) {
            return Failure(Error.GenericError(e.message ?: "RtcSession error occurred."))
        }
        client.state.setActiveCall(this)
        monitorSession(result.value)
        return Success(value = session!!)
    }

    private suspend fun Call.monitorSession(result: JoinCallResponse) {
        sfuEvents?.cancel()
        sfuListener?.cancel()
        startCallStatsReporting(result.statsOptions.reportingIntervalMs.toLong())
        // listen to Signal WS
        sfuEvents = scope.launch {
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
        monitorPublisherPCStateJob = scope.launch {
            session?.publisher?.iceState?.collect {
                when (it) {
                    PeerConnection.IceConnectionState.FAILED, PeerConnection.IceConnectionState.DISCONNECTED -> {
                        session?.publisher?.connection?.restartIce()
                    }

                    else -> {
                        logger.d { "[monitorConnectionState] Ice connection state is $it" }
                    }
                }
            }
        }

        monitorSubscriberPCStateJob?.cancel()
        monitorSubscriberPCStateJob = scope.launch {
            session?.subscriber?.iceState?.collect {
                when (it) {
                    PeerConnection.IceConnectionState.FAILED, PeerConnection.IceConnectionState.DISCONNECTED -> {
                        session?.requestSubscriberIceRestart()
                    }

                    else -> {
                        logger.d { "[monitorConnectionState] Ice connection state is $it" }
                    }
                }
            }
        }
        monitorPublisherStateJob?.cancel()
        monitorPublisherStateJob = scope.launch {
            session?.subscriber?.state?.collect {
                when (it) {
                    PeerConnection.PeerConnectionState.FAILED, PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        fastReconnect()
                    }

                    else -> {
                        logger.d { "[monitorConnectionState] Ice connection state is $it" }
                    }
                }
            }
        }

        monitorSubscriberStateJob?.cancel()
        monitorSubscriberStateJob = scope.launch {
            session?.subscriber?.state?.collect {
                when (it) {
                    PeerConnection.PeerConnectionState.FAILED, PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        fastReconnect()
                    }

                    else -> {
                        logger.d { "[monitorConnectionState] Ice connection state is $it" }
                    }
                }
            }
        }
        network.subscribe(listener)
    }

    private suspend fun startCallStatsReporting(reportingIntervalMs: Long = 10_000) {
        callStatsReportingJob?.cancel()
        callStatsReportingJob = scope.launch {
            // Wait a bit before we start capturing stats
            delay(reportingIntervalMs)

            while (isActive) {
                delay(reportingIntervalMs)

                val publisherStats = session?.getPublisherStats()
                val subscriberStats = session?.getSubscriberStats()
                state.stats.updateFromRTCStats(publisherStats, isPublisher = true)
                state.stats.updateFromRTCStats(subscriberStats, isPublisher = false)
                state.stats.updateLocalStats()
                val local = state.stats._local.value

                val report = CallStatsReport(
                    publisher = publisherStats,
                    subscriber = subscriberStats,
                    local = local,
                    stateStats = state.stats,
                )
                statsReport.value = report
                statLatencyHistory.value += report.stateStats.publisher.latency.value
                if (statLatencyHistory.value.size > 20) {
                    statLatencyHistory.value = statLatencyHistory.value.takeLast(20)
                }

                session?.sendCallStats(report)
            }
        }
    }

    /**
     * Fast reconnect to the same SFU with the same participant session.
     */
    suspend fun fastReconnect() = schedule {
        logger.d { "[fastReconnect] Reconnecting" }
        session?.prepareReconnect()
        this@Call.state._connection.value = RealtimeConnection.Reconnecting
        if (session != null) {
            val session = session!!
            val (prevSessionId, subscriptionsInfo, publishingInfo) = session.currentSfuInfo()
            val reconnectDetails = ReconnectDetails(
                previous_session_id = prevSessionId,
                strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                announced_tracks = publishingInfo,
                subscriptions = subscriptionsInfo,
                reconnect_attempt = reconnectAttepmts,
            )
            session.fastReconnect(reconnectDetails)
        } else {
            logger.e { "[reconnect] Disconnecting" }
            this@Call.state._connection.value = RealtimeConnection.Disconnected
        }
    }

    /**
     * Rejoin a call. Creates a new session and joins as a new participant.
     */
    suspend fun rejoin() = schedule {
        logger.d { "[rejoin] Rejoining" }
        reconnectAttepmts++
        state._connection.value = RealtimeConnection.Reconnecting
        location?.let {
            val joinResponse = joinRequest(location = it)
            if (joinResponse is Success) {
                // switch to the new SFU
                val cred = joinResponse.value.credentials
                val session = this.session!!
                logger.i { "Rejoin SFU ${session?.sfuUrl} to ${cred.server.url}" }

                this.sessionId = UUID.randomUUID().toString()
                val (prevSessionId, subscriptionsInfo, publishingInfo) = session.currentSfuInfo()
                val reconnectDetails = ReconnectDetails(
                    previous_session_id = prevSessionId,
                    strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
                    announced_tracks = publishingInfo,
                    subscriptions = subscriptionsInfo,
                    reconnect_attempt = reconnectAttepmts,
                )
                this.state.removeParticipant(prevSessionId)
                session.prepareRejoin()
                this.session = RtcSession(
                    clientImpl,
                    this,
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
                this.session?.connect(reconnectDetails)
                session.cleanup()
                monitorSession(joinResponse.value)
            } else {
                logger.e {
                    "[rejoin] Failed to get a join response ${joinResponse.errorOrNull()}"
                }
                state._connection.value = RealtimeConnection.Reconnecting
            }
        }
    }

    /**
     * Migrate to another SFU.
     */
    suspend fun migrate() = schedule {
        logger.d { "[migrate] Migrating" }
        state._connection.value = RealtimeConnection.Migrating
        location?.let {
            val joinResponse = joinRequest(location = it)
            if (joinResponse is Success) {
                // switch to the new SFU
                val cred = joinResponse.value.credentials
                val session = this.session!!
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
                    reconnect_attempt = reconnectAttepmts,
                )
                session.prepareRejoin()
                val newSession = RtcSession(
                    clientImpl,
                    this,
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
                this.session?.connect(reconnectDetails)
                monitorSession(joinResponse.value)
                oldSession?.leaveWithReason("migrating")
                oldSession?.cleanup()
            } else {
                logger.e {
                    "[switchSfu] Failed to get a join response during " +
                        "migration - falling back to reconnect. Error ${joinResponse.errorOrNull()}"
                }
                state._connection.value = RealtimeConnection.Reconnecting
            }
        }
    }

    private var reconnectJob: Job? = null

    private suspend fun schedule(block: suspend () -> Unit) = synchronized(this) {
        logger.d { "[schedule] #reconnect; no args" }
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            block()
        }
    }

    /** Leave the call, but don't end it for other users */
    fun leave() {
        logger.d { "[leave] #ringing; no args" }
        leave(disconnectionReason = null)
    }

    private fun leave(disconnectionReason: Throwable?) = safeCall {
        session?.leaveWithReason(disconnectionReason?.message ?: "user")
        session?.cleanup()
        leaveTimeoutAfterDisconnect?.cancel()
        network.unsubscribe(listener)
        sfuListener?.cancel()
        sfuEvents?.cancel()
        state._connection.value = RealtimeConnection.Disconnected
        logger.v { "[leave] #ringing; disconnectionReason: $disconnectionReason" }
        if (isDestroyed) {
            logger.w { "[leave] #ringing; Call already destroyed, ignoring" }
            return
        }
        isDestroyed = true

        sfuSocketReconnectionTime = null
        stopScreenSharing()
        client.state.removeActiveCall() // Will also stop CallService
        client.state.removeRingingCall()
        (client as StreamVideoClient).onCallCleanUp(this)
        camera.disable()
        microphone.disable()
        cleanup()
    }

    /** ends the call for yourself as well as other users */
    suspend fun end(): Result<Unit> {
        // end the call for everyone
        val result = clientImpl.endCall(type, id)
        // cleanup
        leave()
        return result
    }

    suspend fun pinForEveryone(sessionId: String, userId: String): Result<PinResponse> {
        return clientImpl.pinForEveryone(type, id, sessionId, userId)
    }

    suspend fun unpinForEveryone(sessionId: String, userId: String): Result<UnpinResponse> {
        return clientImpl.unpinForEveryone(type, id, sessionId, userId)
    }

    suspend fun sendReaction(
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null,
    ): Result<SendReactionResponse> {
        return clientImpl.sendReaction(this.type, id, type, emoji, custom)
    }

    suspend fun queryMembers(
        filter: Map<String, Any>,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        limit: Int = 25,
        prev: String? = null,
        next: String? = null,
    ): Result<QueriedMembers> {
        return clientImpl.queryMembersInternal(
            type = type,
            id = id,
            filter = filter,
            sort = sort,
            prev = prev,
            next = next,
            limit = limit,
        ).onSuccess { state.updateFromResponse(it) }.map { it.toQueriedMembers() }
    }

    suspend fun muteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            muteAllUsers = true,
            audio = audio,
            video = video,
            screenShare = screenShare,
        )
        return clientImpl.muteUsers(type, id, request)
    }

    fun setVisibility(sessionId: String, trackType: TrackType, visible: Boolean) {
        logger.i {
            "[setVisibility] #track; #sfu; sessionId: $sessionId, trackType: $trackType, visible: $visible"
        }
        session?.updateTrackDimensions(sessionId, trackType, visible)
    }

    fun handleEvent(event: VideoEvent) {
        logger.v { "[call handleEvent] #sfu; event.type: ${event.getEventType()}" }

        when (event) {
            is GoAwayEvent ->
                scope.launch {
                    migrate()
                }
        }
    }

    // TODO: review this
    /**
     * Perhaps it would be nicer to have an interface. Any UI elements that renders video should implement it
     *
     * And call a callback for
     * - visible/hidden
     * - resolution changes
     */
    public fun initRenderer(
        videoRenderer: VideoTextureViewRenderer,
        sessionId: String,
        trackType: TrackType,
        onRendered: (VideoTextureViewRenderer) -> Unit = {},
    ) {
        logger.d { "[initRenderer] #sfu; #track; sessionId: $sessionId" }

        // Note this comes from peerConnectionFactory.eglBase
        videoRenderer.init(
            clientImpl.peerConnectionFactory.eglBase.eglBaseContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    val width = videoRenderer.measuredWidth
                    val height = videoRenderer.measuredHeight
                    logger.i {
                        "[initRenderer.onFirstFrameRendered] #sfu; #track; " +
                            "trackType: $trackType, dimension: ($width - $height), " +
                            "sessionId: $sessionId"
                    }
                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        session?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(width, height),
                        )
                    }
                    onRendered(videoRenderer)
                }

                override fun onFrameResolutionChanged(
                    videoWidth: Int,
                    videoHeight: Int,
                    rotation: Int,
                ) {
                    val width = videoRenderer.measuredWidth
                    val height = videoRenderer.measuredHeight
                    logger.v {
                        "[initRenderer.onFrameResolutionChanged] #sfu; #track; " +
                            "trackType: $trackType, " +
                            "dimension1: ($width - $height), " +
                            "dimension2: ($videoWidth - $videoHeight), " +
                            "sessionId: $sessionId"
                    }

                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        session?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(videoWidth, videoHeight),
                        )
                    }
                }
            },
        )
    }

    suspend fun goLive(
        startHls: Boolean = false,
        startRecording: Boolean = false,
        startTranscription: Boolean = false,
    ): Result<GoLiveResponse> {
        val result = clientImpl.goLive(
            type = type,
            id = id,
            startHls = startHls,
            startRecording = startRecording,
            startTranscription = startTranscription,
        )
        result.onSuccess { state.updateFromResponse(it) }

        return result
    }

    suspend fun stopLive(): Result<StopLiveResponse> {
        val result = clientImpl.stopLive(type, id)
        result.onSuccess { state.updateFromResponse(it) }
        return result
    }

    suspend fun sendCustomEvent(data: Map<String, Any>): Result<SendCallEventResponse> {
        return clientImpl.sendCustomEvent(this.type, this.id, data)
    }

    /** Permissions */
    suspend fun requestPermissions(vararg permission: String): Result<Unit> {
        return clientImpl.requestPermissions(type, id, permission.toList())
    }

    suspend fun startRecording(): Result<Any> {
        return clientImpl.startRecording(type, id)
    }

    suspend fun stopRecording(): Result<Any> {
        return clientImpl.stopRecording(type, id)
    }

    /**
     * User needs to have [OwnCapability.Screenshare] capability in order to start screen
     * sharing.
     *
     * @param mediaProjectionPermissionResultData - intent data returned from the
     * activity result after asking for screen sharing permission by launching
     * MediaProjectionManager.createScreenCaptureIntent().
     * See https://developer.android.com/guide/topics/large-screens/media-projection#recommended_approach
     */
    fun startScreenSharing(mediaProjectionPermissionResultData: Intent) {
        if (state.ownCapabilities.value.contains(OwnCapability.Screenshare)) {
            session?.setScreenShareTrack()
            screenShare.enable(mediaProjectionPermissionResultData)
        } else {
            logger.w { "Can't start screen sharing - user doesn't have wnCapability.Screenshare permission" }
        }
    }

    fun stopScreenSharing() {
        screenShare.disable(fromUser = true)
    }

    suspend fun startHLS(): Result<Any> {
        return clientImpl.startBroadcasting(type, id)
            .onSuccess {
                state.updateFromResponse(it)
            }
    }

    suspend fun stopHLS(): Result<Any> {
        return clientImpl.stopBroadcasting(type, id)
    }

    public fun subscribeFor(
        vararg eventTypes: Class<out VideoEvent>,
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription = synchronized(subscriptions) {
        val filter = { event: VideoEvent ->
            eventTypes.any { type -> type.isInstance(event) }
        }
        val sub = EventSubscription(listener, filter)
        subscriptions.add(sub)
        return sub
    }

    public fun subscribe(
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription = synchronized(subscriptions) {
        val sub = EventSubscription(listener)
        subscriptions.add(sub)
        return sub
    }

    public suspend fun blockUser(userId: String): Result<BlockUserResponse> {
        return clientImpl.blockUser(type, id, userId)
    }

    // TODO: add removeMember (single)

    public suspend fun removeMembers(userIds: List<String>): Result<UpdateCallMembersResponse> {
        val request = UpdateCallMembersRequest(removeMembers = userIds)
        return clientImpl.updateMembers(type, id, request)
    }

    public suspend fun grantPermissions(
        userId: String,
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse> {
        val request = UpdateUserPermissionsData(
            userId = userId,
            grantedPermissions = permissions,
        )
        return clientImpl.updateUserPermissions(type, id, request)
    }

    public suspend fun revokePermissions(
        userId: String,
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse> {
        val request = UpdateUserPermissionsData(
            userId = userId,
            revokedPermissions = permissions,
        )
        return clientImpl.updateUserPermissions(type, id, request)
    }

    public suspend fun updateMembers(memberRequests: List<MemberRequest>): Result<UpdateCallMembersResponse> {
        val request = UpdateCallMembersRequest(updateMembers = memberRequests)
        return clientImpl.updateMembers(type, id, request)
    }

    fun fireEvent(event: VideoEvent) = synchronized(subscriptions) {
        subscriptions.forEach { sub ->
            if (!sub.isDisposed) {
                // subs without filters should always fire
                if (sub.filter == null) {
                    sub.listener.onEvent(event)
                }

                // if there is a filter, check it and fire if it matches
                sub.filter?.let {
                    if (it.invoke(event)) {
                        sub.listener.onEvent(event)
                    }
                }
            }
        }
    }

    private fun updateMediaManagerFromSettings(callSettings: CallSettingsResponse) {
        // Speaker
        if (speaker.status.value is DeviceStatus.NotSelected) {
            val enableSpeaker =
                if (callSettings.video.cameraDefaultOn || camera.status.value is DeviceStatus.Enabled) {
                    // if camera is enabled then enable speaker. Eventually this should
                    // be a new audio.defaultDevice setting returned from backend
                    true
                } else {
                    callSettings.audio.defaultDevice == AudioSettingsResponse.DefaultDevice.Speaker ||
                        callSettings.audio.speakerDefaultOn
                }

            speaker.setEnabled(
                enabled = enableSpeaker,
            )
        }

        // Camera
        if (camera.status.value is DeviceStatus.NotSelected) {
            val defaultDirection =
                if (callSettings.video.cameraFacing == VideoSettingsResponse.CameraFacing.Front) {
                    CameraDirection.Front
                } else {
                    CameraDirection.Back
                }
            camera.setDirection(defaultDirection)
            camera.setEnabled(callSettings.video.cameraDefaultOn)
        }

        // Mic
        if (microphone.status.value == DeviceStatus.NotSelected) {
            val enabled = callSettings.audio.micDefaultOn
            microphone.setEnabled(enabled)
        }
    }

    /**
     * List the recordings for this call.
     *
     * @param sessionId - if session ID is supplied, only recordings for that session will be loaded.
     */
    suspend fun listRecordings(sessionId: String? = null): Result<ListRecordingsResponse> {
        return clientImpl.listRecordings(type, id, sessionId)
    }

    suspend fun muteUser(
        userId: String,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            users = listOf(userId),
            muteAllUsers = false,
            audio = audio,
            video = video,
            screenShare = screenShare,
        )
        return clientImpl.muteUsers(type, id, request)
    }

    suspend fun muteUsers(
        userIds: List<String>,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            users = userIds,
            muteAllUsers = false,
            audio = audio,
            video = video,
            screenShare = screenShare,
        )
        return clientImpl.muteUsers(type, id, request)
    }

    @VisibleForTesting
    internal suspend fun joinRequest(
        create: CreateCallOptions? = null,
        location: String,
        migratingFrom: String? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<JoinCallResponse> {
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
        )
        result.onSuccess {
            state.updateFromResponse(it)
        }
        return result
    }

    fun cleanup() {
        // monitor.stop()
        session?.cleanup()
        supervisorJob.cancel()
        callStatsReportingJob?.cancel()
        mediaManager.cleanup()
        session = null
    }

    suspend fun ring(): Result<GetCallResponse> {
        logger.d { "[ring] #ringing; no args" }
        return clientImpl.ring(type, id)
    }

    suspend fun notify(): Result<GetCallResponse> {
        logger.d { "[notify] #ringing; no args" }
        return clientImpl.notify(type, id)
    }

    suspend fun accept(): Result<AcceptCallResponse> {
        logger.d { "[accept] #ringing; no args" }
        state.acceptedOnThisDevice = true

        clientImpl.state.removeRingingCall()
        clientImpl.state.maybeStopForegroundService()
        return clientImpl.accept(type, id)
    }

    suspend fun reject(reason: RejectReason? = null): Result<RejectCallResponse> {
        logger.d { "[reject] #ringing; rejectReason: $reason" }
        return clientImpl.reject(type, id, reason)
    }

    fun processAudioSample(audioSample: AudioSamples) {
        soundInputProcessor.processSoundInput(audioSample.data)
    }

    fun collectUserFeedback(
        rating: Int,
        reason: String? = null,
        custom: Map<String, Any>? = null,
    ) {
        scope.launch {
            clientImpl.collectFeedback(
                callType = type,
                id = id,
                sessionId = sessionId,
                rating = rating,
                reason = reason,
                custom = custom,
            )
        }
    }

    suspend fun takeScreenshot(track: VideoTrack): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            var screenshotSink: VideoSink? = null
            screenshotSink = VideoSink {
                // make sure we stop after first frame is delivered
                if (!continuation.isActive) {
                    return@VideoSink
                }
                it.retain()
                val bitmap = YuvFrame.bitmapFromVideoFrame(it)
                it.release()

                // This has to be launched asynchronously - removing the sink on the
                // same thread as the videoframe is delivered will lead to a deadlock
                // (needs investigation why)
                scope.launch {
                    track.video.removeSink(screenshotSink)
                }
                continuation.resume(bitmap)
            }

            track.video.addSink(screenshotSink)
        }
    }

    fun isPinnedParticipant(sessionId: String): Boolean =
        state.pinnedParticipants.value.containsKey(
            sessionId,
        )

    fun isServerPin(sessionId: String): Boolean = state._serverPins.value.containsKey(sessionId)

    fun isLocalPin(sessionId: String): Boolean = state._localPins.value.containsKey(sessionId)

    fun hasCapability(vararg capability: OwnCapability): Boolean {
        val elements = capability.toList()
        return state.ownCapabilities.value.containsAll(elements)
    }

    fun isVideoEnabled(): Boolean {
        return state.settings.value?.video?.enabled ?: false
    }

    fun isAudioProcessingEnabled(): Boolean {
        return clientImpl.isAudioProcessingEnabled()
    }

    fun setAudioProcessingEnabled(enabled: Boolean) {
        return clientImpl.setAudioProcessingEnabled(enabled)
    }

    fun toggleAudioProcessing(): Boolean {
        return clientImpl.toggleAudioProcessing()
    }

    suspend fun startTranscription(): Result<StartTranscriptionResponse> {
        return clientImpl.startTranscription(type, id)
    }

    suspend fun stopTranscription(): Result<StopTranscriptionResponse> {
        return clientImpl.stopTranscription(type, id)
    }

    suspend fun listTranscription(): Result<ListTranscriptionsResponse> {
        return clientImpl.listTranscription(type, id)
    }

    @InternalStreamVideoApi
    public val debug = Debug(this)

    @InternalStreamVideoApi
    public class Debug(val call: Call) {

        public fun rejoin() {
            call.scope.launch {
                call.rejoin()
            }
        }

        public fun restartSubscriberIce() {
            call.session?.subscriber?.connection?.restartIce()
        }

        public fun restartPublisherIce() {
            call.session?.publisher?.connection?.restartIce()
        }

        fun migrate() {
            call.scope.launch {
                call.migrate()
            }
        }

        fun fastReconnect() {
            call.scope.launch {
                call.fastReconnect()
            }
        }
    }

    /**
     * I need to do it in active session!! not before the session starts
     * So, I am using [io.getstream.video.android.core.CallState.connection] == [io.getstream.video.android.core.RealtimeConnection.Connected]
     */

    private fun observeTranscription() {
        fun isInActiveSession(callState: CallState): Boolean {
            return callState.connection.value == RealtimeConnection.Connected
        }

        scope.launch {
            state
                .settings
                .filter { isInActiveSession(state) }
                .map { it?.transcription } // Safely map to the `transcription` field
                .distinctUntilChanged() // Prevent duplicate emissions
                .collect { transcription ->
                    executeTranscriptionApis(transcription)
                }
        }
    }

    private suspend fun executeTranscriptionApis(transcriptionSettingsResponse: TranscriptionSettingsResponse?) {
        val mode = transcriptionSettingsResponse?.mode
        if (mode == TranscriptionSettingsResponse.Mode.Disabled && state.transcribing.value) {
            stopTranscription()
            logger.d { "TranscriptionSettings updated with mode:$mode. Will deactivate transcriptions." }
        } else if (mode == TranscriptionSettingsResponse.Mode.AutoOn && !state.transcribing.value) {
            startTranscription()
            logger.d { "TranscriptionSettings updated with mode:$mode. Will activate transcriptions." }
        } else {
            logger.d { "TranscriptionSettings updated with mode:$mode. No action required." }
        }
    }

    companion object {

        internal var testInstanceProvider = TestInstanceProvider()

        internal class TestInstanceProvider {
            var mediaManagerCreator: (() -> MediaManagerImpl)? = null
            var rtcSessionCreator: (() -> RtcSession)? = null
        }
    }
}

public data class CreateCallOptions(
    val memberIds: List<String>? = null,
    val members: List<MemberRequest>? = null,
    val custom: Map<String, Any>? = null,
    val settings: CallSettingsRequest? = null,
    val startsAt: OffsetDateTime? = null,
    val team: String? = null,
) {
    fun memberRequestsFromIds(): List<MemberRequest> {
        val memberRequestList: MutableList<MemberRequest> = mutableListOf<MemberRequest>()
        if (memberIds != null) {
            memberRequestList.addAll(memberIds.map { MemberRequest(userId = it) })
        }
        if (members != null) {
            memberRequestList.addAll(members)
        }
        return memberRequestList
    }
}
