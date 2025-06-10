package io.getstream.video.android.core.call.connection

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.flatMap
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.connection.utils.wrapAPICall
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.trySetEnabled
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackType
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.SendAnswerRequest

internal class Subscriber(
    private val sessionId: String,
    private val sfuClient: SignalServerService,
    private val coroutineScope: CoroutineScope,
    onIceCandidateRequest: ((IceCandidate, StreamPeerType) -> Unit)?
) : StreamPeerConnection(
    coroutineScope = coroutineScope,
    type = StreamPeerType.SUBSCRIBER,
    mediaConstraints = MediaConstraints(),
    onStreamAdded = { _ -> },
    onNegotiationNeeded = { _, _ -> },
    onIceCandidate = { candidate, _ ->
        onIceCandidateRequest?.invoke(
            candidate,
            StreamPeerType.SUBSCRIBER
        )
    },
    maxBitRate = 0 // Set as needed
) {
    private val subscriberLogger by taggedLogger("Video:Subscriber")

    internal var onAddStream: ((MediaStream) -> Unit) = { _ -> }

    internal fun addTransceivers() {
        connection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )
        connection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )
    }

    suspend fun disable() = safeCall {
        subscriberLogger.d { "Disable all transceivers" }
        connection.transceivers?.forEach {
            it.receiver.track()?.trySetEnabled(false)
        }
    }

    suspend fun enable() = safeCall {
        subscriberLogger.d { "Enable all transceivers" }
        connection.transceivers?.forEach {
            it.receiver.track()?.trySetEnabled(true)
        }
    }

    suspend fun negotiate(offerSdp: String) {
        val offerDescription = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        val result = setRemoteDescription(offerDescription).flatMap {
            createAnswer()
        }.flatMap { answerSdp ->
            setLocalDescription(answerSdp).map { answerSdp }
        }.flatMap { answerSdp ->
            val request = SendAnswerRequest(
                PeerType.PEER_TYPE_SUBSCRIBER, answerSdp.description, sessionId,
            )
            safeCallWithResult {
                sfuClient.sendAnswer(request)
            }
        }
        logger.d { "Subscriber negotiate: $result" }
    }

    suspend fun restartIce() = wrapAPICall {
        val request = ICERestartRequest(
            session_id = sessionId,
            peer_type = PeerType.PEER_TYPE_SUBSCRIBER,
        )
        sfuClient.iceRestart(request)
    }

    override fun onAddStream(stream: MediaStream?) {
        if (stream == null) {
            logger.w { "[onAddStream] #sfu; #track; stream is null" }
            return
        }
        this.onAddStream.invoke(stream)
    }
}
