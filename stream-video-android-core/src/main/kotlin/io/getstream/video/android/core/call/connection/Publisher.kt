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

import OptimalVideoLayer
import computeTransceiverEncodings
import findOptimalVideoLayers
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.connection.transceivers.TransceiverCache
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.utils.SdpSession
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
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
import org.webrtc.RtpTransceiver
import org.webrtc.RtpTransceiver.RtpTransceiverDirection
import org.webrtc.RtpTransceiver.RtpTransceiverInit
import org.webrtc.SessionDescription
import stream.video.sfu.event.VideoLayerSetting
import stream.video.sfu.event.VideoSender
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackInfo
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.SetPublisherRequest
import toVideoDimension
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
    private val rejoin: () -> Unit,
) : StreamPeerConnection(
    coroutineScope,
    type,
    mediaConstraints,
    onStreamAdded,
    onNegotiationNeeded,
    onIceCandidate,
    maxBitRate,
) {

    private val transceiverCache = TransceiverCache()
    private val defaultScreenShareFormat = CaptureFormat(1920, 1080, 15, 15)
    private val defaultFormat = CaptureFormat(1080, 720, 24, 30)
    private var isIceRestarting = false

    override fun onRenegotiationNeeded() {
        coroutineScope.launch {
            delay(500)
            negotiate(false)
        }
    }

    fun currentOptions() =
        safeCallWithDefault(emptyList()) { transceiverCache.items().map { it.publishOption } }

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
            logger.e { ("Can't negotiate without announcing any tracks") }
            rejoin.invoke()
        }
        logger.i { "Negotiating with tracks: $trackInfos" }
        logger.i { "Offer: ${offer.description}" }

        safeCall {
            isIceRestarting = iceRestart
            setLocalDescription(offer)
            val request = SetPublisherRequest(
                sdp = offer.description,
                tracks = trackInfos,
                session_id = sessionId,
            )
            val response = sfuClient.setPublisher(request)
            logger.i { "Received answer: ${response.sdp}" }
            logger.e { "Received error: ${response.error}" }
            if (response.error != null) {
                logger.e { response.error.message }
                rejoin()
            }
            setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, response.sdp))
            // Set ice trickle
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
            logger.e { "Can't publish a track that has ended already." }
            return
        }

        if (publishOptions.none { it.track_type == trackType }) {
            logger.e { "No publish options found for $trackType" }
            return
        }

        // enable the track if disabled
        if (!track.enabled()) track.setEnabled(true)

        for (publishOption in publishOptions) {
            if (publishOption.track_type != trackType) continue

            val trackToPublish = newTrackFromSource(publishOption.track_type)
            val transceiver = transceiverCache.get(publishOption)
            if (transceiver != null) {
                safeCall { transceiver.dispose() }
                transceiverCache.remove(publishOption)
            }
            addTransceiver(captureFormat, trackToPublish, publishOption)
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
        captureFormat: CaptureFormat?,
        track: MediaStreamTrack,
        publishOption: PublishOption,
    ) {
        val init = computeTransceiverEncodings(captureFormat, publishOption)
        val transceiver = connection.addTransceiver(
            track,
            RtpTransceiverInit(
                RtpTransceiverDirection.SEND_ONLY,
                emptyList(),
                init,
            ),
        )
        logger.d {
            "Added ${publishOption.track_type} transceiver. (trackID: ${track.id()}, encoding: $init)"
        }
        transceiverCache.add(publishOption, transceiver)
    }

    fun syncPublishOptions(captureFormat: CaptureFormat?, publishOptions: List<PublishOption>) {
        // enable publishing with new options
        logger.d { "New publish options: $publishOptions" }
        for (publishOption in publishOptions) {
            val trackType = publishOption.track_type
            if (!isPublishing(trackType)) {
                logger.d { "Not publishing $trackType" }
                continue
            }
            if (transceiverCache.has(publishOption)) continue

            val track = newTrackFromSource(publishOption.track_type)
            addTransceiver(captureFormat ?: defaultFormat, track, publishOption)
        }

        // stop publishing with options not required anymore
        for (item in transceiverCache.items()) {
            val (option, transceiver) = item
            val hasPublishOption = transceiverCache.has(option)
            if (!hasPublishOption) continue
            transceiver.stop()
            transceiver.dispose()
            transceiverCache.remove(option)
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

    private fun stopPublishing() {
        logger.d { "Stopping publishing all tracks" }
        this.transceiverCache.items().forEach { transceiverId ->
            transceiverId.transceiver.stop()
        }
    }

    private fun VideoSender.decompose(): Triple<TrackType, List<VideoLayerSetting>, Int> {
        return Triple(track_type, layers, publish_option_id)
    }

    private fun VideoSender.toPublishOption(): PublishOption {
        return PublishOption(
            track_type = track_type,
            codec = codec,
            bitrate = maxBitRate,
            fps = 30,
            max_spatial_layers = 3,
            max_temporal_layers = 3,
            video_dimension = VideoDimension(1920, 1080),
            id = publish_option_id,
        )
    }

    suspend fun changePublishQuality(videoSender: VideoSender) {
        val (trackType, layers, publishOptionId) = videoSender.decompose()
        val enabledLayers = layers.filter { it.active }
        logger.i {
            "Update publish quality ($publishOptionId-$trackType-${videoSender.codec?.name}), requested layers by SFU: $enabledLayers"
        }

        val sender =
            transceiverCache.get(videoSender.toPublishOption())?.sender?.takeUnless {
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
        negotiate(true)
    }

    fun getAnnouncedTracks(captureFormat: CaptureFormat?, sdp: String? = null): List<TrackInfo> =
        synchronized(this) {
            val sdpStr = sdp ?: localSdp?.description
            val trackInfos = mutableListOf<TrackInfo>()
            for (transceiverId in transceiverCache.items()
                .filter { it.transceiver.sender.track()?.isDisposed == false }) {
                val (publishOption, transceiver) = transceiverId
                val trackInfo = toTrackInfo(captureFormat, transceiver, publishOption, sdpStr)
                logger.i { "Announced track: $publishOption -> $trackInfo" }
                trackInfos.add(trackInfo)
            }
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
        sdp: String?,
    ): TrackInfo {
        val track = transceiver.sender.track()!!
        val isScreenShare = publishOption.track_type == TrackType.TRACK_TYPE_SCREEN_SHARE
        val captureFormat = if (isScreenShare) {
            publishOption.video_dimension ?: defaultScreenShareFormat.toVideoDimension()
        } else {
            format?.toVideoDimension() ?: publishOption.video_dimension
                ?: defaultFormat.toVideoDimension()
        }
        val isTrackLive = track.state() == MediaStreamTrack.State.LIVE
        val isAudio = isAudioTrackType(publishOption.track_type)
        val layers = if (!isAudio) {
            if (isTrackLive) {
                computeLayers(
                    captureFormat,
                    track,
                    publishOption,
                )
            } else {
                transceiverCache.getLayers(publishOption)
            }
        } else {
            null
        }

        transceiverCache.setLayers(publishOption, layers ?: emptyList())

        val codec = publishOption.codec?.name
        val svcLayers = layers // if (isSvcCodec(codec)) toSvcEncodings(layers) else layers
        logger.i { "Layers for option $publishOption --> $svcLayers" }
        return TrackInfo(
            track_id = track.id(),
            layers = toVideoLayers(svcLayers ?: emptyList()),
            track_type = publishOption.track_type,
            mid = extractMid(transceiver, transceiverCache.indexOf(publishOption), sdp),
            stereo = false,
            muted = !isTrackLive,
        )
    }

    fun extractMid(
        transceiver: RtpTransceiver,
        transceiverIndex: Int,
        sdp: String?,
    ): String {
        // If the transceiver already has a mid, return it
        val transceiverMid = transceiver.mid
        logger.v { "Transceiver mid: $transceiverMid" }
        transceiverMid?.let { return it }

        if (transceiverIndex >= 0) {
            return "$transceiverIndex"
        }

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

        logger.v { "Media section (mid): ${media?.mid?.value}" }
        // If we found a media section with a mid defined, return it.
        media?.mid?.let { return it.value }

        // If we didn't find a mid and the index is known, return it; otherwise return empty string.
        return ""
    }

    private fun computeLayers(
        format: VideoDimension,
        track: MediaStreamTrack,
        publishOption: PublishOption,
    ): List<OptimalVideoLayer>? {
        if (isAudioTrackType(publishOption.track_type)) return null
        return findOptimalVideoLayers(format, publishOption)
    }

    private data class LayerDecomposition(
        val maxFramerate: Int,
        val scaleResolutionDownBy: Double,
        val maxBitrate: Int,
        val scalabilityMode: String?,
    )

    private fun VideoLayerSetting.decompose(): LayerDecomposition {
        return LayerDecomposition(
            maxFramerate = this.max_framerate,
            scaleResolutionDownBy = this.scale_resolution_down_by.toDouble(),
            maxBitrate = this.max_bitrate,
            scalabilityMode = this.scalability_mode,
        )
    }
}