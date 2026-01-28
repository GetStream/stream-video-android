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
import io.getstream.video.android.core.call.CallCleanupManager
import io.getstream.video.android.core.call.CallEventManager
import io.getstream.video.android.core.call.CallJoinCoordinator
import io.getstream.video.android.core.call.CallMediaManager
import io.getstream.video.android.core.call.CallReInitializer
import io.getstream.video.android.core.call.CallRenderer
import io.getstream.video.android.core.call.CallSessionManager
import io.getstream.video.android.core.call.CallStatsReporter
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.audio.InputAudioFilter
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.call.scope.ScopeProvider
import io.getstream.video.android.core.call.scope.ScopeProviderImpl
import io.getstream.video.android.core.call.utils.SoundInputProcessor
import io.getstream.video.android.core.call.video.VideoFilter
import io.getstream.video.android.core.closedcaptions.ClosedCaptionsSettings
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.RampValueUpAndDownHelper
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.model.User
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import org.webrtc.EglBase
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples
import stream.video.sfu.models.ClientCapability
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
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

    internal val clientImpl = client as StreamVideoClient
    internal val scopeProvider: ScopeProvider = ScopeProviderImpl(clientImpl.scope)

    // Atomic controls
    internal var atomicLeave = AtomicUnitCall()

    private val logger by taggedLogger("Call:$type:$id")

    private val callReinitializer = CallReInitializer(clientImpl.scope) {
        reInitialise()
    }

    internal val scope: CoroutineScope
        get() = callReinitializer.currentScope

    private var powerManager: PowerManager? = null

    internal val sessionManager = CallSessionManager(
        call = this,
        clientImpl = clientImpl,
        powerManager = powerManager,
        testInstanceProvider = testInstanceProvider,
    )

    private val callStatsReporter: CallStatsReporter = CallStatsReporter(this)
    internal val callJoinCoordinator = CallJoinCoordinator(
        call = this,
        client = clientImpl,
        callReInitializer = callReinitializer,
        onJoinFail = {
            sessionManager.session = null
        },
        createJoinSession = { create, createOptions, ring, notify ->
            sessionManager._join(create, createOptions, ring, notify)
        },
        onRejoin = { reason -> sessionManager.rejoin(reason) },
    )

    private val callRenderer = CallRenderer()
    private val apiDelegate = CallApiDelegate(
        clientImpl = clientImpl,
        type = type,
        id = id,
        call = this,
        screenShareProvider = { screenShare },
        setScreenTrackCallBack = { sessionManager.session?.setScreenShareTrack() },
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
    internal var isDestroyed = AtomicBoolean(false)

    var sessionId: String
        get() = sessionManager.sessionId
        set(value) {
            sessionManager.sessionId = value
        }

    internal val unifiedSessionId = UUID.randomUUID().toString()

    internal val connectStartTime: Long
        get() = sessionManager.connectStartTime
    internal val reconnectStartTime: Long
        get() = sessionManager.reconnectStartTime

    /**
     * EGL base context shared between peerConnectionFactory and mediaManager
     * to break circular dependency.
     */
    internal val eglBase: EglBase by lazy {
        EglBase.create()
    }
    val events = MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)
    internal val callEventManager =
        CallEventManager(events, sessionManager, scope, { subscriptions })

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
    private val lifecycleManager = CallCleanupManager(
        call = this,
        sessionManager = sessionManager,
        client = clientImpl,
        mediaManagerProvider = { mediaManager },
        callReInitializer = callReinitializer,
        clientScope = clientImpl.scope,
        callStatsReporter = callStatsReporter,
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
        lifecycleManager.leave(reason)
    }

    /** ends the call for yourself as well as other users */
    suspend fun end(): Result<Unit> {
        val result = clientImpl.endCall(type, id)
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
        sessionManager.session?.updateTrackDimensions(
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
        sessionManager.session?.updateTrackDimensions(
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
        sessionManager.session,
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
        sessionManager.session?.let { session ->
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
        sessionManager.session?.trackOverridesHandler?.updateOverrides(
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
        callMediaManager.setIncomingAudioEnabled(sessionManager.session, enabled, sessionIds)

    private fun reInitialise() {
        logger.d { "[reInitialise]" }
        sessionManager.reset()
        state._connection.value = RealtimeConnection.Disconnected
        atomicLeave = AtomicUnitCall()
        scopeProvider.reset()
    }

    @InternalStreamVideoApi
    public val debug = Debug(this)

    @InternalStreamVideoApi
    public class Debug(val call: Call) {

        public fun pause() {
            call.sessionManager.session?.subscriber?.disable()
        }

        public fun resume() {
            call.sessionManager.session?.subscriber?.enable()
        }

        public fun rejoin() {
            call.scope.launch {
                call.rejoin()
            }
        }

        public fun restartSubscriberIce() {
            call.sessionManager.session?.subscriber?.connection?.restartIce()
        }

        public fun restartPublisherIce() {
            call.sessionManager.session?.publisher?.connection?.restartIce()
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
