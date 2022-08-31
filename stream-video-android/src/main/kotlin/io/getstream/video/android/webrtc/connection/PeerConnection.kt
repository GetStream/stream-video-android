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

package io.getstream.video.android.webrtc.connection

import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.events.SfuDataEvent
import io.getstream.video.android.utils.Result
import io.getstream.video.android.webrtc.StreamPeerConnection
import io.getstream.video.android.webrtc.signal.SignalClient
import io.getstream.video.android.webrtc.utils.createValue
import io.getstream.video.android.webrtc.utils.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpParameters
import org.webrtc.RtpSender
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import stream.video.sfu.IceCandidateRequest

private typealias StreamDataChannel = io.getstream.video.android.webrtc.datachannel.DataChannel

public class PeerConnection(
    private val sessionId: String,
    private val type: PeerConnectionType,
    private val signalClient: SignalClient
) : PeerConnection.Observer {

    private val coroutineScope = CoroutineScope(DispatcherProvider.IO)

    private lateinit var connection: PeerConnection
    private var transceiver: RtpTransceiver? = null

    public var onNegotiationNeeded: ((StreamPeerConnection) -> Unit)? =
        null
    public var onStreamAdded: ((MediaStream) -> Unit)? = null
    public var onStreamRemoved: ((MediaStream) -> Unit)? = null

    public fun initialize(peerConnection: PeerConnection) {
        this.connection = peerConnection
    }

    public fun createDataChannel(
        label: String,
        init: DataChannel.Init,
        onMessage: (SfuDataEvent) -> Unit,
        onStateChange: (DataChannel.State) -> Unit
    ): StreamDataChannel {
        return StreamDataChannel(
            connection.createDataChannel(label, init),
            onMessage,
            onStateChange
        )
    }

    public suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> {
        return setValue { connection.setRemoteDescription(it, sessionDescription) }
    }

    public suspend fun createAnswer(): Result<SessionDescription> {
        return createValue { connection.createAnswer(it, MediaConstraints()) }
    }

    public suspend fun createOffer(): Result<SessionDescription> {
        return createValue { connection.createOffer(it, MediaConstraints()) }
    }

    public suspend fun setLocalDescription(sessionDescription: SessionDescription): Result<Unit> {
        return setValue { connection.setLocalDescription(it, sessionDescription) }
    }

    public fun addTrack(
        mediaStreamTrack: MediaStreamTrack,
        streamIds: List<String>
    ): RtpSender {
        return connection.addTrack(mediaStreamTrack, streamIds)
    }

    public fun addTransceiver(track: MediaStreamTrack, streamIds: List<String>) {
        val fullQuality = RtpParameters.Encoding(
            "f",
            true,
            1.0
        ).apply {
            maxBitrateBps = 1_200_000
        }

        val halfQuality = RtpParameters.Encoding(
            "h",
            true,
            2.0
        ).apply {
            maxBitrateBps = 500_000
        }

        val quarterQuality = RtpParameters.Encoding(
            "q",
            true,
            4.0
        ).apply {
            maxBitrateBps = 125_000
        }

        val encodings = listOf(fullQuality, halfQuality, quarterQuality)

        val transceiverInit = RtpTransceiver.RtpTransceiverInit(
            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
            streamIds,
            encodings
        )

        transceiver = connection.addTransceiver(track, transceiverInit)
    }

    /**
     * Peer connection listeners.
     */

    override fun onIceCandidate(candidate: IceCandidate?) {
        if (candidate == null) return

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

    override fun onAddStream(stream: MediaStream?) {
        stream?.let { it -> onStreamAdded?.invoke(it) }
    }

    override fun onRemoveStream(stream: MediaStream?) {
        stream?.let { it -> onStreamRemoved?.invoke(it) }
    }

    override fun onRenegotiationNeeded() {
        onNegotiationNeeded?.invoke(this)
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?): Unit = Unit
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?): Unit = Unit
    override fun onIceConnectionReceivingChange(p0: Boolean): Unit = Unit
    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?): Unit = Unit
    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?): Unit = Unit
    override fun onDataChannel(channel: DataChannel?): Unit = Unit
}
