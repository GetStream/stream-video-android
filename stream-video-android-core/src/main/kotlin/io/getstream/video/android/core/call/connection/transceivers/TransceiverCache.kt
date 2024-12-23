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

package io.getstream.video.android.core.call.connection.transceivers

import OptimalVideoLayer
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

    private fun PublishOption.key(): String { return "$id-$track_type-${codec?.name?.let { "-$it" } ?: ""}" }

    fun add(publishOption: PublishOption, transceiver: RtpTransceiver) {
        cache[publishOption.key()] = TransceiverId(publishOption, transceiver)
    }

    fun remove(publishOption: PublishOption) {
        cache.remove(publishOption.key())
    }

    fun get(publishOption: PublishOption): RtpTransceiver? {
        return findTransceiver(publishOption)?.transceiver
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

    fun indexOf(publishOption: PublishOption): Int {
        return cache.values.indexOfFirst { it.publishOption.key() == publishOption.key() }
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
        return cache["$id-$trackType"]
    }

    private fun findTransceiverWith(trackType: TrackType, id: Int, codecName: String?): TransceiverId? {
        return cache["$id-$trackType${codecName?.let { "-$it" } ?: ""}"]
    }

    private fun findLayer(publishOption: PublishOption): TrackLayersCache? {
        return layers[publishOption.key()]
    }
}

// Helper data classes:
data class TransceiverId(
    val publishOption: PublishOption,
    val transceiver: RtpTransceiver,
)

data class TrackLayersCache(
    val publishOption: PublishOption,
    var layers: List<OptimalVideoLayer>,
)
