/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.call.connection.stats.ComputedStats
import io.getstream.video.android.core.call.connection.stats.StatsTracer
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.call.stats.toRtcStats
import io.getstream.video.android.core.call.utils.addRtcIceCandidate
import io.getstream.video.android.core.call.utils.createValue
import io.getstream.video.android.core.call.utils.setValue
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.toDomainCandidate
import io.getstream.video.android.core.model.toPeerType
import io.getstream.video.android.core.model.toRtcCandidate
import io.getstream.video.android.core.trace.PeerConnectionTraceKey
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.utils.defaultConstraints
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.stringify
import io.getstream.webrtc.CandidatePairChangeEvent
import io.getstream.webrtc.DataChannel
import io.getstream.webrtc.IceCandidateErrorEvent
import io.getstream.webrtc.MediaConstraints
import io.getstream.webrtc.MediaStream
import io.getstream.webrtc.MediaStreamTrack
import io.getstream.webrtc.PeerConnection
import io.getstream.webrtc.RtpParameters
import io.getstream.webrtc.RtpReceiver
import io.getstream.webrtc.RtpTransceiver
import io.getstream.webrtc.RtpTransceiver.RtpTransceiverInit
import io.getstream.webrtc.SessionDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import stream.video.sfu.models.TrackType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import io.getstream.webrtc.IceCandidate as RtcIceCandidate

/**
 * Wrapper around the WebRTC connection that contains tracks.
 *
 * @param coroutineScope The scope used to listen to stats events.
 * @param type The internal type of the PeerConnection. Check [StreamPeerType].
 * @param mediaConstraints Constraints used for the connections.
 * @param onStreamAdded Handler when a new [MediaStream] gets added.
 * @param onNegotiationNeeded Handler when there's a new negotiation.
 * @param onIceCandidate Handler whenever we receive [IceCandidate]s.
 * @param maxBitRate The maximum bitrate for the connection.
 * @param traceCreateAnswer Whether to trace the create answer event or not.
 * @param tracer The tracer used to trace the connection.
 */
open class StreamPeerConnection(
    private val type: StreamPeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)?,
    private val onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)?,
    private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)?,
    private val onRejoinNeeded: () -> Unit,
    private val onFastReconnectNeeded: () -> Unit,
    private val maxBitRate: Int,
    private val traceCreateAnswer: Boolean = true,
    private val tracer: Tracer,
    private val tag: String,
) : PeerConnection.Observer {

    private val localDescriptionMutex = Mutex()
    private val remoteDescriptionMutex = Mutex()
    private val iceCandidatesMutex =
        Mutex() // Not needed in current logic flow, but kept it for safety.

    internal var localSdp: SessionDescription? = null
    internal var remoteSdp: SessionDescription? = null
    private val iceCandidates = mutableListOf<IceCandidate>()
    private val typeTag = type.stringify()

    // see https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/iceConnectionState
    internal val state = MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
    internal val iceState = MutableStateFlow<PeerConnection.IceConnectionState?>(null)

    open suspend fun stats(): ComputedStats? = null

    internal val logger by taggedLogger("Call:PeerConnection:$typeTag")

    /**
     * The wrapped connection for all the WebRTC communication.
     */
    public lateinit var connection: PeerConnection
        private set

    /**
     * Transceiver used to send video in different resolutions.
     */
    public var videoTransceiver: RtpTransceiver? = null
        private set

    /**
     * Transceiver used to send audio.
     */
    public var audioTransceiver: RtpTransceiver? = null
        private set

    fun isHealthy(): Boolean {
        return when (state.value) {
            PeerConnection.PeerConnectionState.NEW,
            PeerConnection.PeerConnectionState.CONNECTED,
            PeerConnection.PeerConnectionState.CONNECTING,
            -> true

            else -> false
        }
    }

    fun isFailedOrClosed(): Boolean {
        val pcFailedOrClosed = when (state.value) {
            PeerConnection.PeerConnectionState.CLOSED,
            PeerConnection.PeerConnectionState.FAILED,
            -> true

            else -> false
        }
        val iceFailedOrClosed = when (iceState.value) {
            PeerConnection.IceConnectionState.FAILED,
            PeerConnection.IceConnectionState.CLOSED,
            -> true

            else -> false
        }

        return pcFailedOrClosed || iceFailedOrClosed
    }

    init {
        logger.i { "<init> #sfu; #$typeTag; mediaConstraints: $mediaConstraints" }
    }

    internal var statsTracer: StatsTracer? = null

    fun tracer(): Tracer = tracer

    /**
     * Initialize a [StreamPeerConnection] using a WebRTC [PeerConnection].
     *
     * @param peerConnection The connection that holds audio and video tracks.
     */
    public fun initialize(peerConnection: PeerConnection) {
        logger.d { "[initialize] #sfu; #$typeTag; peerConnection: $peerConnection" }
        this.connection = peerConnection
        this.statsTracer = StatsTracer(connection, type.toPeerType())
        this.state.value = this.connection.connectionState()
        this.iceState.value = this.connection.iceConnectionState()
    }

    /**
     * Used to create an offer whenever there's a negotiation that we need to process on the
     * publisher side.
     *
     * @return [Result] wrapper of the [SessionDescription] for the publisher.
     */
    public suspend fun createOffer(
        mediaConstraints: MediaConstraints = defaultConstraints,
    ): Result<SessionDescription> {
        logger.d { "[createOffer] #sfu; #$typeTag; no args" }
        return createValue {
            connection.createOffer(
                it,
                mediaConstraints,
            )
            tracer.trace(PeerConnectionTraceKey.CREATE_OFFER.value, mediaConstraints.toString())
        }
    }

    /**
     * Used to create an answer whenever there's a subscriber offer.
     *
     * @return [Result] wrapper of the [SessionDescription] for the subscriber.
     */
    public suspend fun createAnswer(
        mediaConstraints: MediaConstraints = defaultConstraints,
    ): Result<SessionDescription> {
        logger.d { "[createAnswer] #sfu; #$typeTag; no args" }
        return createValue { connection.createAnswer(it, mediaConstraints) }.also { result ->
            logger.d { "[createAnswer] #sfu; #$typeTag; result: $result" }
            when (result) {
                is Result.Success -> {
                    logger.d { "[createAnswer] #sfu; #$typeTag; sdp: ${result.value.description}" }
                    if (traceCreateAnswer) {
                        tracer.trace(
                            PeerConnectionTraceKey.CREATE_ANSWER.value,
                            result.value.description,
                        )
                    }
                }

                is Result.Failure -> {
                    logger.e { "[createAnswer] #sfu; #$typeTag; error: ${result.value.message}" }
                }
            }
        }
    }

    /**
     * Used to set up the SDP on underlying connections and to add [pendingIceCandidates] to the
     * connection for listening.
     *
     * @param sessionDescription That contains the remote SDP.
     * @return An empty [Result], if the operation has been successful or not.
     */
    public suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> {
        remoteSdp = sessionDescription
        val result: Result<Unit>

        remoteDescriptionMutex.withLock {
            result = setValue {
                connection.setRemoteDescription(
                    it,
                    SessionDescription(
                        sessionDescription.type,
                        sessionDescription.description.mungeCodecs(),
                    ),
                )
            }

            logger.d { "[setRemoteDescription] #ice; #sfu; #$typeTag; result: $result" }
            tracer.trace(
                PeerConnectionTraceKey.SET_REMOTE_DESCRIPTION.value,
                sessionDescription.description,
            )
            if (result.isSuccess) processIceCandidates()
        }

        return result
    }

    private suspend fun processIceCandidates() {
        logger.d { "[processIceCandidates] #ice; #sfu; #$typeTag; count: ${iceCandidates.count()}" }

        iceCandidatesMutex.withLock {
            iceCandidates.forEach { addIceCandidate(it) }
            iceCandidates.clear()
        }
    }

    /**
     * Sets the local description for a connection either for the subscriber or publisher based on
     * the flow.
     *
     * @param sessionDescription That contains the subscriber or publisher SDP.
     * @return An empty [Result], if the operation has been successful or not.
     */
    public suspend fun setLocalDescription(sessionDescription: SessionDescription): Result<Unit> {
        val sdp = SessionDescription(
            sessionDescription.type,
            sessionDescription.description.mungeCodecs(),
        )

        localSdp = sdp

        logger.d { "[setLocalDescription] #sfu; #$typeTag; offerSdp: ${sessionDescription.stringify()}" }
        // This needs a mutex because parallel calls will result in:
        // SfuSocketError: subscriber PC: negotiation failed
        return localDescriptionMutex.withLock {
            setValue {
                // Never call this in parallel
                connection.setLocalDescription(it, sdp)
                tracer.trace(PeerConnectionTraceKey.SET_LOCAL_DESCRIPTION.value, sdp.description)
            }
        }
    }

    public suspend fun handleNewIceCandidate(iceCandidate: IceCandidate) {
        remoteDescriptionMutex.withLock {
            if (connection.remoteDescription == null) {
                logger.d {
                    "[handleNewIceCandidate] #ice; #sfu; #$typeTag; Remote desc is null, storing candidate: $iceCandidate"
                }
                iceCandidatesMutex.withLock { iceCandidates.add(iceCandidate) }
            } else {
                logger.d {
                    "[handleNewIceCandidate] #ice; #sfu; #$typeTag; Remote desc is set, adding candidate: $iceCandidate"
                }
                addIceCandidate(iceCandidate)
            }
        }
    }

    /**
     * Adds an [IceCandidate] to the underlying [connection] if it's already been set up, or stores
     * it for later consumption.
     *
     * @param iceCandidate To process and add to the connection.
     * @return An empty [Result], if the operation has been successful or not.
     */
    public suspend fun addIceCandidate(iceCandidate: IceCandidate): Result<Unit> {
        val rtcIceCandidate = iceCandidate.toRtcCandidate()
        logger.d { "[addIceCandidate] #sfu; #$typeTag; rtcIceCandidate: $rtcIceCandidate" }
        return connection.addRtcIceCandidate(rtcIceCandidate).also {
            logger.v { "[addIceCandidate] #sfu; #$typeTag; completed: $it" }
            tracer.trace(PeerConnectionTraceKey.ADD_ICE_CANDIDATE.value, rtcIceCandidate.toString())
        }
    }

    /**
     * Adds a local [MediaStreamTrack] with audio to a given [connection], with its [streamIds].
     * The audio is then sent through a transceiver.
     *
     * @param track The track that contains audio.
     * @param streamIds The IDs that represent the stream tracks.
     */
    public fun addAudioTransceiver(
        track: MediaStreamTrack,
        streamIds: List<String>,
    ) {
        logger.i {
            "[addAudioTransceiver] #sfu; #$typeTag; track: ${track.stringify()}, streamIds: $streamIds"
        }
        val transceiverInit = buildAudioTransceiverInit(streamIds)

        audioTransceiver = connection.addTransceiver(track, transceiverInit)
    }

    /**
     * Creates the initialization configuration for the [RtpTransceiver], when sending audio.
     *
     * @param streamIds The list of stream IDs to bind to this transceiver.
     */
    private fun buildAudioTransceiverInit(streamIds: List<String>): RtpTransceiverInit {
        val fullQuality = RtpParameters.Encoding(
            "a",
            true,
            1.0,
        ).apply {
            maxBitrateBps = 500_000
        }

        val encodings = listOf(fullQuality)

        return RtpTransceiverInit(
            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
            streamIds,
            encodings,
        )
    }

    /**
     * Adds a local [MediaStreamTrack] with video to a given [connection], with its [streamIds].
     * The video is then sent in a few different resolutions using simulcast.
     *
     * @param track The track that contains video.
     * @param streamIds The IDs that represent the stream tracks.
     */
    public fun addVideoTransceiver(
        track: MediaStreamTrack,
        streamIds: List<String>,
        isScreenShare: Boolean,
    ) {
        logger.d {
            "[addVideoTransceiver] #sfu; #$typeTag; track: ${track.stringify()}, streamIds: $streamIds"
        }
        val transceiverInit = buildVideoTransceiverInit(streamIds, isScreenShare)

        videoTransceiver = connection.addTransceiver(track, transceiverInit)
    }

    /**
     * Creates the initialization configuration for the [RtpTransceiver], when sending video.
     *
     * @param streamIds The list of stream IDs to bind to this transceiver.
     */
    internal fun buildVideoTransceiverInit(
        streamIds: List<String>,
        isScreenShare: Boolean,
    ): RtpTransceiverInit {
        val encodings = if (!isScreenShare) {
            /**
             * We create different RTP encodings for the transceiver.
             * Full quality, represented by "f" ID.
             * Half quality, represented by "h" ID.
             * Quarter quality, represented by "q" ID.
             *
             * Their bitrate is also roughly as the name states - maximum for "full", ~half of that
             * for "half" and another half, or total quarter of maximum, for "quarter".
             */
            val quarterQuality = RtpParameters.Encoding(
                "q",
                true,
                4.0,
            ).apply {
                maxBitrateBps = maxBitRate / 4
                maxFramerate = 30
            }

            val halfQuality = RtpParameters.Encoding(
                "h",
                true,
                2.0,
            ).apply {
                maxBitrateBps = maxBitRate / 2
                maxFramerate = 30
            }

            val fullQuality = RtpParameters.Encoding(
                "f",
                true,
                1.0,
            ).apply {
                maxBitrateBps = maxBitRate
                maxFramerate = 30
            }

            listOf(quarterQuality, halfQuality, fullQuality)
        } else {
            // this is aligned with iOS
            val screenshareQuality = RtpParameters.Encoding(
                "q",
                true,
                1.0,
            ).apply {
                maxBitrateBps = 1_000_000
            }

            listOf(screenshareQuality)
        }

        return RtpTransceiverInit(
            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
            streamIds,
            encodings,
        )
    }

    /**
     * Peer connection listeners.
     */

    /**
     * Triggered whenever there's a new [RtcIceCandidate] for the call. Used to update our tracks
     * and subscriptions.
     *
     * @param candidate The new candidate.
     */
    override fun onIceCandidate(candidate: RtcIceCandidate?) {
        logger.i { "[onIceCandidate] #sfu; #$typeTag; candidate: $candidate" }
        if (candidate == null) return

        onIceCandidate?.invoke(candidate.toDomainCandidate(), type)
        tracer.trace(PeerConnectionTraceKey.ON_ICE_CANDIDATE.value, candidate.toString())
    }

    /**
     * Triggered whenever there's a new [MediaStream] that was added to the connection.
     *
     * @param stream The stream that contains audio or video.
     */
    override fun onAddStream(stream: MediaStream?) {
        logger.w { "[onAddStream] #sfu; #track; #$typeTag; stream: $stream" }
        if (stream != null) {
            onStreamAdded?.invoke(stream)
        }
    }

    /**
     * Triggered whenever there's a new [MediaStream] or [MediaStreamTrack] that's been added
     * to the call. It contains all audio and video tracks for a given session.
     *
     * @param receiver The receiver of tracks.
     * @param mediaStreams The streams that were added containing their appropriate tracks.
     */
    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        logger.i {
            "[onAddTrack] #sfu; #track; #$typeTag; receiver: $receiver, mediaStreams: $mediaStreams"
        }
        mediaStreams?.forEach { mediaStream ->
            logger.v { "[onAddTrack] #sfu; #track; #$typeTag; mediaStream: $mediaStream" }
            mediaStream.audioTracks?.forEach { remoteAudioTrack ->
                logger.v {
                    "[onAddTrack] #sfu; #track; #$typeTag; remoteAudioTrack: ${remoteAudioTrack.stringify()}"
                }
                remoteAudioTrack.setEnabled(true)
            }
            mediaStream.videoTracks?.forEach { remoteVideoTrack ->
                logger.v {
                    "[onAddTrack] #sfu; #track; #$typeTag; remoteVideoTrack: ${remoteVideoTrack.stringify()}"
                }
                remoteVideoTrack.setEnabled(true)
            }
            onStreamAdded?.invoke(mediaStream)
        }
    }

    /**
     * Triggered whenever there's a new negotiation needed for the active [PeerConnection].
     */
    override fun onRenegotiationNeeded() {
        logger.w { "[onRenegotiationNeeded] #sfu; #$typeTag; no args" }
        tracer.trace(PeerConnectionTraceKey.ON_NEGOTIATION_NEEDED.value, null)
        onNegotiationNeeded?.invoke(this, type)
    }

    /**
     * Triggered whenever a [MediaStream] was removed.
     *
     * @param stream The stream that was removed from the connection.
     */
    override fun onRemoveStream(stream: MediaStream?) {
        logger.v { "[onRemoveStream] #sfu; #track; #$typeTag; stream: $stream" }
    }

    /**
     * Triggered when the connection state changes.  Used to start and stop the stats observing.
     *
     * @param newState The new state of the [PeerConnection].
     */

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
        logger.i { "[onConnectionChange] #sfu; #$typeTag; newState: $newState" }
        state.value = newState
        tracer.trace(PeerConnectionTraceKey.ON_CONNECTION_STATE_CHANGE.value, newState.name)
    }

    // better to monitor onConnectionChange for the state
    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        logger.i {
            "[onIceConnectionChange] #ice; #sfu; #$typeTag; oldIceState: ${iceState.value}, newState: $newState"
        }
        iceState.value = newState
        tracer.trace(PeerConnectionTraceKey.ON_ICE_CONNECTION_STATE_CHANGE.value, newState?.name)
    }

    fun close() {
        logger.i { "[close] #sfu; #$typeTag; no args, debugText: $tag" }
        tracer.trace(PeerConnectionTraceKey.CLOSE.value, null)
        connection.close()
    }

    /**
     * @return The [RtcStatsReport] for the active connection.
     */
    public suspend fun getStats(): RtcStatsReport? {
        return suspendCoroutine { cont ->
            connection.getStats { origin ->
                if (DEBUG_STATS) {
                    logger.v {
                        "[getStats] #sfu; #$typeTag; " +
                            "stats.keys: ${origin?.statsMap?.keys}"
                    }
                    origin?.statsMap?.values?.forEach {
                        logger.v {
                            "[getStats] #sfu; #$typeTag; " +
                                "report.type: ${it.type}, report.members: $it"
                        }
                    }
                }
                try {
                    cont.resume(origin?.let { RtcStatsReport(it, it.toRtcStats()) })
                } catch (e: Throwable) {
                    logger.e(e) { "[getStats] #sfu; #$typeTag; failed: $e" }
                    cont.resume(null)
                }
            }
        }
    }

    /**
     * Domain - [PeerConnection] and [PeerConnection.Observer] related callbacks.
     */

    override fun onRemoveTrack(receiver: RtpReceiver?) {
        logger.i { "[onRemoveTrack] #sfu; #track; #$typeTag; receiver: $receiver" }
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        tracer.trace(PeerConnectionTraceKey.ON_SIGNALING_STATE_CHANGE.value, newState?.name)
        logger.d { "[onSignalingChange] #sfu; #$typeTag; newState: $newState" }
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        logger.i { "[onIceConnectionReceivingChange] #sfu; #$typeTag; receiving: $receiving" }
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        tracer.trace(PeerConnectionTraceKey.ON_ICE_GATHERING_STATE_CHANGE.value, newState?.name)
        logger.i { "[onIceGatheringChange] #sfu; #$typeTag; newState: $newState" }
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<out io.getstream.webrtc.IceCandidate>?) {
        logger.i { "[onIceCandidatesRemoved] #sfu; #$typeTag; iceCandidates: $iceCandidates" }
    }

    override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
        logger.e { "[onIceCandidateError] #sfu; #$typeTag; event: ${event?.stringify()}" }
    }

    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
        logger.i { "[onSelectedCandidatePairChanged] #sfu; #$typeTag; event: $event" }
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        logger.i { "[onTrack] #sfu; #$typeTag; transceiver: $transceiver" }
    }

    internal fun traceTrack(type: TrackType, trackId: String, streamIds: List<String> = emptyList()) = safeCall {
        if (streamIds.isNotEmpty()) {
            tracer.trace(PeerConnectionTraceKey.ON_TRACK.value, "$type:$trackId $streamIds")
        } else {
            tracer.trace(PeerConnectionTraceKey.ON_TRACK.value, "$type:$trackId")
        }
    }

    override fun onDataChannel(channel: DataChannel?) {
        tracer.trace(PeerConnectionTraceKey.ON_DATA_CHANNEL.value, channel)
        logger.i { "[onDataChannel] #sfu; #$typeTag; channel: $channel" }
    }

    override fun toString(): String =
        "StreamPeerConnection(type='$typeTag', constraints=$mediaConstraints)"

    private fun String.mungeCodecs(): String {
        return this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
    }

    private companion object {
        private const val DEBUG_STATS = false
    }
}
