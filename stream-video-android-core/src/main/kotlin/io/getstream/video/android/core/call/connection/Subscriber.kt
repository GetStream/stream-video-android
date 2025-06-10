package io.getstream.video.android.core.call.connection

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.flatMap
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.TrackDimensions
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
import java.util.concurrent.ConcurrentHashMap

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

    // Track dimensions state for this subscriber
    val trackDimensions = MutableStateFlow<Map<String, Map<TrackType, TrackDimensions>>>(emptyMap())

    // Tracks for all participants (sessionId -> (TrackType -> MediaTrack))
    private val _tracks: ConcurrentHashMap<String, ConcurrentHashMap<TrackType, MediaTrack>> = ConcurrentHashMap()

    /**
     * @return [Map] of all tracks for all participants.
     */
    val tracks: Map<String, Map<TrackType, MediaTrack>>
        get() = _tracks

    /**
     * Returns the track for the given [sessionId] and [type].
     *
     * @param sessionId The session ID of the participant.
     * @param type The type of track.
     * @return [MediaTrack] for the given [sessionId] and [type].
     */
    fun getTrack(sessionId: String, type: TrackType): MediaTrack? {
        _tracks.putIfAbsent(sessionId, ConcurrentHashMap())
        return _tracks[sessionId]?.get(type)
    }

    /**
     * Sets the track for the given [sessionId] and [type].
     *
     * @param sessionId The session ID of the participant.
     * @param type The type of track.
     * @param track The track to set.
     */
    fun setTrack(sessionId: String, type: TrackType, track: MediaTrack) {
        _tracks.putIfAbsent(sessionId, ConcurrentHashMap())
        _tracks[sessionId]?.set(type, track)
    }

    /**
     * Removes the tracks for the given [sessionId].
     *
     * @param sessionId The session ID of the participant.
     * @return [ConcurrentHashMap] of all tracks for the given [sessionId].
     */
    fun removeTracks(sessionId: String): ConcurrentHashMap<TrackType, MediaTrack>? {
        return _tracks.remove(sessionId)
    }

    /**
     * Removes all tracks.
     */
    fun clearTracks() {
        _tracks.clear()
    }

    /**
     * Disables all tracks.
     */
    fun disable() = safeCall {
        subscriberLogger.d { "Disable all transceivers" }
        connection.transceivers?.forEach {
            it.receiver.track()?.trySetEnabled(false)
        }
    }

    /**
     * Enables all tracks.
     */
    fun enable() = safeCall {
        subscriberLogger.d { "Enable all transceivers" }
        connection.transceivers?.forEach {
            it.receiver.track()?.trySetEnabled(true)
        }
    }

    /**
     * Negotiates the connection with the SFU.
     *
     * @param offerSdp The offer SDP from the SFU.
     */
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

    /**
     * Restarts the ICE connection with the SFU.
     */
    suspend fun restartIce() = wrapAPICall {
        val request = ICERestartRequest(
            session_id = sessionId,
            peer_type = PeerType.PEER_TYPE_SUBSCRIBER,
        )
        sfuClient.iceRestart(request)
    }

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

    override fun onAddStream(stream: MediaStream?) {
        if (stream == null) {
            logger.w { "[onAddStream] #sfu; #track; stream is null" }
            return
        }
        this.onAddStream.invoke(stream)
    }
}

