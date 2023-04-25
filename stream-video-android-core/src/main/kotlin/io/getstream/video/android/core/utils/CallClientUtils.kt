/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.utils

import io.getstream.video.android.core.model.IceServer
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

/**
 * Enabling DTX or RED requires mangling the SDP a bit
 */
fun mangleSdpUtil(
    sdp: SessionDescription,
    enableRed: Boolean = true,
    enableDtx: Boolean = true
): SessionDescription {
    // we don't touch the answer (for now)
    if (sdp.type == SessionDescription.Type.ANSWER) {
        return sdp
    }
    var description = sdp.description
    if (enableDtx) {
        description = description.replace("useinbandfec=1", "useinbandfec=1;usedtx=1")
    }

    if (enableRed) {
        val lines = description.split("\r\n").toMutableList()
        val redLine = lines.indices.find { lines[it].contains("a=rtpmap") && lines[it].contains("red/48000") }
        val opusLine = lines.indices.find { lines[it].contains("a=rtpmap") && lines[it].contains("opus/48000") }
        // we only do something if both are enabled
        if (redLine != null && opusLine != null) {
            val opusIsFirst = opusLine < redLine
            if (opusIsFirst) {
                // we need to swap the red and opus lines
                val redLineString = lines[redLine]
                val opusLineString = lines[opusLine]
                lines[redLine] = opusLineString
                lines[opusLine] = redLineString
            }
        }
        description = lines.joinToString("\r\n")
    }

    if (true) {
        // prefer vp8

        val lines = description.split("\r\n").toMutableList()
        val h264Line = lines.indices.find { lines[it].contains("a=rtpmap:100 H264/90000") }
        val vp8Line = lines.indices.find { lines[it].contains("a=rtpmap:96 VP8/90000") }
        // we only do something if both are enabled
        if (h264Line != null && vp8Line != null) {
            val h264IsFirst = h264Line < vp8Line
            if (h264IsFirst) {
                // we need to swap the h264 and vp8 lines
                val h264LineString = lines[h264Line]
                val vp8LineString = lines[vp8Line]
                lines[h264Line] = vp8LineString
                lines[vp8Line] = h264LineString
            }
        }
        description = lines.joinToString("\r\n")
        description = description.replace("m=video 9 UDP/TLS/RTP/SAVPF 100 101 96 97 98 99 35 36 125 124 127", "m=video 9 UDP/TLS/RTP/SAVPF 96 100 101 97 98 99 35 36 125 124 127")

    }

    return SessionDescription(sdp.type, description)
}

@JvmSynthetic
internal fun buildRemoteIceServers(iceServers: List<IceServer>): List<PeerConnection.IceServer> {
    return iceServers.map {
        PeerConnection.IceServer.builder(it.urls)
            .setUsername(it.username)
            .setPassword(it.password)
            .createIceServer()
    }
}

@JvmSynthetic
internal fun buildConnectionConfiguration(
    iceServers: List<PeerConnection.IceServer>,
    sdpSemantics: PeerConnection.SdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
): PeerConnection.RTCConfiguration {
    return PeerConnection.RTCConfiguration(emptyList()).apply {
        this.sdpSemantics = sdpSemantics
        this.iceServers = iceServers
    }
}

@JvmSynthetic
internal fun buildMediaConstraints(): MediaConstraints {
    return MediaConstraints().apply {
        mandatory.addAll(
            listOf(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"),
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
            )
        )
    }
}

@JvmSynthetic
internal fun buildAudioConstraints(): MediaConstraints {
    val mediaConstraints = MediaConstraints()
    val items = listOf(
        MediaConstraints.KeyValuePair(
            "googEchoCancellation",
            true.toString()
        ),
        MediaConstraints.KeyValuePair(
            "googAutoGainControl",
            true.toString()
        ),
        MediaConstraints.KeyValuePair(
            "googHighpassFilter",
            true.toString()
        ),
        MediaConstraints.KeyValuePair(
            "googNoiseSuppression",
            true.toString()
        ),
        MediaConstraints.KeyValuePair(
            "googTypingNoiseDetection",
            true.toString()
        ),
    )

    return mediaConstraints.apply {
        with(optional) {
            add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
            addAll(items)
        }
    }
}
