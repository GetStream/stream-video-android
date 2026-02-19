/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import stream.video.sfu.models.AudioBitrateProfile

data class RtpMapAttribute(val index: Int, val number: String, val codec: String, val line: String)

data class MediaStream(val index: Int, var codecs: List<String>, val line: String) {

    override fun toString(): String {
        val parts = line.split(" ")
        val first = parts.subList(0, 3).toMutableList()
        first.addAll(codecs)
        return first.joinToString(" ")
    }
}

/**
 * A middle ground between a regex based approach vs a full parser
 */
class MinimalSdpParser(var sdp: String) {

    private lateinit var lines: MutableList<String>
    private var red: RtpMapAttribute? = null
    private var opus: RtpMapAttribute? = null
    private var h264: RtpMapAttribute? = null
    private var vp8: RtpMapAttribute? = null
    private var audioM: MediaStream? = null
    private var videoM: MediaStream? = null
    private var useinbandfecLine: Int? = null

    init {
        parse()
    }

    fun parse() {
        lines = sdp.split("\r\n", "\n").toMutableList()
        lines?.let { lines ->
            lines.indices.forEach {
                val line = lines[it]
                if (line.contains("a=rtpmap")) {
                    // we want to detect vp8, h264, red and opus
                    if (line.contains("red/48000")) {
                        red = parseRtpMap(it, line)
                    } else if (line.contains("opus/48000")) {
                        opus = parseRtpMap(it, line)
                    } else if (line.contains("H264/90000")) {
                        h264 = parseRtpMap(it, line)
                    } else if (line.contains("VP8/90000")) {
                        vp8 = parseRtpMap(it, line)
                    }
                } else if (line.contains("m=audio")) {
                    audioM = parseMLine(it, line)
                } else if (line.contains("m=video")) {
                    videoM = parseMLine(it, line)
                } else if (line.contains("useinbandfec=1")) {
                    useinbandfecLine = it
                }
            }
        }
    }

    fun mangle(enableDtx: Boolean = true, enableRed: Boolean = true, enableVp8: Boolean = true): String {
        if (enableDtx) {
            useinbandfecLine?.let {
                lines[it] = lines[it].replace("useinbandfec=1", "useinbandfec=1;usedtx=1")
            }
        }
        if (enableRed) {
            if (audioM != null && red != null && opus != null) {
                val codecs = audioM?.codecs
                val redPosition = codecs?.indices?.find { codecs[it] == red?.number }
                val opusPosition = codecs?.indices?.find { codecs[it] == opus?.number }

                // swap the position in the M line
                if (opusPosition != null && redPosition != null && opusPosition < redPosition) {
                    // remove red from the list
                    val newCodecs = codecs.filter { it != red!!.number }.toMutableList()
                    newCodecs.add(0, red!!.number)
                    audioM!!.codecs = newCodecs

                    audioM?.let {
                        lines[it.index] = it.toString()
                    }
                }
            }
        }
        if (enableVp8) {
            if (videoM != null && vp8 != null && h264 != null) {
                val codecs = videoM?.codecs
                val vp8Position = codecs?.indices?.find { codecs[it] == vp8?.number }
                val h264Position = codecs?.indices?.find { codecs[it] == h264?.number }

                // swap the position in the M line
                if (vp8Position != null && h264Position != null && h264Position < vp8Position) {
                    // remove red from the list
                    val newCodecs = codecs.filter { it != vp8!!.number }.toMutableList()
                    newCodecs.add(0, vp8!!.number)
                    videoM!!.codecs = newCodecs

                    videoM?.let {
                        lines[it.index] = it.toString()
                    }
                }
            }
        }
        val new = lines.joinToString("\r\n")
        return new
    }

    fun parseRtpMap(index: Int, line: String): RtpMapAttribute {
        val parts = line.split(" ")
        val codec = parts[1]
        val number = parts[0].split(":")[1]
        return RtpMapAttribute(index, number, codec, line)
    }

    fun parseMLine(index: Int, line: String): MediaStream {
        val parts = line.split(" ")
        val codecs = parts.subList(3, parts.size)
        return MediaStream(index, codecs, line)
    }
}

/**
 * Enabling DTX or RED requires mangling the SDP a bit
 */
fun mangleSdpUtil(
    sdp: SessionDescription,
    enableRed: Boolean = true,
    enableDtx: Boolean = true,
    enableVp8: Boolean = true,
): SessionDescription {
    // we don't touch the answer (for now)
    if (sdp.type == SessionDescription.Type.ANSWER) {
        return sdp
    }
    var description = sdp.description
    var parser = MinimalSdpParser(description)
    description = parser.mangle(enableDtx = enableDtx, enableRed = enableRed, enableVp8 = enableVp8)

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
    sdpSemantics: PeerConnection.SdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN,
): PeerConnection.RTCConfiguration {
    return PeerConnection.RTCConfiguration(emptyList()).apply {
        this.sdpSemantics = sdpSemantics
        this.iceServers = iceServers
        this.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
    }
}

internal val defaultConstraints = MediaConstraints().apply {
    // No mandatory constraints
    // Only optional ones
    optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
}

internal val iceRestartConstraints = MediaConstraints().apply {
    mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
    optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
}

@JvmSynthetic
internal fun buildAudioConstraints(
    audioBitrateProfileProvider: (() -> AudioBitrateProfile)? = null,
): MediaConstraints {
    val mediaConstraints = MediaConstraints()
    val isMusicHighQuality = audioBitrateProfileProvider?.invoke() ==
        AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY
    val constraintValue = if (isMusicHighQuality) false else true

    val items = listOf(
        MediaConstraints.KeyValuePair(
            "googEchoCancellation",
            constraintValue.toString(),
        ),
        MediaConstraints.KeyValuePair(
            "googAutoGainControl",
            constraintValue.toString(),
        ),
        MediaConstraints.KeyValuePair(
            "googHighpassFilter",
            constraintValue.toString(),
        ),
        MediaConstraints.KeyValuePair(
            "googNoiseSuppression",
            constraintValue.toString(),
        ),
        MediaConstraints.KeyValuePair(
            "googTypingNoiseDetection",
            constraintValue.toString(),
        ),
    )

    return mediaConstraints.apply {
        with(optional) {
            add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
            addAll(items)
        }
    }
}
