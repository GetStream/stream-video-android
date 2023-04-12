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
fun mangleSDP(
    sdp: SessionDescription,
    enableRed: Boolean = true,
    enableDtx: Boolean = true
): SessionDescription {
    return sdp
    val lines = sdp.description.split("\r\n")
    val modifiedLines = mutableListOf<String>()
    var opusPayloadType: String? = null

    for (line in lines) {
        when {
            enableRed && line.contains("opus/48000") -> {
                opusPayloadType = line.split(" ")[0].substringAfter("a=rtpmap:")
                modifiedLines.add("$line;red=1;useinbandfec=1") // Enable RED
            }

            enableDtx && line.startsWith("a=extmap") && line.contains("urn:ietf:params:rtp-hdrext:ssrc-audio-level") && opusPayloadType != null -> {
                modifiedLines.add("$line\r\na=fmtp:$opusPayloadType usedtx=1") // Enable DTX
            }

            else -> modifiedLines.add(line)
        }
    }

    return SessionDescription(sdp.type, modifiedLines.joinToString("\r\n"))
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
