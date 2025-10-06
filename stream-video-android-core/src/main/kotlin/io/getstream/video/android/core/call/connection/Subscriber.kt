/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.call.connection

import androidx.annotation.VisibleForTesting
import io.getstream.result.flatMap
import io.getstream.result.onErrorSuspend
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.TrackDimensions
import io.getstream.video.android.core.call.connection.job.RestartIceJobDelegate
import io.getstream.video.android.core.call.connection.stats.ComputedStats
import io.getstream.video.android.core.call.connection.utils.wrapAPICall
import io.getstream.video.android.core.call.utils.TrackOverridesHandler
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.trace.PeerConnectionTraceKey
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.trySetEnabled
import io.getstream.video.android.core.utils.SerialProcessor
import io.getstream.video.android.core.utils.enableStereo
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.core.utils.safeSuspendingCall
import io.getstream.webrtc.MediaConstraints
import io.getstream.webrtc.MediaStream
import io.getstream.webrtc.MediaStreamTrack
import io.getstream.webrtc.PeerConnection
import io.getstream.webrtc.RtpTransceiver
import io.getstream.webrtc.SessionDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import stream.video.sfu.models.Participant
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.TrackSubscriptionDetails
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import java.util.concurrent.ConcurrentHashMap

internal class Subscriber(
    private val sessionId: String,
    private val sfuClient: SignalServerService,
    private val coroutineScope: CoroutineScope,
    private val enableStereo: Boolean = true,
    private val tracer: Tracer,
    private val rejoin: () -> Unit,
    private val restartIceJobDelegate: RestartIceJobDelegate =
        RestartIceJobDelegate(coroutineScope),
    onIceCandidateRequest: ((IceCandidate, StreamPeerType) -> Unit)?,
) : StreamPeerConnection(
    type = StreamPeerType.SUBSCRIBER,
    mediaConstraints = MediaConstraints(),
    onStreamAdded = { _ -> },
    onNegotiationNeeded = { _, _ -> },
    onIceCandidate = { candidate, _ ->
        onIceCandidateRequest?.invoke(
            candidate,
            StreamPeerType.SUBSCRIBER,
        )
    },
    onRejoinNeeded = rejoin,
    traceCreateAnswer = false,
    tracer = tracer,
    maxBitRate = 0, // Set as needed
) {

    /**
     * Represents a received media stream.
     *
     * @property sessionId The session ID of the participant.
     * @property trackType The type of track.
     * @property mediaStream The media stream.
     */
    data class ReceivedMediaStream(
        val sessionId: String,
        val trackType: TrackType,
        val mediaStream: MediaTrack,
    )

    private data class ViewportCompositeKey(
        val sessionId: String,
        val viewportId: String,
        val trackType: TrackType,
    )

    companion object {
        /**
         * Default video dimension.
         */
        val defaultVideoDimension = VideoDimension(720, 1280)
        val throttledVideoDimension = VideoDimension(180, 320)
        val unknownVideoDimension = VideoDimension(0, 0)
    }

    private var enabled = MutableStateFlow(true)
    private var subscriptionsJob: Job? = null

    // Track dimensions and viewport visibility state for this subscriber
    private val trackDimensions = ConcurrentHashMap<ViewportCompositeKey, TrackDimensions>()
    private val subscriptions =
        ConcurrentHashMap<Pair<String, TrackType>, TrackSubscriptionDetails>()
    private val previousSubscriptions =
        ConcurrentHashMap<Pair<String, TrackType>, TrackSubscriptionDetails>()
    private val trackIdToTrackType = ConcurrentHashMap<String, TrackType>()

    // Tracks for all participants (sessionId -> (TrackType -> MediaTrack))
    internal val tracks: ConcurrentHashMap<String, ConcurrentHashMap<TrackType, MediaTrack>> =
        ConcurrentHashMap()

    private val sdpProcessor = SerialProcessor(coroutineScope)

    override suspend fun stats(): ComputedStats? = safeCallWithDefault(null) {
        return statsTracer?.get(trackIdToTrackType)
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        super.onIceConnectionChange(newState)
        when (newState) {
            PeerConnection.IceConnectionState.CONNECTED -> {
                restartIceJobDelegate.cancelScheduledRestartIce()
            }

            PeerConnection.IceConnectionState.FAILED -> {
                restartIceJobDelegate.scheduleRestartIce {
                    restartIce()
                }
            }

            PeerConnection.IceConnectionState.DISCONNECTED -> {
                restartIceJobDelegate.scheduleRestartIce(3000) {
                    restartIce()
                }
            }

            else -> {
                // no-op
            }
        }
    }

    /**
     * Returns the track dimensions for this subscriber.
     *
     * @return [Map] of track dimensions.
     */
    fun viewportDimensions(): Map<String, Map<TrackType, TrackDimensions>> {
        val result = mutableMapOf<String, Map<TrackType, TrackDimensions>>()
        trackDimensions.forEach { (key, value) ->
            val added = result[key.sessionId]
            if (added != null) {
                val currentDimension = added[key.trackType]
                if (currentDimension != null) {
                    if (currentDimension.dimensions.height * currentDimension.dimensions.width <
                        value.dimensions.height * value.dimensions.width
                    ) {
                        result[key.sessionId] = added + Pair(key.trackType, value)
                    }
                } else {
                    result[key.sessionId] = added + Pair(key.trackType, value)
                }
            } else {
                result[key.sessionId] = mapOf(Pair(key.trackType, value))
            }
        }
        return result
    }

    /**
     * Returns all subscriptions.
     *
     * @return [List] of all subscriptions.
     */
    fun subscriptions() = subscriptions.values.toList()

    /**
     * Returns the track for the given [sessionId] and [type].
     *
     * @param sessionId The session ID of the participant.
     * @param type The type of track.
     * @return [MediaTrack] for the given [sessionId] and [type].
     */
    fun getTrack(sessionId: String, type: TrackType): MediaTrack? {
        tracks.putIfAbsent(sessionId, ConcurrentHashMap())
        return tracks[sessionId]?.get(type)
    }

    /**
     * Sets the track for the given [sessionId] and [type].
     *
     * @param sessionId The session ID of the participant.
     * @param type The type of track.
     * @param track The track to set.
     */
    fun setTrack(sessionId: String, type: TrackType, track: MediaTrack) {
        tracks.putIfAbsent(sessionId, ConcurrentHashMap())
        tracks[sessionId]?.set(type, track)
    }

    /**
     * Removes all tracks.
     */
    fun clear() {
        sdpProcessor.stop()
        tracks.clear()
        trackDimensions.clear()
        subscriptions.clear()
    }

    /**
     * Disables all tracks.
     */
    fun disable() = safeCall {
        logger.d { "Disable all transceivers" }
        enabled.value = false
        connection.transceivers?.forEach {
            it.receiver.track()?.trySetEnabled(false)
        }
    }

    /**
     * Enables all tracks.
     */
    fun enable() = safeCall {
        logger.d { "Enable all transceivers" }
        enabled.value = true
        connection.transceivers?.forEach {
            it.receiver.track()?.trySetEnabled(true)
        }
    }

    /**
     * Negotiates the connection with the SFU.
     *
     * @param offerSdp The offer SDP from the SFU.
     */
    suspend fun negotiate(offerSdp: String) = sdpProcessor.submit("subscriberNegotiate") {
        val offerDescription = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        val result = setRemoteDescription(offerDescription)
            .onErrorSuspend {
                tracer.trace("negotiate-error-setremotedescription", it.message ?: "unknown")
            }
            .flatMap {
                createAnswer()
            }.map { answerSdp ->
                if (enableStereo) {
                    val stereoEnabled =
                        SessionDescription(
                            SessionDescription.Type.ANSWER,
                            enableStereo(offerSdp, answerSdp.description),
                        )
                    tracer.trace(
                        PeerConnectionTraceKey.CREATE_ANSWER.value,
                        stereoEnabled.description,
                    )
                    stereoEnabled
                } else {
                    answerSdp
                }
            }.flatMap { answerSdp ->
                setLocalDescription(answerSdp)
                    .onErrorSuspend {
                        tracer.trace(
                            "negotiate-error-setlocaldescription",
                            it.message ?: "unknown",
                        )
                    }
                    .map { answerSdp }
            }.flatMap { answerSdp ->
                val request = SendAnswerRequest(
                    PeerType.PEER_TYPE_SUBSCRIBER,
                    answerSdp.description,
                    sessionId,
                )
                safeCallWithResult {
                    sfuClient.sendAnswer(request)
                }.onErrorSuspend {
                    tracer.trace("negotiate-error-sendanswer", it.message ?: "unknown")
                }
            }
        logger.d { "Subscriber negotiate: $result" }
        result.getOrThrow()
    }.onFailure {
        tracer.trace("negotiate-error-submit", it.message ?: "unknown")
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
    }.onError {
        tracer.trace("iceRestart-error", it.message ?: "unknown")
    }

    /**
     * Sets the video subscriptions for the subscriber.
     *
     * @param useDefaults Whether to use the default tracks or not.
     */
    suspend fun setVideoSubscriptions(
        trackOverridesHandler: TrackOverridesHandler,
        participants: List<ParticipantState>,
        remoteParticipants: List<ParticipantState>,
        useDefaults: Boolean = false,
    ) = safeSuspendingCall {
        subscriptionsJob?.cancel()

        subscriptionsJob = coroutineScope.launch {
            delay(300)
            logger.d { "[setVideoSubscriptions] #sfu; #track; useDefaults: $useDefaults" }
            val tracks = if (useDefaults) {
                // default is to subscribe to the top 5 sorted participants
                defaultTracks(participants)
            } else {
                // if we're not using the default, sub to visible tracks
                visibleTracks(remoteParticipants)
            }.let(trackOverridesHandler::applyOverrides)

            val newTracks = tracks.associateBy { it.session_id to it.track_type }
            subscriptions.clear()
            subscriptions.putAll(newTracks)

            val subscriptionsChanged = newTracks.size != previousSubscriptions.size ||
                newTracks.any { (key, value) ->
                    val previous = previousSubscriptions[key]
                    previous == null || value != previous
                }

            if (!subscriptionsChanged) {
                logger.w { "[setVideoSubscriptions] Skipped — subscriptions unchanged." }
                return@launch
            }

            val request = UpdateSubscriptionsRequest(
                session_id = sessionId,
                tracks = subscriptions.map { it.value },
            )

            logger.d {
                "[setVideoSubscriptions] #sfu; #track; subscriptions: ${subscriptions.size} -> $subscriptions"
            }
            logger.d { "[setVideoSubscriptions] #sfu; #track; request: $request" }
            val response = sfuClient.updateSubscriptions(request)
            if (response.error == null) {
                logger.v { "[setVideoSubscriptions] #sfu; #track; no error, remembering subscriptions" }
                previousSubscriptions.clear()
                previousSubscriptions.putAll(subscriptions)
            }
        }
    }

    internal fun addTransceivers() {
        connection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY),
        )
        connection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY),
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
        val trackDisplayResolution = trackDimensions
        val tracks = remoteParticipants.map { participant ->
            val trackDisplay =
                trackDisplayResolution.filter { it.key.sessionId == participant.sessionId }

            trackDisplay.entries.filter { it.value.visible }.map { display ->
                logger.i {
                    "[visibleTracks] $sessionId subscribing ${participant.sessionId} to : ${display.key}"
                }
                TrackSubscriptionDetails(
                    user_id = participant.userId.value,
                    track_type = display.key.trackType,
                    dimension = display.value.dimensions.orThrottled(),
                    session_id = participant.sessionId,
                )
            }
        }.flatten()
        return tracks
    }

    override fun onAddStream(stream: MediaStream?) {
        super.onAddStream(stream)
        if (stream == null) {
            logger.w { "[onAddStream] #sfu; #track; stream is null" }
            return
        }
        onNewStream(stream)
    }

    fun participantLeft(participant: Participant) {
        tracks.remove(participant.session_id)
        trackDimensions.keys.removeAll { it.sessionId == participant.session_id }
    }

    private fun VideoDimension.isUnknown() =
        width == unknownVideoDimension.width && height == unknownVideoDimension.height

    private fun adjustForSubscriptionsSize(dimension: VideoDimension = defaultVideoDimension): VideoDimension {
        return if (subscriptions.size > 2 && dimension.height > throttledVideoDimension.height && dimension.width > throttledVideoDimension.width) {
            throttledVideoDimension
        } else {
            dimension
        }
    }

    private fun VideoDimension.normalized(): VideoDimension {
        return if (width <= height) this else VideoDimension(height, width)
    }

    fun setTrackDimension(
        viewportId: String,
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        dimensions: VideoDimension,
    ) {
        val key = ViewportCompositeKey(sessionId, viewportId, trackType)
        val actual = if (dimensions.isUnknown()) {
            val exists = trackDimensions.getOrDefault(key, TrackDimensions(defaultVideoDimension))
            adjustForSubscriptionsSize(exists.dimensions)
        } else {
            adjustForSubscriptionsSize(dimensions)
        }
        trackDimensions[key] = TrackDimensions(actual.normalized(), visible)
    }

    private fun VideoDimension.orThrottled(): VideoDimension {
        return if (subscriptions.size > 2 && width > throttledVideoDimension.width && height > throttledVideoDimension.height) {
            throttledVideoDimension
        } else {
            this
        }
    }

    private val trackPrefixToSessionIdMap = ConcurrentHashMap<String, String>()
    private val trackIdToParticipant = ConcurrentHashMap<String, String>()
    private val pendingStreams = mutableListOf<MediaStream>()

    fun setTrackLookupPrefixes(lookupPrefixes: Map<String, String>) = synchronized(pendingStreams) {
        safeCall {
            logger.d { "[setTrackLookupPrefixes] #sfu; #track; lookupPrefixes: $lookupPrefixes" }
            trackPrefixToSessionIdMap.clear()
            trackPrefixToSessionIdMap.putAll(lookupPrefixes)
            if (pendingStreams.isNotEmpty()) {
                pendingStreams.forEach {
                    onNewStream(it)
                }
                pendingStreams.clear()
            }
        }
    }

    private val streamsFlow =
        MutableSharedFlow<ReceivedMediaStream>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            extraBufferCapacity = 100,
        )

    fun streams(): Flow<ReceivedMediaStream> = streamsFlow

    @VisibleForTesting
    internal fun onNewStream(mediaStream: MediaStream) {
        logger.d { "[addStream] #sfu; #track; mediaStream: $mediaStream" }
        if (trackPrefixToSessionIdMap.isEmpty()) {
            logger.d { "[addStream] #sfu; #track; trackPrefixToSessionIdMap is empty, adding to pending" }
            synchronized(pendingStreams) {
                pendingStreams.add(mediaStream)
            }
            return
        }
        val (trackPrefix, trackTypeString) = mediaStream.id.split(':')
        logger.d {
            "[addStream] #sfu; #track; trackPrefix: $trackPrefix, trackTypeString: $trackTypeString"
        }
        val sessionId = trackPrefixToSessionIdMap[trackPrefix]
        if (sessionId == null || trackPrefixToSessionIdMap[trackPrefix].isNullOrEmpty()) {
            logger.d { "[addStream] skipping unrecognized trackPrefix $trackPrefix $mediaStream.id" }
            return
        }
        val trackTypeMap = mapOf(
            "TRACK_TYPE_UNSPECIFIED" to TrackType.TRACK_TYPE_UNSPECIFIED,
            "TRACK_TYPE_AUDIO" to TrackType.TRACK_TYPE_AUDIO,
            "TRACK_TYPE_VIDEO" to TrackType.TRACK_TYPE_VIDEO,
            "TRACK_TYPE_SCREEN_SHARE" to TrackType.TRACK_TYPE_SCREEN_SHARE,
            "TRACK_TYPE_SCREEN_SHARE_AUDIO" to TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO,
        )
        val trackType =
            trackTypeMap[trackTypeString] ?: TrackType.fromValue(trackTypeString.toInt())
                ?: throw IllegalStateException("trackType not recognized: $trackTypeString")

        logger.i { "[addStream] #sfu; mediaStream: $mediaStream" }
        mediaStream.audioTracks.forEach { track ->
            logger.v { "[addStream] #sfu; audioTrack: ${track.stringify()}" }
            track.setEnabled(true)
            val audioTrack = AudioTrack(
                streamId = mediaStream.id,
                audio = track,
            )
            trackIdToParticipant[track.id()] = sessionId
            trackIdToTrackType[track.id()] = trackType
            traceTrack(trackType, track.id(), listOf(mediaStream.id))
            setTrack(sessionId, trackType, audioTrack)
            streamsFlow.tryEmit(ReceivedMediaStream(sessionId, trackType, audioTrack))
        }

        mediaStream.videoTracks.forEach { track ->
            logger.w { "[addStream] #sfu; #track; videoTrack: ${track.stringify()}" }
            track.setEnabled(true)
            val videoTrack = VideoTrack(
                streamId = mediaStream.id,
                video = track,
            )
            trackIdToParticipant[track.id()] = sessionId
            trackIdToTrackType[track.id()] = trackType
            traceTrack(trackType, track.id(), listOf(mediaStream.id))
            setTrack(sessionId, trackType, videoTrack)
            streamsFlow.tryEmit(ReceivedMediaStream(sessionId, trackType, videoTrack))
        }
    }

    fun trackIdToParticipant(): Map<String, String> = trackIdToParticipant.toMap()

    fun isEnabled() = enabled
}
