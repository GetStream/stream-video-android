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

import android.util.Log
import io.getstream.video.android.events.SfuDataEvent
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.webrtc.utils.createValue
import io.getstream.video.android.webrtc.utils.setValue
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpParameters
import org.webrtc.RtpSender
import org.webrtc.RtpTransceiver
import org.webrtc.RtpTransceiver.RtpTransceiverInit
import org.webrtc.SessionDescription

private typealias StreamDataChannel = io.getstream.video.android.webrtc.datachannel.StreamDataChannel

public class StreamPeerConnection(
    private val type: PeerConnectionType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)?,
    private val onStreamRemoved: ((MediaStream) -> Unit)?,
    private val onNegotiationNeeded: ((StreamPeerConnection) -> Unit)?,
    private val onIceCandidate: ((IceCandidate, PeerConnectionType) -> Unit)?
) : PeerConnection.Observer {

    public lateinit var connection: PeerConnection
        private set
    public var videoTransceiver: RtpTransceiver? = null
        private set
    public var audioTransceiver: RtpTransceiver? = null
        private set

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

    public suspend fun createOffer(): Result<SessionDescription> {
        return createValue { connection.createOffer(it, MediaConstraints()) } // TODO we should send mediaConstraints here too, but BE crashes
    }

    public suspend fun createAnswer(): Result<SessionDescription> {
        return createValue { connection.createAnswer(it, mediaConstraints) }
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

    public fun addAudioTransceiver(track: MediaStreamTrack, streamIds: List<String>) {
        val fullQuality = RtpParameters.Encoding(
            "a",
            true,
            1.0
        ).apply {
            maxBitrateBps = 500_000
        }

        val encodings = listOf(fullQuality)

        val transceiverInit = RtpTransceiverInit(
            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
            streamIds,
            encodings
        )

        audioTransceiver = connection.addTransceiver(track, transceiverInit)
    }

    public fun addVideoTransceiver(track: MediaStreamTrack, streamIds: List<String>) {
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

        val transceiverInit = RtpTransceiverInit(
            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
            streamIds,
            encodings
        )

        videoTransceiver = connection.addTransceiver(track, transceiverInit)
    }

    public suspend fun createJoinOffer(): Result<SessionDescription> {
        val offer = createOffer()

        when (offer) {
            is Success -> {
                Log.d("sfuConnectFlow", "JoinCall, ${offer.data.description}")
                setLocalDescription(offer.data)
            }
            is Failure -> {
                Log.d("sfuConnectFlow", "OfferFailure", offer.error.cause)
            }
        }

        return offer
    }

    public suspend fun onCallJoined(sdp: String) {
        Log.d("sfuConnectFlow", "ExecuteJoin, $sdp")

        setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    public fun addLocalStream(mediaStream: MediaStream) {
        connection.addStream(mediaStream)
    }

    /**
     * Peer connection listeners.
     */

    override fun onIceCandidate(candidate: IceCandidate?) {
        if (candidate == null) return

        onIceCandidate?.invoke(candidate, type)
    }

    override fun onAddStream(stream: MediaStream?) {
        if (stream != null) {
            onStreamAdded?.invoke(stream)
        }
    }

    override fun onRenegotiationNeeded() {
        onNegotiationNeeded?.invoke(this)
    }

    override fun onRemoveStream(stream: MediaStream?) {
        if (stream != null) {
            onStreamRemoved?.invoke(stream)
        }
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?): Unit = Unit
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?): Unit = Unit
    override fun onIceConnectionReceivingChange(p0: Boolean): Unit = Unit
    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?): Unit = Unit
    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?): Unit = Unit
    override fun onDataChannel(channel: DataChannel?): Unit = Unit
}
