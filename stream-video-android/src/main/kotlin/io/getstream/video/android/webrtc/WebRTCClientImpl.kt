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

package io.getstream.video.android.webrtc

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
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.events.AudioLevelChangedEvent
import io.getstream.video.android.events.ChangePublishQualityEvent
import io.getstream.video.android.events.ICETrickleEvent
import io.getstream.video.android.events.MuteStateChangeEvent
import io.getstream.video.android.events.SfuDataEvent
import io.getstream.video.android.events.SfuParticipantJoinedEvent
import io.getstream.video.android.events.SfuParticipantLeftEvent
import io.getstream.video.android.events.SubscriberOfferEvent
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipant
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.buildAudioConstraints
import io.getstream.video.android.utils.buildConnectionConfiguration
import io.getstream.video.android.utils.buildIceServers
import io.getstream.video.android.utils.buildMediaConstraints
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import io.getstream.video.android.webrtc.connection.StreamPeerConnection
import io.getstream.video.android.webrtc.connection.StreamPeerConnectionFactory
import io.getstream.video.android.webrtc.signal.SignalClient
import io.getstream.video.android.webrtc.signal.socket.SignalSocket
import io.getstream.video.android.webrtc.signal.socket.SignalSocketListener
import io.getstream.video.android.webrtc.state.ConnectionState
import io.getstream.video.android.webrtc.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
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
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SetPublisherRequest
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import kotlin.math.absoluteValue
import kotlin.random.Random

internal class WebRTCClientImpl(
    private val context: Context,
    private val credentialsProvider: CredentialsProvider,
    private val signalClient: SignalClient,
    private val signalSocket: SignalSocket,
    servers: List<IceServer>?
) : WebRTCClient, SignalSocketListener {

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
        buildIceServers(servers)
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
        signalSocket.addListener(this)
    }

    private val publisherCandidates = mutableListOf<ICETrickleEvent>()
    private val subscriberCandidates = mutableListOf<ICETrickleEvent>()

    override fun onEvent(event: SfuDataEvent) {
        super.onEvent(event)

        when (event) {
            is ICETrickleEvent -> handleTrickle(event)
            is SubscriberOfferEvent -> setRemoteDescription(event.sdp)
            is SfuParticipantJoinedEvent -> call?.addParticipant(event)
            is SfuParticipantLeftEvent -> call?.removeParticipant(event)
            is ChangePublishQualityEvent -> {
                // updatePublishQuality(event) -> TODO - re-enable once we send the proper quality (dimensions)
            }
            is AudioLevelChangedEvent -> call?.updateAudioLevel(event)
            is MuteStateChangeEvent -> call?.updateMuteState(event)
            else -> Unit
        }
    }

    private fun handleTrickle(event: ICETrickleEvent) {
        if (event.peerType == PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED) {
            handlePublisherTrickle(event)
        } else {
            handleSubscriberTrickle(event)
        }
    }

    private fun handleSubscriberTrickle(iceTrickle: ICETrickleEvent) {
        if (subscriber?.connection?.remoteDescription != null) {
            // TODO - parse candidate from JSON and then set it
//            subscriber?.connection?.addIceCandidate(
//                IceCandidate(
//                    "", // TODO data
//                    0,
//                    candidate
//                )
//            )
        } else {
            subscriberCandidates.add(iceTrickle)
        }
    }

    private fun handlePublisherTrickle(candidate: ICETrickleEvent) {
        if (publisher?.connection?.remoteDescription != null) {
//            publisher?.connection?.addIceCandidate(
//                IceCandidate(
//                    "", // TODO data
//                    0, candidate
//                )
//            )
        } else {
            publisherCandidates.add(candidate)
        }
    }

    override fun clear() {
        logger.i { "[clear] #sfu; no args" }
        peerConnectionFactory.reset()
        supervisorJob.cancelChildren()

        connectionState = ConnectionState.DISCONNECTED
        sessionId = ""

        call?.disconnect()
        call = null

        subscriber?.connection?.close()
        publisher?.connection?.close()
        subscriber = null
        publisher = null

        // TODO - clear up socket connection

        videoCapturer = null
        isCapturingVideo = false
    }

    override fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }
        coroutineScope.launch {
            if (!isCapturingVideo && isEnabled) {
                startCapturingLocalVideo(CameraMetadata.LENS_FACING_FRONT)
            }
            call?.setCameraEnabled(isEnabled)
            localVideoTrack?.setEnabled(isEnabled)
        }
    }

    override fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        coroutineScope.launch {
            call?.setMicrophoneEnabled(isEnabled)
            localAudioTrack?.setEnabled(isEnabled)
        }
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
        autoPublish: Boolean,
        callSettings: CallSettings
    ): Result<Call> {
        logger.d { "[connectToCall] #sfu; sessionId: $sessionId, autoPublish: $autoPublish" }
        if (connectionState != ConnectionState.DISCONNECTED) {
            return Failure(
                VideoError("Already connected or connecting to a call with the session ID: $sessionId")
            )
        }

        connectionState = ConnectionState.CONNECTING
        this.sessionId = sessionId

        when (val initializeResult = initializeCall(autoPublish)) {
            is Success -> {
                connectionState = ConnectionState.CONNECTED

                val call = createCall(sessionId)
                this.call = call

                call.setupAudio()
                listenToParticipants()
                loadParticipantsData(initializeResult.data.call_state, callSettings)
                setupUserMedia(callSettings, autoPublish)

                return Success(call)
            }
            is Failure -> {
                return initializeResult
            }
        }
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
            credentialsProvider = credentialsProvider,
            eglBase = peerConnectionFactory.eglBase,
        )
    }

    private fun loadParticipantsData(callState: CallState?, callSettings: CallSettings) {
        if (callState != null) {
            call?.loadParticipants(callState, callSettings)
        }
    }

    private suspend fun initializeCall(autoPublish: Boolean): Result<JoinResponse> {
        logger.d { "[initializeCall] #sfu; autoPublish: $autoPublish" }

        // TODO - wait for sub/pub to be connected?

        return when (val result = connectToCall()) {
            is Success -> {
                // TODO - process result, create connections
                result
            }
            is Failure -> result
        }

        // TODO - delay the creation until we get the response -> use the response for the answer (subscriber never creates an offer) 1.b
        val subscriber = createSubscriber()
        logger.v { "[initializeCall] #sfu; subscriber: $subscriber" }
        this.subscriber = subscriber

        if (autoPublish) {
            createPublisher()
        }

        return throw IllegalArgumentException("TODO") // TODO
    }

    private fun createSubscriber(): StreamPeerConnection {
        return peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = PeerType.PEER_TYPE_SUBSCRIBER,
            mediaConstraints = mediaConstraints,
            onStreamAdded = { call?.addStream(it) }, // addTrack
            onStreamRemoved = { call?.removeStream(it) },
            onIceCandidateRequest = ::sendIceCandidate
        ).also {
            logger.d { "[createSubscriber] #sfu; subscriber: $it" }
        }
    }

    private fun sendIceCandidate(candidate: IceCandidate, peerType: PeerType) {
        logger.d { "[sendIceCandidate] #sfu; type: $peerType, candidate: $candidate" }
        val trickle = ICETrickle(
            peer_type = peerType,
            ice_candidate = Json.encodeToString(candidate),
            session_id = sessionId // TODO - check if we need to format this
        )

        coroutineScope.launch {
            logger.v { "[sendIceCandidate] #sfu; type: $peerType, candidate is about to be sent" }
            val result = signalClient.sendIceCandidate(trickle)
            logger.v { "[sendIceCandidate] #sfu; type: $peerType, result: $result" }
        }
    }

    private suspend fun connectToCall(): Result<JoinResponse> {
        val joinResponse = executeJoinRequest()

        joinResponse.onSuccessSuspend { response ->
            // connection.onCallJoined(response.own_session_id) TODO
        }
        logger.v { "[connectToCall] #sfu; completed" }
        return joinResponse
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
            )
        )

        return suspendCancellableCoroutine {
            // TODO - socket send data and wait for it to arrive
        }
    }

    private fun createPublisher() {
        publisher = peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED,
            mediaConstraints = MediaConstraints(),
            onNegotiationNeeded = ::onNegotiationNeeded,
            onIceCandidateRequest = ::sendIceCandidate
        )
        logger.d { "[createPublisher] #sfu; publisher: $publisher" }
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

                signalClient.setPublisher(request).onSuccessSuspend {
                    logger.v { "[negotiate] #$id; #sfu; answerSdp: $it" }

                    peerConnection.setRemoteDescription(
                        SessionDescription(SessionDescription.Type.ANSWER, it.sdp)
                    )

                    applyPendingIceCandidates()
                }.onError {
                    logger.e { "[negotiate] #$id; #sfu; failed: $it" }
                }
            }
        }
    }

    private fun applyPendingIceCandidates() {
        publisherCandidates.forEach { candidate ->
//            publisher?.connection?.addIceCandidate(
//                IceCandidate(
//                    "", // TODO data will be a JSON
//                    0, candidate
//                )
//            )
        }
        publisherCandidates.clear()
    }

    private fun setupUserMedia(callSettings: CallSettings, shouldPublish: Boolean) {
        logger.d { "[setupUserMedia] #sfu; shouldPublish: $shouldPublish, callSettings: $callSettings" }
        val manager = context.getSystemService<AudioManager>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            manager?.allowedCapturePolicy = ALLOW_CAPTURE_BY_ALL
        }

        val audioTrack = makeAudioTrack()
        audioTrack.setEnabled(true)
        localAudioTrack = audioTrack
        logger.v { "[setupUserMedia] #sfu; audioTrack: ${audioTrack.stringify()}" }

        val videoTrack = makeVideoTrack()
        localVideoTrack = videoTrack
        videoTrack.setEnabled(callSettings.videoOn)
        logger.v { "[setupUserMedia] #sfu; videoTrack: ${videoTrack.stringify()}" }

        if (shouldPublish) {
            publisher?.addAudioTransceiver(audioTrack, listOf(sessionId))
            publisher?.addVideoTransceiver(videoTrack, listOf(sessionId))
        }
    }

    private fun makeAudioTrack(): AudioTrack {
        val audioSource = peerConnectionFactory.makeAudioSource(audioConstraints)

        return peerConnectionFactory.makeAudioTrack(audioSource)
    }

    private fun makeVideoTrack(isScreenShare: Boolean = false): VideoTrack {
        val videoSource = peerConnectionFactory.makeVideoSource(isScreenShare)

        val capturer = buildCameraCapturer()
        capturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)

        return peerConnectionFactory.makeVideoTrack(videoSource)
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

    private fun setRemoteDescription(sdp: String) {
        logger.d { "[setRemoteDescription] #sfu; #subscriber; offerSdp: $sdp" }
        val subscriber = subscriber ?: return

        val sessionDescription = SessionDescription(
            SessionDescription.Type.OFFER, sdp
        )

        coroutineScope.launch {
            subscriber.setRemoteDescription(sessionDescription)

            subscriberCandidates.forEach { candidate ->
//                subscriber.connection.addIceCandidate(
//                    IceCandidate("", 0, candidate)
//                )
            }
            subscriberCandidates.clear()

            when (val result = subscriber.createAnswer()) {
                is Success -> sendAnswer(result.data.description)
                is Failure -> {
                    // TODO handle error
                }
            }
        }
    }

    private fun sendAnswer(description: String) {
        logger.d { "[sendAnswer] #sfu; answerSdp: $description" }
        val sendAnswerRequest = SendAnswerRequest(
            session_id = sessionId, peer_type = PeerType.PEER_TYPE_SUBSCRIBER, sdp = description
        )

        coroutineScope.launch {
            signalClient.sendAnswer(sendAnswerRequest)
        }
    }

    private fun updateParticipantsSubscriptions(participants: List<CallParticipant>) {
        val subscriptions = mutableMapOf<String, VideoDimension>()
        val userId = credentialsProvider.getUserCredentials().id

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
            when (val result = signalClient.updateSubscriptions(request)) {
                is Success -> {
                    logger.v { "[updateParticipantsSubscriptions] #sfu; succeed" }
                }
                is Failure -> {
                    logger.e { "[updateParticipantsSubscriptions] #sfu; failed: $result" }
                }
            }
        }
    }
}
