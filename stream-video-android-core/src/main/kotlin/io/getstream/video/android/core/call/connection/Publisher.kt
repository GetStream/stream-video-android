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
import io.getstream.result.onErrorSuspend
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.connection.job.RestartIceJobDelegate
import io.getstream.video.android.core.call.connection.stats.ComputedStats
import io.getstream.video.android.core.call.connection.transceivers.TransceiverCache
import io.getstream.video.android.core.call.connection.utils.OptimalVideoLayer
import io.getstream.video.android.core.call.connection.utils.computeTransceiverEncodings
import io.getstream.video.android.core.call.connection.utils.findOptimalVideoLayers
import io.getstream.video.android.core.call.connection.utils.isAudioTrackType
import io.getstream.video.android.core.call.connection.utils.isSvcCodec
import io.getstream.video.android.core.call.connection.utils.stringify
import io.getstream.video.android.core.call.connection.utils.toVideoDimension
import io.getstream.video.android.core.call.connection.utils.toVideoLayers
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.trySetEnabled
import io.getstream.video.android.core.utils.SdpSession
import io.getstream.video.android.core.utils.SerialProcessor
import io.getstream.video.android.core.utils.defaultConstraints
import io.getstream.video.android.core.utils.iceRestartConstraints
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.CameraEnumerationAndroid.CaptureFormat
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
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
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.SetPublisherRequest
import java.util.UUID

internal class Publisher(
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
    private val transceiverCache: TransceiverCache = TransceiverCache(),
    private val tracer: Tracer,
    private val restartIceJobDelegate: RestartIceJobDelegate =
        RestartIceJobDelegate(coroutineScope),
) : StreamPeerConnection(
    coroutineScope,
    type,
    mediaConstraints,
    onStreamAdded,
    onNegotiationNeeded,
    onIceCandidate,
    rejoin,
    maxBitRate,
    true,
    tracer,
) {
    private val defaultScreenShareFormat = CaptureFormat(1280, 720, 24, 30)
    private val defaultFormat = CaptureFormat(1280, 720, 24, 30)
    private var isIceRestarting = false
    private val sdpProcessor = SerialProcessor(coroutineScope)

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
        close()
    }

    private fun dispose() {
        transceiverCache.items().forEach {
            try {
                it.transceiver.stop()
            } catch (e: Exception) {
                logger.w { "Transceiver already stopped: ${e.message}" }
            }

            try {
                it.transceiver.dispose()
            } catch (e: Exception) {
                logger.w { "Transceiver already disposed: ${e.message}" }
            }
        }
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        super.onIceConnectionChange(newState)
        when (newState) {
            PeerConnection.IceConnectionState.CONNECTED -> {
                restartIceJobDelegate.cancelScheduledRestartIce()
            }

            PeerConnection.IceConnectionState.FAILED -> {
                restartIceJobDelegate.scheduleRestartIce {
                    negotiate(true)
                }
            }
            PeerConnection.IceConnectionState.DISCONNECTED -> {
                restartIceJobDelegate.scheduleRestartIce(3000) {
                    negotiate(true)
                }
            }
            else -> {
                // no-op
            }
        }
    }

    @VisibleForTesting
    public suspend fun negotiate(iceRestart: Boolean = false) = sdpProcessor.submit {
        if (isIceRestarting) {
            logger.i { "ICE restart in progress, skipping negotiation" }
            return@submit
        }

        val offer = super.createOffer(
            if (iceRestart) {
                iceRestartConstraints
            } else {
                defaultConstraints
            },
        ).getOrThrow()
        val trackInfos = getAnnouncedTracks(defaultFormat, offer.description)
        tracer.trace(
            "negotiate-with-tracks",
            trackInfos.joinToString(separator = ";") { it.toString() },
        )
        if (trackInfos.isEmpty()) {
            logger.e { ("Can't negotiate without announcing any tracks") }
            rejoin.invoke()
        }
        logger.i { "Negotiating with tracks: $trackInfos" }
        logger.i { "Offer: ${offer.description}" }

        safeCall {
            isIceRestarting = iceRestart
            setLocalDescription(offer).onErrorSuspend {
                tracer.trace("negotiate-error-setlocaldescription", it.message ?: "unknown")
            }
            val request = SetPublisherRequest(
                sdp = offer.description,
                tracks = trackInfos,
                session_id = sessionId,
            )
            val response = sfuClient.setPublisher(request)
            logger.i { "Received answer: ${response.sdp}" }
            logger.e { "Received error: ${response.error}" }
            if (response.error != null) {
                tracer.trace("negotiate-error-setpublisher", response.error.message ?: "unknown")
                logger.e { response.error.message }
                rejoin()
            }
            setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, response.sdp))
                .onErrorSuspend {
                    tracer.trace(
                        "negotiate-error-setremotedescription",
                        it.message ?: "unknown",
                    )
                }
            // Set ice trickle
        }
        isIceRestarting = false
    }

    override suspend fun stats(): ComputedStats? = safeCallWithDefault(null) {
        return statsTracer?.get(
            transceiverCache.items().associate {
                it.transceiver.sender.track()?.id() to it.publishOption.track_type
            }.filterKeys { it != null }.mapKeys { it.key!! },
        )
    }

    /**
     * Starts publishing the given track.
     */
    suspend fun publishStream(
        trackType: TrackType,
        captureFormat: CaptureFormat? = null,
    ): MediaStreamTrack? {
        logger.i { "[trackPublishing] Publishing track: $trackType" }

        if (publishOptions.none { it.track_type == trackType }) {
            logger.e { "[trackPublishing] No publish options found for $trackType" }
            return null
        }

        for (publishOption in publishOptions) {
            if (publishOption.track_type != trackType) continue

            val transceiver = transceiverCache.get(publishOption)
            if (transceiver != null) {
                try {
                    val sender = transceiver.sender
                    val senderTrack = sender.track()
                    if (senderTrack != null && !senderTrack.isDisposed) {
                        logger.d { "[trackPublishing] Track already exists." }
                        senderTrack.trySetEnabled(true)
                        logTrack(senderTrack)
                        traceTrack(trackType, senderTrack.id())
                        return senderTrack
                    } else {
                        logger.d { "[trackPublishing] Track is disposed, creating new one." }
                        val newTrack = newTrackFromSource(publishOption.track_type)
                        traceTrack(trackType, newTrack.id())
                        sender.setTrack(newTrack, true)
                        return newTrack
                    }
                } catch (e: Exception) {
                    // Fallback if anything happens with the sender
                    logger.w { "Failed to set track for ${publishOption.track_type}, creating new transceiver" }
                    transceiverCache.remove(publishOption)
                    val fallbackTrack = newTrackFromSource(publishOption.track_type)
                    traceTrack(trackType, fallbackTrack.id())
                    addTransceiver(captureFormat, fallbackTrack, publishOption)
                    return fallbackTrack
                }
            } else {
                logger.d {
                    "[trackPublishing] No transceiver found for $trackType, creating new track and transceiver."
                }
                // This is the first time we are adding the transceiver.
                val newTrack = newTrackFromSource(publishOption.track_type)
                traceTrack(trackType, newTrack.id())
                addTransceiver(captureFormat, newTrack, publishOption)
                return newTrack
            }
        }
        return null
    }

    private fun logTrack(senderTrack: MediaStreamTrack?) {
        logger.d {
            "[trackPublishing] Track: ${senderTrack?.enabled()}:${senderTrack?.state()}:${senderTrack?.id()}"
        }
    }

    @VisibleForTesting
    public fun newTrackFromSource(trackType: TrackType): MediaStreamTrack {
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

    @VisibleForTesting
    public fun addTransceiver(
        captureFormat: CaptureFormat?,
        track: MediaStreamTrack,
        publishOption: PublishOption,
    ) {
        val init = computeTransceiverEncodings(captureFormat, publishOption)
        try {
            val transceiver = connection.addTransceiver(
                track,
                RtpTransceiverInit(
                    RtpTransceiverDirection.SEND_ONLY,
                    emptyList(),
                    init,
                ),
            )
            logger.d {
                "Added ${publishOption.track_type} transceiver. (trackID: ${track.id()}, encodings: ${transceiver.sender?.parameters?.encodings?.joinToString { it.stringify() }})"
            }
            transceiverCache.add(publishOption, transceiver)
        } catch (e: Exception) {
            logger.e(e) { "Failed to add transceiver for ${publishOption.track_type}" }
        }
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
            safeCall {
                transceiver.stop()
                transceiver.dispose()
            }
            transceiverCache.remove(option)
        }
    }

    suspend fun unpublishStream(trackType: TrackType) {
        transceiverCache.getByTrackType(trackType).forEach { transceiver ->
            logger.d { "[trackPublishing] Unpublishing track: $trackType" }
            val sender = transceiver.sender
            val senderTrack = sender.track()
            senderTrack?.let { track ->
                val result = track.trySetEnabled(false)
                tracer().trace("unpublishtrack", "$trackType:${track.id()}:$result")
                logTrack(track)
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
            safeCall {
                transceiverId.transceiver.stop()
            }
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

        val sender = transceiverCache.get(videoSender.toPublishOption())?.sender?.takeUnless {
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
        val changed = updateEncodings(params, usesSvcCodec, enabledLayers)

        val activeLayers = params.encodings.filter { it.active }
        if (!changed) {
            logger.i { "Update publish quality, no change: $activeLayers" }
            return
        }

        sender.parameters = params
        logger.i { "Update publish quality, enabled rids: $activeLayers" }
        activeLayers.forEach { layer ->
            logger.i { "Update publish quality, enabled rid: ${layer.rid}" }
        }
    }

    internal fun updateEncodings(
        params: RtpParameters,
        usesSvcCodec: Boolean,
        enabledLayers: List<VideoLayerSetting>,
    ): Boolean {
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
        return changed
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
        return TrackInfo(
            track_id = track.id(),
            layers = toVideoLayers(layers ?: emptyList()),
            track_type = publishOption.track_type,
            mid = extractMid(transceiver, transceiverCache.indexOf(publishOption), sdp),
            stereo = false,
            muted = !isTrackLive,
            codec = publishOption.codec,
            publish_option_id = publishOption.id,
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
