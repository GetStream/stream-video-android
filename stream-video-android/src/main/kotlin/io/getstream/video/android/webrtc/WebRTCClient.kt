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
import android.util.Log
import androidx.core.content.getSystemService
import io.getstream.video.android.events.ChangePublishQualityEvent
import io.getstream.video.android.events.MuteStateChangeEvent
import io.getstream.video.android.events.SfuDataEvent
import io.getstream.video.android.events.SfuParticipantJoinedEvent
import io.getstream.video.android.events.SfuParticipantLeftEvent
import io.getstream.video.android.events.SubscriberOfferEvent
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.webrtc.connection.PeerConnectionFactory
import io.getstream.video.android.webrtc.connection.PeerConnectionType
import io.getstream.video.android.webrtc.signal.SignalClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.ByteString.Companion.encode
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import stream.video.sfu.CodecSettings
import stream.video.sfu.JoinRequest
import stream.video.sfu.JoinResponse
import stream.video.sfu.Participant
import stream.video.sfu.PeerType
import stream.video.sfu.SendAnswerRequest
import stream.video.sfu.SetPublisherRequest
import stream.video.sfu.UpdateSubscriptionsRequest
import stream.video.sfu.VideoCodecs
import stream.video.sfu.VideoDimension
import java.util.concurrent.TimeUnit

internal typealias StreamPeerConnection = io.getstream.video.android.webrtc.connection.PeerConnection
internal typealias StreamDataChannel = io.getstream.video.android.webrtc.datachannel.DataChannel

public class WebRTCClient(
    private val sessionId: String,
    private val context: Context,
    private val credentialsProvider: CredentialsProvider,
    private val signalClient: SignalClient
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var subscriber: StreamPeerConnection? = null
    private var publisher: StreamPeerConnection? = null

    private val callParticipants: MutableMap<String, Participant> = mutableMapOf()

    private var signalChannel: StreamDataChannel? = null
    private var signalChannelState: DataChannel.State = DataChannel.State.CLOSED

    private val peerConnectionFactory by lazy { PeerConnectionFactory(context, signalClient) }
    public val eglBase: EglBase by lazy { peerConnectionFactory.eglBase }

    /**
     * Video tracks
     */
    private val cameraManager by lazy { context.getSystemService<CameraManager>() }
    private var videoCapturer: VideoCapturer? = null
    private val cameraEnumerator: CameraEnumerator by lazy {
        Camera2Enumerator(context)
    }
    private val surfaceTextureHelper by lazy {
        SurfaceTextureHelper.create(
            "CaptureThread",
            peerConnectionFactory.eglBase.eglBaseContext
        )
    }

    private var localVideoTrack: VideoTrack? = null
        set(value) {
            field = value
            if (value != null) {
                onLocalVideoTrackChange(value)
            }
        }
    private var localAudioTrack: AudioTrack? = null

    public var onLocalVideoTrackChange: (VideoTrack) -> Unit = {}
    public var onStreamAdded: (MediaStream) -> Unit = {}
    public var onStreamRemoved: (MediaStream) -> Unit = {}
    public var onParticipantsUpdated: (Map<String, Participant>) -> Unit = {}

    public fun connect(shouldPublish: Boolean) {
        val connection = createConnection()
        Log.d("sfuConnectFlow", connection.toString())

        coroutineScope.launch {
            createSubscriber(connection)
            Log.d("sfuConnectFlow", subscriber.toString())

            joinCall(connection)

            val isConnectionOpen = listenForConnectionOpened()
            Log.d("sfuConnectFlow", "ConnectionOpen, $isConnectionOpen")

            if (!isConnectionOpen) return@launch

            if (shouldPublish) {
                createPublisher()
            }
            setupUserMedia(CallSettings(), shouldPublish)
        }
    }

    private fun createConnection(): StreamPeerConnection {
        val connectionConfig = PeerConnection.RTCConfiguration(emptyList()).apply {
            this.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        return peerConnectionFactory.makePeerConnection(
            sessionId,
            connectionConfig,
            PeerConnectionType.SUBSCRIBER
        ).apply {
            onStreamAdded = ::addStream
            onStreamRemoved = ::removeStream
        }
    }

    private fun createSubscriber(connection: StreamPeerConnection) {
        subscriber = connection

        signalChannel = connection.createDataChannel(
            "signaling",
            DataChannel.Init().apply {
                // TODO - setup init data
            },
            onMessage = ::onMessage,
            onStateChange = { state ->
                this.signalChannelState = state
            }
        )
        Log.d("sfuConnectFlow", signalChannel.toString())
    }

    private fun addStream(mediaStream: MediaStream) {
        Log.d("sfuConnectFlow", "StreamAdded, $mediaStream")
        onStreamAdded(mediaStream)
    }

    private fun removeStream(mediaStream: MediaStream) {
        Log.d("sfuConnectFlow", "StreamRemoved, $mediaStream")
        onStreamRemoved(mediaStream)
    }

    private suspend fun joinCall(connection: StreamPeerConnection) {
        when (val offer = connection.createOffer()) {
            is Success -> {
                Log.d("sfuConnectFlow", "JoinCall, ${offer.data.description}")
                connection.setLocalDescription(offer.data)

                when (val joinResponse = executeJoinRequest(offer.data)) {
                    is Success -> {
                        Log.d("sfuConnectFlow", "ExecuteJoin, ${joinResponse.data.sdp}")
                        val sdp = joinResponse.data.sdp

                        callParticipants.putAll(loadParticipants(joinResponse.data))
                        Log.d("sfuConnectFlow", "ExecuteJoin, $callParticipants")

                        connection.setRemoteDescription(
                            SessionDescription(SessionDescription.Type.ANSWER, sdp)
                        )
                    }
                    is Failure -> {
                        Log.d("sfuConnectFlow", "JoinFailure", joinResponse.error.cause)
                    }
                }
            }
            is Failure -> {
                Log.d("sfuConnectFlow", "OfferFailure", offer.error.cause)
            }
        }
    }

    private fun loadParticipants(data: JoinResponse): Map<String, Participant> {
        return data.call_state?.participants?.associateBy {
            it.user?.id!!
        } ?: emptyMap()
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
                )
            )
        )

        return signalClient.join(request)
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
            throw IllegalStateException("Couldn't connect to data channel.")
        }
    }

    private fun createPublisher() {
        val connectionConfig = PeerConnection.RTCConfiguration(emptyList()).apply {
            this.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        publisher = peerConnectionFactory.makePeerConnection(
            sessionId,
            connectionConfig,
            PeerConnectionType.PUBLISHER
        )

        Log.d("sfuConnectFlow", "CreatePublisher, $publisher")
        publisher?.onNegotiationNeeded = ::handleNegotiationNeeded
    }

    private fun updateParticipantsSubscriptions() {
        val subscriptions = mutableMapOf<String, VideoDimension>()
        val userId = credentialsProvider.getUserCredentials().id

        for ((id, user) in callParticipants) {
            if (id != userId) {
                Log.d("sfuConnectFlow", "Updating subscriptions for $id")

                // TODO - fetch the track size
                val dimension = VideoDimension(
//                    height = user.trackSize.height,
//                    width = user.trackSize.width
                )
                subscriptions[id] = dimension
            }
        }
        val request = UpdateSubscriptionsRequest(
            session_id = sessionId,
            subscriptions = subscriptions
        )

        coroutineScope.launch {
            signalClient.updateSubscriptions(request)
        }
    }

    private fun handleNegotiationNeeded(peerConnection: StreamPeerConnection) {
        negotiate(peerConnection)
    }

    private fun negotiate(peerConnection: StreamPeerConnection) {
        coroutineScope.launch {
            when (val offer = peerConnection.createOffer()) {
                is Success -> {
                    val data = offer.data
                    Log.d("sfuConnectFlow", "Negotiate, ${data.description}")

                    peerConnection.setLocalDescription(data)

                    val request = SetPublisherRequest(
                        sdp = data.description,
                        session_id = sessionId
                    )

                    when (val response = signalClient.setPublisher(request)) {
                        is Success -> {
                            Log.d("sfuConnectFlow", "SetPublisher, ${response.data.sdp}")
                            val publisherData = response.data
                            val sdp = publisherData.sdp

                            peerConnection.setRemoteDescription(
                                SessionDescription(SessionDescription.Type.ANSWER, sdp)
                            )
                        }
                        is Failure -> {
                            // TODO - error handling
                        }
                    }
                }
                is Failure -> {
                    // TODO - error handling
                }
            }
        }
    }

    private fun setupUserMedia(callSettings: CallSettings, shouldPublish: Boolean) {
        val audioTrack = makeAudioTrack()
        localAudioTrack = audioTrack
        Log.d("sfuConnectFlow", "SetupMedia, $audioTrack")

        val videoTrack = makeVideoTrack()
        localVideoTrack = videoTrack
        Log.d("sfuConnectFlow", "SetupMedia, $videoTrack")

        if (shouldPublish) {
            publisher?.addTrack(audioTrack, listOf(sessionId))
            publisher?.addTransceiver(videoTrack, listOf(sessionId))
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

    public fun startCapturingLocalVideo(
        renderer: SurfaceViewRenderer,
        position: Int,
    ) {
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

        /**
         * We pick the highest resolution.
         */
        val resolution = supportedFormats.firstOrNull() ?: return

        capturer.startCapture(resolution.width, resolution.height, resolution.framerate.max)
        localVideoTrack?.addSink(renderer)
    }

    /**
     * Processes messages from the Data channel.
     */
    private fun onMessage(event: SfuDataEvent) {
        when (event) {
            is SubscriberOfferEvent -> setRemoteDescription(event.sdp)
            is SfuParticipantJoinedEvent -> addParticipant(event)
            is SfuParticipantLeftEvent -> removeParticipant(event)
            is ChangePublishQualityEvent -> updatePublishQuality(event)
            is MuteStateChangeEvent -> updateMuteState(event)
            else -> Unit
        }
    }

    private fun updateMuteState(event: MuteStateChangeEvent) {
    }

    private fun updatePublishQuality(event: ChangePublishQualityEvent) {
    }

    private fun addParticipant(event: SfuParticipantJoinedEvent) {
        val userId = event.participant.user?.id ?: return

        callParticipants[userId] = event.participant
        onParticipantsUpdated(callParticipants)
        updateParticipantsSubscriptions()
    }

    private fun removeParticipant(event: SfuParticipantLeftEvent) {
        callParticipants.remove(event.participant.user?.id)
        onParticipantsUpdated(callParticipants)
        updateParticipantsSubscriptions()
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
                is Success -> sendAnswer(result.data)
                is Failure -> {
                    // TODO handle error
                }
            }
        }
    }

    private suspend fun sendAnswer(sessionDescription: SessionDescription) {
        val sendAnswerRequest = SendAnswerRequest(
            session_id = sessionId,
            peer_type = PeerType.SUBSCRIBER,
            sdp = sessionDescription.description
        )

        signalClient.sendAnswer(sendAnswerRequest)
    }
}
