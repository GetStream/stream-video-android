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

import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Success
import io.getstream.video.android.webrtc.connection.PeerConnectionFactory
import io.getstream.video.android.webrtc.connection.PeerConnectionType
import io.getstream.video.android.webrtc.signal.SignalClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.VideoCapturer
import stream.video.sfu.PeerType
import stream.video.sfu.SendAnswerRequest
import stream.video.sfu.SubscriberOffer
import java.util.*

private typealias StreamPeerConnection = io.getstream.video.android.webrtc.connection.PeerConnection

public class WebRTCClient(
    private val credentialsProvider: CredentialsProvider,
    private val signalClient: SignalClient
) : DataChannel.Observer {
    private val coroutineScope = CoroutineScope(Dispatchers.Main) // TODO scope

    private var subscriber: StreamPeerConnection? = null
    private var publisher: StreamPeerConnection? = null

    private var signalChannel: DataChannel? = null

    private val sessionId: String = UUID.randomUUID().toString() // TODO - pass in to Client

    private val peerConnectionFactory by lazy { PeerConnectionFactory(signalClient) }

    /**
     * Video tracks
     */
    private var videoCapturer: VideoCapturer? = null
    private var localVideoTrack: VideoCapturer? = null
        set(value) {
            // TODO - update local track
            field = value
        }

    private var localAudioTrack: AudioTrack? = null

    public fun connect(shouldPublishVideo: Boolean) {
        val connectionConfig = PeerConnection.RTCConfiguration(emptyList()).apply {
            this.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        val connection = peerConnectionFactory.makePeerConnection(
            sessionId,
            connectionConfig,
            PeerConnectionType.SUBSCRIBER
        )
        subscriber = connection

        signalChannel = connection.createDataChannel(
            "signaling",
            DataChannel.Init().apply {
                // TODO - setup init data
            }
        )

        signalChannel?.registerObserver(this)
    }

    /**
     * Data Channel callbacks.
     */
    override fun onBufferedAmountChange(p0: Long): Unit = Unit
    override fun onStateChange(): Unit = Unit

    /**
     * Processes messages from the Data channel.
     */
    override fun onMessage(buffer: DataChannel.Buffer?) {
        val event = buffer?.data ?: return
        // TODO - parse event
        val offer = try {
            SubscriberOffer.ADAPTER.decode(event.array())
        } catch (error: Throwable) {
            null
        }

        if (offer != null) {
            setRemoteDescription(offer)
        }
    }

    private fun setRemoteDescription(offer: SubscriberOffer) {
        val subscriber = subscriber ?: return

        val sdp = offer.sdp
        val sessionDescription = SessionDescription(
            SessionDescription.Type.OFFER,
            sdp
        )

        coroutineScope.launch {
            subscriber.setRemoteDescription(sessionDescription)

            when (val result = subscriber.createOffer()) {
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

        when (signalClient.sendAnswer(sendAnswerRequest)) {
            is Success -> setupCall()
            is Failure -> {}
        }
    }

    private fun setupCall() {
    }
}
