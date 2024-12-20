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
import io.getstream.video.android.core.call.connection.utils.AvailableCodec
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.call.stats.toRtcStats
import io.getstream.video.android.core.call.utils.addRtcIceCandidate
import io.getstream.video.android.core.call.utils.createValue
import io.getstream.video.android.core.call.utils.setValue
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.VideoCodec
import io.getstream.video.android.core.model.toDomainCandidate
import io.getstream.video.android.core.model.toRtcCandidate
import io.getstream.video.android.core.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import stream.video.sfu.models.PublishOption
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

    private val localDescriptionMutex = Mutex()
    private val remoteDescriptionMutex = Mutex()
    private val iceCandidatesMutex = Mutex() // Not needed in current logic flow, but kept it for safety.

    internal var localSdp: SessionDescription? = null
    internal var remoteSdp: SessionDescription? = null
    private val iceCandidates = mutableListOf<IceCandidate>()
    private val typeTag = type.stringify()

    // see https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/iceConnectionState
    internal val state = MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
    internal val iceState = MutableStateFlow<PeerConnection.IceConnectionState?>(null)

    private val logger by taggedLogger("Call:PeerConnection:$typeTag")

    /**
     * The wrapped connection for all the WebRTC communication.
     */
    public lateinit var connection: PeerConnection
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

    private lateinit var mTransceiverManager: SenderTransceiversManager

    internal fun transceiverManager(platformCodec: List<AvailableCodec>): SenderTransceiversManager {
        if (!::mTransceiverManager.isInitialized) {
            mTransceiverManager = SenderTransceiversManager(platformCodec)
        }
        return mTransceiverManager
    }

    internal fun addTransceiver(platformCodec: List<AvailableCodec>, trackPrefix: String, track: MediaStreamTrack, publishOption: PublishOption) =
        transceiverManager(platformCodec).add(connection, trackPrefix, track, publishOption)

    fun isFailedOrClosed(): Boolean {
        return when (state.value) {
            PeerConnection.PeerConnectionState.CLOSED,
            PeerConnection.PeerConnectionState.FAILED,
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
    ): RtpTransceiver? {
        logger.i {
            "[addAudioTransceiver] #sfu; #$typeTag; track: ${track.stringify()}, streamIds: $streamIds"
        }
        val transceiverInit = buildAudioTransceiverInit(streamIds)

        return connection.addTransceiver(track, transceiverInit)
    }

    /**
     * Creates the initialization configuration for the [RtpTransceiver], when sending audio.
     *
     * @param streamIds The list of stream IDs to bind to this transceiver.
     */
    // TODO-neg: we should omit the encodings for audio in the transceiver
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
        publishOption: PublishOption? = null,
        streamIds: List<String>,
    ): RtpTransceiver? {
        val transceiverInit = buildVideoTransceiverInit(
            streamIds,
            publishOption,
        )

        return connection.addTransceiver(track, transceiverInit)
    }

    /**
     * Creates the initialization configuration for the [RtpTransceiver], when sending video.
     *
     * @param streamIds The list of stream IDs to bind to this transceiver.
     */
    private fun buildVideoTransceiverInit(
        streamIds: List<String>,
        publishOption: PublishOption?,
    ): RtpTransceiverInit {
        val isSvcCodec = publishOption?.codec?.let {
            VideoCodec.valueOf(it.name.uppercase()).supportsSvc()
        } ?: false // TODO-neg add as PublishOption extension method, used in other places also

        val encodings = publishOption?.let {
            transceiverManager(emptyList()).getEncodingsFor(publishOption)
        } ?: emptyList()

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
        logger.i { "[onIceConnectionChange] #ice; #sfu; #$typeTag; newState: $newState" }
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
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        logger.i { "[onTrack] #sfu; #$typeTag; transceiver: $transceiver" }
    }

    override fun onDataChannel(channel: DataChannel?): Unit = Unit

    private fun String.mungeCodecs(): String {
        return this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
    }

    override fun toString(): String =
        "StreamPeerConnection(type='$typeTag', constraints=$mediaConstraints)"

    private companion object {
        private const val DEBUG_STATS = false
    }
}
