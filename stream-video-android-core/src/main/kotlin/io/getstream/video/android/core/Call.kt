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
import io.getstream.android.video.generated.models.AudioSettingsResponse
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
import io.getstream.android.video.generated.models.UpdateCallRequest
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.android.video.generated.models.UpdateUserPermissionsResponse
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.android.video.generated.models.VideoSettingsResponse
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.flatMap
import io.getstream.video.android.core.analytics.call.CallAnalytics
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.call.observer.model.TelemetryModel
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.FastReconnectResult
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.call.audio.InputAudioFilter
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.call.scope.ScopeProvider
import io.getstream.video.android.core.call.scope.ScopeProviderImpl
import io.getstream.video.android.core.call.utils.SoundInputProcessor
import io.getstream.video.android.core.call.video.VideoFilter
import io.getstream.video.android.core.call.video.YuvFrame
import io.getstream.video.android.core.closedcaptions.ClosedCaptionsSettings
import io.getstream.video.android.core.events.GoAwayEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.model.toIceServer
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
import io.getstream.video.android.core.recording.RecordingType
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.RampValueUpAndDownHelper
import io.getstream.video.android.core.utils.debugOnly
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.core.utils.toQueriedMembers
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import org.threeten.bp.OffsetDateTime
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.RendererCommon
import org.webrtc.VideoSink
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.models.ClientCapability
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

@Deprecated(
    message = "No longer used internally. The reconnect deadline is now driven by the server's " +
        "fastReconnectDeadlineSeconds. This constant will be removed in a future release.",
    level = DeprecationLevel.WARNING,
)
const val sfuReconnectTimeoutMillis = 30_000
internal typealias CallSessionId = String

/**
 * Outcome of a single reconnect attempt. Each reconnect method returns one of
 * these instead of throwing, making the control flow in the reconnect loop
 * explicit and exhaustively checked by the compiler.
 */
internal sealed class ReconnectOutcome {
    /** Reconnect succeeded — exit the loop. */
    object Success : ReconnectOutcome()

    /** A required precondition is missing (no session, no location). Terminal — don't retry. */
    data class PreconditionNotMet(val reason: String) : ReconnectOutcome()

    /** Peer connections are stale and can't be reused. Should escalate to REJOIN. */
    object PeerConnectionStale : ReconnectOutcome()

    /** Server-initiated disconnect — leave the call cleanly. */
    object Disconnect : ReconnectOutcome()

    /** A transient failure occurred. The loop should retry with escalation. */
    data class Failed(val error: Exception) : ReconnectOutcome()
}

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

    /**
     * Increment this only for REJOIN and MIGRATION strategies
     */
    internal var nonFastReconnectAttempts = 0
    internal val clientImpl = client as StreamVideoClient
    internal val scopeProvider: ScopeProvider = ScopeProviderImpl(clientImpl.scope)

    // Atomic controls
    private var atomicLeave = AtomicUnitCall()

    private val logger by taggedLogger("Call:$type:$id")
    private val supervisorJob = SupervisorJob()
    private var callStatsReportingJob: Job? = null
    private var powerManager: PowerManager? = null

    internal val scope = CoroutineScope(clientImpl.scope.coroutineContext + supervisorJob)

    // Must be initialized before `state` — CallState → SortedParticipantsState
    // launches a coroutine that reads `call.events` (leaking-this race).
    val events = MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)

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
    internal val session: MutableStateFlow<RtcSession?> = MutableStateFlow(null)

    var sessionId = UUID.randomUUID().toString()
    internal val unifiedSessionId = UUID.randomUUID().toString()

    /**
     * SFU IDs (edge names) we failed to connect to (e.g. SFU_FULL). Sent in migrating_from_list
     * when requesting new credentials so the coordinator can exclude them.
     */
    private val failedSfuIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

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
                    webRtcLoggingLevel = clientImpl.loggingLevel.webRtcLoggingLevel,
                )
            }
            return _peerConnectionFactory!!
        }
        set(value) {
            _peerConnectionFactory = value
        }

    internal val callAnalytics =
        CallAnalytics(
            clientImpl.context,
            this.id,
            this.type,
            state.connection,
            state.participants,
            client.state.clientEventReporter,
            scope,
        )

    /**
     * Checks if the audioBitrateProfile has changed since the factory was created,
     * and recreates the factory if needed. This should only be called before joining.
     *
     * If the factory hasn't been created yet, it will be created with the current profile
     * when first accessed, so no recreation is needed.
     */
    internal fun ensureFactoryMatchesAudioProfile() {
        val factory = _peerConnectionFactory

        // If factory hasn't been created yet, it will be created with current profile automatically
        if (factory == null) {
            return
        }

        // Check if current profile differs from the profile used to create the factory
        val factoryProfile = factory.audioBitrateProfile
        val currentProfile = mediaManager.microphone.audioBitrateProfile.value

        if (factoryProfile != null && currentProfile != factoryProfile) {
            logger.i {
                "Audio bitrate profile changed from $factoryProfile to $currentProfile. " +
                    "Recreating factory before joining."
            }
            recreateFactoryAndAudioTracks()
        }
    }

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
                "[NetworkStateListener#onConnected] #network; no args, elapsedTimeMils:$elapsedTimeMils, lastDisconnect:$lastDisconnect, reconnectDeadlineMils:$reconnectDeadlineMillis"
            }
            val strategy = if (lastDisconnect > 0 && elapsedTimeMils < reconnectDeadlineMillis) {
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST
            } else {
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
            }
            reconnect(strategy, "NetworkStateListener#onConnected")
        }

        override suspend fun onDisconnected() {
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; old lastDisconnect:$lastDisconnect, clientImpl.leaveAfterDisconnectSeconds:${clientImpl.leaveAfterDisconnectSeconds}"
            }
            lastDisconnect = System.currentTimeMillis()
            logger.d {
                "[NetworkStateListener#onDisconnected] #network; new lastDisconnect:$lastDisconnect"
            }
            leaveTimeoutAfterDisconnect = scope.launch {
                delay(clientImpl.leaveAfterDisconnectSeconds * 1000)
                val conn = state.connection.value
                if (conn is RealtimeConnection.Connected) {
                    logger.d {
                        "[NetworkStateListener#onDisconnected] #network; Already reconnected ($conn) — not leaving"
                    }
                    return@launch
                }
                val message = "Leaving after being disconnected for ${clientImpl.leaveAfterDisconnectSeconds}"
                logger.d {
                    "[NetworkStateListener#onDisconnected] #network; $message (connection=$conn)"
                }
                leave(CallLeaveReason.Backend(cause = BackendCause.LEAVE_TIMEOUT_AFTER_DISCONNECT, message = message)) // TODO Rahul before merge
            }
            logger.d { "[NetworkStateListener#onDisconnected] #network; at $lastDisconnect" }
        }
    }

    private var leaveTimeoutAfterDisconnect: Job? = null
    private var lastDisconnect = 0L
    private var reconnectDeadlineMillis: Int = 10_000
    private val reconnectMutex = Mutex()

    private var monitorPublisherPCStateJob: Job? = null
    private var monitorSubscriberPCStateJob: Job? = null
    private var sfuListener: Job? = null
    private var sfuEvents: Job? = null

    /** Reports join lifecycle events to the backend for success-rate analytics. Set by StreamVideoClient. */

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
                video = video,
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
                video = video,
            )
        }

        response.onSuccess {
            /**
             * Because [CallState.updateFromResponse] reads the value of [ClientState.ringingCall]
             */
            if (ring) {
                client.state._ringingCall.value = this
            }
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
        hintHighScaleLivestreamPublisher: Boolean? = null,
        callJoinInterceptor: CallJoinInterceptor? = null,
    ): Result<RtcSession> {
        callAnalytics.joinObserver.onJoinFunctionStart()
        callAnalytics.mediaPermissionObserver.mediaPermissionStatus()
        logger.d {
            "[join] #ringing; #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }
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

        this.state.callJoinInterceptor = callJoinInterceptor

        // the join flow should retry up to 3 times
        // if the error is not permanent
        // and fail immediately on permanent errors
        state._connection.value = RealtimeConnection.InProgress
        var retryCount = 0

        var result: Result<RtcSession>

        atomicLeave = AtomicUnitCall()
        while (retryCount < 3) {
            val tempResult =
                _join(
                    create,
                    createOptions,
                    ring,
                    notify,
                    hintHighScaleLivestreamPublisher,
                    TelemetryModel(retryCount, JoinReason.FirstAttempt),
                )
            if (tempResult is Success) {
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

                result = Success<RtcSession>(tempResult.value.first)
                return result
            }
            if (tempResult is Failure) {
                result = tempResult
                session.value = null
                logger.e { "Join failed with error $result" }
                if (isPermanentError(result.value)) {
                    state._connection.value = RealtimeConnection.Failed(result.value)
                    callAnalytics.joinObserver.onJoinRequestPermanentError(
                        retryCount,
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
        callAnalytics.joinObserver.onJoinRequestRetryExhausted(
            retryCount,
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
            ring(RingCallRequest(isVideoEnabled(), members)).map {
                logger.d { "[joinAndRing] Ringed #ringing; #track; ring: $members" }
                clientImpl.state._ringingCall.value = this
                rtcSession
            }.onError {
                logger.e { "[joinAndRing] Ring failed #ringing; #track; error: $it" }
                state.toggleJoinAndRingProgress(false)
                leave(
                    CallLeaveReason.Backend(
                        BackendCause.RING_FAILED,
                        message = "ring-failed (${it.message})",
                    ),
                )
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
        hintHighScaleLivestreamPublisher: Boolean? = null,
        telemetryModel: TelemetryModel,
    ): Result<Pair<RtcSession, CallSessionId>> {
        nonFastReconnectAttempts = 0
        sfuEvents?.cancel()
        sfuListener?.cancel()

        if (session.value != null) {
            return Failure(Error.GenericError("Call $cid has already been joined"))
        }
        logger.d {
            "[joinInternal] #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }

        connectStartTime = System.currentTimeMillis()

        // step 1. call the join endpoint to get a list of SFUs
        val locationResult = clientImpl.getCachedLocation()
        if (locationResult is Success) {
            location = locationResult.value
        }

        val options = createOptions
            ?: if (create) {
                CreateCallOptions()
            } else {
                null
            }
        val result =
            joinRequest(
                options,
                location ?: "auto",
                ring = ring,
                notify = notify,
                hintHighScaleLivestreamPublisher = hintHighScaleLivestreamPublisher,
                telemetryModel = telemetryModel,
            )

        if (result !is Success) {
            return result as Failure
        }
        val sfuToken = result.value.credentials.token
        val sfuUrl = result.value.credentials.server.url
        val sfuWsUrl = result.value.credentials.server.wsEndpoint
        val sfuName = result.value.credentials.server.edgeName
        val iceServers = result.value.credentials.iceServers.map { it.toIceServer() }
        val localSession = if (testInstanceProvider.rtcSessionCreator != null) {
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
                sfuName = sfuName,
                remoteIceServers = iceServers,
                powerManager = powerManager,
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
        client.state.setActiveCall(this)
        monitorSession(result.value)
        return Success(value = Pair(session.value!!, result.value.call.currentSessionId))
    }

    private fun Call.monitorSession(result: JoinCallResponse) {
        sfuEvents?.cancel()
        sfuListener?.cancel()
        startCallStatsReporting(result.statsOptions.reportingIntervalMs.toLong())
        // listen to Signal WS
        sfuEvents = scope.launch {
            session.value?.let {
                it.socket.events().collect { event ->
                    if (event is JoinCallResponseEvent) {
                        reconnectDeadlineMillis = event.fastReconnectDeadlineSeconds * 1000
                        logger.d { "[join] #deadline for reconnect is ${reconnectDeadlineMillis / 1000} seconds" }
                    }
                }
            }
        }
        monitorPublisherPCStateJob?.cancel()
        callAnalytics.peerConnectionObserver.stop()
        callAnalytics.peerConnectionObserver.observePeerConnections(session)
        monitorPublisherPCStateJob = scope.launch {
            session
                .filterNotNull()
                .flatMapLatest { it.publisher.filterNotNull() }
                .flatMapLatest { publisher ->
                    publisher.iceState.map { publisher to it }
                }
                .collect { (publisher, state) ->
                    when (state) {
                        PeerConnection.IceConnectionState.FAILED,
                        PeerConnection.IceConnectionState.DISCONNECTED,
                        -> {
                            publisher.connection.restartIce() // TODO Rahul might send a request to the server
                        }
                        else -> {
                            logger.d { "[monitorPubConnectionState] Ice connection state is $state" }
                        }
                    }
                }
        }

        monitorSubscriberPCStateJob?.cancel()
        monitorSubscriberPCStateJob = scope.launch {
            session.value?.subscriber?.value?.iceState?.collect {
                when (it) {
                    PeerConnection.IceConnectionState.FAILED, PeerConnection.IceConnectionState.DISCONNECTED -> {
                        session.value?.requestSubscriberIceRestart() // TODO Rahul might send a request to the server
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
                session.value?.sendCallStats(
                    report = collectStats(),
                )
            }
        }
    }

    internal suspend fun collectStats(): CallStatsReport {
        val publisherStats = runCatching { session.value?.getPublisherStats() }.getOrNull()
        val subscriberStats = runCatching { session.value?.getSubscriberStats() }.getOrNull()
        runCatching {
            state.stats.updateFromRTCStats(publisherStats, isPublisher = true)
            state.stats.updateFromRTCStats(subscriberStats, isPublisher = false)
            state.stats.updateLocalStats()
        }.onFailure { logger.w { "[collectStats] Failed to update stats: ${it.message}" } }
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

    // region Reconnection — unified loop

    /**
     * Unified reconnection entry point.
     *
     * All callers (stateJob, NetworkStateListener, error handlers) funnel through
     * this method. It acquires [reconnectMutex] so only one flow runs at a time,
     * and implements a retry loop that **escalates** the strategy on failure:
     *
     * - **FAST** → **REJOIN** after [MAX_FAST_RECONNECT_ATTEMPTS] failures *or*
     *   when the elapsed time exceeds [reconnectDeadlineMillis].
     * - **MIGRATE** → **REJOIN** if the migration attempt fails.
     * - **DISCONNECT** → leaves the call (server-initiated).
     * - **UNSPECIFIED** → treated as FAST (HealthMonitor already attempted the WS).
     *
     * The loop exits when the connection state becomes [RealtimeConnection.Connected],
     * [RealtimeConnection.ReconnectingFailed], or [RealtimeConnection.Disconnected].
     *
     * @param strategy the initial reconnection strategy requested by the caller.
     * @param reason a human-readable reason for logging / tracing.
     */
    internal suspend fun reconnect(
        strategy: WebsocketReconnectStrategy,
        reason: String,
    ) {
        val conn = state.connection.value
        logger.d { "[reconnect] Entry — strategy=$strategy reason=$reason connection=$conn" }

        if (isDestroyed || conn is RealtimeConnection.Disconnected) {
            logger.d {
                "[reconnect] Call already left/destroyed (isDestroyed=$isDestroyed, conn=$conn) — skipping ($reason)"
            }
            return
        }

        // Use tryLock so concurrent triggers (stateJob, NetworkStateListener,
        // SfuSocket errors) don't queue up. If a reconnect loop is already
        // running it will handle recovery; redundant callers return immediately.
        if (!reconnectMutex.tryLock()) {
            logger.d { "[reconnect] Active reconnect loop running — skipping ($reason)" }
            return
        }
        var currentStrategy = strategy
        try {
            // Re-check after acquiring the lock — bail only if the user left.
            // We deliberately allow reconnect from Connected (SFU/network may
            // request it) and from ReconnectingFailed (fresh trigger like
            // network recovery should be retried).
            val currentConn = state.connection.value
            if (currentConn is RealtimeConnection.Disconnected) {
                logger.d { "[reconnect] State is $currentConn — no reconnect needed ($reason)" }
                return
            }

            val isMigrate = strategy ==
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE
            state._connection.value = if (isMigrate) {
                RealtimeConnection.Migrating
            } else {
                RealtimeConnection.Reconnecting
            }

            val loopStartTime = System.currentTimeMillis()
            // Local iteration counter for this reconnect() invocation only.
            // Controls MAX_RECONNECT_ATTEMPTS cap and FAST→REJOIN escalation.
            // Distinct from the class-level reconnectAttempts which is cumulative.
            var loopIteration = 0

            while (true) {
                // EARLY EXIT CASE 1 - State based
                val connectionState = state.connection.value
                if (connectionState is RealtimeConnection.Connected ||
                    connectionState is RealtimeConnection.ReconnectingFailed ||
                    connectionState is RealtimeConnection.Disconnected
                ) {
                    logger.i { "[reconnect] Loop finished — state=$connectionState" }
                    break
                }

                // EARLY EXIT CASE 2 - count based
                if (loopIteration >= MAX_RECONNECT_ATTEMPTS) {
                    logger.w { "[reconnect] Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached — giving up" }
                    state._connection.value = RealtimeConnection.ReconnectingFailed
                    break
                }

                // EARLY EXIT CASE 3 - time based
                val elapsedMs = System.currentTimeMillis() - loopStartTime
                if (clientImpl.leaveAfterDisconnectSeconds > 0 &&
                    elapsedMs / 1000 > clientImpl.leaveAfterDisconnectSeconds
                ) {
                    logger.w { "[reconnect] Disconnection timeout reached — giving up" }
                    state._connection.value = RealtimeConnection.ReconnectingFailed
                    break
                }

                // Wait for network before doing anything else. Polls without
                // consuming the attempt budget — the elapsed-time guard below
                // will still fire if we wait too long.
                if (!network.isConnected()) {
                    logger.d {
                        "[reconnect] Network unavailable — waiting for connectivity (loopIteration=$loopIteration)"
                    }
                    delay(RECONNECT_DELAY_MS)
                    continue
                }

                val currentTimeInMillis = System.currentTimeMillis()
                if (currentTimeInMillis - loopStartTime >= reconnectDeadlineMillis) {
                    currentStrategy = when (currentStrategy) {
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
                        -> {
                            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
                        }

                        else -> currentStrategy
                    }
                }

                logger.i {
                    "[reconnect] loopIteration=$loopIteration strategy=$currentStrategy reason=$reason"
                }

                val outcome = when (currentStrategy) {
                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
                    -> { reconnectFast(reason, TelemetryModel(loopIteration)) }

                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN -> {
                        nonFastReconnectAttempts++
                        reconnectRejoin(
                            reason,
                            TelemetryModel(nonFastReconnectAttempts, JoinReason.ReJoin),
                        )
                    }

                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE -> {
                        nonFastReconnectAttempts++
                        reconnectMigrate(
                            TelemetryModel(nonFastReconnectAttempts, JoinReason.Migrate),
                        )
                    }

                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_DISCONNECT ->
                        ReconnectOutcome.Disconnect
                }

                when (outcome) {
                    is ReconnectOutcome.Success -> break

                    is ReconnectOutcome.Disconnect -> {
                        logger.w { "[reconnect] DISCONNECT requested — leaving call" }
                        leave(
                            CallLeaveReason.Backend(BackendCause.SFU_DISCONNECT, "SFU:DISCONNECT"),
                        )
                        break
                    }

                    is ReconnectOutcome.PreconditionNotMet -> {
                        logger.w { "[reconnect] Precondition not met — giving up: ${outcome.reason}" }
                        state._connection.value = RealtimeConnection.ReconnectingFailed
                        break
                    }

                    is ReconnectOutcome.PeerConnectionStale -> {
                        logger.w { "[reconnect] Peer connections stale — escalating to REJOIN" }
                        delay(RECONNECT_DELAY_MS)
                        loopIteration++
                        currentStrategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
                    }

                    is ReconnectOutcome.Failed -> {
                        logger.w {
                            "[reconnect] $currentStrategy ($nonFastReconnectAttempts) failed: ${outcome.error.message}"
                        }

                        delay(RECONNECT_DELAY_MS)
                        loopIteration++

                        val wasMigrating = currentStrategy ==
                            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE
                        val pastFastReconnectDeadline = (System.currentTimeMillis() - loopStartTime) >
                            reconnectDeadlineMillis
                        val shouldEscalateToRejoin = wasMigrating ||
                            pastFastReconnectDeadline

                        if (shouldEscalateToRejoin) {
                            currentStrategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
                        }
                        logger.i { "[reconnect] Next strategy: $currentStrategy (loopIteration=$loopIteration)" }
                    }
                }
            }

            if (state.connection.value is RealtimeConnection.ReconnectingFailed) {
                logger.w { "[reconnect] All recovery attempts exhausted — leaving call ($reason)" }
                callAnalytics.joinObserver.onJoinRequestRetryExhausted(
                    loopIteration,
                    "All recovery attempts exhausted — leaving call ($reason)",
                )
                leave(
                    CallLeaveReason.RetryExhausted(
                        loopIteration,
                        "reconnect-failed",
                        "All recovery attempts exhausted — leaving call ($reason)",
                    ),
                )
            }
        } finally {
            // Always release the mutex — even on exceptions or coroutine
            // cancellation — so future reconnect() calls aren't permanently blocked.
            reconnectMutex.unlock()
            logger.d {
                "[reconnect] Free reconnectMutex, initialStrategy: $strategy, finalStrategy: $currentStrategy"
            }
        }
    }

    /**
     * Fast reconnect to the same SFU with the same participant session.
     * Reuses the existing session ID — no previous_session_id needed since the
     * SFU already knows this participant.
     */
    private suspend fun reconnectFast(
        reason: String,
        telemetryModel: TelemetryModel? = null,
    ): ReconnectOutcome {
        logger.d { "[reconnectFast] reconnectAttempts=$nonFastReconnectAttempts" }
        val currentSession = session.value
            ?: return ReconnectOutcome.PreconditionNotMet("No active session for fast reconnect")

        val stats = collectStats()
        currentSession.sendCallStats(stats)

        currentSession.prepareReconnect()
        state._connection.value = RealtimeConnection.Reconnecting
        reconnectStartTime = System.currentTimeMillis()

        val (_, subscriptionsInfo, publishingInfo) = currentSession.currentSfuInfo()
        val reconnectDetails = ReconnectDetails(
            previous_session_id = "",
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            announced_tracks = publishingInfo,
            subscriptions = subscriptionsInfo,
            reconnect_attempt = nonFastReconnectAttempts,
            reason = reason,
        )
        return when (val result = currentSession.fastReconnect(reconnectDetails, telemetryModel)) {
            is FastReconnectResult.Connected -> ReconnectOutcome.Success
            is FastReconnectResult.PeerConnectionStale -> ReconnectOutcome.PeerConnectionStale
            is FastReconnectResult.Failed -> ReconnectOutcome.Failed(result.error)
        }
    }

    /**
     * Rejoin a call. Creates a new session ID and joins as a new participant.
     * previous_session_id is set so the SFU can transfer state (tracks,
     * subscriptions) from the old session to the new one.
     */
    private suspend fun reconnectRejoin(
        reason: String,
        telemetryModel: TelemetryModel,
    ): ReconnectOutcome {
        logger.d { "[reconnectRejoin] reconnectAttempts=$nonFastReconnectAttempts" }
        state._connection.value = RealtimeConnection.Reconnecting
        val loc = location ?: "auto"
//            ?: return ReconnectOutcome.PreconditionNotMet("No location available for rejoin")
        val oldSession = session.value
            ?: return ReconnectOutcome.PreconditionNotMet("No active session for rejoin")
        reconnectStartTime = System.currentTimeMillis()

        val joinResponse =
            joinRequest(location = loc, telemetryModel = telemetryModel)
        if (joinResponse !is Success) {
            return ReconnectOutcome.Failed(
                Exception("Failed to get join response: ${joinResponse.errorOrNull()}"),
            )
        }

        val cred = joinResponse.value.credentials
        val currentOptions = oldSession.publisher.value?.currentOptions()
        logger.i { "Rejoin SFU ${oldSession.sfuUrl} to ${cred.server.url}" }

        this.sessionId = UUID.randomUUID().toString()
        val (prevSessionId, subscriptionsInfo, publishingInfo) = oldSession.currentSfuInfo()
        val reconnectDetails = ReconnectDetails(
            previous_session_id = prevSessionId,
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            announced_tracks = publishingInfo,
            subscriptions = subscriptionsInfo,
            reconnect_attempt = nonFastReconnectAttempts,
            reason = reason,
        )
        this.state.removeParticipant(prevSessionId)
        oldSession.prepareRejoin("rejoin")
        val newSession = RtcSession(
            clientImpl,
            nonFastReconnectAttempts,
            powerManager,
            this,
            sessionId,
            clientImpl.apiKey,
            clientImpl.coordinatorConnectionModule.lifecycle,
            cred.server.url,
            cred.server.wsEndpoint,
            cred.token,
            cred.server.edgeName,
            cred.iceServers.map { ice -> ice.toIceServer() },
        )
        this.session.value = newSession

        return when (
            val result = newSession.connectInternal(
                reconnectDetails,
                currentOptions,
                TelemetryModel(telemetryModel.retryAttempt),
            )
        ) {
            is SfuConnectionResult.Connected -> {
                newSession.sfuTracer.trace("rejoin", reason)
                monitorSession(joinResponse.value)
                ReconnectOutcome.Success
            }
            is SfuConnectionResult.Failed -> ReconnectOutcome.Failed(result.error)
        }
    }

    /**
     * Migrate to another SFU. Reuses the same session ID — the SFU
     * identifies the participant via from_sfu_id, not previous_session_id.
     */
    private suspend fun reconnectMigrate(telemetryModel: TelemetryModel): ReconnectOutcome {
        logger.d { "[reconnectMigrate] Migrating" }
        state._connection.value = RealtimeConnection.Migrating
        val loc = location ?: "auto"
//            ?: return ReconnectOutcome.PreconditionNotMet("No location available for migrate")
        val oldSession = session.value
            ?: return ReconnectOutcome.PreconditionNotMet("No active session for migrate")
        reconnectStartTime = System.currentTimeMillis()
        addFailedSfuId(oldSession.sfuName)

        val joinResponse =
            joinRequest(
                location = loc,
                migratingFrom = oldSession.sfuName,
                telemetryModel = telemetryModel,
            )
        if (joinResponse !is Success) {
            return ReconnectOutcome.Failed(
                Exception(
                    "Failed to get join response during migration: ${joinResponse.errorOrNull()}",
                ),
            )
        }

        val cred = joinResponse.value.credentials
        val currentOptions = oldSession.publisher.value?.currentOptions()
        val oldSfuName = oldSession.sfuName
        logger.i { "[reconnectMigrate] Migrate SFU $oldSfuName to ${cred.server.edgeName}" }

        val (_, subscriptionsInfo, publishingInfo) = oldSession.currentSfuInfo()
        val reconnectDetails = ReconnectDetails(
            previous_session_id = "",
            strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
            announced_tracks = publishingInfo,
            subscriptions = subscriptionsInfo,
            from_sfu_id = oldSfuName,
            reconnect_attempt = nonFastReconnectAttempts,
        )

        val stats = collectStats()
        oldSession.sendCallStats(stats)
        oldSession.enterMigration()

        val newSession = RtcSession(
            clientImpl,
            nonFastReconnectAttempts,
            powerManager,
            this,
            sessionId,
            clientImpl.apiKey,
            clientImpl.coordinatorConnectionModule.lifecycle,
            cred.server.url,
            cred.server.wsEndpoint,
            cred.token,
            cred.server.edgeName,
            cred.iceServers.map { ice -> ice.toIceServer() },
        )
        this.session.value = newSession

        return try {
            val result = newSession.connectInternal(
                reconnectDetails,
                currentOptions,
                TelemetryModel(telemetryModel.retryAttempt),
            )
            when (result) {
                is SfuConnectionResult.Connected -> {
                    monitorSession(joinResponse.value)
                    ReconnectOutcome.Success
                }
                is SfuConnectionResult.Failed -> ReconnectOutcome.Failed(result.error)
            }
        } finally {
            oldSession.finalizeMigration()
        }
    }

    // Keep public wrappers for backward compatibility and Debug class
    suspend fun fastReconnect(reason: String = "unknown") {
        reconnect(WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST, reason)
    }

    suspend fun rejoin(reason: String = "unknown") {
        reconnect(WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN, reason)
    }

    suspend fun migrate() {
        reconnect(WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE, "migrate")
    }

    // endregion

    /** Leave the call, but don't end it for other users */
    @InternalStreamVideoApi
    fun leave(reason: CallLeaveReason) {
        logger.d { "[leave] #ringing; call_cid:$cid" }
        internalLeave(reason)
    }

    fun leave(reason: String = "user") {
        logger.d { "[leave] #ringing; no args, call_cid:$cid" }
        internalLeave(CallLeaveReason.Custom(reason))
    }

    private fun internalLeave(reason: CallLeaveReason) = atomicLeave {
        monitorSubscriberPCStateJob?.cancel()
        monitorPublisherPCStateJob?.cancel()
        callAnalytics.stopObservers()
        monitorPublisherPCStateJob = null
        monitorSubscriberPCStateJob = null
        leaveTimeoutAfterDisconnect?.cancel()
        network.unsubscribe(listener)
        sfuListener?.cancel()
        sfuEvents?.cancel()
        state._connection.value = RealtimeConnection.Disconnected
        logger.v { "[leave] #ringing; call_id = $id" }
        if (isDestroyed) {
            logger.w { "[leave] #ringing; Call already destroyed, ignoring" }
            return@atomicLeave
        }
        isDestroyed = true

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
            val leaveReason = "[reason=${reason::class.simpleName}, message=${reason.message}]"
            callAnalytics.onCallLeave(reason)

            safeCall {
                session.value?.sfuTracer?.trace("leave-call", leaveReason)
                val stats = collectStats()
                session.value?.sendCallStats(stats)
            }
            // Must complete before cleanup() cancels the session's supervisor job.
            safeCall { session.value?.sendLeaveEvent(leaveReason) }
            cleanup()
        }
    }

    /** ends the call for yourself as well as other users */
    suspend fun end(): Result<Unit> {
        // end the call for everyone
        val result = clientImpl.endCall(type, id)
        // cleanup
        leave(
            CallLeaveReason.SdkDriven(
                cause = SdkCause.END_CALL,
                message = "CALL_ENDED", // Call ended by local user
            ),
        )
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

    fun setVisibility(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        viewportId: String = sessionId,
    ) {
        logger.i {
            "[setVisibility] #track; #sfu; viewportId: $viewportId, sessionId: $sessionId, trackType: $trackType, visible: $visible"
        }
        session.value?.updateTrackDimensions(
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
        session.value?.updateTrackDimensions(
            sessionId,
            trackType,
            visible,
            VideoDimension(width, height),
            viewportId,
        )
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
        viewportId: String = sessionId,
    ) {
        logger.d { "[initRenderer] #sfu; #track; sessionId: $sessionId" }

        // Note this comes from the shared eglBase
        videoRenderer.init(
            eglBase.eglBaseContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    val width = videoRenderer.measuredWidth
                    val height = videoRenderer.measuredHeight
                    logger.i {
                        "noob [initRenderer.onFirstFrameRendered] #sfu; #track; " +
                            "trackType: $trackType, dimension: ($width - $height), " +
                            "sessionId: $sessionId"
                    }
                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        session.value?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(width, height),
                            viewportId,
                        )
                    }
                    onRendered(videoRenderer)
                    callAnalytics.videoObserver.firstVideoFrameRendered(
                        trackType,
                        width,
                        height,
                        session.value,
                        sessionId,
                        this@Call.sessionId,
                    )
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
                            "viewport size: ($width - $height), " +
                            "video size: ($videoWidth - $videoHeight), " +
                            "sessionId: $sessionId"
                    }

                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        session.value?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(width, height),
                            viewportId,
                        )
                    }
                }
            },
        )
    }

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
        return startRecording(RecordingType.Composite)
    }
    suspend fun startRecording(recordingType: RecordingType): Result<Any> {
        return clientImpl.startRecording(type, id, recordingType = recordingType)
    }

    suspend fun stopRecording(): Result<Any> {
        return stopRecording(RecordingType.Composite)
    }

    suspend fun stopRecording(recordingType: RecordingType): Result<Any> {
        return clientImpl.stopRecording(type, id, recordingType)
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
    ) {
        if (state.ownCapabilities.value.contains(OwnCapability.Screenshare)) {
            session.value?.setScreenShareTrack()
            screenShare.enable(mediaProjectionPermissionResultData, includeAudio = includeAudio)
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

            speaker.setEnabled(enabled = enableSpeaker)
        }

        monitorHeadset()

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

    /** Adds the given SFU ID (edge name) to the failed set (for migrating_from_list). */
    private fun addFailedSfuId(sfuId: String) {
        if (sfuId.isBlank()) return
        failedSfuIds.add(sfuId)
    }

    /** Returns a snapshot of failed SFU IDs to send as migrating_from_list. */
    private fun getFailedSfuIdsSnapshot(): List<String> = failedSfuIds.toList()

    /** Clears the failed SFU list (e.g. after a successful join). */
    private fun clearFailedSfuIds() {
        failedSfuIds.clear()
    }

    /**
     * Called by [RtcSession] when connection to the SFU is established successfully.
     * Clears the failed SFU list so we don't exclude this SFU on future requests.
     */
    internal fun onSfuConnectionEstablished() {
        clearFailedSfuIds()
    }

    @VisibleForTesting
    internal suspend fun joinRequest(
        create: CreateCallOptions? = null,
        location: String,
        migratingFrom: String? = null,
        migratingFromList: List<String>? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        hintHighScaleLivestreamPublisher: Boolean? = null,
        telemetryModel: TelemetryModel,
    ): Result<JoinCallResponse> {
        callAnalytics.joinObserver.onJoinRequestStart(telemetryModel.joinReason)
        val migratingFromList = migratingFromList ?: getFailedSfuIdsSnapshot().takeIf { it.isNotEmpty() }
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
            callAnalytics.joinObserver.onJoinRequestSuccess(
                telemetryModel,
                it.call.currentSessionId,
            )
            state.updateFromResponse(it)
        }
        return result
    }

    fun cleanup() {
        // monitor.stop()
        state.cleanup()
        session.value?.cleanup()
        shutDownJobsGracefully()
        callStatsReportingJob?.cancel()
        mediaManager.cleanup() // TODO Rahul, Verify Later: need to check which call has owned the media at the moment(probably use active call)
        session.value = null
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

    suspend fun accept(): Result<AcceptCallResponse> {
        logger.d { "[accept] #ringing; no args, call_id:$id" }
        state.acceptedOnThisDevice = true

        clientImpl.state.transitionToAcceptCall(this)
        return clientImpl.accept(type, id)
    }

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

    fun isServerPin(sessionId: String): Boolean = state.pinManager.serverPins.value.containsKey(
        sessionId,
    )

    fun isLocalPin(sessionId: String): Boolean = state.pinManager.localPins.value.containsKey(
        sessionId,
    )

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
        session.value?.let { session ->
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
        session.value?.trackOverridesHandler?.updateOverrides(sessionIds, visible = enabled)
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
    fun setIncomingAudioEnabled(enabled: Boolean, sessionIds: List<String>? = null) {
        val participantTrackMap = session.value?.subscriber?.value?.tracks ?: return

        val targetTracks = when {
            sessionIds != null -> sessionIds.mapNotNull { participantTrackMap[it] }
            else -> participantTrackMap.values.toList()
        }

        targetTracks
            .mapNotNull { it[TrackType.TRACK_TYPE_AUDIO] as? AudioTrack }
            .forEach { it.enableAudio(enabled) }
    }

    @InternalStreamVideoApi
    public val debug = Debug(this)

    @InternalStreamVideoApi
    public class Debug(val call: Call) {

        public fun pause() {
            call.session.value?.subscriber?.value?.disable()
        }

        public fun resume() {
            call.session.value?.subscriber?.value?.enable()
        }

        public fun rejoin(reason: String = "Debug") {
            call.scope.launch {
                call.rejoin(reason)
            }
        }

        public fun restartSubscriberIce() {
            call.session.value?.subscriber?.value?.connection?.restartIce()
        }

        public fun restartPublisherIce() {
            call.session.value?.publisher?.value?.connection?.restartIce()
        }

        fun migrate() {
            call.scope.launch {
                call.migrate()
            }
        }

        fun simulateSfuFull() = debugOnly {
            call.session.value?.simulateSfuFull()
        }

        fun fastReconnect(reason: String = "Debug") {
            call.scope.launch {
                call.fastReconnect(reason)
            }
        }
    }

    companion object {
        /** How many consecutive FAST reconnect failures are allowed before
         *  escalating to a full REJOIN. Kept small because each failed FAST
         *  attempt can cost up to DEFAULT_SOCKET_TIMEOUT (10 s) waiting for
         *  the WebSocket handshake to time out. */
        private const val MAX_FAST_RECONNECT_ATTEMPTS = 3

        /** Absolute upper bound on loop iterations across all strategies
         *  (FAST + REJOIN + MIGRATE combined). Prevents infinite retries
         *  when every strategy keeps failing. */
        private const val MAX_RECONNECT_ATTEMPTS = 10

        /** Delay between consecutive reconnect attempts (both after a
         *  failed attempt and while polling for network availability
         *  during FAST reconnect). Kept short so the SDK reacts quickly
         *  once conditions improve. */
        private const val RECONNECT_DELAY_MS = 500L

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
