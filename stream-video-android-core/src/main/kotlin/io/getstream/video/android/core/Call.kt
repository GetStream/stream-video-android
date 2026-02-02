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

package io.getstream.video.android.core

import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.os.PowerManager
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Stable
import io.getstream.android.video.generated.models.AcceptCallResponse
import io.getstream.android.video.generated.models.BlockUserResponse
import io.getstream.android.video.generated.models.CallSettingsRequest
import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.GetCallResponse
import io.getstream.android.video.generated.models.GetOrCreateCallResponse
import io.getstream.android.video.generated.models.GoLiveResponse
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.KickUserResponse
import io.getstream.android.video.generated.models.ListRecordingsResponse
import io.getstream.android.video.generated.models.ListTranscriptionsResponse
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.android.video.generated.models.MuteUsersResponse
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.PinResponse
import io.getstream.android.video.generated.models.RejectCallResponse
import io.getstream.android.video.generated.models.RingCallRequest
import io.getstream.android.video.generated.models.RingCallResponse
import io.getstream.android.video.generated.models.SendCallEventResponse
import io.getstream.android.video.generated.models.SendReactionResponse
import io.getstream.android.video.generated.models.StartTranscriptionResponse
import io.getstream.android.video.generated.models.StopLiveResponse
import io.getstream.android.video.generated.models.StopTranscriptionResponse
import io.getstream.android.video.generated.models.UnpinResponse
import io.getstream.android.video.generated.models.UpdateCallMembersRequest
import io.getstream.android.video.generated.models.UpdateCallMembersResponse
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.android.video.generated.models.UpdateUserPermissionsResponse
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.flatMap
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.CallApiDelegate
import io.getstream.video.android.core.call.CallEventManager
import io.getstream.video.android.core.call.CallJoinCoordinator
import io.getstream.video.android.core.call.CallMediaManager
import io.getstream.video.android.core.call.CallReInitializer
import io.getstream.video.android.core.call.CallRenderer
import io.getstream.video.android.core.call.CallSessionManager
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.audio.InputAudioFilter
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.call.scope.ScopeProvider
import io.getstream.video.android.core.call.scope.ScopeProviderImpl
import io.getstream.video.android.core.call.utils.SoundInputProcessor
import io.getstream.video.android.core.call.video.VideoFilter
import io.getstream.video.android.core.closedcaptions.ClosedCaptionsSettings
import io.getstream.video.android.core.coroutines.scopes.RestartableProducerScope
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.model.toIceServer
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.RampValueUpAndDownHelper
import io.getstream.video.android.core.utils.StreamSingleFlightProcessorImpl
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.model.User
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.models.ClientCapability
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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
    internal val scopeProvider: ScopeProvider = ScopeProviderImpl(clientImpl.scope)

    // Atomic controls
    internal var atomicLeave = AtomicUnitCall()

    private val logger by taggedLogger("Call:$type:$id")
    private val supervisorJob = SupervisorJob()
    private var callStatsReportingJob: Job? = null
    private var powerManager: PowerManager? = null

    internal val scope = CoroutineScope(clientImpl.scope.coroutineContext + supervisorJob)

    internal val restartableProducerScope = RestartableProducerScope()

    /** The call state contains all state such as the participant list, reactions etc */
    val state = CallState(client, this, user, restartableProducerScope)

    private val network by lazy { clientImpl.coordinatorConnectionModule.networkStateProvider }

    /** Camera gives you access to the local camera */
    val camera by lazy(LazyThreadSafetyMode.PUBLICATION) { mediaManager.camera }
    val microphone by lazy(LazyThreadSafetyMode.PUBLICATION) { mediaManager.microphone }
    val speaker by lazy(LazyThreadSafetyMode.PUBLICATION) { mediaManager.speaker }
    val screenShare by lazy(LazyThreadSafetyMode.PUBLICATION) { mediaManager.screenShare }

    private val callMediaManager = CallMediaManager(
        this,
        { mediaManager },
        { camera },
        { microphone },
        { speaker },
        { screenShare },
        { _peerConnectionFactory },
        { _peerConnectionFactory = null },
    )

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
    internal var isDestroyed = AtomicBoolean(false)

    /** Session handles all real time communication for video and audio */
//    internal var session: RtcSession? = null
//    var sessionId = UUID.randomUUID().toString()
    internal val unifiedSessionId = UUID.randomUUID().toString()

    internal var connectStartTime = 0L
    internal var reconnectStartTime = 0L

    /**
     * EGL base context shared between peerConnectionFactory and mediaManager
     * to break circular dependency.
     */
    internal val eglBase: EglBase by lazy {
        EglBase.create()
    }

    // peerConnectionFactory is nullable and recreated when audioBitrateProfile changes (before joining)
    private var _peerConnectionFactory: StreamPeerConnectionFactory? = null

    internal var peerConnectionFactory: StreamPeerConnectionFactory
        get() {
            if (_peerConnectionFactory == null) {
                _peerConnectionFactory = StreamPeerConnectionFactory(
                    context = clientImpl.context,
                    audioProcessing = clientImpl.audioProcessing,
                    audioUsage = clientImpl.callServiceConfigRegistry.get(type).audioUsage,
                    audioUsageProvider = { clientImpl.callServiceConfigRegistry.get(type).audioUsage },
                    audioBitrateProfileProvider = { mediaManager.microphone.audioBitrateProfile.value },
                    sharedEglBaseProvider = { eglBase },
                )
            }
            return _peerConnectionFactory!!
        }
        set(value) {
            _peerConnectionFactory = value
        }

    val events = MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)
    private val callRenderer = CallRenderer()
    internal val sessionManager = CallSessionManager(
        call = this,
        clientImpl = clientImpl,
        powerManager = powerManager,
        testInstanceProvider = testInstanceProvider,
    )

    internal val session: RtcSession?
        get() = sessionManager.session.get()
    val sessionId: String
        get() = sessionManager.sessionId.get()

    private val apiDelegate = CallApiDelegate(
        clientImpl = clientImpl,
        type = type,
        id = id,
        call = this,
        screenShareProvider = { screenShare },
        setScreenTrackCallBack = { sessionManager.session.get()?.setScreenShareTrack() },
    )
    internal val callEventManager =
        CallEventManager(events, sessionManager, restartableProducerScope, { subscriptions })

    private val callReInitializer = CallReInitializer(clientImpl.scope) {
        reInitialise()
    }

    internal val callJoinCoordinator = CallJoinCoordinator(
        call = this,
        client = clientImpl,
        callReInitializer = callReInitializer,
        onJoinFail = {
            sessionManager.session.set(null)
        },
        createJoinSession = { create, createOptions, ring, notify ->
            sessionManager._join(create, createOptions, ring, notify)
        },
        onRejoin = { reason -> sessionManager.rejoin(reason) },
    )

    private fun reInitialise() {
        logger.d { "[reInitialise]" }
        sessionManager.reset()
        state._connection.value = RealtimeConnection.Disconnected
        atomicLeave = AtomicUnitCall()
        scopeProvider.reset()
        with(restartableProducerScope) {
            detach()
            attach(scope)
        }
    }

    /**
     * Checks if the audioBitrateProfile has changed since the factory was created,
     * and recreates the factory if needed. This should only be called before joining.
     *
     * If the factory hasn't been created yet, it will be created with the current profile
     * when first accessed, so no recreation is needed.
     */
    internal fun ensureFactoryMatchesAudioProfile() =
        callMediaManager.ensureFactoryMatchesAudioProfile()

    /**
     * Recreates peerConnectionFactory, audioSource, audioTrack, videoSource and videoTrack
     * with the current audioBitrateProfile. This should only be called before the call is joined.
     */
    internal fun recreateFactoryAndAudioTracks() {
        val wasMicrophoneEnabled = microphone.status.value is DeviceStatus.Enabled
        val wasCameraEnabled = camera.status.value is DeviceStatus.Enabled

        // Dispose all tracks and sources first
        mediaManager.disposeTracksAndSources()

        // Recreate the factory (which will use the new audioBitrateProfile)
        recreatePeerConnectionFactory()

        // Re-enable tracks if they were enabled
        if (wasMicrophoneEnabled) {
            // audioTrack will be recreated on next access, then we enable it
            microphone.enable(fromUser = false)
        }
        if (wasCameraEnabled) {
            // videoTrack will be recreated on next access, then we enable it
            camera.enable(fromUser = false)
        }
    }

    /**
     * Recreates peerConnectionFactory with the current audioBitrateProfile.
     * This should only be called before the call is joined.
     */
    internal fun recreatePeerConnectionFactory() {
        _peerConnectionFactory?.dispose()
        _peerConnectionFactory = null
        // Next access to peerConnectionFactory will recreate it with current profile
    }

    internal val clientCapabilities = ConcurrentHashMap<String, ClientCapability>().apply {
        put(
            ClientCapability.CLIENT_CAPABILITY_SUBSCRIBER_VIDEO_PAUSE.name,
            ClientCapability.CLIENT_CAPABILITY_SUBSCRIBER_VIDEO_PAUSE,
        )
    }

    internal val mediaManager by lazy {
        if (testInstanceProvider.mediaManagerCreator != null) {
            testInstanceProvider.mediaManagerCreator!!.invoke()
        } else {
            MediaManagerImpl(
                clientImpl.context,
                this,
                scope,
                eglBase.eglBaseContext,
                clientImpl.callServiceConfigRegistry.get(type).audioUsage,
            ) { clientImpl.callServiceConfigRegistry.get(type).audioUsage }
        }
    }

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
            state._connection.value = RealtimeConnection.Reconnecting
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; old lastDisconnect:$lastDisconnect, clientImpl.leaveAfterDisconnectSeconds:${clientImpl.leaveAfterDisconnectSeconds}"
            }
            lastDisconnect = System.currentTimeMillis()
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; new lastDisconnect:$lastDisconnect"
            }
            leaveTimeoutAfterDisconnect = scope.launch {
                delay(clientImpl.leaveAfterDisconnectSeconds * 1000)
                logger.d {
                    "[NetworkStateListener#onDisconnected] #network; Leaving after being disconnected for ${clientImpl.leaveAfterDisconnectSeconds}"
                }
                leave()
            }
            logger.d { "[NetworkStateListener#onDisconnected] #network; at $lastDisconnect" }
        }
    }

    private var leaveTimeoutAfterDisconnect: Job? = null
    private var lastDisconnect = 0L
    private var reconnectDeadlineMils: Int = 10_000

    private var monitorPublisherPCStateJob: Job? = null
    private var monitorSubscriberPCStateJob: Job? = null
    private var sfuEvents: Job? = null
    private val streamSingleFlightProcessorImpl = StreamSingleFlightProcessorImpl(scope)

    init {
        scope.launch {
            soundInputProcessor.currentAudioLevel.collect {
                audioLevelOutputHelper.rampToValue(it)
            }
        }
        powerManager = safeCallWithDefault(null) {
            clientImpl.context.getSystemService(POWER_SERVICE) as? PowerManager
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
        video: Boolean? = null,
    ): Result<GetOrCreateCallResponse> {
        return apiDelegate.create(
            memberIds,
            members,
            custom,
            settings,
            startsAt,
            team,
            ring,
            notify,
            video,
        )
    }

    /** Update a call */
    suspend fun update(
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
    ): Result<UpdateCallResponse> {
        return apiDelegate.update(custom, settingsOverride, startsAt)
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
        with(callReInitializer) {
            waitFromCleanup()
            reinitialiseCoroutinesIfNeeded()
        }

        isDestroyed.set(false)

        val permissionPass =
            clientImpl.permissionCheck.checkAndroidPermissionsGroup(clientImpl.context, this)
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
        ensureFactoryMatchesAudioProfile()

        // the join flow should retry up to 3 times
        // if the error is not permanent
        // and fail immediately on permanent errors
        state._connection.value = RealtimeConnection.InProgress
        var retryCount = 0

        var result: Result<RtcSession>

        atomicLeave = AtomicUnitCall()
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
                sessionManager.session.set(null)
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
        sessionManager.session.set(null)
        val errorMessage = "Join failed after 3 retries"
        state._connection.value = RealtimeConnection.Failed(errorMessage)
        return Failure(value = Error.GenericError(errorMessage))
    }

    suspend fun join1(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<RtcSession> = callJoinCoordinator.join(create, createOptions, ring, notify)

    suspend fun joinAndRing(
        members: List<String>,
        createOptions: CreateCallOptions? = CreateCallOptions(members),
        video: Boolean = isVideoEnabled(),
    ): Result<RtcSession> {
        logger.d { "[joinAndRing] #ringing; #track; members: $members, video: $video" }
        state.toggleRingingStateUpdates(true)
        return join(ring = false, createOptions = createOptions).flatMap { rtcSession ->
            logger.d { "[joinAndRing] Joined #ringing; #track; ring: $members" }
            ring(RingCallRequest(isVideoEnabled(), members)).map {
                logger.d { "[joinAndRing] Ringed #ringing; #track; ring: $members" }
                clientImpl.state._ringingCall.value = this
                rtcSession
            }.onError {
                logger.e { "[joinAndRing] Ring failed #ringing; #track; error: $it" }
                state.toggleRingingStateUpdates(false)
                leave("ring-failed (${it.message})")
            }
        }
    }

    internal fun isPermanentError(error: Any): Boolean {
        if (error is Error.ThrowableError) {
            if (error.message.contains("Unable to resolve host")) {
                return false
            }
        }
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

        if (sessionManager.session.get() != null) {
            return Failure(Error.GenericError("Call $cid has already been joined"))
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
        location = locationResult.value

        val result =
            joinRequest(
                sessionManager.getOptions(create),
                locationResult.value,
                ring = ring,
                notify = notify,
            )

        if (result !is Success) {
            return result as Failure
        }
        try {
            val session = sessionManager.createJoinRtcSessionInner(result.value)
            sessionManager.session.set(session)
            state._connection.value = RealtimeConnection.Joined(session)
            session.connect()
        } catch (e: Exception) {
            return Failure(Error.GenericError(e.message ?: "RtcSession error occurred."))
        }
        client.state.setActiveCall(this)
        monitorSession(result.value)
        return Success(value = sessionManager.session.get()!!)
    }

    private fun Call.monitorSession(result: JoinCallResponse) {
        sfuEvents?.cancel()
        startCallStatsReporting(result.statsOptions.reportingIntervalMs.toLong())
        // listen to Signal WS
        val session = sessionManager.session.get()
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
                        logger.d { "[monitorPubConnectionState] Ice connection state is $it" }
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
                        logger.d { "[monitorSubConnectionState] Ice connection state is $it" }
                    }
                }
            }
        }
        network.subscribe(listener)
    }

    private fun startCallStatsReporting(reportingIntervalMs: Long = 10_000) {
        callStatsReportingJob?.cancel()
        callStatsReportingJob = scope.launch {
            // Wait a bit before we start capturing stats
            delay(reportingIntervalMs)

            while (isActive) {
                delay(reportingIntervalMs)
                sessionManager.session.get()?.sendCallStats(
                    report = collectStats(),
                )
            }
        }
    }

    internal suspend fun collectStats(): CallStatsReport {
        val session = sessionManager.session.get()
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

        return report
    }

    /**
     * Fast reconnect to the same SFU with the same participant session.
     */
    suspend fun fastReconnect(reason: String = "unknown") = schedule("fast") {
        val session = sessionManager.session.get()
        logger.d { "[fastReconnect] Reconnecting, reconnectAttepmts:$reconnectAttepmts" }
        session?.prepareReconnect()
        this@Call.state._connection.value = RealtimeConnection.Reconnecting
        if (session != null) {
            reconnectStartTime = System.currentTimeMillis()

//            val session = session!!
            val (prevSessionId, subscriptionsInfo, publishingInfo) = session.currentSfuInfo()
            val reconnectDetails = ReconnectDetails(
                previous_session_id = prevSessionId,
                strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                announced_tracks = publishingInfo,
                subscriptions = subscriptionsInfo,
                reconnect_attempt = reconnectAttepmts,
                reason = reason,
            )
            session.fastReconnect(reconnectDetails)
            val oldSessionStats = collectStats()
            session.sendCallStats(oldSessionStats)
        } else {
            logger.d { "[fastReconnect] [RealtimeConnection.Disconnected], call_id:$id" }
            this@Call.state._connection.value = RealtimeConnection.Disconnected
        }
    }

    /**
     * Rejoin a call. Creates a new session and joins as a new participant.
     */
    suspend fun rejoin(reason: String = "unknown") = schedule("rejoin") {
        val session = sessionManager.session.get()
        logger.d { "[rejoin] Rejoining" }
        reconnectAttepmts++
        state._connection.value = RealtimeConnection.Reconnecting
        location?.let {
            reconnectStartTime = System.currentTimeMillis()

            val joinResponse = joinRequest(location = it)
            if (joinResponse is Success) {
                // switch to the new SFU
                val cred = joinResponse.value.credentials
                val oldSession = session!!
                val oldSessionStats = collectStats()
                val currentOptions = session?.publisher?.currentOptions()
                logger.i { "Rejoin SFU ${oldSession?.sfuUrl} to ${cred.server.url}" }
                sessionManager.sessionId.set(UUID.randomUUID().toString())
                val (prevSessionId, subscriptionsInfo, publishingInfo) = oldSession.currentSfuInfo()
                val reconnectDetails = ReconnectDetails(
                    previous_session_id = prevSessionId,
                    strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
                    announced_tracks = publishingInfo,
                    subscriptions = subscriptionsInfo,
                    reconnect_attempt = reconnectAttepmts,
                    reason = reason,
                )
                this.state.removeParticipant(prevSessionId)
                oldSession.prepareRejoin()

                try {
                    val session = RtcSession(
                        clientImpl,
                        reconnectAttepmts,
                        powerManager,
                        this,
                        sessionManager.sessionId.get(),
                        clientImpl.apiKey,
                        clientImpl.coordinatorConnectionModule.lifecycle,
                        cred.server.url,
                        cred.server.wsEndpoint,
                        cred.token,
                        cred.iceServers.map { ice ->
                            ice.toIceServer()
                        },
                    )
                    this.sessionManager.session.set(session)
                    session.connect(reconnectDetails, currentOptions)
                    session.sfuTracer.trace("rejoin", reason)
                    oldSession.sendCallStats(oldSessionStats)
                    oldSession.leaveWithReason("Rejoin :: $reason")
                    oldSession.cleanup()
                    monitorSession(joinResponse.value)
                } catch (ex: Exception) {
                    logger.e(ex) {
                        "[rejoin] Failed to join response with ex: ${ex.message}"
                    }
                    state._connection.value = RealtimeConnection.Failed(ex)
                }
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
    suspend fun migrate() = schedule("migrate") {
        logger.d { "[migrate] Migrating" }
        state._connection.value = RealtimeConnection.Migrating
        location?.let {
            reconnectStartTime = System.currentTimeMillis()

            val joinResponse = joinRequest(location = it)
            if (joinResponse is Success) {
                // switch to the new SFU
                val cred = joinResponse.value.credentials
                val session = this.sessionManager.session.get()!!
                val currentOptions = session.publisher?.currentOptions()
                val oldSfuUrl = session.sfuUrl
                logger.i { "Rejoin SFU $oldSfuUrl to ${cred.server.url}" }
                val sessionId = UUID.randomUUID().toString()
                sessionManager.sessionId.set(sessionId)
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
                try {
                    val newSession = RtcSession(
                        clientImpl,
                        reconnectAttepmts,
                        powerManager,
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
                    val oldSession = this.sessionManager.session.get()
                    this.sessionManager.session.set(newSession)
                    session.connect(reconnectDetails, currentOptions)
                    monitorSession(joinResponse.value)
                    oldSession?.leaveWithReason("migrating")
                    oldSession?.cleanup()
                } catch (ex: Exception) {
                    logger.e(ex) {
                        "[switchSfu] Failed to join during " +
                            "migration - Error ${ex.message}"
                    }
                    state._connection.value = RealtimeConnection.Failed(ex)
                }
            } else {
                logger.e {
                    "[switchSfu] Failed to get a join response during " +
                        "migration - falling back to reconnect. Error ${joinResponse.errorOrNull()}"
                }
                state._connection.value = RealtimeConnection.Reconnecting
            }
        }
    }

    private suspend fun schedule(key: String, block: suspend () -> Unit) {
        logger.d { "[schedule] #reconnect; no args" }

        streamSingleFlightProcessorImpl.run(key, block)
    }

    /** Leave the call, but don't end it for other users */
    fun leave(reason: String = "user") {
        logger.d { "[leave] #ringing; no args, call_cid:$cid" }
        internalLeave(null, reason)
    }

    private fun internalLeave(disconnectionReason: Throwable?, reason: String) = atomicLeave {
        monitorSubscriberPCStateJob?.cancel()
        monitorPublisherPCStateJob?.cancel()
        monitorPublisherPCStateJob = null
        monitorSubscriberPCStateJob = null
        sessionManager.session.get()?.leaveWithReason(
            "[reason=$reason, error=${disconnectionReason?.message}]",
        )
        leaveTimeoutAfterDisconnect?.cancel()
        network.unsubscribe(listener)
        sfuEvents?.cancel()
        state._connection.value = RealtimeConnection.Disconnected
        logger.v { "[leave] #ringing; disconnectionReason: $disconnectionReason, call_id = $id" }
        if (isDestroyed.get()) {
            logger.w { "[leave] #ringing; Call already destroyed, ignoring" }
            return@atomicLeave
        }
        isDestroyed.set(true)

        sfuSocketReconnectionTime = null

        /**
         * TODO Rahul, need to check which call has owned the media at the moment(probably use active call)
         */
        stopScreenSharing()
        camera.disable()
        microphone.disable()

        if (id == client.state.activeCall.value?.id) {
            client.state.removeActiveCall(this) // Will also stop CallService
        }

        if (id == client.state.ringingCall.value?.id) {
            client.state.removeRingingCall(this)
        }

        TelecomCallController(client.context)
            .leaveCall(this)

        (client as StreamVideoClient).onCallCleanUp(this)

        clientImpl.scope.launch {
            safeCall {
                val session = sessionManager.session.get()
                session?.sfuTracer?.trace(
                    "leave-call",
                    "[reason=$reason, error=${disconnectionReason?.message}]",
                )
                val stats = collectStats()
                session?.sendCallStats(stats)
            }
            cleanup()
        }
    }

    /** ends the call for yourself as well as other users */
    suspend fun end(): Result<Unit> {
        // end the call for everyone
        val result = clientImpl.endCall(type, id)
        // cleanup
        leave("call-ended")
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
    ): Result<QueriedMembers> = apiDelegate.queryMembers(filter, sort, limit, prev, next)

    suspend fun muteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> = apiDelegate.muteAllUsers(audio, video, screenShare)

    fun setVisibility(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        viewportId: String = sessionId,
    ) {
        logger.i {
            "[setVisibility] #track; #sfu; viewportId: $viewportId, sessionId: $sessionId, trackType: $trackType, visible: $visible"
        }
        val session = sessionManager.session.get()
        session?.updateTrackDimensions(
            sessionId,
            trackType,
            visible,
            Subscriber.defaultVideoDimension,
            viewportId,
        )
    }

    fun setVisibility(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        viewportId: String = sessionId,
        width: Int,
        height: Int,
    ) {
        logger.i {
            "[setVisibility] #track; #sfu; viewportId: $viewportId, sessionId: $sessionId, trackType: $trackType, visible: $visible"
        }
        val session = sessionManager.session.get()
        session?.updateTrackDimensions(
            sessionId,
            trackType,
            visible,
            VideoDimension(width, height),
            viewportId,
        )
    }

    fun handleEvent(event: VideoEvent) {
        callEventManager.handleEvent(event)
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
        viewportId: String = sessionId,
    ) = callRenderer.initRenderer(
        videoRenderer,
        sessionId,
        trackType,
        eglBase,
        sessionManager.session.get(),
        onRendered,
        viewportId,
    )

    /**
     * Enables the provided client capabilities.
     */
    fun enableClientCapabilities(capabilities: List<ClientCapability>) {
        capabilities.forEach {
            this.clientCapabilities[it.name] = it
        }
    }

    /**
     * Disables the provided client capabilities.
     */
    fun disableClientCapabilities(capabilities: List<ClientCapability>) {
        capabilities.forEach {
            this.clientCapabilities.remove(it.name)
        }
    }

    suspend fun goLive(
        startHls: Boolean = false,
        startRecording: Boolean = false,
        startTranscription: Boolean = false,
    ): Result<GoLiveResponse> = apiDelegate.goLive(startHls, startRecording, startTranscription)

    suspend fun stopLive(): Result<StopLiveResponse> = apiDelegate.stopLive()

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
    fun startScreenSharing(
        mediaProjectionPermissionResultData: Intent,
        includeAudio: Boolean = false,
    ): Unit = apiDelegate.startScreenSharing(mediaProjectionPermissionResultData, includeAudio)

    fun stopScreenSharing(): Unit = apiDelegate.stopScreenSharing()

    suspend fun startHLS(): Result<Any> = apiDelegate.startHLS()

    suspend fun stopHLS(): Result<Any> = apiDelegate.stopHLS()

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

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Deprecated in favor of the `events` flow.",
        replaceWith = ReplaceWith("events.collect { }"),
    )
    public fun subscribe(
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription = synchronized(subscriptions) {
        val sub = EventSubscription(listener)
        subscriptions.add(sub)
        return sub
    }

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Deprecated in favor of the `events` flow.",
        replaceWith = ReplaceWith("events.collect { }"),
    )
    public fun unsubscribe(eventSubscription: EventSubscription) = synchronized(subscriptions) {
        subscriptions.remove(eventSubscription)
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

        if (!events.tryEmit(event)) {
            logger.e { "Failed to emit event to observers: [event: $event]" }
        }
    }

    private fun monitorHeadset() {
        microphone.devices.onEach { availableDevices ->
            logger.d {
                "[monitorHeadset] new available devices, prev selected: ${microphone.nonHeadsetFallbackDevice}"
            }

            val bluetoothHeadset =
                availableDevices.find { it is StreamAudioDevice.BluetoothHeadset }
            val wiredHeadset = availableDevices.find { it is StreamAudioDevice.WiredHeadset }

            if (bluetoothHeadset != null) {
                logger.d { "[monitorHeadset] BT headset selected" }
                microphone.select(bluetoothHeadset)
            } else if (wiredHeadset != null) {
                logger.d { "[monitorHeadset] wired headset found" }
                microphone.select(wiredHeadset)
            } else {
                logger.d { "[monitorHeadset] no headset found" }

                microphone.nonHeadsetFallbackDevice?.let { deviceBeforeHeadset ->
                    logger.d { "[monitorHeadset] before device selected" }
                    microphone.select(deviceBeforeHeadset)
                }
            }
        }.launchIn(scope)
    }

    internal fun updateMediaManagerFromSettings(callSettings: CallSettingsResponse) {
        callMediaManager.updateMediaManagerFromSettings(callSettings)
    }

    /**
     * List the recordings for this call.
     *
     * @param sessionId - if session ID is supplied, only recordings for that session will be loaded.
     */
    suspend fun listRecordings(sessionId: String? = null): Result<ListRecordingsResponse> {
        return clientImpl.listRecordings(type, id, sessionId)
    }

    /**
     * Kick a user from the call.
     *
     * @param userId - the user to kick
     * @param block - if true, the user will be blocked from rejoining the call
     */
    suspend fun kickUser(
        userId: String,
        block: Boolean = false,
    ): Result<KickUserResponse> = clientImpl.kickUser(
        type,
        id,
        userId,
        block,
    )

    suspend fun muteUser(
        userId: String,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> = apiDelegate.muteUser(userId, audio, video, screenShare)

    suspend fun muteUsers(
        userIds: List<String>,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> = apiDelegate.muteUsers(userIds, audio, video, screenShare)

    @VisibleForTesting
    internal suspend fun joinRequest(
        create: CreateCallOptions? = null,
        location: String,
        migratingFrom: String? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<JoinCallResponse> = apiDelegate.joinRequest(
        create,
        location,
        migratingFrom,
        ring,
        notify,
    )

    fun cleanup() {
        val session = sessionManager.session.get()
        // monitor.stop()
        session?.cleanup()
        shutDownJobsGracefully()
        callStatsReportingJob?.cancel()
        mediaManager.cleanup() // TODO Rahul, Verify Later: need to check which call has owned the media at the moment(probably use active call)
        sessionManager.session.set(null)
        // Cleanup the call's scope provider
        scopeProvider.cleanup()
    }

    // This will allow the Rest APIs to be executed which are in queue before leave
    private fun shutDownJobsGracefully() {
        UserScope(ClientScope()).launch {
            supervisorJob.children.forEach { it.join() }
            supervisorJob.cancel()
        }
        scope.cancel()
    }

    suspend fun ring(): Result<GetCallResponse> {
        logger.d { "[ring] #ringing; no args" }
        return clientImpl.ring(type, id)
    }

    suspend fun ring(ringCallRequest: RingCallRequest): Result<RingCallResponse> {
        logger.d { "[ring] #ringing ringCallRequest: $ringCallRequest" }
        return clientImpl.ring(type, id, ringCallRequest)
    }

    suspend fun notify(): Result<GetCallResponse> {
        logger.d { "[notify] #ringing; no args" }
        return clientImpl.notify(type, id)
    }

    suspend fun accept(): Result<AcceptCallResponse> = apiDelegate.accept()

    /**
     * Should outlive both the call scope and the service scope and needs to be executed in the client-level scope.
     * Because the call scope or service scope may be cancelled or finished while the network request is still in flight
     * TODO: Run this in clientImpl.scope internally
     */
    suspend fun reject(reason: RejectReason? = null): Result<RejectCallResponse> {
        logger.d { "[reject] #ringing; rejectReason: $reason, call_id:$id" }
        return clientImpl.reject(type, id, reason)
    }

    // For debugging
    internal suspend fun reject(
        source: String = "n/a",
        reason: RejectReason? = null,
    ): Result<RejectCallResponse> {
        logger.d { "[reject] source: $source" }
        return reject(reason)
    }

    fun processAudioSample(audioSample: AudioSamples) {
        soundInputProcessor.processSoundInput(audioSample.data)
    }

    fun collectUserFeedback(
        rating: Int,
        reason: String? = null,
        custom: Map<String, Any>? = null,
    ): Unit = apiDelegate.collectUserFeedback(rating, reason, custom)

    suspend fun takeScreenshot(track: VideoTrack): Bitmap? = apiDelegate.takeScreenshot(track)

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
        return peerConnectionFactory.isAudioProcessingEnabled()
    }

    fun setAudioProcessingEnabled(enabled: Boolean) {
        return peerConnectionFactory.setAudioProcessingEnabled(enabled)
    }

    fun toggleAudioProcessing(): Boolean {
        return peerConnectionFactory.toggleAudioProcessing()
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

    suspend fun startClosedCaptions(): Result<io.getstream.android.video.generated.models.StartClosedCaptionsResponse> {
        return clientImpl.startClosedCaptions(type, id)
    }

    suspend fun stopClosedCaptions(): Result<io.getstream.android.video.generated.models.StopClosedCaptionsResponse> {
        return clientImpl.stopClosedCaptions(type, id)
    }

    fun updateClosedCaptionsSettings(closedCaptionsSettings: ClosedCaptionsSettings) {
        state.closedCaptionManager.updateClosedCaptionsSettings(closedCaptionsSettings)
    }

    /**
     * Sets the preferred incoming video resolution.
     *
     * @param resolution The preferred resolution. Set to `null` to switch back to auto.
     * @param sessionIds The participant session IDs to apply the resolution to. If `null`, the resolution will be applied to all participants.
     */
    fun setPreferredIncomingVideoResolution(
        resolution: PreferredVideoResolution?,
        sessionIds: List<String>? = null,
    ) {
        val session = sessionManager.session.get()
        session?.let { session ->
            session.trackOverridesHandler.updateOverrides(
                sessionIds = sessionIds,
                dimensions = resolution?.let { VideoDimension(it.width, it.height) },
            )
        }
    }

    /**
     * Enables/disables incoming video feed.
     *
     * @param enabled Whether the video feed should be enabled or disabled. Set to `null` to switch back to auto.
     * @param sessionIds The participant session IDs to enable/disable the video feed for. If `null`, the setting will be applied to all participants.
     */
    fun setIncomingVideoEnabled(enabled: Boolean?, sessionIds: List<String>? = null) {
        sessionManager.session.get()?.trackOverridesHandler?.updateOverrides(
            sessionIds,
            visible = enabled,
        )
    }

    /**
     * Enables or disables the reception of incoming audio tracks for all or specified participants.
     *
     * This method allows selective control over whether the local client receives audio from remote participants.
     * It's particularly useful in scenarios such as livestreams or group calls where the user may want to mute
     * specific participants' audio without affecting the overall session.
     *
     * @param enabled `true` to enable (subscribe to) incoming audio, `false` to disable (unsubscribe from) it.
     * @param sessionIds Optional list of participant session IDs for which to toggle incoming audio.
     * If `null`, the audio setting is applied to all participants currently in the session.
     */
    fun setIncomingAudioEnabled(enabled: Boolean, sessionIds: List<String>? = null) =
        callMediaManager.setIncomingAudioEnabled(sessionManager.session.get(), enabled, sessionIds)

    @InternalStreamVideoApi
    public val debug = Debug(this)

    @InternalStreamVideoApi
    public class Debug(val call: Call) {

        public fun pause() {
            call.sessionManager.session.get()?.subscriber?.disable()
        }

        public fun resume() {
            call.sessionManager.session.get()?.subscriber?.enable()
        }

        public fun rejoin() {
            call.scope.launch {
                call.rejoin()
            }
        }

        public fun restartSubscriberIce() {
            call.sessionManager.session.get()?.subscriber?.connection?.restartIce()
        }

        public fun restartPublisherIce() {
            call.sessionManager.session.get()?.publisher?.connection?.restartIce()
        }

        fun migrate() {
            call.scope.launch {
                call.migrate()
            }
        }

        fun fastReconnect(reason: String = "Debug") {
            call.scope.launch {
                call.fastReconnect(reason)
            }
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
