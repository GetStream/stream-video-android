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
import android.util.Log
import androidx.core.content.getSystemService
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
import io.getstream.video.android.model.CallParticipant
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.Room
import io.getstream.video.android.module.VideoModule
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.buildConnectionConfiguration
import io.getstream.video.android.utils.buildLocalIceServers
import io.getstream.video.android.utils.buildMediaConstraints
import io.getstream.video.android.utils.buildRemoteIceServers
import io.getstream.video.android.utils.onSuccessSuspend
import io.getstream.video.android.webrtc.connection.PeerConnectionType
import io.getstream.video.android.webrtc.connection.StreamPeerConnection
import io.getstream.video.android.webrtc.connection.StreamPeerConnectionFactory
import io.getstream.video.android.webrtc.datachannel.StreamDataChannel
import io.getstream.video.android.webrtc.signal.SignalClient
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
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpParameters
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import stream.video.sfu.AudioCodecs
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

public class WebRTCClientImpl(
    private val context: Context,
    private val credentialsProvider: CredentialsProvider,
    private val signalClient: SignalClient,
) : WebRTCClient {

    private var sessionId: String = ""
    private var room: Room? = null

    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(DispatcherProvider.IO + supervisorJob)

    /**
     * Connection and WebRTC.
     */
    private val peerConnectionFactory by lazy { StreamPeerConnectionFactory(context) }
    private val iceServers by lazy {
        if (VideoModule.REDIRECT_SIGNAL_URL == null) {
            buildRemoteIceServers(VideoModule.SIGNAL_HOST_BASE)
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

    private var subscriber: StreamPeerConnection? = null
    private var publisher: StreamPeerConnection? = null

    private var localVideoTrack: VideoTrack? = null
        set(value) {
            field = value
            if (value != null) {
                room?.updateLocalVideoTrack(value)
            }
        }
    private var localAudioTrack: AudioTrack? = null

    private var localMediaStream: MediaStream? = null

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

    override fun clear() {
        supervisorJob.cancelChildren()

        sessionId = ""

        room?.disconnect()
        room = null

        subscriber?.connection?.close()
        publisher?.connection?.close()
        subscriber = null
        publisher = null

        signalChannel = null
        signalChannelState = DataChannel.State.CLOSED

        videoCapturer = null
    }

    override fun setCameraEnabled(isEnabled: Boolean) {
        coroutineScope.launch {
            room?.setCameraEnabled(isEnabled)
            localVideoTrack?.setEnabled(isEnabled)
        }
    }

    override fun setMicrophoneEnabled(isEnabled: Boolean) {
        coroutineScope.launch {
            room?.setMicrophoneEnabled(isEnabled)
            localAudioTrack?.setEnabled(isEnabled)
        }
    }

    override fun flipCamera() {
        (videoCapturer as? Camera2Capturer)?.switchCamera(null)
    }

    private fun getAudioHandler(): AudioSwitchHandler? {
        return room?.audioHandler as? AudioSwitchHandler
    }

    override fun getAudioDevices(): List<AudioDevice> {
        val handler = getAudioHandler() ?: return emptyList()

        return handler.availableAudioDevices
    }

    override fun selectAudioDevice(device: AudioDevice) {
        val handler = getAudioHandler() ?: return

        handler.selectDevice(device)
    }

    override fun startCall(sessionId: String, shouldPublish: Boolean): Room {
        val room = createRoom(sessionId)
        this.room = room
        this.sessionId = sessionId

        listenToParticipants()
        initializeCall(shouldPublish)

        return room
    }

    private fun listenToParticipants() {
        val room = room ?: throw IllegalStateException("Room is in incorrect state, null!")

        coroutineScope.launch {
            room.callParticipants.collectLatest { participants ->
                updateParticipantsSubscriptions(participants)
            }
        }
    }

    override fun joinCall(sessionId: String, shouldPublish: Boolean): Room {
        val room = createRoom(sessionId)
        this.room = room
        this.sessionId = sessionId

        listenToParticipants()
        initializeCall(shouldPublish)

        return room
    }

    private fun createRoom(sessionId: String): Room {
        this.sessionId = sessionId

        return buildRoom(sessionId)
    }

    private fun buildRoom(sessionId: String): Room {
        return Room(
            context = context,
            sessionId = sessionId,
            credentialsProvider = credentialsProvider,
            eglBase = peerConnectionFactory.eglBase,
        )
    }

    private fun initializeCall(shouldPublish: Boolean) {
        val connection = createConnection()
        subscriber = connection
        Log.d("sfuConnectFlow", connection.toString())

        coroutineScope.launch {
            createSignalingChannel(connection)
            Log.d("sfuConnectFlow", subscriber.toString())

            val joinResult = connectToCall(connection)

            if (joinResult is Success) {
                val isConnectionOpen = listenForConnectionOpened(joinResult.data)
                Log.d("sfuConnectFlow", "ConnectionOpen, $isConnectionOpen")

                if (!isConnectionOpen) {
                    clear()
                    return@launch
                }

                if (shouldPublish) {
                    createPublisher()
                }
                setupUserMedia(CallSettings(), shouldPublish)
            } else {
                clear()
            }
        }
    }

    private fun createConnection(): StreamPeerConnection {
        return peerConnectionFactory.makePeerConnection(
            configuration = connectionConfiguration,
            type = PeerConnectionType.SUBSCRIBER,
            mediaConstraints = mediaConstraints,
            onStreamAdded = { room?.addStream(it) },
            onStreamRemoved = { room?.removeStream(it) },
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
        signalChannel = connection.createDataChannel(
            "signaling",
            DataChannel.Init(),
            onMessage = ::onMessage,
            onStateChange = { state ->
                this.signalChannelState = state
            }
        )
        Log.d("sfuConnectFlow", signalChannel.toString())
    }

    private suspend fun connectToCall(connection: StreamPeerConnection): Result<JoinResponse> {
        val offerResult = connection.createJoinOffer()
        return if (offerResult is Success) {
            val joinResponse = executeJoinRequest(offerResult.data)

            joinResponse.onSuccessSuspend { response ->
                connection.onCallJoined(response.sdp)
            }

            return joinResponse
        } else {
            val error = offerResult as? Failure

            Failure(error?.error ?: VideoError())
        }
    }

    private suspend fun executeJoinRequest(data: SessionDescription): Result<JoinResponse> {
        val decoderCodecs = peerConnectionFactory.getVideoDecoderCodecs()
        val encoderCodecs = peerConnectionFactory.getVideoEncoderCodecs()

        val request = JoinRequest(
            subscriber_sdp_offer = data.description,
            session_id = sessionId,
            codec_settings = CodecSettings(
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

        return signalClient.join(request)
    }

    private suspend fun listenForConnectionOpened(response: JoinResponse): Boolean {
        var connected = signalChannelState == DataChannel.State.OPEN

        val timeoutTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)

        while (System.currentTimeMillis() < timeoutTime && !connected) {
            connected = signalChannelState == DataChannel.State.OPEN

            if (!connected) {
                delay(1000)
            }
        }

        return if (connected) {
            room?.startAudio()
            room?.loadParticipants(requireNotNull(response.call_state))
            signalChannel?.send("ss".encode()) ?: false
        } else {
            throw IllegalStateException("Couldn't connect to data channel.")
        }
    }

    private fun createPublisher() {
        publisher = peerConnectionFactory.makePeerConnection(
            connectionConfiguration,
            PeerConnectionType.PUBLISHER,
            mediaConstraints = MediaConstraints(),
            onNegotiationNeeded = ::negotiate
        )

        Log.d("sfuConnectFlow", "CreatePublisher, $publisher")
    }

    private fun negotiate(peerConnection: StreamPeerConnection) {
        coroutineScope.launch {
            peerConnection.createOffer().onSuccessSuspend { data ->
                Log.d("sfuConnectFlow", "Negotiate, ${data.description}")

                peerConnection.setLocalDescription(data)

                val request = SetPublisherRequest(
                    sdp = data.description,
                    session_id = sessionId
                )

                signalClient.setPublisher(request).onSuccessSuspend {
                    Log.d("sfuConnectFlow", "SetPublisher, ${it.sdp}")

                    peerConnection.setRemoteDescription(
                        SessionDescription(SessionDescription.Type.ANSWER, it.sdp)
                    )
                }
            }
        }
    }

    private fun setupUserMedia(callSettings: CallSettings, shouldPublish: Boolean) {
        val manager = context.getSystemService<AudioManager>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            manager?.allowedCapturePolicy = ALLOW_CAPTURE_BY_ALL
        }

        val mediaStream = peerConnectionFactory.createLocalMediaStream()
        localMediaStream = mediaStream

        val audioTrack = makeAudioTrack()
        audioTrack.setEnabled(true)
        localAudioTrack = audioTrack
        localMediaStream?.addTrack(audioTrack)
        Log.d("sfuConnectFlow", "SetupMedia, $audioTrack")

        val videoTrack = makeVideoTrack()
        localVideoTrack = videoTrack
        localMediaStream?.addTrack(videoTrack)
        Log.d("sfuConnectFlow", "SetupMedia, $videoTrack")

        if (shouldPublish) {
            publisher?.addAudioTransceiver(audioTrack, listOf(sessionId))
            publisher?.addVideoTransceiver(videoTrack, listOf(sessionId))
        }
    }

    private fun makeAudioTrack(): AudioTrack {
        val audioConstrains = MediaConstraints()
        val audioSource = peerConnectionFactory.makeAudioSource(audioConstrains)

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

        Log.d("sfuConnectFlow", "UpdateQuality, $enabledRids")
        val params = transceiver.sender.parameters

        val updatedEncodings = mutableListOf<RtpParameters.Encoding>()

        var encodingChanged = false
        Log.d("sfuConnectFlow", "CurrentQuality, $params")

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
            Log.d("sfuConnectFlow", "Updated encoding, $updatedEncodings")
            params.encodings.clear()
            params.encodings.addAll(updatedEncodings)

            publisher?.videoTransceiver?.sender?.parameters = params
        }
    }

    /**
     * Processes messages from the Data channel.
     */
    private fun onMessage(event: SfuDataEvent) {
        when (event) {
            is SubscriberOfferEvent -> setRemoteDescription(event.sdp)
            is SfuParticipantJoinedEvent -> room?.addParticipant(event)
            is SfuParticipantLeftEvent -> room?.removeParticipant(event)
            is ChangePublishQualityEvent -> {
                // updatePublishQuality(event) -> TODO - re-enable once we send the proper quality (dimensions)
            }
            is MuteStateChangeEvent -> room?.updateMuteState(event)
            else -> Unit
        }
    }

    private fun setRemoteDescription(sdp: String) {
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
                Log.d("sfuConnectFlow", "Updating subscriptions for ${user.id}")

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
            when (signalClient.updateSubscriptions(request)) {
                is Success -> {
                    Log.d("sfuConnectFlow", "Successful subscription update")
                }
                is Failure -> {
                    Log.d("sfuConnectFlow", "Failed subscription update")
                }
            }
        }
    }
}
