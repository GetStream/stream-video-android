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
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.call.scope.ScopeProvider
import io.getstream.video.android.core.call.scope.ScopeProviderImpl
import io.getstream.video.android.core.call.video.VideoFilter
import io.getstream.video.android.core.closedcaptions.ClosedCaptionsSettings
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.recording.RecordingType
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.utils.debugOnly
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.model.User
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    private val sessionManager = CallSessionManager(this)

    /** Session handles all real time communication for video and audio */
    internal val session: MutableStateFlow<RtcSession?> get() = sessionManager.session

    var sessionId: String
        get() = sessionManager.sessionId
        set(value) {
            sessionManager.sessionId = value
        }
    internal val unifiedSessionId: String get() = sessionManager.unifiedSessionId

    internal var location: String?
        get() = sessionManager.location
        set(value) {
            sessionManager.location = value
        }

    /**
     * Increment this only for REJOIN and MIGRATION strategies
     */
    internal var nonFastReconnectAttempts: Int
        get() = sessionManager.nonFastReconnectAttempts
        set(value) {
            sessionManager.nonFastReconnectAttempts = value
        }

    internal var connectStartTime: Long
        get() = sessionManager.connectStartTime
        set(value) {
            sessionManager.connectStartTime = value
        }
    internal var reconnectStartTime: Long
        get() = sessionManager.reconnectStartTime
        set(value) {
            sessionManager.reconnectStartTime = value
        }

    /** Delegate that owns the event flow, subscriptions and event dispatch. */
    private val eventManager = CallEventManager(this)

    // Must be initialized before `state` — CallState → SortedParticipantsState
    // launches a coroutine that reads `call.events` (leaking-this race).
    val events: MutableSharedFlow<VideoEvent> = eventManager.events

    /** The call state contains all state such as the participant list, reactions etc */
    val state = CallState(client, this, user, scope)

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

    /**
     * EGL base context shared between peerConnectionFactory and mediaManager
     * to break circular dependency.
     */
    internal val eglBase: EglBase by lazy {
        EglBase.create()
    }

    internal var peerConnectionFactory: StreamPeerConnectionFactory
        get() = media.peerConnectionFactory
        set(value) {
            media.peerConnectionFactory = value
        }

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

    /** Delegate that wraps all coordinator (REST) API calls for this call. */
    private val apiClient by lazy { CallApiClient(this) }

    /** Delegate that periodically collects and reports WebRTC stats. */
    private val statsReporter by lazy { CallStatsReporter(this) }

    /** Delegate that binds video tracks to renderers and handles media-quality overrides. */
    private val callRenderer by lazy { CallRenderer(this) }

    /** Delegate that owns the peer-connection factory, media manager and audio pipeline. */
    private val media = CallMediaManager(this)

    /**
     * Checks if the audioBitrateProfile has changed since the factory was created,
     * and recreates the factory if needed. This should only be called before joining.
     *
     * If the factory hasn't been created yet, it will be created with the current profile
     * when first accessed, so no recreation is needed.
     */
    internal fun ensureFactoryMatchesAudioProfile() = media.ensureFactoryMatchesAudioProfile()

    /**
     * Recreates peerConnectionFactory, audioSource, audioTrack, videoSource and videoTrack
     * with the current audioBitrateProfile. This should only be called before the call is joined.
     */
    internal fun recreateFactoryAndAudioTracks() = media.recreateFactoryAndAudioTracks()

    /**
     * Recreates peerConnectionFactory with the current audioBitrateProfile.
     * This should only be called before the call is joined.
     */
    internal fun recreatePeerConnectionFactory() = media.recreatePeerConnectionFactory()

    internal val clientCapabilities = ConcurrentHashMap<String, ClientCapability>().apply {
        put(
            ClientCapability.CLIENT_CAPABILITY_SUBSCRIBER_VIDEO_PAUSE.name,
            ClientCapability.CLIENT_CAPABILITY_SUBSCRIBER_VIDEO_PAUSE,
        )
    }

    internal val mediaManager get() = media.mediaManager

    /** Delegate that reacts to device connectivity changes (reconnect / leave-on-timeout). */
    private val connectivityMonitor = CallConnectivityMonitor(this)

    /** Delegate that drives the join flow (permissions, retry loop, session creation). */
    private val joinCoordinator = CallJoinCoordinator(this)

    internal var reconnectDeadlineMillis: Int = 10_000

    /** Delegate that owns the unified reconnect state machine (fast / rejoin / migrate). */
    private val reconnector = CallReconnector(this)

    /** Delegate that owns leave / end / cleanup teardown and the destroyed flag. */
    private val lifecycle = CallLifecycleManager(this)

    /** Returns whether the device currently has network connectivity. */
    internal fun isNetworkConnected(): Boolean = connectivityMonitor.isConnected()

    /** Stops the ICE and connectivity monitors (used during teardown). */
    internal fun stopConnectionMonitors() {
        iceMonitor.stop()
        connectivityMonitor.cancelLeaveTimeout()
        connectivityMonitor.unsubscribe()
    }

    /** Stops periodic WebRTC stats reporting (used during teardown). */
    internal fun stopStatsReporting() {
        statsReporter.stop()
    }

    private var sfuListener: Job? = null
    private var sfuEvents: Job? = null

    /** Delegate that restarts ICE when the publisher/subscriber connections drop. */
    private val iceMonitor = CallIceConnectionMonitor(this)

    init {
        media.startAudioLevelMonitoring()
        powerManager = safeCallWithDefault(null) {
            clientImpl.context.getSystemService(POWER_SERVICE) as? PowerManager
        }
    }

    /** Basic crud operations */
    suspend fun get(): Result<GetCallResponse> = apiClient.get()

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
    ): Result<GetOrCreateCallResponse> = apiClient.create(
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

    /** Update a call */
    suspend fun update(
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
    ): Result<UpdateCallResponse> = apiClient.update(custom, settingsOverride, startsAt)

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

        return joinCoordinator.join(
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
    ): Result<RtcSession> = joinCoordinator.joinAndRing(
        members,
        createOptions,
        video,
        callJoinInterceptor,
    )

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

    /** Cancels the SFU socket observers (signal WS + fast-reconnect deadline listener). */
    internal fun cancelSfuObservers() {
        sfuEvents?.cancel()
        sfuListener?.cancel()
    }

    /** Resets the leave guard so a fresh join can run after a previous leave. */
    internal fun resetLeaveGuard() = lifecycle.resetLeaveGuard()

    /** Applies server-provided call settings to the local media manager. */
    internal fun updateMediaManagerFromSettings(callSettings: CallSettingsResponse) =
        media.updateMediaManagerFromSettings(callSettings)

    internal fun monitorSession(result: JoinCallResponse) {
        sfuEvents?.cancel()
        sfuListener?.cancel()
        statsReporter.start(result.statsOptions.reportingIntervalMs.toLong())
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
        callAnalytics.peerConnectionAnalytics.stopAndObservePeerConnections(session)
        callAnalytics.audioAnalytics.observeFirstRemoteParticipantAudioMuteState(
            session,
            state.participants,
        )
        iceMonitor.start()
        connectivityMonitor.subscribe()
    }

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
    suspend fun end(): Result<Unit> = lifecycle.end()

    suspend fun pinForEveryone(sessionId: String, userId: String): Result<PinResponse> =
        apiClient.pinForEveryone(sessionId, userId)

    suspend fun unpinForEveryone(sessionId: String, userId: String): Result<UnpinResponse> =
        apiClient.unpinForEveryone(sessionId, userId)

    suspend fun sendReaction(
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null,
    ): Result<SendReactionResponse> = apiClient.sendReaction(type, emoji, custom)

    suspend fun queryMembers(
        filter: Map<String, Any>,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        limit: Int = 25,
        prev: String? = null,
        next: String? = null,
    ): Result<QueriedMembers> = apiClient.queryMembers(filter, sort, limit, prev, next)

    suspend fun muteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> = apiClient.muteAllUsers(audio, video, screenShare)

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
    ): Result<GoLiveResponse> = apiClient.goLive(startHls, startRecording, startTranscription)

    suspend fun stopLive(): Result<StopLiveResponse> = apiClient.stopLive()

    suspend fun sendCustomEvent(data: Map<String, Any>): Result<SendCallEventResponse> =
        apiClient.sendCustomEvent(data)

    /** Permissions */
    suspend fun requestPermissions(vararg permission: String): Result<Unit> =
        apiClient.requestPermissions(*permission)

    suspend fun startRecording(): Result<Any> {
        return startRecording(RecordingType.Composite)
    }
    suspend fun startRecording(recordingType: RecordingType): Result<Any> =
        apiClient.startRecording(recordingType)

    suspend fun stopRecording(): Result<Any> {
        return stopRecording(RecordingType.Composite)
    }

    suspend fun stopRecording(recordingType: RecordingType): Result<Any> =
        apiClient.stopRecording(recordingType)

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

    suspend fun startHLS(): Result<Any> = apiClient.startHLS()

    suspend fun stopHLS(): Result<Any> = apiClient.stopHLS()

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
        apiClient.blockUser(userId)

    // TODO: add removeMember (single)

    public suspend fun removeMembers(userIds: List<String>): Result<UpdateCallMembersResponse> =
        apiClient.removeMembers(userIds)

    public suspend fun grantPermissions(
        userId: String,
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse> = apiClient.grantPermissions(userId, permissions)

    public suspend fun revokePermissions(
        userId: String,
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse> = apiClient.revokePermissions(userId, permissions)

    public suspend fun updateMembers(memberRequests: List<MemberRequest>): Result<UpdateCallMembersResponse> =
        apiClient.updateMembers(memberRequests)

    fun fireEvent(event: VideoEvent) = eventManager.fireEvent(event)

    /**
     * List the recordings for this call.
     *
     * @param sessionId - if session ID is supplied, only recordings for that session will be loaded.
     */
    suspend fun listRecordings(sessionId: String? = null): Result<ListRecordingsResponse> =
        apiClient.listRecordings(sessionId)

    /**
     * Kick a user from the call.
     *
     * @param userId - the user to kick
     * @param block - if true, the user will be blocked from rejoining the call
     */
    suspend fun kickUser(
        userId: String,
        block: Boolean = false,
    ): Result<KickUserResponse> = apiClient.kickUser(userId, block)

    suspend fun muteUser(
        userId: String,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> = apiClient.muteUser(userId, audio, video, screenShare)

    suspend fun muteUsers(
        userIds: List<String>,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse> = apiClient.muteUsers(userIds, audio, video, screenShare)

    /** Returns a snapshot of failed SFU IDs to send as migrating_from_list. */
    internal fun getFailedSfuIdsSnapshot(): List<String> = reconnector.getFailedSfuIdsSnapshot()

    /**
     * Called by [RtcSession] when connection to the SFU is established successfully.
     * Clears the failed SFU list so we don't exclude this SFU on future requests.
     */
    internal fun onSfuConnectionEstablished() {
        reconnector.clearFailedSfuIds()
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
    ): Result<JoinCallResponse> = joinCoordinator.joinRequest(
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

    suspend fun ring(): Result<GetCallResponse> = apiClient.ring()

    suspend fun ring(ringCallRequest: RingCallRequest): Result<RingCallResponse> =
        apiClient.ring(ringCallRequest)

    suspend fun notify(): Result<GetCallResponse> = apiClient.notify()

    suspend fun accept(): Result<AcceptCallResponse> = apiClient.accept()

    /**
     * Should outlive both the call scope and the service scope and needs to be executed in the client-level scope.
     * Because the call scope or service scope may be cancelled or finished while the network request is still in flight
     * TODO: Run this in clientImpl.scope internally
     */
    suspend fun reject(reason: RejectReason? = null): Result<RejectCallResponse> =
        apiClient.reject(reason)

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
        apiClient.startTranscription()

    suspend fun stopTranscription(): Result<StopTranscriptionResponse> =
        apiClient.stopTranscription()

    suspend fun listTranscription(): Result<ListTranscriptionsResponse> =
        apiClient.listTranscription()

    suspend fun startClosedCaptions(): Result<io.getstream.android.video.generated.models.StartClosedCaptionsResponse> =
        apiClient.startClosedCaptions()

    suspend fun stopClosedCaptions(): Result<io.getstream.android.video.generated.models.StopClosedCaptionsResponse> =
        apiClient.stopClosedCaptions()

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
