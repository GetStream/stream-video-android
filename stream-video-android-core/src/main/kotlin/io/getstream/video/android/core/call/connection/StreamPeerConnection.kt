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

import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.call.stats.toRtcStats
import io.getstream.video.android.core.call.utils.addRtcIceCandidate
import io.getstream.video.android.core.call.utils.createValue
import io.getstream.video.android.core.call.utils.setValue
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.toDomainCandidate
import io.getstream.video.android.core.model.toRtcCandidate
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.core.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.AudioTrack
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpParameters
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.RtpTransceiver.RtpTransceiverInit
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.webrtc.IceCandidate as RtcIceCandidate

/**
 * Wrapper around the WebRTC connection that contains tracks.
 *
 * @param coroutineScope The scope used to listen to stats events.
 * @param type The internal type of the PeerConnection. Check [StreamPeerType].
 * @param mediaConstraints Constraints used for the connections.
 * @param onStreamAdded Handler when a new [MediaStream] gets added.
 * @param onNegotiationNeeded Handler when there's a new negotiation.
 * @param onIceCandidate Handler whenever we receive [IceCandidate]s.
 */
public class StreamPeerConnection(
    private val coroutineScope: CoroutineScope,
    private val type: StreamPeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)?,
    private val onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)?,
    private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)?,
    private val maxBitRate: Int,
) : PeerConnection.Observer {

    private val setDescriptionMutex = Mutex()

    internal var localSdp: SessionDescription? = null
    internal var remoteSdp: SessionDescription? = null
    private val typeTag = type.stringify()

    // see https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/iceConnectionState
    internal val state = MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
    internal val iceState = MutableStateFlow<PeerConnection.IceConnectionState?>(null)

    private val logger by taggedLogger("Video:PC:$typeTag")

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
     * Transceiver used to send video in different resolutions.
     */
    public var screenShareTransceiver: RtpTransceiver? = null
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

    init {
        logger.i { "<init> #sfu; #$typeTag; mediaConstraints: $mediaConstraints" }
    }

    /**
     * Initialize a [StreamPeerConnection] using a WebRTC [PeerConnection].
     *
     * @param peerConnection The connection that holds audio and video tracks.
     */
    public fun initialize(peerConnection: PeerConnection) {
        logger.d { "[initialize] #sfu; #$typeTag; peerConnection: $peerConnection" }
        this.connection = peerConnection

        this.state.value = this.connection.connectionState()
        this.iceState.value = this.connection.iceConnectionState()
    }

    /**
     * Used to create an offer whenever there's a negotiation that we need to process on the
     * publisher side.
     *
     * @return [Result] wrapper of the [SessionDescription] for the publisher.
     */
    public suspend fun createOffer(): Result<SessionDescription> {
        logger.d { "[createOffer] #sfu; #$typeTag; no args" }
        return createValue {
            connection.createOffer(
                it,
                MediaConstraints(),
            )
        }
    }

    /**
     * Used to create an answer whenever there's a subscriber offer.
     *
     * @return [Result] wrapper of the [SessionDescription] for the subscriber.
     */
    public suspend fun createAnswer(): Result<SessionDescription> {
        logger.d { "[createAnswer] #sfu; #$typeTag; no args" }
        return createValue { connection.createAnswer(it, mediaConstraints) }
    }

    /**
     * Used to set up the SDP on underlying connections and to add [pendingIceCandidates] to the
     * connection for listening.
     *
     * @param sessionDescription That contains the remote SDP.
     * @return An empty [Result], if the operation has been successful or not.
     */
    public suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> {
        logger.d { "[setRemoteDescription] #sfu; #$typeTag; answerSdp: ${sessionDescription.stringify()}" }

        remoteSdp = sessionDescription

        return setValue {
            connection.setRemoteDescription(
                it,
                SessionDescription(
                    sessionDescription.type,
                    sessionDescription.description.mungeCodecs(),
                ),
            )
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
        return setDescriptionMutex.withLock {
            setValue {
                // Never call this in parallel
                connection.setLocalDescription(it, sdp)
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

    public fun addScreenShareTransceiver(
        track: MediaStreamTrack,
        streamIds: List<String>,
    ) {
        logger.i {
            "[addScreenShareTransceiver] #sfu; #$typeTag; track: ${track.stringify()}, streamIds: $streamIds"
        }
        val transceiverInit = buildVideoTransceiverInit(streamIds, true)

        screenShareTransceiver = connection.addTransceiver(track, transceiverInit)
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
    ) {
        logger.d {
            "[addVideoTransceiver] #sfu; #$typeTag; track: ${track.stringify()}, streamIds: $streamIds"
        }
        val transceiverInit = buildVideoTransceiverInit(streamIds, false)

        videoTransceiver = connection.addTransceiver(track, transceiverInit)
    }

    /**
     * Creates the initialization configuration for the [RtpTransceiver], when sending video.
     *
     * @param streamIds The list of stream IDs to bind to this transceiver.
     */
    private fun buildVideoTransceiverInit(
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
//            networkPriority = 3
//            bitratePriority = 4.0
            }

            listOf(quarterQuality, halfQuality, fullQuality)
        } else {
            // this is aligned with iOS
            val screenshareQuality = RtpParameters.Encoding(
                "q",
                true,
                1.0,
            ).apply {
                maxBitrateBps = 350_000
                maxFramerate = 24
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
    }

    // better to monitor onConnectionChange for the state
    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        logger.i { "[onIceConnectionChange] #sfu; #$typeTag; newState: $newState" }
        iceState.value = newState
        when (newState) {
            PeerConnection.IceConnectionState.CLOSED, PeerConnection.IceConnectionState.FAILED, PeerConnection.IceConnectionState.DISCONNECTED -> {
            }

            PeerConnection.IceConnectionState.CONNECTED -> {
            }

            else -> Unit
        }
    }

    /**
     * @return The [RtcStatsReport] for the active connection.
     */
    public suspend fun getStats(): RtcStatsReport? {
        return suspendCoroutine { cont ->
            connection.getStats { origin ->
                coroutineScope.launch(Dispatchers.IO) {
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
    }

    /**
     * Domain - [PeerConnection] and [PeerConnection.Observer] related callbacks.
     */

    override fun onRemoveTrack(receiver: RtpReceiver?) {
        logger.i { "[onRemoveTrack] #sfu; #track; #$typeTag; receiver: $receiver" }
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        logger.d { "[onSignalingChange] #sfu; #$typeTag; newState: $newState" }
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        logger.i { "[onIceConnectionReceivingChange] #sfu; #$typeTag; receiving: $receiving" }
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        logger.i { "[onIceGatheringChange] #sfu; #$typeTag; newState: $newState" }
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<out org.webrtc.IceCandidate>?) {
        logger.i { "[onIceCandidatesRemoved] #sfu; #$typeTag; iceCandidates: $iceCandidates" }
    }

    override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
        logger.e { "[onIceCandidateError] #sfu; #$typeTag; event: ${event?.stringify()}" }
    }

    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
        logger.i { "[onSelectedCandidatePairChanged] #sfu; #$typeTag; event: $event" }
        connection.restartIce()
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        logger.i { "[onTrack] #sfu; #$typeTag; transceiver: $transceiver" }
    }

    override fun onDataChannel(channel: DataChannel?): Unit = Unit

    override fun toString(): String =
        "StreamPeerConnection(type='$typeTag', constraints=$mediaConstraints)"

    private fun String.mungeCodecs(): String {
        return this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
    }

    fun updateVideoTransceiver(
        videoTrack: VideoTrack,
    ) {
        logger.d { "[updateVideoTransceiver] #sfu; #$typeTag; videoTrack: $videoTrack" }
        safeCall {
            videoTransceiver?.sender?.setTrack(videoTrack, false)
        }
    }

    fun updateAudioTransceiver(audioTrack: AudioTrack) {
        logger.d { "[updateAudioTransceiver] #sfu; #$typeTag; audioTrack: $audioTrack" }
        audioTransceiver?.sender?.setTrack(audioTrack, false)
    }

    fun updateScreenShareTransceiver(screenShareTrack: VideoTrack) {
        logger.d { "[updateScreenShareTransceiver] #sfu; #$typeTag; screenShareTrack: $screenShareTrack" }
        safeCall {
            val track = screenShareTransceiver?.sender?.track()
            if (track?.state() == MediaStreamTrack.State.LIVE) {
                track.dispose()
            }
        }
        screenShareTransceiver?.sender?.setTrack(screenShareTrack, false)
    }

    private companion object {
        private const val DEBUG_STATS = false
    }
}
