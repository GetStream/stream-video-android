package io.getstream.video.android.core.call.connection

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.model.VideoCodec
import io.getstream.video.android.core.model.getScalabilityMode
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpParameters
import org.webrtc.RtpTransceiver
import org.webrtc.RtpTransceiver.RtpTransceiverInit
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import java.util.Collections

class SenderTransceiversManager {
    // State
    private val logger by taggedLogger("TransceiversManager")
    private val cachedTransceivers: MutableMap<String, RtpTransceiver> =
        Collections.synchronizedMap(mutableMapOf<String, RtpTransceiver>())

    // API
    /**
     * Add a transceiver to the peer connection.
     *
     * @param peerConnection the peer connection.
     * @param trackIdPrefix the prefix for the track id.
     * @param track the media stream track.
     * @param publishOption the publish option.
     */
    fun add(
        peerConnection: PeerConnection,
        trackIdPrefix: String,
        track: MediaStreamTrack,
        publishOption: PublishOption
    ): RtpTransceiver = synchronized(this) {
        if (contains(publishOption)) {
            logger.w { "[add] Transceiver already added for $publishOption" }
            return cachedTransceivers[publishOption.key()]!!
        }
        logger.d { "[add] $trackIdPrefix-$track\n{${publishOption.key()} -> $publishOption" }
        val transceiverInit = publishOption.toTransceiverInit(trackIdPrefix)
        val transceiver = peerConnection.addTransceiver(track, transceiverInit)
        cachedTransceivers[publishOption.key()] = transceiver
        return transceiver
    }

    /**
     * Get an already existing transceiver.
     */
    operator fun get(option: PublishOption): RtpTransceiver? = cachedTransceivers[option.key()]

    /**
     * Get by key.
     * Note: The key calculation is based on the publish option id and track type it is "$id-$track_type"
     * @param key the key.
     */
    operator fun get(key: String): RtpTransceiver? = cachedTransceivers[key]

    /**
     * Get all the transceivers as list.
     */
    fun asList(): List<RtpTransceiver> = cachedTransceivers.values.toList()

    /**
     * Get all the transceivers as map.
     */
    fun asMap(): Map<String, RtpTransceiver> = cachedTransceivers

    /**
     * Iterate over all the transceivers
     */
    fun forEach(block: (RtpTransceiver) -> Unit) = asList().forEach(block)

    /**
     * Check if the transceiver is already added.
     *
     * @param option the publishing option.
     */
    fun contains(option: PublishOption): Boolean = contains(option.key())

    /**
     * Check if the transceiver is already added.
     *
     * @param key the key.
     */
    fun contains(key: String) = cachedTransceivers.contains(key)

    /**
     * Disable a track.
     *
     * @param option the publishing option
     */
    fun disable(option: PublishOption): Boolean = disable(option.key())

    /**
     * Disable a track.
     *
     * @param key the key.
     */
    fun disable(key: String) : Boolean  {
        logger.d { "[disable] Disabling transceiver for $key" }
        return contains(key).and(toggleTrack(key, false))
    }

    /**
     * Disable a transceiver if the predicate is true.
     *
     * @param predicate the predicate.
     */
    fun disableIf(predicate: (String, RtpTransceiver) -> Boolean) {
        val entries = cachedTransceivers.filter { predicate(it.key, it.value) }
        entries.forEach { disable(it.key) }
    }

    /**
     * Enable a track.
     *
     * @param option the publishing option.
     */
    fun enable(option: PublishOption): Boolean = enable(option.key())

    /**
     * Enable a track.
     *
     * @param key the key.
     */
    fun enable(key: String) : Boolean = contains(key).and(toggleTrack(key, true))

    /**
     * Remove a transceiver.
     *
     * @param option the publishing option.
     */
    fun remove(option: PublishOption) {
        val key = option.key()
        remove(key)
    }

    /**
     * Remove by key.
     *
     * @param key the key.
     */
    fun remove(key: String) = safeCall {
        logger.d { "[remove] Removing transceiver for $key" }
        val rtpTransceiver = cachedTransceivers[key]
        cachedTransceivers.remove(key)
        safeCall { rtpTransceiver?.sender?.track()?.setEnabled(false) }
        safeCall { rtpTransceiver?.sender?.track()?.dispose() }
        safeCall { rtpTransceiver?.stopStandard() }
        safeCall { rtpTransceiver?.stop() }
    }

    /**
     * Remove  a transceiver if the predicate is true.
     *
     * @param predicate the predicate.
     */
    fun removeIf(predicate: (String, RtpTransceiver) -> Boolean) {
        val entries = cachedTransceivers.filter { predicate(it.key, it.value) }
        entries.forEach { remove(it.key) }
    }

    /**
     * Get the key for the option.
     *
     * @param option the option.
     */
    fun asKey(option: PublishOption) = option.key()

    /**
     * Clear all transceivers.
     */
    fun clear() {
        logger.d { "[clear] Clearing all transceivers" }
        asList().forEach { it.dispose() }
        cachedTransceivers.clear()
    }

    /**
     * Get the encodings for the option.
     *
     * @param option the option.
     */
    fun getEncodingsFor(option: PublishOption): List<RtpParameters.Encoding> {
        return option.encodings(
            if (option.isVideoStream()) {
                listOf("f", "h", "q")
            } else {
                listOf("a")
            },
            option.max_spatial_layers,
            1.0
        )
    }

    // Internal logic
    private fun PublishOption.toTransceiverInit(trackIdPrefix: String): RtpTransceiverInit {
        val rids =
            if (isVideoStream()) {
                listOf("f", "h", "q")
            } else {
                listOf("a")
            }
        val encodingCount = max_spatial_layers
        val factor = 1.0
        var encodings = encodings(rids, encodingCount, factor)

        if (isVideoStream() && isSvcCodec()) {
            // If we need  to stream SVC codec, send only the full encoding as a rid = "q"
            encodings = encodings.filter { it.rid == "f" }.map { it.apply { rid = "q" } }
        }

        return RtpTransceiverInit(
            RtpTransceiver.RtpTransceiverDirection.SEND_ONLY,
            streamIds(trackIdPrefix),
            encodings,
        )
    }

    private fun PublishOption.encodings(
        rids: List<String>,
        encodingCount: Int,
        factor: Double
    ) = rids.take(encodingCount).map { rid ->
        RtpParameters.Encoding(
            rid,
            true,
            factor,
        ).apply {
            maxBitrateBps = bitrate
            maxFramerate = fps
            if (isSvcCodec()) {
                scalabilityMode = getScalabilityMode()
            } else {
                scaleResolutionDownBy = factor
            }
        }
    }.reversed()

    private fun PublishOption.isVideoStream() =
        track_type == TrackType.TRACK_TYPE_VIDEO || track_type == TrackType.TRACK_TYPE_SCREEN_SHARE

    private fun PublishOption.streamIds(trackIdPrefix: String) =
        listOf("$trackIdPrefix:${track_type.value}:${randomSuffix()}")

    private fun PublishOption.isSvcCodec() = codec?.let {
        VideoCodec.valueOf(it.name.uppercase()).supportsSvc()
    } ?: false

    private fun PublishOption.key() = "$id-$track_type"

    private fun toggleTrack(key: String, enabled: Boolean) = safeCallWithDefault(false) {
        cachedTransceivers[key]?.sender?.track()?.setEnabled(enabled) ?: false
    }

    private fun randomSuffix(from: Int = 0, to: Int = 100): Int {
        require(from <= to) { "Invalid range: from ($from) must be less than or equal to to ($to)" }
        return from + (Math.random() * (to - from + 1)).toInt()
    }
}