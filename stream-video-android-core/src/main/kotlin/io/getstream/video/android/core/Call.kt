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
import io.getstream.android.video.generated.models.UpdateCallMembersResponse
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.android.video.generated.models.UpdateUserPermissionsResponse
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.analytics.call.CallAnalytics
import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.audio.InputAudioFilter
import io.getstream.video.android.core.call.components.CallApiClient
import io.getstream.video.android.core.call.components.CallConnectivityMonitor
import io.getstream.video.android.core.call.components.CallEventManager
import io.getstream.video.android.core.call.components.CallIceConnectionMonitor
import io.getstream.video.android.core.call.components.CallJoinCoordinator
import io.getstream.video.android.core.call.components.CallLifecycleManager
import io.getstream.video.android.core.call.components.CallMediaManager
import io.getstream.video.android.core.call.components.CallReconnector
import io.getstream.video.android.core.call.components.CallRenderer
import io.getstream.video.android.core.call.components.CallSessionManager
import io.getstream.video.android.core.call.components.CallStatsReporter
import io.getstream.video.android.core.call.components.ClientCallRegistry
import io.getstream.video.android.core.call.components.MediaManagerFactory
import io.getstream.video.android.core.call.components.RtcSessionFactory
import io.getstream.video.android.core.call.components.SessionMonitor
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.call.scope.ScopeProvider
import io.getstream.video.android.core.call.scope.ScopeProviderImpl
import io.getstream.video.android.core.call.video.VideoFilter
import io.getstream.video.android.core.closedcaptions.ClosedCaptionsSettings
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
import io.getstream.video.android.core.recording.RecordingType
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.utils.debugOnly
import io.getstream.video.android.core.utils.runResultCatchingCancellable
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.model.User
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import org.webrtc.EglBase
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples
import stream.video.sfu.models.ClientCapability
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.concurrent.ConcurrentHashMap

@Deprecated(
    message = "No longer used internally. The reconnect deadline is now driven by the server's " +
        "fastReconnectDeadlineSeconds. This constant will be removed in a future release.",
    level = DeprecationLevel.WARNING,
)
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
    internal val clientImpl = client as StreamVideoClient
    internal val scopeProvider: ScopeProvider = ScopeProviderImpl(clientImpl.scope)

    private val logger by taggedLogger("Call:$type:$id")
    private val supervisorJob = SupervisorJob()
    internal var powerManager: PowerManager? = null

    internal val scope = CoroutineScope(clientImpl.scope.coroutineContext + supervisorJob)

    /** Delegate that owns the live RTC session state and reconnect bookkeeping. */
    private val sessionManager = CallSessionManager()

    /**
     * Session handles all real time communication for video and audio.
     *
     * This is the read path for collaborators outside the decomposition — `CallStats`,
     * `StreamVideoClient`, `ActiveStateGate`, the media session controller and [Debug]. Components
     * under `call.components` take [CallSessionManager] directly rather than reading it from here.
     */
    // TODO(v2): hand those consumers the CallSessionManager and drop this accessor. Blocked on
    //  binary compatibility today: CallStats' constructor is published ABI, and adding a parameter
    //  replaces it rather than overloading it (defaults are source-level only). CallSessionManager
    //  is also internal, so it cannot appear in a public signature at all.
    internal val session: StateFlow<RtcSession?> get() = sessionManager.session

    var sessionId: String
        get() = sessionManager.sessionId
        set(value) {
            sessionManager.sessionId = value
        }

    // Unit-test only hook for replacing RtcSession construction.
    // TODO(v2): replace this with a proper dependency injection boundary.
    internal var unitTestRtcSessionFactory: (() -> RtcSession)? = null

    /**
     * Creates [RtcSession] instances for join / rejoin / migrate. Captures `this` so the
     * session's Call dependency never leaks into the join/reconnect orchestrators, and hands it
     * the [CallSessionManager] directly so session identity and reconnect timings are read from
     * their owner rather than routed back through this facade.
     */
    private val sessionFactory = RtcSessionFactory {
            sessionId, sessionCounter, sfuUrl, sfuWsUrl, sfuToken, sfuName, iceServers ->
        unitTestRtcSessionFactory?.invoke() ?: RtcSession(
            client = clientImpl,
            sessionCounter = sessionCounter,
            powerManager = powerManager,
            call = this,
            sessionManager = sessionManager,
            sessionId = sessionId,
            apiKey = clientImpl.apiKey,
            lifecycle = clientImpl.coordinatorConnectionModule.lifecycle,
            sfuUrl = sfuUrl,
            sfuWsUrl = sfuWsUrl,
            sfuToken = sfuToken,
            sfuName = sfuName,
            remoteIceServers = iceServers,
            sfuAnalytics = callAnalytics.sfuAnalytics.apply {
                sfuAnalyticsStateHolder.updateSfuId(sfuName)
            },
        )
    }

    /**
     * The call's registration in the client-level ringing / active / telecom registries. Every
     * operation needs this instance, which the extracted components deliberately don't hold.
     */
    private val callRegistry = object : ClientCallRegistry {
        override fun markRinging() {
            clientImpl.state._ringingCall.value = this@Call
        }

        override fun registerOutgoingRing() {
            client.state.addRingingCall(this@Call, RingingState.Outgoing())
        }

        override fun markActive() {
            client.state.setActiveCall(this@Call)
        }

        override fun markAccepted() {
            clientImpl.state.transitionToAcceptCall(this@Call)
        }

        override fun detach() {
            if (id == client.state.activeCall.value?.id) {
                client.state.removeActiveCall(this@Call) // Will also stop CallService
            }
            if (id == client.state.ringingCall.value?.id) {
                client.state.removeRingingCall(this@Call)
            }
            TelecomCallController(client.context).leaveCall(this@Call)
            clientImpl.onCallCleanUp(this@Call)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Component graph. Declaration order is load-bearing: Kotlin initialises properties top to
    // bottom, so a component can only take a collaborator directly if that collaborator is
    // declared above it.
    //
    // A `() -> T` parameter below is never an ordering accident; it is one of three things:
    //  - a cycle: two components need each other, so the later one is injected as a provider
    //  - a deferral: `eglBase` stays lazy so the native EGL context is only created if used
    //  - a live read: mutable state such as `reconnectDeadlineMillis`, where the component needs
    //    the current value rather than a snapshot taken at construction
    // ---------------------------------------------------------------------------------------

    /**
     * EGL base context shared between peerConnectionFactory and mediaManager
     * to break circular dependency.
     */
    internal val eglBase: EglBase by lazy {
        EglBase.create()
    }

    /** Delegate that restarts ICE when the publisher/subscriber connections drop. */
    private val iceMonitor: CallIceConnectionMonitor =
        CallIceConnectionMonitor(type, id, scope, sessionManager)

    /** Delegate that owns the event flow, subscriptions and event dispatch. */
    private val eventManager = CallEventManager(type, id, scope, reconnector = { reconnector })

    // Must be initialized before `state` — CallState → SortedParticipantsState
    // launches a coroutine that reads `call.events` (leaking-this race).
    val events: MutableSharedFlow<VideoEvent> = eventManager.events

    /** The call state contains all state such as the participant list, reactions etc */
    val state = CallState(client, this, user, scope)

    internal val callAnalytics =
        CallAnalytics(
            clientImpl.context,
            this.id,
            this.type,
            state.me,
            state.connection,
            state.participants,
            client.state.clientEventReporter,
            scope,
        )

    /** Delegate that periodically collects and reports WebRTC stats. */
    private val statsReporter = CallStatsReporter(type, id, scope, sessionManager, state)

    /** Delegate that wraps all coordinator (REST) API calls for this call. */
    private val apiClient = CallApiClient(
        type = type,
        id = id,
        state = state,
        clientImpl = clientImpl,
        scope = scope,
        callSessionId = { sessionId },
        callRegistry = callRegistry,
        callAnalytics = callAnalytics,
        sessionManager = sessionManager,
    )

    /**
     * Creates [MediaManagerImpl] for this call. Captures `this` (and the test hook) so
     * [CallMediaManager] never needs a Call reference.
     */
    private val mediaManagerFactory = MediaManagerFactory { audioUsage, audioUsageProvider ->
        testInstanceProvider.mediaManagerCreator?.invoke()
            ?: MediaManagerImpl(
                clientImpl.context,
                this,
                scope,
                eglBase.eglBaseContext,
                audioUsage,
                audioUsageProvider,
            )
    }

    /** Delegate that owns the peer-connection factory, media manager and audio pipeline. */
    private val media = CallMediaManager(
        type = type,
        id = id,
        clientImpl = clientImpl,
        scope = scope,
        state = state,
        sessionManager = sessionManager,
        eglBase = { eglBase },
        mediaManagerFactory = mediaManagerFactory,
    )

    /** Delegate that owns leave / end / cleanup teardown and the destroyed flag. */
    private val lifecycle: CallLifecycleManager = CallLifecycleManager(
        clientImpl = clientImpl,
        sessionManager = sessionManager,
        scopeProvider = scopeProvider,
        callRegistry = callRegistry,
        // Lets the REST calls queued before leave finish before the scope is torn down.
        shutDownJobs = {
            UserScope(ClientScope()).launch {
                supervisorJob.children.forEach { it.join() }
                supervisorJob.cancel()
            }
            scope.cancel()
        },
        state = state,
        callAnalytics = callAnalytics,
        statsReporter = statsReporter,
        media = media,
        iceMonitor = iceMonitor,
        sessionMonitor = { sessionMonitor },
        connectivityMonitor = { connectivityMonitor },
        type = type,
        id = id,
    )

    /** Delegate that owns the unified reconnect state machine (fast / rejoin / migrate). */
    private val reconnector: CallReconnector = CallReconnector(
        clientImpl = clientImpl,
        sessionManager = sessionManager,
        sessionFactory = sessionFactory,
        lifecycle = lifecycle,
        apiClient = apiClient,
        state = state,
        callAnalytics = callAnalytics,
        statsReporter = statsReporter,
        sessionMonitor = { sessionMonitor },
        type = type,
        id = id,
    )

    /** Delegate that reacts to device connectivity changes (reconnect / leave-on-timeout). */
    private val connectivityMonitor: CallConnectivityMonitor = CallConnectivityMonitor(
        type = type,
        id = id,
        clientImpl = clientImpl,
        scope = scope,
        state = state,
        reconnector = reconnector,
        lifecycle = lifecycle,
        reconnectDeadlineMillis = { reconnectDeadlineMillis },
    )

    /** Delegate that owns the SFU signal/event observers and (re)wires per-session monitoring. */
    private val sessionMonitor: SessionMonitor = SessionMonitor(
        type = type,
        id = id,
        scope = scope,
        state = state,
        sessionManager = sessionManager,
        statsReporter = statsReporter,
        iceMonitor = iceMonitor,
        connectivityMonitor = connectivityMonitor,
        callAnalytics = callAnalytics,
    )

    /** Camera gives you access to the local camera */
    val camera get() = mediaManager.camera
    val microphone get() = mediaManager.microphone
    val speaker get() = mediaManager.speaker
    val screenShare get() = mediaManager.screenShare

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

    /**
     * This returns the local microphone volume level. The audio volume is a linear
     * value between 0 (no sound) and 1 (maximum volume). This is not a raw output -
     * it is a smoothed-out volume level that gradually goes to the highest measured level
     * and will then gradually over 250ms return back to 0 or next measured value. This value
     * can be used directly in your UI for displaying a volume/speaking indicator for the local
     * participant.
     * Note: Doesn't return any values until the session is established!
     */
    val localMicrophoneAudioLevel: StateFlow<Float> get() = media.localMicrophoneAudioLevel

    /**
     * Contains stats events for observation.
     */
    val statsReport: MutableStateFlow<CallStatsReport?> get() = statsReporter.statsReport

    /**
     * Contains stats history.
     */
    val statLatencyHistory: MutableStateFlow<List<Int>> get() = statsReporter.statLatencyHistory

    /**
     * Call has been left and the object is cleaned up and destroyed.
     */
    internal val isDestroyed: Boolean get() = lifecycle.isDestroyed

    internal var peerConnectionFactory: StreamPeerConnectionFactory
        get() = media.peerConnectionFactory
        set(value) {
            media.peerConnectionFactory = value
        }

    /** Delegate that binds video tracks to renderers and handles media-quality overrides. */
    private val callRenderer = CallRenderer(
        type = type,
        id = id,
        scope = scope,
        sessionManager = sessionManager,
        callAnalytics = callAnalytics,
        eglBase = { eglBase },
        callSessionId = { sessionId },
    )

    /**
     * Checks if the audioBitrateProfile has changed since the factory was created,
     * and recreates the factory if needed. This should only be called before joining.
     *
     * If the factory hasn't been created yet, it will be created with the current profile
     * when first accessed, so no recreation is needed.
     */
    internal fun ensureFactoryMatchesAudioProfile() = media.ensureFactoryMatchesAudioProfile()

    internal val clientCapabilities = ConcurrentHashMap<String, ClientCapability>().apply {
        put(
            ClientCapability.CLIENT_CAPABILITY_SUBSCRIBER_VIDEO_PAUSE.name,
            ClientCapability.CLIENT_CAPABILITY_SUBSCRIBER_VIDEO_PAUSE,
        )
    }

    internal val mediaManager get() = media.mediaManager

    /** Delegate that drives the join flow (permissions, retry loop, session creation). */
    private val joinCoordinator = CallJoinCoordinator(
        clientImpl = clientImpl,
        state = state,
        callAnalytics = callAnalytics,
        type = type,
        id = id,
        scope = scope,
        sessionManager = sessionManager,
        sessionFactory = sessionFactory,
        media = media,
        lifecycle = lifecycle,
        apiClient = apiClient,
        reconnector = reconnector,
        sessionMonitor = sessionMonitor,
        callRegistry = callRegistry,
        hasRequiredPermissions = {
            clientImpl.permissionCheck
                .checkAndroidPermissionsGroup(clientImpl.context, this@Call).first
        },
    )

    internal var reconnectDeadlineMillis: Int
        get() = sessionManager.reconnectDeadlineMillis
        set(value) {
            sessionManager.reconnectDeadlineMillis = value
        }

    init {
        media.startAudioLevelMonitoring()
        powerManager = safeCallWithDefault(null) {
            clientImpl.context.getSystemService(POWER_SERVICE) as? PowerManager
        }
    }

    /** Basic crud operations */
    suspend fun get(): Result<GetCallResponse> = runResultCatchingCancellable { apiClient.get() }

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
    ): Result<GetOrCreateCallResponse> = runResultCatchingCancellable {
        apiClient.create(
            memberIds = memberIds,
            members = members,
            custom = custom,
            settings = settings,
            startsAt = startsAt,
            team = team,
            ring = ring,
            notify = notify,
            video = video,
        )
    }

    /** Update a call */
    suspend fun update(
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
    ): Result<UpdateCallResponse> = runResultCatchingCancellable {
        apiClient.update(custom, settingsOverride, startsAt)
    }

    suspend fun join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        hintHighScaleLivestreamPublisher: Boolean? = null,
        callJoinInterceptor: CallJoinInterceptor? = null,
    ): Result<RtcSession> = runResultCatchingCancellable {
        joinCoordinator.join(
            create,
            createOptions,
            ring,
            notify,
            hintHighScaleLivestreamPublisher,
            callJoinInterceptor,
        )
    }

    suspend fun joinAndRing(
        members: List<String>,
        createOptions: CreateCallOptions? = CreateCallOptions(members),
        video: Boolean = isVideoEnabled(),
        callJoinInterceptor: CallJoinInterceptor? = null,
    ): Result<RtcSession> = runResultCatchingCancellable {
        joinCoordinator.joinAndRing(
            members,
            createOptions,
            video,
            callJoinInterceptor,
        )
    }

    internal fun isPermanentError(error: Any): Boolean = joinCoordinator.isPermanentError(error)

    internal suspend fun _join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        hintHighScaleLivestreamPublisher: Boolean? = null,
        joinAnalyticsModel: JoinAnalyticsModel,
    ): Result<RtcSession> = joinCoordinator.joinInternal(
        create,
        createOptions,
        ring,
        notify,
        hintHighScaleLivestreamPublisher,
        joinAnalyticsModel,
    )

    /** Resets the leave guard so a fresh join can run after a previous leave. */
    internal fun resetLeaveGuard() = lifecycle.resetLeaveGuard()

    /** Applies server-provided call settings to the local media manager. */
    internal fun updateMediaManagerFromSettings(callSettings: CallSettingsResponse) =
        media.updateMediaManagerFromSettings(callSettings)

    internal suspend fun collectStats(): CallStatsReport = statsReporter.collectStats()

    // region Reconnection — unified loop

    /**
     * Unified reconnection entry point. Delegates to [CallReconnector], which owns the
     * FAST / REJOIN / MIGRATE state machine and the single-flight reconnect mutex.
     */
    internal suspend fun reconnect(
        strategy: WebsocketReconnectStrategy,
        reason: String,
    ) = reconnector.reconnect(strategy, reason)

    // Keep public wrappers for backward compatibility and Debug class
    suspend fun fastReconnect(reason: String = "unknown") = reconnector.fastReconnect(reason)

    suspend fun rejoin(reason: String = "unknown") = reconnector.rejoin(reason)

    suspend fun migrate() = reconnector.migrate()

    // endregion

    @InternalStreamVideoApi
    fun leave(reason: CallLeaveReason) = lifecycle.leave(reason)

    fun leave(reason: String = "user") = lifecycle.leave(reason)

    /** ends the call for yourself as well as other users */
    suspend fun end(): Result<Unit> = runResultCatchingCancellable { lifecycle.end() }

    suspend fun pinForEveryone(sessionId: String, userId: String): Result<PinResponse> =
        runResultCatchingCancellable { apiClient.pinForEveryone(sessionId, userId) }

    suspend fun unpinForEveryone(sessionId: String, userId: String): Result<UnpinResponse> =
        runResultCatchingCancellable { apiClient.unpinForEveryone(sessionId, userId) }

    suspend fun sendReaction(
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null,
    ): Result<SendReactionResponse> = runResultCatchingCancellable {
        apiClient.sendReaction(type, emoji, custom)
    }

    suspend fun queryMembers(
        filter: Map<String, Any>,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        limit: Int = 25,
        prev: String? = null,
        next: String? = null,
    ): Result<QueriedMembers> = runResultCatchingCancellable {
        apiClient.queryMembers(filter, sort, limit, prev, next)
    }

    suspend fun muteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> = runResultCatchingCancellable {
        apiClient.muteAllUsers(audio, video, screenShare)
    }

    fun setVisibility(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        viewportId: String = sessionId,
    ) = callRenderer.setVisibility(sessionId, trackType, visible, viewportId)

    fun setVisibility(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        viewportId: String = sessionId,
        width: Int,
        height: Int,
    ) = callRenderer.setVisibility(sessionId, trackType, visible, viewportId, width, height)

    fun handleEvent(event: VideoEvent) = eventManager.handleEvent(event)

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
    ) = callRenderer.initRenderer(videoRenderer, sessionId, trackType, onRendered, viewportId)

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
    ): Result<GoLiveResponse> = runResultCatchingCancellable {
        apiClient.goLive(startHls, startRecording, startTranscription)
    }

    suspend fun stopLive(): Result<StopLiveResponse> = runResultCatchingCancellable {
        apiClient.stopLive()
    }

    suspend fun sendCustomEvent(data: Map<String, Any>): Result<SendCallEventResponse> =
        runResultCatchingCancellable { apiClient.sendCustomEvent(data) }

    /** Permissions */
    suspend fun requestPermissions(vararg permission: String): Result<Unit> =
        runResultCatchingCancellable { apiClient.requestPermissions(*permission) }

    suspend fun startRecording(): Result<Any> {
        return startRecording(RecordingType.Composite)
    }
    suspend fun startRecording(recordingType: RecordingType): Result<Any> =
        runResultCatchingCancellable { apiClient.startRecording(recordingType) }

    suspend fun stopRecording(): Result<Any> {
        return stopRecording(RecordingType.Composite)
    }

    suspend fun stopRecording(recordingType: RecordingType): Result<Any> =
        runResultCatchingCancellable { apiClient.stopRecording(recordingType) }

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
    ) = media.startScreenSharing(mediaProjectionPermissionResultData, includeAudio)

    fun stopScreenSharing() = media.stopScreenSharing()

    suspend fun startHLS(): Result<Any> = runResultCatchingCancellable { apiClient.startHLS() }

    suspend fun stopHLS(): Result<Any> = runResultCatchingCancellable { apiClient.stopHLS() }

    public fun subscribeFor(
        vararg eventTypes: Class<out VideoEvent>,
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription = eventManager.subscribeFor(*eventTypes, listener = listener)

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Deprecated in favor of the `events` flow.",
        replaceWith = ReplaceWith("events.collect { }"),
    )
    public fun subscribe(
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription = eventManager.subscribe(listener)

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Deprecated in favor of the `events` flow.",
        replaceWith = ReplaceWith("events.collect { }"),
    )
    public fun unsubscribe(eventSubscription: EventSubscription) =
        eventManager.unsubscribe(eventSubscription)

    public suspend fun blockUser(userId: String): Result<BlockUserResponse> =
        runResultCatchingCancellable { apiClient.blockUser(userId) }

    // TODO: add removeMember (single)

    public suspend fun removeMembers(userIds: List<String>): Result<UpdateCallMembersResponse> =
        runResultCatchingCancellable { apiClient.removeMembers(userIds) }

    public suspend fun grantPermissions(
        userId: String,
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse> = runResultCatchingCancellable {
        apiClient.grantPermissions(userId, permissions)
    }

    public suspend fun revokePermissions(
        userId: String,
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse> = runResultCatchingCancellable {
        apiClient.revokePermissions(userId, permissions)
    }

    public suspend fun updateMembers(memberRequests: List<MemberRequest>): Result<UpdateCallMembersResponse> =
        runResultCatchingCancellable { apiClient.updateMembers(memberRequests) }

    fun fireEvent(event: VideoEvent) = eventManager.fireEvent(event)

    /**
     * List the recordings for this call.
     *
     * @param sessionId - if session ID is supplied, only recordings for that session will be loaded.
     */
    suspend fun listRecordings(sessionId: String? = null): Result<ListRecordingsResponse> =
        runResultCatchingCancellable { apiClient.listRecordings(sessionId) }

    /**
     * Kick a user from the call.
     *
     * @param userId - the user to kick
     * @param block - if true, the user will be blocked from rejoining the call
     */
    suspend fun kickUser(
        userId: String,
        block: Boolean = false,
    ): Result<KickUserResponse> = runResultCatchingCancellable { apiClient.kickUser(userId, block) }

    suspend fun muteUser(
        userId: String,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> = runResultCatchingCancellable {
        apiClient.muteUser(userId, audio, video, screenShare)
    }

    suspend fun muteUsers(
        userIds: List<String>,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> = runResultCatchingCancellable {
        apiClient.muteUsers(userIds, audio, video, screenShare)
    }

    /**
     * Called by [RtcSession] when connection to the SFU is established successfully.
     * Clears the failed SFU list so we don't exclude this SFU on future requests.
     */
    internal fun onSfuConnectionEstablished() {
        sessionManager.clearFailedSfuIds()
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
        joinAnalyticsModel: JoinAnalyticsModel,
    ): Result<JoinCallResponse> = apiClient.joinRequest(
        create,
        location,
        migratingFrom,
        migratingFromList,
        ring,
        notify,
        hintHighScaleLivestreamPublisher,
        joinAnalyticsModel,
    )

    fun cleanup() = lifecycle.cleanup()

    // This will allow the Rest APIs to be executed which are in queue before leave
    internal fun shutDownJobsGracefully() {
        UserScope(ClientScope()).launch {
            supervisorJob.children.forEach { it.join() }
            supervisorJob.cancel()
        }
        scope.cancel()
    }

    suspend fun ring(): Result<GetCallResponse> = runResultCatchingCancellable { apiClient.ring() }

    suspend fun ring(ringCallRequest: RingCallRequest): Result<RingCallResponse> =
        runResultCatchingCancellable { apiClient.ring(ringCallRequest) }

    suspend fun notify(): Result<GetCallResponse> = runResultCatchingCancellable { apiClient.notify() }

    suspend fun accept(): Result<AcceptCallResponse> = runResultCatchingCancellable {
        apiClient.accept()
    }

    /**
     * Should outlive both the call scope and the service scope and needs to be executed in the client-level scope.
     * Because the call scope or service scope may be cancelled or finished while the network request is still in flight
     * TODO: Run this in clientImpl.scope internally
     */
    suspend fun reject(reason: RejectReason? = null): Result<RejectCallResponse> =
        runResultCatchingCancellable { apiClient.reject(reason) }

    // For debugging
    internal suspend fun reject(
        source: String = "n/a",
        reason: RejectReason? = null,
    ): Result<RejectCallResponse> {
        logger.d { "[reject] source: $source" }
        return reject(reason)
    }

    fun processAudioSample(audioSample: AudioSamples) = media.processAudioSample(audioSample)

    fun collectUserFeedback(
        rating: Int,
        reason: String? = null,
        custom: Map<String, Any>? = null,
    ) = apiClient.collectUserFeedback(rating, reason, custom)

    suspend fun takeScreenshot(track: VideoTrack): Bitmap? = callRenderer.takeScreenshot(track)

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

    fun isAudioProcessingEnabled(): Boolean = media.isAudioProcessingEnabled()

    fun setAudioProcessingEnabled(enabled: Boolean) = media.setAudioProcessingEnabled(enabled)

    fun toggleAudioProcessing(): Boolean = media.toggleAudioProcessing()

    suspend fun startTranscription(): Result<StartTranscriptionResponse> =
        runResultCatchingCancellable { apiClient.startTranscription() }

    suspend fun stopTranscription(): Result<StopTranscriptionResponse> =
        runResultCatchingCancellable { apiClient.stopTranscription() }

    suspend fun listTranscription(): Result<ListTranscriptionsResponse> =
        runResultCatchingCancellable { apiClient.listTranscription() }

    suspend fun startClosedCaptions(): Result<io.getstream.android.video.generated.models.StartClosedCaptionsResponse> =
        runResultCatchingCancellable { apiClient.startClosedCaptions() }

    suspend fun stopClosedCaptions(): Result<io.getstream.android.video.generated.models.StopClosedCaptionsResponse> =
        runResultCatchingCancellable { apiClient.stopClosedCaptions() }

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
    ) = callRenderer.setPreferredIncomingVideoResolution(resolution, sessionIds)

    /**
     * Enables/disables incoming video feed.
     *
     * @param enabled Whether the video feed should be enabled or disabled. Set to `null` to switch back to auto.
     * @param sessionIds The participant session IDs to enable/disable the video feed for. If `null`, the setting will be applied to all participants.
     */
    fun setIncomingVideoEnabled(enabled: Boolean?, sessionIds: List<String>? = null) =
        callRenderer.setIncomingVideoEnabled(enabled, sessionIds)

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
        callRenderer.setIncomingAudioEnabled(enabled, sessionIds)

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
        internal var testInstanceProvider = TestInstanceProvider()

        internal class TestInstanceProvider {
            var mediaManagerCreator: (() -> MediaManagerImpl)? = null
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
