package io.getstream.video.android.core.call.connection

import io.getstream.log.taggedLogger
import io.getstream.result.flatMap
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.TrackDimensions
import io.getstream.video.android.core.call.connection.utils.wrapAPICall
import io.getstream.video.android.core.call.utils.TrackOverridesHandler
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.trySetEnabled
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.core.utils.safeSuspendingCallWithResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.TrackSubscriptionDetails
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse
import java.util.concurrent.ConcurrentHashMap
import io.getstream.result.Result

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
    companion object {
        val defaultVideoDimension = VideoDimension(720, 1080)
    }
    internal var onAddStream: ((MediaStream) -> Unit) = { _ -> }

    // Track dimensions state for this subscriber
    val trackDimensions = MutableStateFlow<Map<String, Map<TrackType, TrackDimensions>>>(emptyMap())
    val subscriptions = MutableStateFlow<List<TrackSubscriptionDetails>>(emptyList())

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

    /**
     * Sets the video subscriptions for the subscriber.
     *
     * @param useDefaults Whether to use the default tracks or not.
     */
    suspend fun setVideoSubscriptions(trackOverridesHandler: TrackOverridesHandler, participants: List<ParticipantState>, remoteParticipants: List<ParticipantState>, useDefaults: Boolean = false) : Result<UpdateSubscriptionsResponse> = safeSuspendingCallWithResult {
        logger.d { "[setVideoSubscriptions] #sfu; #track; useDefaults: $useDefaults" }
        var tracks = if (useDefaults) {
            // default is to subscribe to the top 5 sorted participants
            defaultTracks(participants)
        } else {
            // if we're not using the default, sub to visible tracks
            visibleTracks(remoteParticipants)
        }.let(trackOverridesHandler::applyOverrides)
        subscriptions.value = tracks
        val request = UpdateSubscriptionsRequest(
            session_id = sessionId,
            tracks = subscriptions.value,
        )
        sfuClient.updateSubscriptions(request)
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

    private fun defaultTracks(participants: List<ParticipantState>): List<TrackSubscriptionDetails> {
        val otherParticipants = participants.filter { it.sessionId != sessionId }.take(5)
        val tracks = mutableListOf<TrackSubscriptionDetails>()
        otherParticipants.forEach { participant ->
            if (participant.videoEnabled.value) {
                val track = TrackSubscriptionDetails(
                    user_id = participant.userId.value,
                    track_type = TrackType.TRACK_TYPE_VIDEO,
                    dimension = defaultVideoDimension,
                    session_id = participant.sessionId,
                )
                tracks.add(track)
            }
            if (participant.screenSharingEnabled.value) {
                val track = TrackSubscriptionDetails(
                    user_id = participant.userId.value,
                    track_type = TrackType.TRACK_TYPE_SCREEN_SHARE,
                    dimension = defaultVideoDimension,
                    session_id = participant.sessionId,
                )
                tracks.add(track)
            }
        }

        return tracks
    }

    private fun visibleTracks(remoteParticipants: List<ParticipantState>): List<TrackSubscriptionDetails> {
        val trackDisplayResolution = trackDimensions.value
        val tracks = remoteParticipants.map { participant ->
            val trackDisplay = trackDisplayResolution[participant.sessionId] ?: emptyMap()

            trackDisplay.entries.filter { it.value.visible }.map { display ->
                subscriberLogger.i {
                    "[visibleTracks] $sessionId subscribing ${participant.sessionId} to : ${display.key}"
                }
                TrackSubscriptionDetails(
                    user_id = participant.userId.value,
                    track_type = display.key,
                    dimension = display.value.dimensions,
                    session_id = participant.sessionId,
                )
            }
        }.flatten()
        return tracks
    }

    override fun onAddStream(stream: MediaStream?) {
        if (stream == null) {
            logger.w { "[onAddStream] #sfu; #track; stream is null" }
            return
        }
        this.onAddStream.invoke(stream)
    }
}



