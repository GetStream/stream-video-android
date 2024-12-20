package io.getstream.video.android.core.call.connection.transceivers

import OptimalVideoLayer
import io.getstream.video.android.core.call.connection.Publisher
import org.webrtc.RtpTransceiver
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import java.util.Collections

class TransceiverCache {
    private val cache = Collections.synchronizedMap(linkedMapOf<String, TransceiverId>())
    private val layers = Collections.synchronizedMap(linkedMapOf<String, TrackLayersCache>())

    /**
     * An array maintaining the order of how transceivers were added
     * to the peer connection.
     */
    private val transceiverOrder = mutableListOf<RtpTransceiver>()

    private fun PublishOption.key(): String { return "$id-$track_type" }

    fun add(publishOption: PublishOption, transceiver: RtpTransceiver) {
        cache[publishOption.key()] = TransceiverId(publishOption, transceiver)
        transceiverOrder.add(transceiver)
    }

    fun remove(publishOption: PublishOption) {
        val transceiver = findTransceiver(publishOption)
        cache.remove(publishOption.key())
        transceiverOrder.remove(transceiver?.transceiver)
    }

    fun get(publishOption: PublishOption): RtpTransceiver? {
        return findTransceiver(publishOption)?.transceiver
    }

    fun getWith(trackType: TrackType, id: Int): RtpTransceiver? {
        return findTransceiverWith(trackType, id)?.transceiver
    }

    fun has(publishOption: PublishOption): Boolean {
        return get(publishOption) != null
    }

    fun find(predicate: (TransceiverId) -> Boolean): TransceiverId? {
        return cache.values.find(predicate)
    }

    fun items(): List<TransceiverId> {
        return cache.values.toList().filter { it.transceiver.sender.track()?.isDisposed == false }
    }

    fun indexOf(transceiver: RtpTransceiver): Int {
        return transceiverOrder.indexOf(transceiver)
    }

    fun getLayers(publishOption: PublishOption): List<OptimalVideoLayer>? {
        val entry = layers[publishOption.key()]
        return entry?.layers
    }

    fun setLayers(publishOption: PublishOption, newLayers: List<OptimalVideoLayer> = emptyList()) {
        val entry = findLayer(publishOption)
        if (entry != null) {
            entry.layers = newLayers
        } else {
            layers[publishOption.key()] = TrackLayersCache(publishOption, newLayers)
        }
    }

    private fun findTransceiver(publishOption: PublishOption): TransceiverId? {
        return cache[publishOption.key()]
    }

    private fun findTransceiverWith(trackType: TrackType, id: Int): TransceiverId? {
        return cache["${id}-${trackType}"]
    }

    private fun findLayer(publishOption: PublishOption): TrackLayersCache? {
        return layers[publishOption.key()]
    }
}

// Helper data classes:
data class TransceiverId(
    val publishOption: PublishOption,
    val transceiver: RtpTransceiver
)

data class TrackLayersCache(
    val publishOption: PublishOption,
    var layers: List<OptimalVideoLayer>
)