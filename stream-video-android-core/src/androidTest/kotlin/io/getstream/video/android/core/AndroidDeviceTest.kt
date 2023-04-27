/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.utils.buildAudioConstraints
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStats
import org.webrtc.VideoCodecInfo
import stream.video.sfu.event.ChangePublishQuality


/**
 * Things to test in a real android environment
 *
 * * Video decoding: VP8, VP9, H264
 * * Audio decoding: Opus, Opus Red, Opus DTX
 * * Video encoding
 * * Connection/join call flow
 *
 * For the connection flow
 * * Does the coordinator WS connect
 * * Does the SFU WS connect
 * * Do we receive the join event
 */
class AndroidDeviceTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:AndroidDeviceTest")

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule
        .grant(Manifest.permission.BLUETOOTH_CONNECT)

    @Test
    fun camera() = runTest {

        val camera = call.mediaManager.camera
        assertThat(camera).isNotNull()
        camera.startCapture()
    }

    @Test
    fun cleanupCall() = runTest {
        val result = call.join()
        // cleanup the media manager
        call.mediaManager.cleanup()
        // cleanup rtc
        call.session?.cleanup()
        // cleanup the call
        call.cleanup()
    }

    @Test
    fun cleanupClient() = runTest {
        val newClient = StreamVideoBuilder(
            context = context,
            apiKey = apiKey,
            geo = GEO.GlobalEdgeNetwork,
            testData.users["thierry"]!!,
            testData.tokens["thierry"]!!,
        ).build()
        val call = newClient.call("default", randomUUID())
        val result = call.join()
        // destroy and cleanup the client
        newClient.cleanup()
    }



    @Test
    fun codecsFun() = runTest {
        val codecs = getSupportedCodecs()
        println("Supported codecs:")
        codecs.forEach { codec ->
            println("Name: ${codec.name}, Payload: $codec")
        }
        println(codecs)
    }

    fun getSupportedCodecs(): List<VideoCodecInfo> {
        val rootEglBase = clientImpl.peerConnectionFactory.eglBase
        val encoderFactory = DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        val supportedEncoderCodecs: MutableList<VideoCodecInfo> = mutableListOf()
        encoderFactory.supportedCodecs.forEach { codec ->
            supportedEncoderCodecs.add(codec)
        }

        val supportedDecoderCodecs: MutableList<VideoCodecInfo> = mutableListOf()
        decoderFactory.supportedCodecs.forEach { codec ->
            supportedDecoderCodecs.add(codec)
        }

        return (supportedEncoderCodecs + supportedDecoderCodecs).distinctBy { it.name }
    }

    @Test
    fun audioAndVideoSource() = runTest {
        val audioConstraints = buildAudioConstraints()
        val videoSource = clientImpl.peerConnectionFactory.makeVideoSource(false)
        assertThat(videoSource).isNotNull()
        val audioSource = clientImpl.peerConnectionFactory.makeAudioSource(audioConstraints)
        assertThat(audioSource).isNotNull()
    }

    @Test
    fun createACall() = runTest {
        val call = client.call("default", randomUUID())
        val createResult = call.create()
        assertSuccess(createResult)
    }

    @Test
    fun coordinatorWSConnection() = runTest {
        assertThat(client.state.connection.value).isEqualTo(ConnectionState.Connected)
    }

    @Test
    fun joinACall() = runTest {
        val joinResult = call.join()
        assertSuccess(joinResult)
        val joinResponse = waitForNextEvent<JoinCallResponseEvent>()
        assertThat(call.state.connection.value).isEqualTo(ConnectionState.Connected)

        val participantsResponse = joinResponse.callState.participants
        assertThat(participantsResponse.size).isEqualTo(1)
        val participants = call.state.participants
        assertThat(participants.value.size).isEqualTo(1)

        Thread.sleep(2000)
        clientImpl.debugInfo.log()
    }

    @Test
    fun localTrack() = runTest {
        // join will automatically start the audio and video capture
        // based on the call settings
        val joinResult = call.join()
        assertSuccess(joinResult)

        // verify the video track is present and working
        val videoWrapper = call.state.me.value?.videoTrack?.value
        assertThat(videoWrapper?.video?.enabled()).isTrue()
        assertThat(videoWrapper?.video?.state()).isEqualTo(MediaStreamTrack.State.LIVE)

        // verify the audio track is present and working
        val audioWrapper = call.state.me.value?.audioTrack?.value
        assertThat(audioWrapper?.audio?.enabled()).isTrue()
        assertThat(audioWrapper?.audio?.state()).isEqualTo(MediaStreamTrack.State.LIVE)
    }

    @Test
    fun publishing() = runTest {
        // TODO: disable simulcast
        // join will automatically start the audio and video capture
        val call = client.call("default", "NnXAIvBKE4Hy")
        val joinResult = call.join()
        assertSuccess(joinResult)

        // wait for the ice connection state
        withTimeout(3000) {
            while (true) {
                val iceConnectionState = call.session?.publisher?.connection?.iceConnectionState()
                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                    break
                }
            }
        }

        // verify we have running tracks
        assertThat(call.mediaManager.videoTrack.enabled())
        assertThat(call.mediaManager.audioTrack.enabled())
        assertThat(call.mediaManager.videoTrack.state()).isEqualTo(MediaStreamTrack.State.LIVE)

        // check that the transceiver is setup
        assertThat(call.session?.publisher?.videoTransceiver).isNotNull()

        // see if we're sending data

        Thread.sleep(20000)
        val report = call.session?.getPublisherStats()?.value
        assertThat(report).isNotNull()

        // verify we are sending data to the SFU
        // it is RTCOutboundRtpStreamStats && it.bytesSent > 0
        val allStats = report?.statsMap?.values
        val networkOut = allStats?.filter { it.type == "outbound-rtp" }?.map { it as RTCStats }
        val localSdp = call.session?.publisher?.localSdp
        val remoteSdp = call.session?.publisher?.remoteSdp

        println(call.session?.publisher?.localSdp)
        println(call.session?.publisher?.remoteSdp)

        println(networkOut)
    }

    @Test
    fun receiving() = runTest {
        // TODO: have a specific SFU setting to send back fake data
        // TODO: replace the id with your active call
        val call = client.call("default", "NnXAIvBKE4Hy")
        val joinResult = call.join()
        assertSuccess(joinResult)


        // wait for the ice connection state
        withTimeout(3000) {
            while (true) {
                val iceConnectionState = call.session?.subscriber?.connection?.iceConnectionState()
                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                    break
                }
            }
        }

        // see if the ice connection is ok on the subscriber
        val iceConnectionState = call.session?.subscriber?.connection?.iceConnectionState()
        assertThat(iceConnectionState).isEqualTo(PeerConnection.IceConnectionState.CONNECTED)

        assertThat(call.state.participants.value.size).isGreaterThan(1)
        // loop over the participants
        call.state.participants.value.forEach { participant ->
            val videoTrack = participant.videoTrack.value?.video
            assertThat(videoTrack).isNotNull()
            assertThat(videoTrack?.enabled()).isTrue()
            assertThat(videoTrack?.state()).isEqualTo(MediaStreamTrack.State.LIVE)
            assertThat(participant.videoEnabled.value).isTrue()

            val audioTrack = participant.audioTrack.value?.audio
            assertThat(audioTrack).isNotNull()
            assertThat(audioTrack?.enabled()).isTrue()
            assertThat(audioTrack?.state()).isEqualTo(MediaStreamTrack.State.LIVE)
            assertThat(participant.audioEnabled.value).isTrue()
        }

        // verify the stats are being tracked
        val report = call.session?.getSubscriberStats()?.value

        Thread.sleep(20000)
        val allStats = report?.statsMap?.values
        val networkOut = allStats?.filter { it.type == "inbound-rtp" }?.map { it as RTCStats }

        assertThat(report).isNotNull()
        clientImpl.debugInfo.log()
    }

    @Test
    fun dynascale() = runTest {
        // join will automatically start the audio and video capture
        // based on the call settings
        val joinResult = call.join()
        assertSuccess(joinResult)
        delay(500)

        val quality = ChangePublishQuality()
        val event = ChangePublishQualityEvent(changePublishQuality = quality)
        call.session?.updatePublishQuality(event)
    }
}
