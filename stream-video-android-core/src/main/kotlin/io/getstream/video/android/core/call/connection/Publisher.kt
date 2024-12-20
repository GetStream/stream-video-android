package io.getstream.video.android.core.call.connection

import OptimalVideoLayer
import findOptimalVideoLayers
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.connection.transceivers.TransceiverCache
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.utils.SdpSession
import io.getstream.video.android.core.utils.safeCall
import isAudioTrackType
import isSvcCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.CameraEnumerationAndroid.CaptureFormat
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpCapabilities
import org.webrtc.RtpParameters
import org.webrtc.RtpTransceiver
import org.webrtc.RtpTransceiver.RtpTransceiverDirection
import org.webrtc.RtpTransceiver.RtpTransceiverInit
import org.webrtc.SessionDescription
import stream.video.sfu.event.VideoLayerSetting
import stream.video.sfu.event.VideoSender
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackInfo
import stream.video.sfu.models.TrackType
import stream.video.sfu.signal.SetPublisherRequest
import toSvcEncodings
import toVideoLayers
import java.util.UUID

internal class Publisher(
    private val localParticipant: ParticipantState,
    private val mediaManager: MediaManagerImpl,
    private val peerConnectionFactory: StreamPeerConnectionFactory,
    private val publishOptions: List<PublishOption>,
    private val coroutineScope: CoroutineScope,
    private val type: StreamPeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)?,
    private val onNegotiationNeeded: (StreamPeerConnection, StreamPeerType) -> Unit,
    private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)?,
    private val maxBitRate: Int,
    private val sfuClient: SignalServerService,
    private val sessionId: String,
) : StreamPeerConnection(
    coroutineScope,
    type,
    mediaConstraints,
    onStreamAdded,
    onNegotiationNeeded,
    onIceCandidate,
    maxBitRate
) {

    private val transceiverCache = TransceiverCache()
    private val knownTrackIds = mutableSetOf<String>()
    private val defaultScreenShareFormat = CaptureFormat(1920, 1080, 15, 15)
    private val defaultFormat = CaptureFormat(1080, 720, 24, 30)
    private var isIceRestarting = false

    override fun onRenegotiationNeeded() {
        coroutineScope.launch {
            delay(500)
            negotiate(false)
        }
    }

    /**
     * Closes the publisher PeerConnection and cleans up the resources.
     */
    fun close(stopTracks: Boolean) {
        if (stopTracks) {
            stopPublishing()
        }
        dispose()
        connection.close()
    }

    private fun dispose() {
        transceiverCache.items().forEach {
            it.transceiver.stop()
            it.transceiver.dispose()
        }
    }

    private suspend fun negotiate(iceRestart: Boolean = false) {
        val offer = super.createOffer().getOrThrow()
        val trackInfos = getAnnouncedTracks(defaultFormat, offer.description)

        if (isIceRestarting) {
            logger.i { "ICE restart in progress, skipping negotiation" }
            return
        }

        if (trackInfos.isEmpty()) {
            throw Exception("Can't negotiate without announcing any tracks")
        }
        logger.i { "Negotiating with tracks: $trackInfos" }
        logger.i { "Offer: ${offer.description}" }

        safeCall {
            isIceRestarting = iceRestart
            setLocalDescription(offer)
            val request = SetPublisherRequest(
                sdp = offer.description, tracks = trackInfos, session_id = sessionId
            )
            val response = sfuClient.setPublisher(request)
            logger.i { "Received answer: ${response.sdp}" }
            logger.e { "Received error: ${response.error}" }
            if (response.error != null) throw Exception(response.error.message)
            setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, response.sdp))
        }
        isIceRestarting = false
    }

    /**
     * Starts publishing the given track.
     */
    suspend fun publishStream(
        track: MediaStreamTrack,
        trackType: TrackType,
        captureFormat: CaptureFormat? = null,
    ) {
        if (track.state() == MediaStreamTrack.State.ENDED) {
            throw Exception("Can't publish a track that has ended already.")
        }

        if (publishOptions.none { it.track_type == trackType }) {
            throw Exception("No publish options found for $trackType")
        }

        // enable the track if disabled
        if (!track.enabled()) track.setEnabled(true)

        if (!knownTrackIds.contains(track.id())) {
            knownTrackIds.add(track.id())
        }

        val option = publishOptions.find { it.track_type == trackType }
        if (option == null) {
            logger.w { "No publish option found for $trackType" }
        }
        for (publishOption in publishOptions) {
            if (publishOption.track_type != trackType) continue

            val trackToPublish = newTrackFromSource(publishOption.track_type)
            val transceiver = transceiverCache.get(publishOption)
            if (transceiver != null) {
                safeCall { transceiver.dispose() }
                transceiverCache.remove(publishOption)
            } else {
                addTransceiver(captureFormat, trackToPublish, publishOption)
            }
        }
    }

    private fun newTrackFromSource(trackType: TrackType): MediaStreamTrack {
        return when (trackType) {
            TrackType.TRACK_TYPE_AUDIO -> {
                val id = UUID.randomUUID().toString()
                peerConnectionFactory.makeAudioTrack(mediaManager.audioSource, id)
            }

            TrackType.TRACK_TYPE_VIDEO -> {
                val id = UUID.randomUUID().toString()
                peerConnectionFactory.makeVideoTrack(mediaManager.videoSource, id)
            }

            TrackType.TRACK_TYPE_SCREEN_SHARE -> {
                val id = UUID.randomUUID().toString()
                peerConnectionFactory.makeVideoTrack(mediaManager.screenShareVideoSource, id)
            }

            else -> throw IllegalArgumentException("Unknown track type: $trackType")
        }
    }

    private fun addTransceiver(
        captureFormat: CaptureFormat?, track: MediaStreamTrack, publishOption: PublishOption
    ) {
        val videoEncodings = computeLayers(captureFormat ?: defaultFormat, track, publishOption)
        val sendEncodings =
            if (!isAudioTrackType(publishOption.track_type) && isSvcCodec(publishOption.codec?.name)) {
                toSvcEncodings(
                    videoEncodings
                )
            } else videoEncodings


        val transceiver = connection.addTransceiver(track,
            RtpTransceiverInit(RtpTransceiverDirection.SEND_ONLY, emptyList(), sendEncodings?.map {
                RtpParameters.Encoding(
                    it.rid, it.active, it.scaleResolutionDownBy
                ).apply {
                    maxBitrateBps = it.maxBitrate
                    maxFramerate = it.maxFramerate
                    scalabilityMode = it.scalabilityMode
                    scaleResolutionDownBy = it.scaleResolutionDownBy
                }
            } ?: emptyList()))

        if (!isAudioTrackType(publishOption.track_type)) {
            val capabilities = peerConnectionFactory.getSenderCapabilities(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO)
            transceiver.sortVideoCodecPreferences(publishOption.codec?.name, capabilities)
        }

        logger.d { "Added ${publishOption.track_type} transceiver. (trackID: ${track.id()}, encoding: $sendEncodings)" }
        transceiverCache.add(publishOption, transceiver)
    }

    internal fun RtpTransceiver.sortVideoCodecPreferences(targetCodec: String?, capabilities: RtpCapabilities) {
        if (targetCodec == null) {
            logger.w { "No target codec provided" }
            return
        }
        logger.v { "Set codec preferences to $targetCodec" }
        capabilities.codecs.forEach { codec ->
            logger.v { "codec: ${codec.name}, ${codec.kind}, ${codec.mimeType}, ${codec.parameters}, ${codec.preferredPayloadType}" }
        }
        for (codec in capabilities.codecs) {
            val name = codec.name.uppercase()
            if (name == targetCodec.uppercase()) {
                setCodecPreferences(listOf(codec))
                return
            }
        }
    }

    private fun updateTransceiver(
        transceiver: RtpTransceiver, track: MediaStreamTrack
    ) {
        /*val previousTrack = transceiver.sender.track()
        if (previousTrack != null && previousTrack != track) {
            try {
                previousTrack.dispose()
            } catch (e: Exception) {
                logger.e(e) { "Couldn't dispose previous track" }
            }
        }
        transceiver.sender.setTrack(track, true)
         */
    }

    fun syncPublishOptions(captureFormat: CaptureFormat?, publishOptions: List<PublishOption>) {
        // enable publishing with new options
        logger.d { "New publish options: $publishOptions" }
        for (publishOption in publishOptions) {
            val trackType = publishOption.track_type
            if (!isPublishing(trackType)) continue
            if (transceiverCache.has(publishOption)) continue

            val item = transceiverCache.find {
                it.transceiver.sender.track() != null && it.transceiver.sender.track()?.isDisposed == false && it.publishOption.track_type == trackType
            }
            if (item == null) continue

            val track =
                item.transceiver.sender.track() ?: newTrackFromSource(publishOption.track_type)
            addTransceiver(captureFormat ?: defaultFormat, track, publishOption)
        }

        // stop publishing with options not required anymore
        for (item in transceiverCache.items()) {
            val (option, transceiver) = item
            val hasPublishOption = publishOptions.any {
                it.id == option.id && it.track_type == option.track_type
            }
            if (hasPublishOption) continue
            transceiver.stop()
            transceiver.dispose()
        }
    }

    suspend fun unpublishStream(trackType: TrackType, stopTrack: Boolean) {
        for (option in publishOptions) {
            if (option.track_type != trackType) continue
            val transceiver = transceiverCache.get(option)
            if (transceiver?.sender?.track()?.isDisposed == false) continue
            try {
                transceiver?.stop()
                transceiver?.dispose()
            } catch (e: Exception) {
                logger.w { "Transceiver for option ${option.id}-${option.track_type}" }
            }
            /*val track = transceiver?.sender?.track() ?: continue

            if (stopTrack && track.state() == MediaStreamTrack.State.LIVE) {
                track.setEnabled(false)
            } else if (!track.isDisposed && track.enabled()) {
                track.setEnabled(false)
            }*/
        }
    }

    fun isPublishing(trackType: TrackType): Boolean {
        for (item in transceiverCache.items()) {
            if (item.publishOption.track_type != trackType) continue
            val track = item.transceiver.sender.track() ?: continue
            if (track.state() == MediaStreamTrack.State.LIVE && track.enabled()) return true
        }
        return false
    }

    fun getTrackType(trackId: String): TrackType? {
        for (transceiverId in transceiverCache.items()) {
            val (option, transceiver) = transceiverId
            if (transceiver.sender.track()?.id() == trackId) {
                return option.track_type
            }
        }
        return null
    }

    private suspend fun notifyTrackMuteStateChanged(
        mediaStream: MediaStream?, trackType: TrackType, isMuted: Boolean
    ) {/*//sfuClient.updateMuteState(trackType, isMuted)

        val key = trackTypeToParticipantStreamKey(trackType)
        if (key == null) return

        if (isMuted) {
            state.updateParticipant(sfuClient.sessionId) { p ->
                p.copy(
                    publishedTracks = p.publishedTracks.filter { it != trackType },
                    audio = if (key == "audio") null else p.audio,
                    video = if (key == "video") null else p.video,
                    screenShare = if (key == "screenShare") null else p.screenShare
                )
            }
        } else {
            state.updateParticipant(sfuClient.sessionId) { p ->
                val updatedTracks = if (p.publishedTracks.contains(trackType)) p.publishedTracks else p.publishedTracks + trackType
                when (key) {
                    "audio" -> p.copy(publishedTracks = updatedTracks, audio = mediaStream)
                    "video" -> p.copy(publishedTracks = updatedTracks, video = mediaStream)
                    "screenShare" -> p.copy(publishedTracks = updatedTracks, screenShare = mediaStream)
                    else -> p
                }
            }
        }*/
    }

    private fun stopPublishing() {
        logger.d { "Stopping publishing all tracks" }
        this.transceiverCache.items().forEach { transceiverId ->
            transceiverId.transceiver.stop()
        }
    }

    private fun VideoSender.decompose(): Triple<TrackType, List<VideoLayerSetting>, Int> {
        return Triple(track_type, layers, publish_option_id)
    }

    suspend fun changePublishQuality(videoSender: VideoSender) {
        val (trackType, layers, publishOptionId) = videoSender.decompose()
        val enabledLayers = layers.filter { it.active }
        logger.i { "Update publish quality ($publishOptionId), requested layers by SFU: $enabledLayers" }

        val sender = transceiverCache.getWith(trackType, publishOptionId)?.sender?.takeUnless {
            it.track()?.isDisposed == true
        }

        if (sender == null) {
            logger.w { "Update publish quality, no video sender found." }
            return
        }

        val params = sender.parameters
        if (params.encodings.isEmpty()) {
            logger.w { "Update publish quality, No suitable video encoding quality found" }
            return
        }

        val codecInUse = params.codecs.firstOrNull()
        val usesSvcCodec = codecInUse != null && isSvcCodec(codecInUse.name)
        var changed = false

        for (encoder in params.encodings) {
            val layer = if (usesSvcCodec) {
                enabledLayers.firstOrNull()
            } else {
                enabledLayers.find { it.name == encoder.rid }
                    ?: (if (params.encodings.size == 1) enabledLayers.firstOrNull() else null)
            }

            val shouldActivate = (layer?.active == true)
            if (shouldActivate != encoder.active) {
                encoder.active = shouldActivate
                changed = true
            }

            if (layer == null) continue

            val (maxFramerate, scaleResolutionDownBy, maxBitrate, scalabilityMode) = layer.decompose()
            if (scaleResolutionDownBy >= 1 && scaleResolutionDownBy != encoder.scaleResolutionDownBy) {
                encoder.scaleResolutionDownBy = scaleResolutionDownBy
                changed = true
            }
            if (maxBitrate > 0 && maxBitrate != encoder.maxBitrateBps) {
                encoder.maxBitrateBps = maxBitrate
                changed = true
            }
            if (maxFramerate > 0 && maxFramerate != encoder.maxFramerate) {
                encoder.maxFramerate = maxFramerate
                changed = true
            }
            if (scalabilityMode != encoder.scalabilityMode) {
                encoder.scalabilityMode = scalabilityMode
                changed = true
            }
        }

        val activeLayers = params.encodings.filter { it.active }
        if (!changed) {
            logger.i { "Update publish quality, no change: $activeLayers" }
            return
        }

        sender.parameters = params
        logger.i { "Update publish quality, enabled rids: $activeLayers" }
    }

    suspend fun restartIce() {
        logger.i { "Restarting ICE connection" }
        val signalingState = connection.signalingState()
        if (isIceRestarting || signalingState == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
            logger.d { "ICE restart is already in progress" }
            return
        }
        isIceRestarting = true
        negotiate(true)
    }

    fun getAnnouncedTracks(captureFormat: CaptureFormat?, sdp: String? = null): List<TrackInfo> {
        val sdpStr = sdp ?: localSdp?.description
        val trackInfos = mutableListOf<TrackInfo>()
        for (transceiverId in transceiverCache.items()
            .filter { it.transceiver.sender.track()?.isDisposed == false }) {
            val (publishOption, transceiver) = transceiverId
            trackInfos.add(toTrackInfo(captureFormat, transceiver, publishOption, sdpStr))
        }
        logger.i { "Announced tracks: $trackInfos" }
        return trackInfos
    }

    fun getAnnouncedTracksForReconnect(): List<TrackInfo> {
        val sdp = connection.localDescription?.description
        val trackInfos = mutableListOf<TrackInfo>()
        for (publishOption in publishOptions) {
            val transceiver = transceiverCache.get(publishOption)
            if (transceiver?.sender?.track() == null) continue
            trackInfos.add(toTrackInfo(null, transceiver, publishOption, sdp))
        }
        logger.i { "Announced tracks for reconnect: $trackInfos" }
        return trackInfos
    }

    private fun toTrackInfo(
        format: CaptureFormat?,
        transceiver: RtpTransceiver,
        publishOption: PublishOption,
        sdp: String?
    ): TrackInfo {
        val track = transceiver.sender.track()!!
        val isScreenShare = publishOption.track_type == TrackType.TRACK_TYPE_SCREEN_SHARE
        val captureFormat = if (isScreenShare) {
            format ?: defaultScreenShareFormat
        } else format ?: defaultFormat
        val isTrackLive = track.state() == MediaStreamTrack.State.LIVE
        val isAudio = isAudioTrackType(publishOption.track_type)
        val layers = if (!isAudio) {
            if (isTrackLive && captureFormat != null) {
                computeLayers(
                    captureFormat, track, publishOption
                )
            } else transceiverCache.getLayers(publishOption)
        } else null

        transceiverCache.setLayers(publishOption, layers ?: emptyList())
        val transceiverIndex = transceiverCache.indexOf(transceiver)

        //val codec = publishOption.codec?.name

        //val svcLayers = if (isSvcCodec(codec)) toSvcEncodings(layers) else layers
        return TrackInfo(
            track_id = track.id(),
            layers = toVideoLayers(layers ?: emptyList()),
            track_type = publishOption.track_type,
            mid = extractMid(transceiver, transceiverIndex, sdp),
            stereo = false,
            muted = !isTrackLive
        )
    }

    fun extractMid(
        transceiver: RtpTransceiver, transceiverInitIndex: Int, sdp: String?
    ): String {
        // If the transceiver already has a mid, return it
        transceiver.mid?.let { return it }

        // If there's no SDP, we can't determine the mid
        if (sdp == null) return ""

        val track = transceiver.sender.track() ?: return ""

        // Parse the SDP using a hypothetical SDP parser.
        // You'll need to replace `SDP.parse(sdp)` with your actual parsing logic.

        val sdpSession = SdpSession()
        sdpSession.parse(sdp)

        // Find the media section that corresponds to this track's kind and msid
        val media = sdpSession.media.firstOrNull { m ->
            m.mline?.type == track.kind() && (m.msid?.value?.contains(track.id()) ?: true)
        }

        // If we found a media section with a mid defined, return it.
        media?.mid?.let { return it.value }

        // If we didn't find a mid and the index is known, return it; otherwise return empty string.
        return if (transceiverInitIndex == -1) "" else transceiverInitIndex.toString()
    }

    private fun computeLayers(
        format: CaptureFormat, track: MediaStreamTrack, publishOption: PublishOption
    ): List<OptimalVideoLayer>? {
        if (isAudioTrackType(publishOption.track_type)) return null
        return findOptimalVideoLayers(format, publishOption)
    }

    private data class LayerDecomposition(
        val maxFramerate: Int,
        val scaleResolutionDownBy: Double,
        val maxBitrate: Int,
        val scalabilityMode: String?
    )

    private fun VideoLayerSetting.decompose(): LayerDecomposition {
        return LayerDecomposition(
            maxFramerate = this.max_framerate,
            scaleResolutionDownBy = this.scale_resolution_down_by.toDouble(),
            maxBitrate = this.max_bitrate,
            scalabilityMode = this.scalability_mode
        )
    }
}