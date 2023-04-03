/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioAttributes.ALLOW_CAPTURE_BY_ALL
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.getSystemService
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call2
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.connection.StreamPeerConnection
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.call.signal.socket.SfuSocket
import io.getstream.video.android.core.call.signal.socket.SfuSocketFactory
import io.getstream.video.android.core.call.signal.socket.SfuSocketImpl
import io.getstream.video.android.core.call.signal.socket.SfuSocketListener
import io.getstream.video.android.core.call.state.ConnectionState
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.errors.VideoError
import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.PublisherAnswerEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.filter.InFilterObject
import io.getstream.video.android.core.filter.toMap
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.internal.module.SFUConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.*
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.Failure
import io.getstream.video.android.core.utils.Result
import io.getstream.video.android.core.utils.Success
import io.getstream.video.android.core.utils.buildAudioConstraints
import io.getstream.video.android.core.utils.buildConnectionConfiguration
import io.getstream.video.android.core.utils.buildMediaConstraints
import io.getstream.video.android.core.utils.buildRemoteIceServers
import io.getstream.video.android.core.utils.onError
import io.getstream.video.android.core.utils.onSuccessSuspend
import io.getstream.video.android.core.utils.stringify
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.CameraEnumerator
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpParameters
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import stream.video.sfu.event.JoinRequest
import stream.video.sfu.event.JoinResponse
import stream.video.sfu.models.CallState
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.models.Participant
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackInfo
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.VideoLayer
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SendAnswerResponse
import stream.video.sfu.signal.SetPublisherRequest
import stream.video.sfu.signal.SetPublisherResponse
import stream.video.sfu.signal.TrackMuteState
import stream.video.sfu.signal.TrackSubscriptionDetails
import stream.video.sfu.signal.UpdateMuteStatesRequest
import stream.video.sfu.signal.UpdateMuteStatesResponse
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * Alright, what does this do?
 *
 * - Holds the subscriber and publisher peer connection
 * - Connects to the SFU
 *
 * - Makes API calls to the SFU
 * - Camera & Rendering helpers
 *
 *
 */
public class ActiveSFUSession internal constructor(
    private val client: StreamVideo,
    private val connectionModule: ConnectionModule,
    private val call2: Call2,
    private val SFUUrl: String,
    private val SFUToken: String,
    private val latencyResults: Map<String, List<Float>>,
    private val remoteIceServers: List<IceServer>,
) : CallClient, SfuSocketListener {
    private val context = client.context
    private val logger by taggedLogger("Call:WebRtcClient")



    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private var sessionId: String = ""
    private val scope = CoroutineScope(DispatcherProvider.IO)

    private lateinit var sfuConnectionModule: SFUConnectionModule

    init {
        val preferences = UserPreferencesManager.getPreferences()
        val user = preferences.getUserCredentials()
        if (preferences.getApiKey().isBlank() ||
            user?.id.isNullOrBlank()
        ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

        // TODO: setup the connection module properly
        sfuConnectionModule = connectionModule.createSFUConnectionModule(SFUUrl, SFUToken)

        sfuConnectionModule.sfuSocket.addListener(this)
        sfuConnectionModule.sfuSocket.connectSocket()
    }

    // ensure we parse errors and run on the right coroutineContext
    internal suspend fun <T : Any> wrapAPICall(apiCall: suspend () -> T): Result<T> {
        return withContext(scope.coroutineContext) {
            try {
                Success(apiCall())
            } catch (e: HttpException) {
                parseError(e)
            }
        }
    }
    private var call: Call? = null

    suspend fun parseError(e: Throwable): Failure {
        return Failure(VideoError("CallClientImpl error needs to be handled"))
    }

    suspend fun sendAnswer(request: SendAnswerRequest): Result<SendAnswerResponse> =
        wrapAPICall { sfuConnectionModule.signalService.sendAnswer(request)
    }

    suspend fun sendIceCandidate(request: ICETrickle): Result<ICETrickleResponse> =
        wrapAPICall { sfuConnectionModule.signalService.iceTrickle(request) }

    suspend fun setPublisher(request: SetPublisherRequest): Result<SetPublisherResponse> =
        wrapAPICall { sfuConnectionModule.signalService.setPublisher(request) }

    suspend fun updateSubscriptions(request: UpdateSubscriptionsRequest): Result<UpdateSubscriptionsResponse> =
        wrapAPICall { sfuConnectionModule.signalService.updateSubscriptions(request) }

    suspend fun updateMuteState(muteStateRequest: UpdateMuteStatesRequest): Result<UpdateMuteStatesResponse> =
        wrapAPICall { sfuConnectionModule.signalService.updateMuteStates(muteStateRequest) }

    /**
     * State that indicates whether the camera is capturing and sending video or not.
     */
    private val _isVideoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled

    /**
     * State that indicates whether the mic is capturing and sending the audio or not.
     */
    private val _isAudioEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isAudioEnabled: StateFlow<Boolean> = _isAudioEnabled

    /**
     * State that indicates whether the speakerphone is on or not.
     */
    private val _isSpeakerPhoneEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isSpeakerPhoneEnabled: StateFlow<Boolean> = _isSpeakerPhoneEnabled

    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(scope.coroutineContext + supervisorJob)

    /**
     * Connection and WebRTC.
     */
    private val peerConnectionFactory by lazy { StreamPeerConnectionFactory(context) }
    private val iceServers by lazy { buildRemoteIceServers(remoteIceServers) }

    private val connectionConfiguration: PeerConnection.RTCConfiguration by lazy {
        buildConnectionConfiguration(iceServers)
    }

    private val mediaConstraints: MediaConstraints by lazy {
        buildMediaConstraints()
    }

    private val audioConstraints: MediaConstraints by lazy {
        buildAudioConstraints()
    }

    private var subscriber: StreamPeerConnection? = null
    private var publisher: StreamPeerConnection? = null

    internal var localVideoTrack: VideoTrack? = null
        set(value) {
            field = value
            if (value != null) {
                call?.updateLocalVideoTrack(value)
            }
        }
    internal var localAudioTrack: AudioTrack? = null

    private val isConnected = MutableStateFlow(value = false)
    private val sfuEvents = MutableSharedFlow<SfuDataEvent>()

    /**
     * Video track helpers.
     */
    private val cameraManager by lazy { context.getSystemService<CameraManager>() }
    private val cameraEnumerator: CameraEnumerator by lazy {
        Camera2Enumerator(context)
    }
    private val surfaceTextureHelper by lazy {
        SurfaceTextureHelper.create(
            "CaptureThread", peerConnectionFactory.eglBase.eglBaseContext
        )
    }

    private var videoCapturer: VideoCapturer? = null
    private var isCapturingVideo: Boolean = false
    private var captureResolution: CameraEnumerationAndroid.CaptureFormat? = null


    override fun clear() {
        logger.i { "[clear] #sfu; no args" }
        supervisorJob.cancelChildren()

        connectionState = ConnectionState.DISCONNECTED
        sessionId = ""

        call?.disconnect()
        call = null

        subscriber?.connection?.close()
        publisher?.connection?.close()
        subscriber = null
        publisher = null

        sfuConnectionModule.sfuSocket.releaseConnection()

        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null

        surfaceTextureHelper.stopListening()

        isCapturingVideo = false
    }

    override fun addSocketListener(sfuSocketListener: SfuSocketListener) {
        sfuConnectionModule.sfuSocket.addListener(sfuSocketListener)
    }

    override fun removeSocketListener(sfuSocketListener: SfuSocketListener) {
        sfuConnectionModule.sfuSocket.removeListener(sfuSocketListener)
    }

    override fun setInitialCallSettings(callSettings: CallSettings) {
        logger.d { "[setCallSettings] call settings: $callSettings" }
        _isVideoEnabled.value = callSettings.cameraOn
        _isAudioEnabled.value = callSettings.microphoneOn
        _isSpeakerPhoneEnabled.value = callSettings.speakerOn
    }

    override fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }
        coroutineScope.launch {
            // If we are not connected to the SFU we do not want to send requests or initialise any tracks not to waste
            // resources if the user declines a call.
            // TODO
//            if (callEngine.callState.value !is StreamCallState.Connected) {
//                _isVideoEnabled.value = isEnabled
//                return@launch
//            }

            setupVideoTrack()

            if (!isCapturingVideo && isEnabled) {
                startCapturingLocalVideo(CameraMetadata.LENS_FACING_FRONT)
            }
            val request = UpdateMuteStatesRequest(
                session_id = sessionId,
                mute_states = listOf(
                    TrackMuteState(
                        track_type = TrackType.TRACK_TYPE_VIDEO,
                        muted = !isEnabled
                    )
                ),
            )

            updateMuteState(request).onSuccessSuspend {
                call?.setCameraEnabled(isEnabled)
                localVideoTrack?.setEnabled(isEnabled)
                _isVideoEnabled.value = isEnabled
            }
        }
    }

    override fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        coroutineScope.launch {
            // If we are not connected to the SFU we do not want to send requests or initialise any tracks not to waste
            // resources if the user declines a call.
            // TODO
//            if (callEngine.callState.value !is StreamCallState.Connected) {
//                _isAudioEnabled.value = isEnabled
//                return@launch
//            }

            setupAudioTrack()

            val request = UpdateMuteStatesRequest(
                session_id = sessionId,
                mute_states = listOf(
                    TrackMuteState(
                        track_type = TrackType.TRACK_TYPE_AUDIO,
                        muted = !isEnabled
                    )
                ),
            )

            updateMuteState(request).onSuccessSuspend {
                call?.setMicrophoneEnabled(isEnabled)
                localAudioTrack?.setEnabled(isEnabled)
                _isAudioEnabled.value = isEnabled
            }
        }
    }

    override fun setSpeakerphoneEnabled(isEnabled: Boolean) {
        val devices = getAudioDevices()

        val activeDevice = devices.firstOrNull {
            if (isEnabled) {
                it.name.contains("speaker", true)
            } else {
                !it.name.contains("speaker", true)
            }
        }

        getAudioHandler()?.selectDevice(activeDevice)?.also {
            _isSpeakerPhoneEnabled.value = isEnabled
        }
    }

    override fun flipCamera() {
        logger.d { "[flipCamera] #sfu; no args" }
        (videoCapturer as? Camera2Capturer)?.switchCamera(null)
    }

    private fun getAudioHandler(): io.getstream.video.android.core.audio.AudioSwitchHandler? {
        return call?.audioHandler as? io.getstream.video.android.core.audio.AudioSwitchHandler
    }

    override fun getAudioDevices(): List<io.getstream.video.android.core.audio.AudioDevice> {
        logger.d { "[getAudioDevices] #sfu; no args" }
        val handler = getAudioHandler() ?: return emptyList()

        return handler.availableAudioDevices
    }

    override fun selectAudioDevice(device: io.getstream.video.android.core.audio.AudioDevice) {
        logger.d { "[selectAudioDevice] #sfu; device: $device" }
        val handler = getAudioHandler() ?: return

        handler.selectDevice(device)
    }

    // TODO: call participants should be a map
    fun getParticipant(userId: String): ParticipantState? {
        return call?.callParticipants?.value?.associate { it.user.value.id to it }?.get(userId)
    }


    private fun listenToParticipants() {
        val call = call ?: throw IllegalStateException("Call is in an incorrect state, null!")

        coroutineScope.launch {
            call.callParticipants.collectLatest { participants ->
                updateParticipantsSubscriptions(participants)
            }
        }
    }

    override suspend fun connectToCall(
        sessionId: String,
        autoPublish: Boolean
    ): Result<Call> {
        logger.d { "[connectToCall] #sfu; sessionId: $sessionId, autoPublish: $autoPublish" }
        if (connectionState != ConnectionState.DISCONNECTED) {
            return Failure(
                VideoError("Already connected or connecting to a call with the session ID: $sessionId")
            )
        }

        connectionState = ConnectionState.CONNECTING
        this.sessionId = sessionId

        return when (val initializeResult = initializeCall(autoPublish)) {
            is Success -> {
                connectionState = ConnectionState.CONNECTED

                Success(call!!)
            }
            is Failure -> initializeResult
        }
    }

    /**
     * @return The active call instance, if it exists.
     */
    override fun getActiveCall(): Call? = call

    /**
     * @return [StateFlow] that holds [RTCStatsReport] that the publisher exposes.
     */
    override fun getPublisherStats(): StateFlow<RTCStatsReport?> {
        return publisher?.getStats() ?: MutableStateFlow(null)
    }

    /**
     * @return [StateFlow] that holds [RTCStatsReport] that the subscriber exposes.
     */
    override fun getSubscriberStats(): StateFlow<RTCStatsReport?> {
        return subscriber?.getStats() ?: MutableStateFlow(null)
    }

    private fun createCall(sessionId: String): Call {
        logger.d { "[createCall] #sfu; sessionId: $sessionId" }
        this.sessionId = sessionId

        return buildCall()
    }

    private fun buildCall(): Call {
        logger.d { "[buildCall] #sfu; sessionId: $sessionId" }
        return Call(
            context = context,
            getCurrentUserId = {client.user.id},
            eglBase = peerConnectionFactory.eglBase,
        )
    }

    private suspend fun loadParticipantsData(
        callId: StreamCallId,
        callState: CallState?,
        callSettings: CallSettings,
        callType: String
    ) {
        logger.d { "[loadParticipantsData] #sfu; callState: $callState, callSettings: $callSettings" }
        if (callState != null) {
            setPartialParticipants(callState.participants)

            val cid = "$callType:$callId"
            val query = QueryMembersData(
                streamCallCid = cid,
                filters = InFilterObject(
                    "id",
                    callState.participants.map { it.user_id }.toSet()
                ).toMap()
            )
            val userQueryResult = client.queryMembers(callType, callId, query)
            if (userQueryResult is Success) {
                //call?.upsertParticipants(userQueryResult.data)
            }
        }
    }

    private fun setPartialParticipants(participants: List<Participant>) {
//        call?.setParticipants(
//            participants.map {
//                ParticipantState(
//                    id = it.user_id,
//                    sessionId = it.session_id,
//                    idPrefix = it.track_lookup_prefix,
//                    isLocal = it.session_id == sessionId,
//                    name = "",
//                    profileImageURL = "",
//                    role = "",
//                    publishedTracks = it.published_tracks.toSet()
//                )
//            }
//        )
    }

    private suspend fun initializeCall(
        autoPublish: Boolean,
    ): Result<JoinResponse> {
        logger.d { "[initializeCall] #sfu; autoPublish: $autoPublish" }

        val call = createCall(sessionId)
        this.call = call
        listenToParticipants()

        val result = connectToCall()
        logger.v { "[initializeCall] #sfu; result: $result" }
        val callSettings = CallSettings(
            autoPublish = autoPublish,
            microphoneOn = _isAudioEnabled.value,
            cameraOn = _isVideoEnabled.value,
            speakerOn = _isSpeakerPhoneEnabled.value
        )
        logger.d { "[initializeCall] callSettings: $callSettings" }
        return when (result) {
            is Success -> {
                createPeerConnections(autoPublish)
                loadParticipantsData(
                    callId = call2.id,
                    callType = call2.type,
                    callState = result.data.call_state,
                    callSettings = callSettings
                )
                createUserTracks(callSettings)
                call.setupAudio(callSettings)

                result
            }
            is Failure -> result
        }
    }

    private fun createPeerConnections(autoPublish: Boolean) {
        logger.i { "[createPeerConnections] #sfu; autoPublish: $autoPublish" }
        createSubscriber()

        if (autoPublish) {
            createPublisher()
        }
    }

    private fun createSubscriber() {
        this.subscriber = peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.SUBSCRIBER,
            mediaConstraints = mediaConstraints,
            onStreamAdded = { call?.addStream(it) }, // addTrack
            onIceCandidateRequest = ::sendIceCandidate
        ).also {
            logger.i { "[createSubscriber] #sfu; subscriber: $it" }
        }
    }

    private fun sendIceCandidate(candidate: IceCandidate, peerType: StreamPeerType) {
        coroutineScope.launch {
            logger.d { "[sendIceCandidate] #sfu; #${peerType.stringify()}; candidate: $candidate" }
            val iceTrickle = ICETrickle(
                peer_type = peerType.toPeerType(),
                ice_candidate = Json.encodeToString(candidate),
                session_id = sessionId
            )
            logger.v { "[sendIceCandidate] #sfu; #${peerType.stringify()}; iceTrickle: $iceTrickle" }
            val result = sendIceCandidate(iceTrickle)
            logger.v { "[sendIceCandidate] #sfu; #${peerType.stringify()}; completed: $result" }
        }
    }

    private suspend fun connectToCall(): Result<JoinResponse> {
        logger.d { "[connectToCall] #sfu; no args" }
        return executeJoinRequest().also {
            logger.v { "[connectToCall] #sfu; completed $it" }
        }
    }

    private suspend fun executeJoinRequest(): Result<JoinResponse> {
        val sdp = getGenericSdp()

        val request = JoinRequest(
            session_id = sessionId,
            token = "TODO: TOKEN",
            subscriber_sdp = sdp
        )
        logger.d { "[executeJoinRequest] request: $request" }

        return try {
            withTimeout(TIMEOUT) {
                val connected = isConnected.value
                logger.d { "[executeJoinRequest] is connected: $connected" }
                sfuConnectionModule.sfuSocket.sendJoinRequest(request)
                logger.d { "[executeJoinRequest] sfu join request is sent" }
                // TODO: callEngine.onSfuJoinSent(request)
                logger.d { "[executeJoinRequest] request is sent" }
                val event = sfuEvents.filterIsInstance<JoinCallResponseEvent>().first()
                logger.d { "[executeJoinRequest] completed: $event" }
                Success(JoinResponse(event.callState))
            }
        } catch (e: Throwable) {
            logger.e { "[executeJoinRequest] failed: $e" }
            Failure(VideoError(e.message, e))
        }
    }

    private suspend fun getGenericSdp(): String {
        val streamPeerConnection = peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.SUBSCRIBER,
            mediaConstraints = mediaConstraints,
        )

        val connection = streamPeerConnection.connection

        connection.apply {
            addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                RtpTransceiver.RtpTransceiverInit(
                    RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
                )
            )
            addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                RtpTransceiver.RtpTransceiverInit(
                    RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
                )
            )
        }

        val offer = streamPeerConnection.createOffer()

        try {
            connection.dispose()
        } catch (error: Throwable) {
            error.printStackTrace()
        }

        return if (offer is Success) {
            offer.data.description
        } else {
            ""
        }
    }

    private fun createPublisher() {
        publisher = peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.PUBLISHER,
            mediaConstraints = MediaConstraints(),
            onNegotiationNeeded = ::onNegotiationNeeded,
            onIceCandidateRequest = ::sendIceCandidate,
        ).also {
            logger.i { "[createPublisher] #sfu; publisher: $it" }
        }
    }

    override fun onConnecting() {
        coroutineScope.launch {
            logger.i { "[onConnecting] no args" }
            isConnected.value = false
        }
    }

    override fun onConnected(event: ConnectedEvent) {
        coroutineScope.launch {
            logger.i { "[onConnected] event: $event" }
            isConnected.value = true
        }
    }

    override fun onDisconnected(cause: DisconnectCause) {
        coroutineScope.launch {
            logger.i { "[onDisconnected] cause: $cause" }
            isConnected.value = false
        }
    }

    override fun onError(error: VideoError) {
        coroutineScope.launch {
            logger.e { "[onError] cause: $error" }
        }
    }

    override fun onEvent(event: SfuDataEvent) {
        val clientImpl = client as StreamVideoImpl
        // trigger an event in the client as well for SFU events. makes it easier to subscribe
        clientImpl.fireEvent(event, call2.cid)

        coroutineScope.launch {
            logger.v { "[onRtcEvent] event: $event" }
            sfuEvents.emit(event)
            when (event) {
                is ICETrickleEvent -> handleTrickle(event)
                is SubscriberOfferEvent -> handleSubscriberOffer(event)
                is PublisherAnswerEvent -> Unit
                is ChangePublishQualityEvent -> Unit

                is TrackPublishedEvent -> {
                    call?.updateMuteState(event.userId, event.sessionId, event.trackType, true)
                }
                is TrackUnpublishedEvent -> {
                    call?.updateMuteState(event.userId, event.sessionId, event.trackType, false)
                }
                else -> Unit
            }
        }
    }

    private suspend fun addParticipant(event: ParticipantJoinedEvent) {
        val query = InFilterObject("id", setOf(event.participant.user_id)).toMap()

        val userQueryResult = client.queryMembers(
            call2.type,
            call2.id,
            QueryMembersData(
                streamCallCid = call2.cid,
                filters = query
            )
        )

        if (userQueryResult is Success) {
            if (userQueryResult.data.isEmpty()) {
                addPartialParticipant(event.participant)
                return
            }

            val user = userQueryResult.data.first()
            val isLocal = event.participant.session_id == sessionId

//            call?.addParticipant(
//                ParticipantState(
//                    id = user.id,
//                    role = user.role,
//                    name = user.name,
//                    profileImageURL = user.imageUrl,
//                    sessionId = event.participant.session_id,
//                    idPrefix = event.participant.track_lookup_prefix,
//                    isLocal = isLocal,
//                )
//            )
        } else {
            addPartialParticipant(event.participant)
        }
    }

    private fun addPartialParticipant(participant: Participant) {
//        call?.addParticipant(
//            ParticipantState(
//                id = participant.user_id,
//                idPrefix = participant.track_lookup_prefix,
//                sessionId = participant.session_id,
//                name = "",
//                profileImageURL = "",
//                role = ""
//            )
//        )
    }

    private suspend fun handleTrickle(event: ICETrickleEvent) {
        logger.d { "[handleTrickle] #sfu; #${event.peerType.stringify()}; candidate: ${event.candidate}" }
        val iceCandidate: IceCandidate = Json.decodeFromString(event.candidate)
        val result = if (event.peerType == PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED) {
            publisher?.addIceCandidate(iceCandidate)
        } else {
            subscriber?.addIceCandidate(iceCandidate)
        }
        logger.v { "[handleTrickle] #sfu; #${event.peerType.stringify()}; result: $result" }
    }

    private fun onNegotiationNeeded(
        peerConnection: StreamPeerConnection,
        peerType: StreamPeerType
    ) {
        val id = Random.nextInt().absoluteValue
        logger.d { "[negotiate] #$id; #sfu; #${peerType.stringify()}; peerConnection: $peerConnection" }
        coroutineScope.launch {
            peerConnection.createOffer().onSuccessSuspend { data ->
                logger.v { "[negotiate] #$id; #sfu; #${peerType.stringify()}; offerSdp: $data" }

                peerConnection.setLocalDescription(data)

                val trackInfos = peerConnection.connection.transceivers.filter {
                    it.direction == RtpTransceiver.RtpTransceiverDirection.SEND_ONLY && it.sender?.track() != null
                }.map { transceiver ->
                    val track = transceiver.sender.track()!!

                    val trackType = when (track.kind()) {
                        "audio" -> TrackType.TRACK_TYPE_AUDIO
                        "screen" -> TrackType.TRACK_TYPE_SCREEN_SHARE
                        "video" -> TrackType.TRACK_TYPE_VIDEO
                        else -> TrackType.TRACK_TYPE_UNSPECIFIED
                    }

                    val layers: List<VideoLayer> = transceiver.sender.parameters.encodings.map {
                        VideoLayer(
                            rid = it.rid ?: "",
                            video_dimension = VideoDimension(
                                width = captureResolution?.width ?: 0,
                                height = captureResolution?.height ?: 0
                            ),
                            bitrate = it.maxBitrateBps ?: 0,
                            fps = captureResolution?.framerate?.max ?: 0
                        )
                    }

                    TrackInfo(
                        track_id = track.id(),
                        track_type = trackType,
                        layers = layers
                    )
                }

                val request = SetPublisherRequest(
                    sdp = data.description,
                    session_id = sessionId,
                    tracks = trackInfos
                )

                setPublisher(request).onSuccessSuspend {
                    logger.v { "[negotiate] #$id; #sfu; #${peerType.stringify()}; answerSdp: $it" }

                    val answerDescription = SessionDescription(
                        SessionDescription.Type.ANSWER, it.sdp
                    )
                    peerConnection.setRemoteDescription(answerDescription)
                }.onError {
                    logger.e { "[negotiate] #$id; #sfu; #${peerType.stringify()}; failed: $it" }
                }
            }
        }
    }

    private fun createUserTracks(callSettings: CallSettings) {
        val autoPublish = callSettings.autoPublish

        logger.d { "[createUserTracks] #sfu; autoPublish: $autoPublish, callSettings: $callSettings" }
        val manager = context.getSystemService<AudioManager>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            manager?.allowedCapturePolicy = ALLOW_CAPTURE_BY_ALL
        }

        if (_isAudioEnabled.value) {
            setupAudioTrack(autoPublish)
        }

        if (_isVideoEnabled.value) {
            setupVideoTrack(autoPublish)
        }
    }

    private fun setupAudioTrack(autoPublish: Boolean = true) {
        if (localAudioTrack != null) return

        val audioTrack = makeAudioTrack()
        audioTrack.setEnabled(_isAudioEnabled.value)
        localAudioTrack = audioTrack
        logger.v { "[createUserTracks] #sfu; audioTrack: ${audioTrack.stringify()}" }

        if (autoPublish) {
            publisher?.addAudioTransceiver(localAudioTrack!!, listOf(sessionId))
        }
    }

    private fun setupVideoTrack(autoPublish: Boolean = true) {
        if (localVideoTrack != null) return

        val videoTrack = makeVideoTrack()
        localVideoTrack = videoTrack
        videoTrack.setEnabled(_isVideoEnabled.value)
        logger.v { "[createUserTracks] #sfu; videoTrack: ${videoTrack.stringify()}" }

        if (autoPublish) {
            publisher?.addVideoTransceiver(localVideoTrack!!, listOf(sessionId))
        }
    }

    private fun makeAudioTrack(): AudioTrack {
        val audioSource = peerConnectionFactory.makeAudioSource(audioConstraints)

        return peerConnectionFactory.makeAudioTrack(
            source = audioSource, trackId = buildTrackId(TRACK_TYPE_AUDIO)
        )
    }

    private fun makeVideoTrack(isScreenShare: Boolean = false): VideoTrack {
        val videoSource = peerConnectionFactory.makeVideoSource(isScreenShare)

        val capturer = buildCameraCapturer()
        capturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)

        return peerConnectionFactory.makeVideoTrack(
            source = videoSource, trackId = buildTrackId(TRACK_TYPE_VIDEO)
        )
    }

    private fun buildTrackId(trackTypeVideo: String): String {
        return "${call?.localParticipantIdPrefix}:$trackTypeVideo:${(Math.random() * 100).toInt()}"
    }

    override fun startCapturingLocalVideo(position: Int) {
        val capturer = videoCapturer as? Camera2Capturer ?: return
        val enumerator = cameraEnumerator as? Camera2Enumerator ?: return

        val frontCamera = enumerator.deviceNames.first {
            if (position == 0) {
                enumerator.isFrontFacing(it)
            } else {
                enumerator.isBackFacing(it)
            }
        }

        val supportedFormats = enumerator.getSupportedFormats(frontCamera) ?: emptyList()

        val resolution = supportedFormats.firstOrNull {
            (it.width == 720 || it.width == 480 || it.width == 360)
        } ?: return

        capturer.startCapture(resolution.width, resolution.height, 30)
        isCapturingVideo = true
        captureResolution = resolution
    }

    private fun buildCameraCapturer(): VideoCapturer? {
        val manager = cameraManager ?: return null

        val ids = manager.cameraIdList
        var foundCamera = false
        var cameraId = ""

        for (id in ids) {
            val characteristics = manager.getCameraCharacteristics(id)
            val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (cameraLensFacing == CameraMetadata.LENS_FACING_FRONT) {
                foundCamera = true
                cameraId = id
            }
        }

        if (!foundCamera && ids.isNotEmpty()) {
            cameraId = ids.first()
        }

        val camera2Capturer = Camera2Capturer(context, cameraId, null)
        videoCapturer = camera2Capturer
        return camera2Capturer
    }

    private fun updatePublishQuality(event: ChangePublishQualityEvent) {
        val transceiver = publisher?.videoTransceiver ?: return

        val enabledRids =
            event.changePublishQuality.video_senders.firstOrNull()?.layers?.filter { it.active }
                ?.map { it.name } ?: emptyList()

        logger.v { "[updatePublishQuality] #sfu; updateQuality: $enabledRids" }
        val params = transceiver.sender.parameters

        val updatedEncodings = mutableListOf<RtpParameters.Encoding>()

        var encodingChanged = false
        logger.v { "[updatePublishQuality] #sfu; currentQuality: $params" }

        for (encoding in params.encodings) {
            if (encoding.rid != null) {
                val shouldEnable = encoding.rid in enabledRids

                if (shouldEnable && encoding.active) {
                    updatedEncodings.add(encoding)
                } else if (!shouldEnable && !encoding.active) {
                    updatedEncodings.add(encoding)
                } else {
                    encodingChanged = true
                    encoding.active = shouldEnable
                    updatedEncodings.add(encoding)
                }
            }
        }
        if (encodingChanged) {
            logger.v { "[updatePublishQuality] #sfu; updatedEncodings: $updatedEncodings" }
            params.encodings.clear()
            params.encodings.addAll(updatedEncodings)

            publisher?.videoTransceiver?.sender?.parameters = params
        }
    }

    private suspend fun handleSubscriberOffer(offerEvent: SubscriberOfferEvent) {
        logger.d { "[handleSubscriberOffer] #sfu; #subscriber; event: $offerEvent" }
        val subscriber = subscriber ?: return

        val offerDescription = SessionDescription(
            SessionDescription.Type.OFFER, offerEvent.sdp
        )
        subscriber.setRemoteDescription(offerDescription)
        val answerResult = subscriber.createAnswer()
        if (answerResult !is Success) {
            logger.w { "[handleSubscriberOffer] #sfu; #subscriber; rejected (createAnswer failed): $answerResult" }
            return
        }
        val answerSdp = answerResult.data
        logger.v { "[handleSubscriberOffer] #sfu; #subscriber; answerSdp: ${answerSdp.description}" }
        val setAnswerResult = subscriber.setLocalDescription(answerSdp)
        if (setAnswerResult !is Success) {
            logger.w { "[handleSubscriberOffer] #sfu; #subscriber; rejected (setAnswer failed): $setAnswerResult" }
            return
        }
        logger.v { "[handleSubscriberOffer] #sfu; #subscriber; setAnswerResult: $setAnswerResult" }
        val sendAnswerRequest = SendAnswerRequest(
            PeerType.PEER_TYPE_SUBSCRIBER, answerSdp.description, sessionId
        )
        val sendAnswerResult = sendAnswer(sendAnswerRequest)
        logger.v { "[handleSubscriberOffer] #sfu; #subscriber; sendAnswerResult: $sendAnswerResult" }
    }

    private fun updateParticipantsSubscriptions(participants: List<ParticipantState>) {
        val subscriptions = mutableMapOf<ParticipantState, VideoDimension>()
        val userId = client.user.id

        for (participant in participants) {
            val user = participant.user.value
            if (user.id != userId) {
                logger.d { "[updateParticipantsSubscriptions] #sfu; user.id: ${user.id}" }

                val dimension = VideoDimension(
                    width = participant.videoTrackSize.first, height = participant.videoTrackSize.second
                )
                logger.d { "[updateParticipantsSubscriptions] #sfu; user.id: ${user.id}, dimension: $dimension" }
                subscriptions[participant] = dimension
            }
        }
        if (subscriptions.isEmpty()) {
            return
        }

        val request = UpdateSubscriptionsRequest(
            session_id = sessionId,
            tracks = subscriptions.flatMap { (participant, videoDimensions) ->
                val user = participant.user.value
                listOf(
                    TrackSubscriptionDetails(
                        user_id = user.id,
                        track_type = TrackType.TRACK_TYPE_VIDEO,
                        dimension = videoDimensions,
                        session_id = participant.sessionId
                    ),
                    TrackSubscriptionDetails(
                        user_id = user.id,
                        track_type = TrackType.TRACK_TYPE_SCREEN_SHARE,
                        dimension = videoDimensions,
                        session_id = participant.sessionId
                    ),
                    TrackSubscriptionDetails(
                        user_id = user.id,
                        track_type = TrackType.TRACK_TYPE_AUDIO,
                        dimension = null,
                        session_id = participant.sessionId
                    )
                )
            }
        )
        logger.d { "[updateParticipantsSubscriptions] #sfu; request: $request" }

        coroutineScope.launch {
            when (val result = updateSubscriptions(request)) {
                is Success -> {
                    logger.v { "[updateParticipantsSubscriptions] #sfu; succeed" }
                }
                is Failure -> {
                    logger.e { "[updateParticipantsSubscriptions] #sfu; failed: $result" }
                }
            }
        }
    }

    companion object {
        private const val TRACK_TYPE_VIDEO = "v"
        private const val TRACK_TYPE_AUDIO = "a"
        private const val TRACK_TYPE_SCREEN_SHARE = "s"
        private const val TIMEOUT = 30_000L
    }
}
