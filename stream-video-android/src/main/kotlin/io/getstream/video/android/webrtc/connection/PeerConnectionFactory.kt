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

package io.getstream.video.android.webrtc.connection

import android.content.Context
import io.getstream.video.android.webrtc.StreamPeerConnection
import io.getstream.video.android.webrtc.signal.SignalClient
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.EglBase
import org.webrtc.EglBase.CONFIG_RGBA
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import stream.video.sfu.Codec
import java.util.*

public class PeerConnectionFactory(
    private val context: Context,
    private val signalClient: SignalClient
) {

    private val eglContext by lazy {
        EglBase.createEgl14(CONFIG_RGBA).eglBaseContext
    }

    private val decoderFactory by lazy {
        DefaultVideoDecoderFactory(
            eglContext
        )
    }

    private val encoderFactory by lazy {
        HardwareVideoEncoderFactory(eglContext, false, false)
    }

    private val factory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        // TODO init SSL?

        PeerConnectionFactory.builder()
            .setOptions(
                PeerConnectionFactory.Options().apply {
                    // TODO - connection options
                }
            )
            .setVideoDecoderFactory(decoderFactory)
            .setVideoEncoderFactory(encoderFactory)
            .createPeerConnectionFactory()
    }

    public fun getEncoderCodecs(): List<Codec> {
        return decoderFactory.supportedCodecs.map {
            Codec(it.name)
        }
    }

    public fun getDecoderCodecs(): List<Codec> {
        return encoderFactory.supportedCodecs.map {
            Codec(it.name)
        }
    }

    /**
     * Peer connection.
     */
    public fun makePeerConnection(
        sessionId: String,
        configuration: PeerConnection.RTCConfiguration,
        type: PeerConnectionType
    ): StreamPeerConnection {
        val peerConnection = PeerConnection(
            sessionId,
            type,
            signalClient
        )
        val connection = makePeerConnectionInternal(
            configuration,
            peerConnection
        )
        return peerConnection.apply { initialize(connection) }
    }

    private fun makePeerConnectionInternal(
        configuration: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer?
    ): PeerConnection {
        return requireNotNull(
            factory.createPeerConnection(
                configuration,
                observer
            )
        )
    }

    /**
     * Audio and Video sources.
     */
    public fun makeVideoSource(isScreencast: Boolean): VideoSource =
        factory.createVideoSource(isScreencast)

    public fun makeVideoTrack(source: VideoSource): VideoTrack = factory.createVideoTrack(
        UUID.randomUUID().toString(), source
    )

    public fun makeAudioSource(constraints: MediaConstraints = MediaConstraints()): AudioSource =
        factory.createAudioSource(constraints)

    public fun makeAudioTrack(source: AudioSource): AudioTrack =
        factory.createAudioTrack(UUID.randomUUID().toString(), source)
}
