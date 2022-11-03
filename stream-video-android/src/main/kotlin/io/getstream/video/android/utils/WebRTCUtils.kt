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

package io.getstream.video.android.utils

import io.getstream.video.android.model.IceServer
import io.getstream.video.android.module.WebRTCModule
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection

internal fun buildIceServers(servers: List<IceServer>?): List<PeerConnection.IceServer> {
    return if (servers != null && servers.isNotEmpty()) {
        servers.map {
            PeerConnection.IceServer.builder(it.urls.first())
                .setUsername(it.username)
                .setPassword(it.password)
                .createIceServer()
        }
    } else {
        if (WebRTCModule.REDIRECT_SIGNAL_URL == null) {
            buildRemoteIceServers(WebRTCModule.SIGNAL_HOST_BASE)
        } else {
            buildLocalIceServers()
        }
    }
}

internal fun buildTestIceServers(): List<PeerConnection.IceServer> {
    return if (WebRTCModule.REDIRECT_SIGNAL_URL == null) {
        buildRemoteIceServers(WebRTCModule.SIGNAL_HOST_BASE)
    } else {
        buildLocalIceServers()
    }
}

internal fun buildLocalIceServers(): List<PeerConnection.IceServer> {
    return listOf(
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),

        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),
    )
}

internal fun buildRemoteIceServers(iceServers: List<IceServer>): List<PeerConnection.IceServer> {
    return iceServers.map {
        PeerConnection.IceServer.builder(it.urls)
            .setUsername(it.username)
            .setPassword(it.password)
            .createIceServer()
    }
}

internal fun buildRemoteIceServers(hostUrl: String): List<PeerConnection.IceServer> {
    return listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:$hostUrl:3478")
            .setUsername("video")
            .setPassword("video")
            .createIceServer(),
    )
}

internal fun buildConnectionConfiguration(
    iceServers: List<PeerConnection.IceServer>,
    sdpSemantics: PeerConnection.SdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
): PeerConnection.RTCConfiguration {
    return PeerConnection.RTCConfiguration(emptyList()).apply {
        this.sdpSemantics = sdpSemantics
        this.iceServers = iceServers
    }
}

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
