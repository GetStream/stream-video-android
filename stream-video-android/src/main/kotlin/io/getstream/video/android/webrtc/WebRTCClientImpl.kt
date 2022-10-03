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
import io.getstream.video.android.events.ChangePublishQualityEvent
import io.getstream.video.android.events.MuteStateChangeEvent
import io.getstream.video.android.events.SfuDataEvent
import io.getstream.video.android.events.SfuParticipantJoinedEvent
import io.getstream.video.android.events.SfuParticipantLeftEvent
import io.getstream.video.android.events.SubscriberOfferEvent
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipant
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.module.WebRTCModule.Companion.REDIRECT_SIGNAL_URL
import io.getstream.video.android.module.WebRTCModule.Companion.SIGNAL_HOST_BASE
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.buildAudioConstraints
import io.getstream.video.android.utils.buildConnectionConfiguration
import io.getstream.video.android.utils.buildLocalIceServers
import io.getstream.video.android.utils.buildMediaConstraints
import io.getstream.video.android.utils.buildRemoteIceServers
import io.getstream.video.android.utils.onSuccessSuspend
import io.getstream.video.android.utils.stringify
import io.getstream.video.android.webrtc.connection.PeerConnectionType
import io.getstream.video.android.webrtc.connection.StreamPeerConnection
import io.getstream.video.android.webrtc.connection.StreamPeerConnectionFactory
import io.getstream.video.android.webrtc.datachannel.StreamDataChannel
import io.getstream.video.android.webrtc.signal.SignalClient
import io.getstream.video.android.webrtc.state.ConnectionState
import io.getstream.video.android.webrtc.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okio.ByteString.Companion.encode
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.RtpParameters
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import stream.video.sfu.AudioCodecs
import stream.video.sfu.CallState
import stream.video.sfu.CodecSettings
import stream.video.sfu.IceCandidateRequest
import stream.video.sfu.JoinRequest
import stream.video.sfu.JoinResponse
import stream.video.sfu.PeerType
import stream.video.sfu.SendAnswerRequest
import stream.video.sfu.SetPublisherRequest
import stream.video.sfu.UpdateSubscriptionsRequest
import stream.video.sfu.VideoCodecs
import stream.video.sfu.VideoDimension
import java.util.concurrent.TimeUnit

internal class WebRTCClientImpl(
    private val context: Context,
    private val credentialsProvider: CredentialsProvider,
    private val signalClient: SignalClient,
) : WebRTCClient {

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
        if (REDIRECT_SIGNAL_URL == null) {
            buildRemoteIceServers(SIGNAL_HOST_BASE)
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

    /**
     * Data channels.
     */
    private var signalChannel: StreamDataChannel? = null
    private var signalChannelState: DataChannel.State = DataChannel.State.CLOSED

    /**
     * Video track helpers.
     */
    private val cameraManager by lazy { context.getSystemService<CameraManager>() }
    private val cameraEnumerator: CameraEnumerator by lazy {
        Camera2Enumerator(context)
    }
    private val surfaceTextureHelper by lazy {
        SurfaceTextureHelper.create(
            "CaptureThread",
            peerConnectionFactory.eglBase.eglBaseContext
        )
    }

    private var videoCapturer: VideoCapturer? = null
    private var isCapturingVideo: Boolean = false

    override fun clear() {
        logger.d { "[clear] #sfu; no args" }
        supervisorJob.cancelChildren()

        connectionState = ConnectionState.DISCONNECTED
        sessionId = ""

        call?.disconnect()
        call = null

        subscriber?.connection?.close()
        publisher?.connection?.close()
        subscriber = null
        publisher = null

        signalChannel = null
        signalChannelState = DataChannel.State.CLOSED

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

        return buildCall(sessionId)
    }

    private fun buildCall(sessionId: String): Call {
        logger.d { "[buildCall] #sfu; sessionId: $sessionId" }
        return Call(
            context = context,
            sessionId = sessionId,
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
        val subscriber = createSubscriber()
        this.subscriber = subscriber
        logger.d { "[initializeCall] #sfu; autoPublish: $autoPublish" }

        createSignalingChannel(subscriber)
        logger.v { "[initializeCall] #sfu; subscriber: $subscriber" }

        when (val joinResult = connectToCall(subscriber)) {
            is Success -> {
                val isConnectionOpen = listenForConnectionOpened()
                logger.v { "[initializeCall] #sfu; isConnectionOpen: $isConnectionOpen" }

                if (!isConnectionOpen) {
                    clear()
                    return Failure(VideoError("Couldn't connect to the data channel."))
                }

                if (autoPublish) {
                    createPublisher()
                }
                return Success(joinResult.data)
            }
            is Failure -> {
                clear()
                return Failure(
                    VideoError(
                        "Couldn't connect to call. Join request failed.",
                        joinResult.error.cause
                    )
                )
            }
        }
    }

    private fun createSubscriber(): StreamPeerConnection {
        return peerConnectionFactory.makePeerConnection(
            configuration = connectionConfiguration,
            type = PeerConnectionType.SUBSCRIBER,
            mediaConstraints = mediaConstraints,
            onStreamAdded = { call?.addStream(it) },
            onStreamRemoved = { call?.removeStream(it) },
            onIceCandidateRequest = ::sendIceCandidate
        )
    }

    private fun sendIceCandidate(candidate: IceCandidate, type: PeerConnectionType) {
        val request = IceCandidateRequest(
            publisher = type == PeerConnectionType.PUBLISHER,
            candidate = candidate.sdp ?: "",
            sdpMid = candidate.sdpMid ?: "",
            sdpMLineIndex = candidate.sdpMLineIndex,
            session_id = sessionId
        )

        coroutineScope.launch {
            signalClient.sendIceCandidate(request)
        }
    }

    private fun createSignalingChannel(connection: StreamPeerConnection) {
        logger.d { "[createSignalingChannel] #sfu; connection: $connection" }
        signalChannel = connection.createDataChannel(
            "signaling",
            DataChannel.Init(),
            onMessage = ::onMessage,
            onStateChange = { state ->
                this.signalChannelState = state
            }
        )
        logger.v { "[initializeCall] #sfu; signalChannel: $signalChannel" }
    }

    private suspend fun connectToCall(connection: StreamPeerConnection): Result<JoinResponse> {
        logger.d { "[connectToCall] #sfu; connection: $connection" }
        val offerResult = connection.createJoinOffer()
        logger.v { "[connectToCall] #sfu; offerResult: ${offerResult.stringify { it.stringify() }}" }
        return if (offerResult is Success) {
            val joinResponse = executeJoinRequest(offerResult.data)

            joinResponse.onSuccessSuspend { response ->
                connection.onCallJoined(response.sdp)
            }
            logger.v { "[connectToCall] #sfu; completed" }
            return joinResponse
        } else {
            val error = offerResult as? Failure
            logger.e { "[connectToCall] #sfu; failed: $error" }

            Failure(error?.error ?: VideoError())
        }
    }

    private suspend fun executeJoinRequest(data: SessionDescription): Result<JoinResponse> {
        logger.d { "[executeJoinRequest] #sfu; offerSdp: ${data.stringify()}" }
        val decoderCodecs = peerConnectionFactory.getVideoDecoderCodecs()
        val encoderCodecs = peerConnectionFactory.getVideoEncoderCodecs()

        val request = JoinRequest(
            subscriber_sdp_offer = data.description,
            session_id = sessionId,
            codec_settings = CodecSettings( // TODO - layers
                video = VideoCodecs(
                    encode = encoderCodecs,
                    decode = decoderCodecs
                ),
                audio = AudioCodecs(
                    encode = peerConnectionFactory.getAudioEncoderCoders(),
                    decode = peerConnectionFactory.getAudioDecoderCoders()
                )
            )
        )

        return signalClient.join(request).also {
            logger.v { "[executeJoinRequest] #sfu; result: $it" }
        }
    }

    private suspend fun listenForConnectionOpened(): Boolean {
        var connected = signalChannelState == DataChannel.State.OPEN

        val timeoutTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)

        while (System.currentTimeMillis() < timeoutTime && !connected) {
            connected = signalChannelState == DataChannel.State.OPEN

            if (!connected) {
                delay(1000)
            }
        }

        return if (connected) {
            signalChannel?.send("ss".encode()) ?: false
        } else {
            false
        }
    }

    private fun createPublisher() {
        publisher = peerConnectionFactory.makePeerConnection(
            connectionConfiguration,
            PeerConnectionType.PUBLISHER,
            mediaConstraints = MediaConstraints(),
            onNegotiationNeeded = ::negotiate
        )
        logger.d { "[createPublisher] #sfu; publisher: $publisher" }
    }

    private fun negotiate(peerConnection: StreamPeerConnection) {
        logger.d { "[negotiate] #sfu; peerConnection: $peerConnection" }
        coroutineScope.launch {
            peerConnection.createOffer().onSuccessSuspend { data ->
                logger.v { "[negotiate] #sfu; offerSdp: $data" }

                peerConnection.setLocalDescription(data)

                val request = SetPublisherRequest(
                    sdp = data.description,
                    session_id = sessionId
                )

                signalClient.setPublisher(request).onSuccessSuspend {
                    logger.v { "[negotiate] #sfu; answerSdp: $it" }

                    peerConnection.setRemoteDescription(
                        SessionDescription(SessionDescription.Type.ANSWER, it.sdp)
                    )
                }
            }
        }
    }

    private fun setupUserMedia(callSettings: CallSettings, shouldPublish: Boolean) {
        logger.d { "[setupUserMedia] #sfu; shouldPublish: $shouldPublish, callSettings: $callSettings" }
        val manager = context.getSystemService<AudioManager>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            manager?.allowedCapturePolicy = ALLOW_CAPTURE_BY_ALL
        }

        val audioTrack = makeAudioTrack()
        audioTrack.setEnabled(true)
        //audioTrack.setVolume(1.0)
        localAudioTrack = audioTrack
        logger.v { "[setupUserMedia] #sfu; audioTrack: ${audioTrack.stringify()}" }

        val videoTrack = makeVideoTrack()
        localVideoTrack = videoTrack
        videoTrack.setEnabled(callSettings.videoOn)
        logger.v { "[setupUserMedia] #sfu; videoTrack: ${videoTrack.stringify()}" }

        if (shouldPublish) {
            //publisher?.addTrack(audioTrack, listOf(sessionId))
            publisher?.addAudioTransceiver(audioTrack, listOf(sessionId))
            publisher?.addVideoTransceiver(videoTrack, listOf(sessionId))
        }
    }

    private fun makeAudioTrack(): AudioTrack {
        val audioSource = peerConnectionFactory.makeAudioSource(audioConstraints)

        return peerConnectionFactory.makeAudioTrack(audioSource, trackId = "tpa-audio")
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

        val enabledRids = event.changePublishQuality.video_sender.firstOrNull()?.layers
            ?.filter { it.active }
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

    /**
     * Processes messages from the Data channel.
     */
    private fun onMessage(event: SfuDataEvent) {
        logger.v { "[onMessage] #sfu; event: $event" }
        when (event) {
            is SubscriberOfferEvent -> setRemoteDescription(event.sdp)
            is SfuParticipantJoinedEvent -> call?.addParticipant(event)
            is SfuParticipantLeftEvent -> call?.removeParticipant(event)
            is ChangePublishQualityEvent -> {
                // updatePublishQuality(event) -> TODO - re-enable once we send the proper quality (dimensions)
            }
            is MuteStateChangeEvent -> call?.updateMuteState(event)
            else -> Unit
        }
    }

    private fun setRemoteDescription(sdp: String) { // TODO - check with iOS if the SDP communication is correct
        logger.d { "[setRemoteDescription] #sfu; offerSdp: $sdp" }
        val subscriber = subscriber ?: return

        val sessionDescription = SessionDescription(
            SessionDescription.Type.OFFER,
            sdp
        )

        coroutineScope.launch {
            subscriber.setRemoteDescription(sessionDescription)

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
            session_id = sessionId,
            peer_type = PeerType.SUBSCRIBER,
            sdp = description
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
                    width = user.trackSize.first,
                    height = user.trackSize.second
                )
                subscriptions[user.id] = dimension
            }
        }
        if (subscriptions.isEmpty()) {
            return
        }

        val request = UpdateSubscriptionsRequest(
            session_id = sessionId,
            subscriptions = subscriptions
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
