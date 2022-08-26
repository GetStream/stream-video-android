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

import io.getstream.video.android.utils.Result
import io.getstream.video.android.webrtc.signal.SignalClient
import io.getstream.video.android.webrtc.utils.createValue
import io.getstream.video.android.webrtc.utils.setValue
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

public class PeerConnection(
    private val sessionId: String,
    private val type: PeerConnectionType,
    private val signalClient: SignalClient
) : PeerConnection.Observer {

    private lateinit var connection: PeerConnection

    public var onNegotiationNeeded: ((PeerConnection) -> Unit)? = null
    public var onStreamAdded: ((MediaStream) -> Unit)? = null
    public var onStreamRemoved: ((MediaStream) -> Unit)? = null

    public fun initialize(peerConnection: PeerConnection) {
        this.connection = peerConnection
    }

    public fun createDataChannel(label: String, init: DataChannel.Init): DataChannel {
        return connection.createDataChannel(label, init)
    }

    public suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> {
        return setValue { connection.setRemoteDescription(it, sessionDescription) }
    }

    public suspend fun createOffer(): Result<SessionDescription> {
        return createValue { connection.createOffer(it, null) }
    }

    /**
     * Peer connection listeners.
     */
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
    }

    override fun onIceCandidate(p0: IceCandidate?) {
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
    }

    override fun onAddStream(p0: MediaStream?) {
    }

    override fun onRemoveStream(p0: MediaStream?) {
    }

    override fun onDataChannel(p0: DataChannel?) {
    }

    override fun onRenegotiationNeeded() {
    }
}
