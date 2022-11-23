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

package io.getstream.video.android.call

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioAttributes.ALLOW_CAPTURE_BY_ALL
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import io.getstream.logging.StreamLog
import io.getstream.video.android.audio.AudioDevice
import io.getstream.video.android.audio.AudioSwitchHandler
import io.getstream.video.android.call.connection.StreamPeerConnection
import io.getstream.video.android.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.call.signal.SfuClient
import io.getstream.video.android.call.signal.socket.SfuSocket
import io.getstream.video.android.call.signal.socket.SfuSocketListener
import io.getstream.video.android.call.state.ConnectionState
import io.getstream.video.android.call.utils.stringify
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.engine.StreamCallEngine
import io.getstream.video.android.engine.adapter.SfuSocketListenerAdapter
import io.getstream.video.android.errors.DisconnectCause
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.events.AudioLevelChangedEvent
import io.getstream.video.android.events.ChangePublishQualityEvent
import io.getstream.video.android.events.ConnectedEvent
import io.getstream.video.android.events.ICETrickleEvent
import io.getstream.video.android.events.JoinCallResponseEvent
import io.getstream.video.android.events.MuteStateChangeEvent
import io.getstream.video.android.events.ParticipantJoinedEvent
import io.getstream.video.android.events.ParticipantLeftEvent
import io.getstream.video.android.events.SfuDataEvent
import io.getstream.video.android.events.SubscriberOfferEvent
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.IceCandidate
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.model.SfuToken
import io.getstream.video.android.model.StreamPeerType
import io.getstream.video.android.model.toPeerType
import io.getstream.video.android.module.SfuClientModule
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.buildAudioConstraints
import io.getstream.video.android.utils.buildConnectionConfiguration
import io.getstream.video.android.utils.buildLocalIceServers
import io.getstream.video.android.utils.buildMediaConstraints
import io.getstream.video.android.utils.buildRemoteIceServers
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import io.getstream.video.android.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpParameters
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import stream.video.sfu.event.JoinRequest
import stream.video.sfu.event.JoinResponse
import stream.video.sfu.models.AudioCodecs
import stream.video.sfu.models.CallState
import stream.video.sfu.models.CodecSettings
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.VideoCodecs
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.AudioMuteChanged
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SetPublisherRequest
import stream.video.sfu.signal.UpdateMuteStateRequest
import stream.video.sfu.signal.UpdateMuteStateResponse
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.VideoMuteChanged
import kotlin.math.absoluteValue
import kotlin.random.Random

internal class CallClientImpl(
    private val context: Context,
    private val getCurrentUserId: () -> String,
    private val getSfuToken: () -> SfuToken,
    private val callEngine: StreamCallEngine,
    private val sfuClient: SfuClient,
    private val sfuSocket: SfuSocket,
    private val remoteIceServers: List<IceServer>,
) : CallClient, SfuSocketListener {

    private val logger = StreamLog.getLogger("Call:WebRtcClient")

    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private var sessionId: String = ""
    private var call: Call? = null

    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(DispatcherProvider.IO + supervisorJob)

    /**
     * Connection and WebRTC.
     */
    private val peerConnectionFactory by lazy { StreamPeerConnectionFactory(context) }
    private val iceServers by lazy {
        if (SfuClientModule.REDIRECT_SIGNAL_URL == null) {
            buildRemoteIceServers(remoteIceServers)
        } else {
            buildLocalIceServers()
        }
    }

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

    private var localVideoTrack: VideoTrack? = null
        set(value) {
            field = value
            if (value != null) {
                call?.updateLocalVideoTrack(value)
            }
        }
    private var localAudioTrack: AudioTrack? = null

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

    init {
        sfuSocket.addListener(this)
        sfuSocket.addListener(SfuSocketListenerAdapter(callEngine))
        sfuSocket.connectSocket()
    }

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

        sfuSocket.releaseConnection()

        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null

        surfaceTextureHelper.stopListening()

        isCapturingVideo = false
    }

    override fun addSocketListener(sfuSocketListener: SfuSocketListener) {
        sfuSocket.addListener(sfuSocketListener)
    }

    override fun removeSocketListener(sfuSocketListener: SfuSocketListener) {
        sfuSocket.removeListener(sfuSocketListener)
    }

    override fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }
        coroutineScope.launch {
            if (!isCapturingVideo && isEnabled) {
                startCapturingLocalVideo(CameraMetadata.LENS_FACING_FRONT)
            }
            val request = UpdateMuteStateRequest(
                sessionId,
                video_mute_changed = VideoMuteChanged(muted = !isEnabled)
            )

            updateMuteState(request).onSuccessSuspend {
                call?.setCameraEnabled(isEnabled)
                localVideoTrack?.setEnabled(isEnabled)
            }
        }
    }

    override fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        coroutineScope.launch {
            if (isEnabled) setupAudioTrack()

            val request = UpdateMuteStateRequest(
                sessionId,
                audio_mute_changed = AudioMuteChanged(muted = !isEnabled),
            )

            updateMuteState(request).onSuccessSuspend {
                call?.setMicrophoneEnabled(isEnabled)
                localAudioTrack?.setEnabled(isEnabled)
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

        getAudioHandler()?.selectDevice(activeDevice)
    }

    private suspend fun updateMuteState(muteStateRequest: UpdateMuteStateRequest): Result<UpdateMuteStateResponse> {
        return sfuClient.updateMuteState(muteStateRequest)
    }

    override fun flipCamera() {
        logger.d { "[flipCamera] #sfu; no args" }
        (videoCapturer as? Camera2Capturer)?.switchCamera(null)
    }

    private fun getAudioHandler(): AudioSwitchHandler? {
        return call?.audioHandler as? AudioSwitchHandler
    }

    override fun getAudioDevices(): List<AudioDevice> {
        logger.d { "[getAudioDevices] #sfu; no args" }
        val handler = getAudioHandler() ?: return emptyList()

        return handler.availableAudioDevices
    }

    override fun selectAudioDevice(device: AudioDevice) {
        logger.d { "[selectAudioDevice] #sfu; device: $device" }
        val handler = getAudioHandler() ?: return

        handler.selectDevice(device)
    }

    private fun listenToParticipants() {
        val room = call ?: throw IllegalStateException("Call is in an incorrect state, null!")

        coroutineScope.launch {
            room.callParticipants.collectLatest { participants ->
                updateParticipantsSubscriptions(participants)
            }
        }
    }

    override suspend fun connectToCall(
        sessionId: String,
        getCallSettings: () -> CallSettings,
    ): Result<Call> {
        logger.d { "[connectToCall] #sfu; sessionId: $sessionId, autoPublish: ${getCallSettings().autoPublish}" }
        if (connectionState != ConnectionState.DISCONNECTED) {
            return Failure(
                VideoError("Already connected or connecting to a call with the session ID: $sessionId")
            )
        }

        connectionState = ConnectionState.CONNECTING
        this.sessionId = sessionId

        return when (val initializeResult = initializeCall(getCallSettings)) {
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
            getCurrentUserId = getCurrentUserId,
            eglBase = peerConnectionFactory.eglBase,
        )
    }

    private fun loadParticipantsData(callState: CallState?, callSettings: CallSettings) {
        logger.d { "[loadParticipantsData] #sfu; callState: $callState, callSettings: $callSettings" }
        if (callState != null) {
            call?.loadParticipants(callState, callSettings)
        }
    }

    private suspend fun initializeCall(
        getCallSettings: () -> CallSettings,
    ): Result<JoinResponse> {
        val autoPublish = getCallSettings().autoPublish
        logger.d { "[initializeCall] #sfu; autoPublish: $autoPublish" }

        val call = createCall(sessionId)
        this.call = call
        listenToParticipants()

        val result = connectToCall()
        logger.v { "[initializeCall] #sfu; result: $result" }
        return when (result) {
            is Success -> {
                createPeerConnections(autoPublish)
                val callSettings = getCallSettings()
                loadParticipantsData(result.data.call_state, callSettings)
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
            onStreamRemoved = { call?.removeStream(it) },
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
            val result = sfuClient.sendIceCandidate(iceTrickle)
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
        val decoderCodecs = peerConnectionFactory.getVideoDecoderCodecs()
        val encoderCodecs = peerConnectionFactory.getVideoEncoderCodecs()

        val request = JoinRequest(
            session_id = sessionId,
            codec_settings = CodecSettings( // TODO - layers
                video = VideoCodecs(
                    encodes = encoderCodecs, decodes = decoderCodecs
                ),
                audio = AudioCodecs(
                    encodes = peerConnectionFactory.getAudioEncoderCoders(),
                    decodes = peerConnectionFactory.getAudioDecoderCoders()
                )
            ),
            token = getSfuToken()
        )
        logger.d { "[executeJoinRequest] request: $request" }

        return try {
            withTimeout(TIMEOUT) {
                isConnected.first { it }
                sfuSocket.sendJoinRequest(request)
                callEngine.onSfuJoinSent(request)
                logger.v { "[executeJoinRequest] request is sent" }
                val event = sfuEvents.first { it is JoinCallResponseEvent } as JoinCallResponseEvent
                logger.v { "[executeJoinRequest] completed: $event" }
                Success(
                    JoinResponse(
                        event.callState,
                        event.ownSessionId
                    )
                )
            }
        } catch (e: Throwable) {
            logger.e { "[executeJoinRequest] failed: $e" }
            Failure(VideoError(e.message, e))
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
            onConnectionChange = { connection, peerType ->
                callEngine.onCallConnectionChange(sessionId, peerType, connection)
            }
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
        coroutineScope.launch {
            logger.v { "[onRtcEvent] event: $event" }
            sfuEvents.emit(event)
            when (event) {
                is ICETrickleEvent -> handleTrickle(event)
                is SubscriberOfferEvent -> handleSubscriberOffer(event)
                is ParticipantJoinedEvent -> call?.addParticipant(event)
                is ParticipantLeftEvent -> call?.removeParticipant(event)
                is ChangePublishQualityEvent -> {
                    // updatePublishQuality(event) -> TODO - re-enable once we send the proper quality (dimensions)
                }
                is AudioLevelChangedEvent -> call?.updateAudioLevel(event)
                is MuteStateChangeEvent -> call?.updateMuteState(event)
                else -> Unit
            }
        }
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

    private fun onNegotiationNeeded(peerConnection: StreamPeerConnection) {
        val id = Random.nextInt().absoluteValue
        logger.d { "[negotiate] #$id; #sfu; peerConnection: $peerConnection" }
        coroutineScope.launch {
            peerConnection.createOffer().onSuccessSuspend { data ->
                logger.v { "[negotiate] #$id; #sfu; offerSdp: $data" }

                peerConnection.setLocalDescription(data)

                val request = SetPublisherRequest(
                    sdp = data.description, session_id = sessionId
                )

                sfuClient.setPublisher(request).onSuccessSuspend {
                    logger.v { "[negotiate] #$id; #sfu; answerSdp: $it" }

                    val answerDescription = SessionDescription(
                        SessionDescription.Type.ANSWER, it.sdp
                    )
                    peerConnection.setRemoteDescription(answerDescription)
                }.onError {
                    logger.e { "[negotiate] #$id; #sfu; failed: $it" }
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

        if (callSettings.audioOn) {
            setupAudioTrack(callSettings.autoPublish)
        }

        val videoTrack = makeVideoTrack()
        localVideoTrack = videoTrack
        videoTrack.setEnabled(callSettings.videoOn)
        logger.d { "[createUserTracks] #sfu; videoTrack: ${videoTrack.stringify()}" }

        if (autoPublish) {
            publisher?.addVideoTransceiver(localVideoTrack!!, listOf(sessionId))
        }
    }

    private fun setupAudioTrack(autoPublish: Boolean = true) {
        if (localAudioTrack != null) return

        val audioTrack = makeAudioTrack()
        audioTrack.setEnabled(true)
        localAudioTrack = audioTrack
        logger.d { "[setupAudioTrack] #sfu; audioTrack: ${audioTrack.stringify()}" }

        if (autoPublish) {
            publisher?.addTrack(localAudioTrack!!, listOf(sessionId))
        }
    }

    private fun makeAudioTrack(): AudioTrack {
        val audioSource = peerConnectionFactory.makeAudioSource(audioConstraints)

        return peerConnectionFactory.makeAudioTrack(
            source = audioSource,
            trackId = buildTrackId(TRACK_TYPE_AUDIO)
        )
    }

    private fun makeVideoTrack(isScreenShare: Boolean = false): VideoTrack {
        val videoSource = peerConnectionFactory.makeVideoSource(isScreenShare)

        val capturer = buildCameraCapturer()
        capturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)

        return peerConnectionFactory.makeVideoTrack(
            source = videoSource,
            trackId = buildTrackId(TRACK_TYPE_VIDEO)
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
            (it.width == 1080 || it.width == 720 || it.width == 480)
        } ?: return

        capturer.startCapture(resolution.width, resolution.height, 30)
        isCapturingVideo = true
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
        val sendAnswerResult = sfuClient.sendAnswer(sendAnswerRequest)
        logger.v { "[handleSubscriberOffer] #sfu; #subscriber; sendAnswerResult: $sendAnswerResult" }
    }

    private fun updateParticipantsSubscriptions(participants: List<CallParticipantState>) {
        val subscriptions = mutableMapOf<String, VideoDimension>()
        val userId = getCurrentUserId()

        for (user in participants) {
            if (user.id != userId) {
                logger.d { "[updateParticipantsSubscriptions] #sfu; user.id: ${user.id}" }

                val dimension = VideoDimension(
                    width = user.trackSize.first, height = user.trackSize.second
                )
                subscriptions[user.id] = dimension
            }
        }
        if (subscriptions.isEmpty()) {
            return
        }

        val request = UpdateSubscriptionsRequest(
            session_id = sessionId, subscriptions = subscriptions
        )

        coroutineScope.launch {
            when (val result = sfuClient.updateSubscriptions(request)) {
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
