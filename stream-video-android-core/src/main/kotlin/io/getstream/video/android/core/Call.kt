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
import io.getstream.result.Result
import io.getstream.result.flatMap
import io.getstream.video.android.core.call.CallApiDelegate
import io.getstream.video.android.core.call.CallEventManager
import io.getstream.video.android.core.call.CallLifecycleManager
import io.getstream.video.android.core.call.CallMediaManager
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
import io.getstream.video.android.core.events.GoAwayEvent
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.RampValueUpAndDownHelper
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.model.User
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.threeten.bp.OffsetDateTime
import org.webrtc.EglBase
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples
import stream.video.sfu.models.ClientCapability
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

    internal val clientImpl = client as StreamVideoClient
    internal val scopeProvider: ScopeProvider = ScopeProviderImpl(clientImpl.scope)

    // Atomic controls
    internal var atomicLeave = AtomicUnitCall()

    private val logger by taggedLogger("Call:$type:$id")

    /**
     * The coroutine scope for this Call.
     * Gets the current scope, which may be recreated after leave() and rejoin.
     *
     * THREAD SAFETY: Safe to access from any thread due to @Volatile on currentScope.
     */
    internal val scope: CoroutineScope
        get() = currentScope

    /**
     * The supervisor job for this Call's scope.
     * Gets the current supervisor job, which may be recreated after leave() and rejoin.
     *
     * THREAD SAFETY: Safe to access from any thread due to @Volatile on currentSupervisorJob.
     */
    private val supervisorJob: Job
        get() = currentSupervisorJob
    private var powerManager: PowerManager? = null

    /**
     * Session manager handles RTC sessions.
     * INTERNAL: Not part of public API.
     */
    private val sessionManager = CallSessionManager(
        call = this,
        clientImpl = clientImpl,
        powerManager = powerManager,
    )

    private val callRenderer = CallRenderer()

    /**
     * API delegate for backend calls.
     * INTERNAL: Not part of public API.
     */
    private val apiDelegate = CallApiDelegate(
        clientImpl = clientImpl,
        type = type,
        id = id,
        call = this,
        screenShareProvider = { screenShare },
        setScreenTrackCallBack = { session?.setScreenShareTrack() },
    )

    /** The call state contains all state such as the participant list, reactions etc */
    val state = CallState(client, this, user, scope)

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
     * Call has been left and the object is cleaned up and destroyed.
     */
    internal var isDestroyed = false

    /** Session handles all real time communication for video and audio */
    internal var session: RtcSession? = null
    var sessionId = UUID.randomUUID().toString()
    internal val unifiedSessionId = UUID.randomUUID().toString()

    internal var connectStartTime = 0L
    internal var reconnectStartTime = 0L

// ============================================================================
// NEW: Thread-safe state management with Mutex
// ============================================================================

    /**
     * Mutex for thread-safe access to cleanup state.
     * Used to protect access to cleanupJob and hasBeenLeft.
     *
     * Using Mutex instead of synchronized because:
     * - Mutex is suspendable (doesn't block threads)
     * - Better for coroutine-based code
     * - More efficient in coroutine contexts
     *
     * CRITICAL: Always use mutex.withLock { } and keep critical sections minimal.
     * Never perform blocking operations inside withLock { }.
     */
    internal val cleanupMutex = Mutex()

    /**
     * Tracks the cleanup job so join() can wait for it to complete.
     * Cleanup happens in background but runs synchronously within the job.
     *
     * THREAD SAFETY: Access must be protected using cleanupMutex.withLock { }.
     * Reading/writing this field outside mutex is NOT safe.
     */
    internal var cleanupJob: Job? = null

    /**
     * Indicates whether this Call has been left at least once.
     * Used to determine if reinitialization is needed on next join().
     *
     * THREAD SAFETY: Access must be protected using cleanupMutex.withLock { }.
     * Reading/writing this field outside mutex is NOT safe.
     */
    internal var hasBeenLeft = false

    /**
     * Current supervisor job for this Call's coroutine scope.
     * Recreated after leave() to allow rejoin.
     *
     * THREAD SAFETY: This is safe to access without mutex because:
     * - It's only modified during reinitialization (which is already synchronized)
     * - The scope property getter provides safe access
     */
    @Volatile
    private var currentSupervisorJob: Job = SupervisorJob()

    /**
     * Current coroutine scope for this Call.
     * Recreated after leave() to allow rejoin.
     *
     * THREAD SAFETY: This is safe to access without mutex because:
     * - It's only modified during reinitialization (which is already synchronized)
     * - The scope property getter provides safe access
     */
    @Volatile
    private var currentScope: CoroutineScope =
        CoroutineScope(clientImpl.scope.coroutineContext + currentSupervisorJob)

    /**
     * EGL base context shared between peerConnectionFactory and mediaManager
     * to break circular dependency.
     */
    internal val eglBase: EglBase by lazy {
        EglBase.create()
    }
    val events = MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)
    internal val callEventManager = CallEventManager(events, { subscriptions })

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

    /**
     * Checks if the audioBitrateProfile has changed since the factory was created,
     * and recreates the factory if needed. This should only be called before joining.
     *
     * If the factory hasn't been created yet, it will be created with the current profile
     * when first accessed, so no recreation is needed.
     */
    internal fun ensureFactoryMatchesAudioProfile() {
        callMediaManager.ensureFactoryMatchesAudioProfile()
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

    /**
     * Lifecycle manager handles join/leave/cleanup.
     * INTERNAL: Not part of public API.
     */
    private val lifecycleManager = CallLifecycleManager(
        call = this,
        sessionManager = sessionManager,
        mediaManagerProvider = { mediaManager },
        clientScope = clientImpl.scope,
    )

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
    ): Result<RtcSession> = sessionManager.join(create, createOptions, ring, notify)

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

    /**
     * Fast reconnect to the same SFU with the same participant session.
     */
    suspend fun fastReconnect(reason: String = "unknown") {
        sessionManager.fastReconnect(reason)
    }

    /**
     * Rejoin a call. Creates a new session and joins as a new participant.
     */
    suspend fun rejoin(reason: String = "unknown") {
        sessionManager.rejoin(reason)
    }

    /**
     * Migrate to another SFU.
     */
    suspend fun migrate() {
        sessionManager.migrate()
    }

    /** Leave the call, but don't end it for other users */
    fun leave(reason: String = "user") {
        logger.d { "[leave] #ringing; no args, call_cid:$cid" }
        // Launch coroutine to check cleanup state with mutex
        // This allows leave() to remain non-suspending
        scope.launch {
            // Thread-safe check for existing cleanup using mutex
            val shouldProceed = cleanupMutex.withLock {
                val currentJob = cleanupJob
                if (currentJob?.isActive == true) {
                    logger.w {
                        "[leave] Cleanup already in progress (job: $currentJob), " +
                            "ignoring duplicate leave call"
                    }
                    false
                } else {
                    logger.v { "[leave] No active cleanup, proceeding with leave" }
                    true
                }
            }

            if (shouldProceed) {
                internalLeave(null, reason)
            }
        }
    }

    private fun internalLeave(disconnectionReason: Throwable?, reason: String) = atomicLeave {
        sessionManager.cleanupMonitor()

        // Leave session
        session?.leaveWithReason("[reason=$reason, error=${disconnectionReason?.message}]")

        // Cancel network monitoring
        sessionManager.cleanupNetworkMonitoring()

        // Update connection state
        state._connection.value = RealtimeConnection.Disconnected

        logger.v { "[leave] #ringing; disconnectionReason: $disconnectionReason, call_id = $id" }
        if (isDestroyed) {
            logger.w { "[leave] #ringing; Call already destroyed, ignoring" }
            return@atomicLeave
        }
        isDestroyed = true

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

        val newCleanupJob = clientImpl.scope.launch {
            safeCall {
                session?.sfuTracer?.trace(
                    "leave-call",
                    "[reason=$reason, error=${disconnectionReason?.message}]",
                )
                val stats = sessionManager.collectStats()
                session?.sendCallStats(stats)
            }
            cleanup()
        }

        // Clear the job reference after it completes
        newCleanupJob.invokeOnCompletion {
            scope.launch {
                cleanupMutex.withLock {
                    if (cleanupJob == newCleanupJob) {
                        cleanupJob = null
                        logger.v { "[cleanupJob] Cleared job reference" }
                    }
                }
            }
        }
    }

    internal suspend fun collectStats(): CallStatsReport = sessionManager.collectStats()

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
        session?.updateTrackDimensions(
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
    ) = callRenderer.initRenderer(
        videoRenderer,
        sessionId,
        trackType,
        eglBase,
        session,
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
    ): Result<GoLiveResponse> {
        return apiDelegate.goLive(startHls, startRecording, startTranscription)
    }

    suspend fun stopLive(): Result<StopLiveResponse> {
        return apiDelegate.stopLive()
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
    fun startScreenSharing(
        mediaProjectionPermissionResultData: Intent,
        includeAudio: Boolean = false,
    ) {
        apiDelegate.startScreenSharing(mediaProjectionPermissionResultData, includeAudio)
    }

    fun stopScreenSharing() {
        apiDelegate.stopScreenSharing()
    }

    suspend fun startHLS(): Result<Any> {
        return apiDelegate.startHLS()
    }

    suspend fun stopHLS(): Result<Any> {
        return apiDelegate.stopHLS()
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

    fun fireEvent(event: VideoEvent) = callEventManager.fireEvent(event)

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
        lifecycleManager.cleanup()
    }

    /**
     * Reinitializes the Call instance after it has been left, preparing it for rejoin.
     *
     * This method:
     * - Recreates the coroutine scope and supervisor job
     * - Generates a new session ID
     * - Resets connection state
     * - Resets atomic leave guard
     *
     * THREAD SAFETY: Uses synchronized block to ensure thread-safe modification.
     * This should only be called after confirming hasBeenLeft is true and resetting it to false
     * (both operations should be done atomically inside cleanupMutex.withLock).
     */
    internal fun reinitializeForRejoin() {
        synchronized(this) {
            val oldSessionId = sessionId
            val oldSupervisorJob = currentSupervisorJob

            logger.d {
                "[reinitializeForRejoin] Starting reinitialization. " +
                    "oldSessionId: $oldSessionId"
            }

            // Recreate coroutine infrastructure
            currentSupervisorJob = SupervisorJob()
            currentScope = CoroutineScope(
                clientImpl.scope.coroutineContext + currentSupervisorJob,
            )

            // Generate new session ID
            sessionId = UUID.randomUUID().toString()

            // Reset connection state
            state._connection.value = RealtimeConnection.Disconnected

            // Reset atomic leave guard
            atomicLeave = AtomicUnitCall()

            logger.i {
                "[reinitializeForRejoin] ✓ Reinitialization complete. " +
                    "oldSessionId: $oldSessionId → newSessionId: $sessionId, " +
                    "oldJob cancelled: ${oldSupervisorJob.isCancelled}"
            }
        }
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
        return apiDelegate.accept()
    }

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
        apiDelegate.collectUserFeedback(rating, reason, custom)
    }

    suspend fun takeScreenshot(track: VideoTrack): Bitmap? {
        return apiDelegate.takeScreenshot(track)
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
        session?.trackOverridesHandler?.updateOverrides(sessionIds, visible = enabled)
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
        callMediaManager.setIncomingAudioEnabled(session, enabled, sessionIds)

    @InternalStreamVideoApi
    public val debug = Debug(this)

    @InternalStreamVideoApi
    public class Debug(val call: Call) {

        public fun pause() {
            call.session?.subscriber?.disable()
        }

        public fun resume() {
            call.session?.subscriber?.enable()
        }

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
