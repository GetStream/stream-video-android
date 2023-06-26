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

import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.utils.buildAudioConstraints
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStats
import org.webrtc.VideoCodecInfo
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import stream.video.sfu.event.ChangePublishQuality
import stream.video.sfu.event.VideoLayerSetting
import stream.video.sfu.event.VideoMediaRequest
import stream.video.sfu.event.VideoSender
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import stream.video.sfu.signal.UpdateMuteStatesRequest
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertNull

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

    private val newClient: StreamVideo = StreamVideoBuilder(
        context = context,
        apiKey = apiKey,
        geo = GEO.GlobalEdgeNetwork,
        testData.users["thierry"]!!,
        testData.tokens["thierry"]!!,
        ensureSingleInstance = false
    ).build()

    internal class InterceptorTest : Interceptor {

        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()

            val updated = original.newBuilder()
                .url(original.url)
                .build()

            return chain.proceed(updated)
        }
    }

    internal class InterceptorBreaks : Interceptor {

        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()

            throw InterruptedIOException()

            return chain.proceed(original)
        }
    }

    @Test(expected = InterruptedIOException::class)
    fun trythis() = runTest {
        // this hangs:
        val connectionTimeoutInMs = 10000L
        val ok = OkHttpClient.Builder()
            .addInterceptor(InterceptorTest())
            .addInterceptor(InterceptorBreaks())
            .retryOnConnectionFailure(true)
            .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .build()
        val url = "https://hint.stream-io-video.com/"
        val request: Request = Request.Builder()
            .url(url)
            .method("HEAD", null)
            .build()
        val call = ok.newCall(request)

        val retro = Retrofit.Builder()
            .client(ok)
            .addConverterFactory(WireConverterFactory.create())
            .baseUrl(url)
            .build()
        val service = retro.create(SignalServerService::class.java)
        val result = service.updateMuteStates(UpdateMuteStatesRequest("123", emptyList()))
    }

    @Test
    fun camera() = runTest {
        val camera = call.mediaManager.camera
        assertThat(camera).isNotNull()
        camera.startCapture()
    }

    @Test
    fun cleanupCall() = runTest {
        val call = newClient.call("default", UUID.randomUUID().toString())
        // join a call
        val result = call.join(create = true)
        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(result.getOrThrow())
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }
        // leave and cleanup the joining call
        call.leave()
        // cleanup the media manager
        call.mediaManager.cleanup()
        // cleanup rtc
        call.session?.cleanup()
        // cleanup the call
        call.cleanup()
        assertNull(call.session)
        // await until disconnect a call
        assertThat(connectionState.awaitItem()).isEqualTo(RealtimeConnection.Disconnected)
    }

    @Test
    fun cleanupClient() = runTest {
        val call = newClient.call("default", UUID.randomUUID().toString())
        // join a call
        val result = call.join(create = true)
        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(result.getOrThrow())
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }
        // leave and cleanup the joining call
        call.leave()
        call.cleanup()
        // destroy and cleanup the call and client
        newClient.cleanup()
        // assert the sessions was cleared properly
        assertNull(call.session)
        // create a turbine connection state
        val connectionStates = call.state.connection.testIn(backgroundScope)
        // await until disconnect a call
        assertThat(connectionStates.awaitItem()).isEqualTo(RealtimeConnection.Disconnected)
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
        // join a call
        val call = client.call("default", UUID.randomUUID().toString())
        val result = call.join(create = true)
        assertSuccess(result)

        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(result.getOrThrow())
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }

        val participantsCounts = call.state.participantCounts.testIn(backgroundScope)
        assertThat(participantsCounts.awaitItem()?.total).isEqualTo(1)

        delay(1000)

        clientImpl.debugInfo.log()

        // leave and cleanup the joining call
        call.leave()
        call.cleanup()
        // create a turbine connection state
        val connectionStates = call.state.connection.testIn(backgroundScope)
        // await until disconnect a call
        assertThat(connectionStates.awaitItem()).isEqualTo(RealtimeConnection.Disconnected)
    }

    @Test
    fun localTrack() = runTest {
        // join will automatically start the audio and video capture
        // based on the call settings
        val call = client.call("default", UUID.randomUUID().toString())
        val result = call.join(create = true)
        assertSuccess(result)

        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(result.getOrThrow())
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }

        // verify the video track is present and working
        val videoWrapper = call.state.me.value?.videoTrack?.value
        assertThat(videoWrapper?.video?.enabled()).isTrue()
        assertThat(videoWrapper?.video?.state()).isEqualTo(MediaStreamTrack.State.LIVE)

        // verify the audio track is present and working
        val audioWrapper = call.state.me.value?.audioTrack?.value
        assertThat(audioWrapper?.audio?.enabled()).isTrue()
        assertThat(audioWrapper?.audio?.state()).isEqualTo(MediaStreamTrack.State.LIVE)

        // leave and cleanup the joining call
        call.leave()
        call.cleanup()
        // create a turbine connection state
        val connectionStates = call.state.connection.testIn(backgroundScope)
        // await until disconnect a call
        assertThat(connectionStates.awaitItem()).isEqualTo(RealtimeConnection.Disconnected)
    }

    @Test
    fun publishing() = runTest {
        // TODO: disable simulcast
        // join will automatically start the audio and video capture
        val call = client.call("default", "NnXAIvBKE4Hy")
        val joinResult = call.join(create = true)
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
        assertThat(call.mediaManager.videoTrack.enabled()).isTrue()
        assertThat(call.mediaManager.audioTrack.enabled()).isTrue()
        assertThat(call.mediaManager.videoTrack.state()).isEqualTo(MediaStreamTrack.State.LIVE)

        // check that the transceiver is setup
        assertThat(call.session?.publisher?.videoTransceiver).isNotNull()

        // see if we're sending data

        delay(1000)
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
        call.leave()
    }

    @Test
    fun receiving() = runTest {
        val call = client.call("default", UUID.randomUUID().toString())
        // join a call
        val result = call.join(create = true)
        assertSuccess(result)

        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(result.getOrThrow())
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }

        val publisher = call.session?.publisher?.state?.testIn(backgroundScope)

        // assert ice connection state flows
        assertThat(publisher?.awaitItem()).isEqualTo(PeerConnection.PeerConnectionState.NEW)
        assertThat(publisher?.awaitItem()).isEqualTo(PeerConnection.PeerConnectionState.CONNECTING)
        assertThat(publisher?.awaitItem()).isEqualTo(PeerConnection.PeerConnectionState.CONNECTED)

        // assert the participant counts is correct
        val participantCount = call.state.participantCounts.testIn(backgroundScope)
        assertThat(participantCount.awaitItem()?.total).isEqualTo(1)

        // assert the participant's video & audio track
        val participants = call.state.participants.testIn(backgroundScope)
        val participant = participants.awaitItem().first()

        participant.videoTrack.test {
            val videoTrack = awaitItem()?.video
            assertThat(videoTrack).isNotNull()
            assertThat(videoTrack?.enabled()).isTrue()
            assertThat(videoTrack?.state()).isEqualTo(MediaStreamTrack.State.LIVE)
        }

        participant.audioTrack.test {
            val audioTrack = awaitItem()?.audio
            assertThat(audioTrack).isNotNull()
            assertThat(audioTrack?.state()).isEqualTo(MediaStreamTrack.State.LIVE)
        }

        // verify the stats are being tracked
//        val session = call.session
//        assertThat(session!!).isNotNull()
//        session.getSubscriberStats().test {
//            val first = awaitItem()
//            val report = awaitItem()
//
//            assertThat(report).isNotNull()
//
//            val allStats = report?.statsMap?.values
//            val networkOut =
//                allStats?.filter { it.type == "inbound-rtp" }?.map { it as RTCStats }
//
//            // log debug info
//            logger.d { networkOut.toString() }
//        }
//        clientImpl.debugInfo.log()

        // leave and clean up a call
        call.leave()
        call.cleanup()
        // create a turbine connection state
        val connectionStates = call.state.connection.testIn(backgroundScope)
        // await until disconnect a call
        assertThat(connectionStates.awaitItem()).isEqualTo(RealtimeConnection.Disconnected)
    }

    @Test
    fun leaveCall() = runTest {
        // join a call
        val call = client.call("default", UUID.randomUUID().toString())
        val result = call.join(create = true)
        assertSuccess(result)

        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(result.getOrThrow())
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }

        // leave and clean up a call
        call.leave()
        call.cleanup()
        // await until disconnect a call
        assertThat(connectionState.awaitItem()).isEqualTo(RealtimeConnection.Disconnected)
    }

    @Test
    fun endCall() = runTest {
        // join a call
        val call = client.call("default", UUID.randomUUID().toString())
        val result = call.join(create = true)
        assertSuccess(result)

        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(result.getOrThrow())
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }

        // end call
        val endResult = call.end()
        assertSuccess(endResult)

        // clean up a call
        call.cleanup()

        // await until disconnect a call
        assertThat(connectionState.awaitItem()).isEqualTo(RealtimeConnection.Disconnected)
    }

    @Test
    fun dynascale() = runTest {
        // join will automatically start the audio and video capture
        // based on the call settings
        val joinResult = call.join(create = true)
        assertSuccess(joinResult)

        // create a turbine connection state
        val connectionState = call.state.connection.testIn(backgroundScope)
        // asset that the connection state is connected
        val connectionStateItem = connectionState.awaitItem()
        assertThat(connectionStateItem).isAnyOf(
            RealtimeConnection.Connected,
            RealtimeConnection.Joined(joinResult.getOrThrow())
        )
        if (connectionStateItem is RealtimeConnection.Joined) {
            connectionState.awaitItem()
        }

        // fake a participant joining
        val joinEvent = ParticipantJoinedEvent(
            callCid = call.cid,
            participant = Participant(session_id = "fake", user_id = "fake")
        )
        clientImpl.fireEvent(joinEvent, call.cid)
        assertThat(call.state.participants.value.size).isEqualTo(2)
        assertThat(call.state.remoteParticipants.value.size).isEqualTo(1)
        assertThat(call.state.sortedParticipants.value.size).isEqualTo(2)

        // set their video as visible
        call.setVisibility(sessionId = "fake", TrackType.TRACK_TYPE_VIDEO, true)
        call.setVisibility(sessionId = "fake", TrackType.TRACK_TYPE_SCREEN_SHARE, true)

        val tracks1 = call.session?.defaultTracks()
        val tracks2 = call.session?.visibleTracks()

        assertThat(tracks2?.size).isEqualTo(2)
        assertThat(tracks2?.map { it.session_id }).contains("fake")
        assertThat(tracks1?.size).isEqualTo(2)
        assertThat(tracks1?.map { it.session_id }).contains("fake")

        // if their video isn't visible it shouldn't be in the tracks
        call.setVisibility(sessionId = "fake", TrackType.TRACK_TYPE_VIDEO, false)
        val tracks3 = call.session?.visibleTracks()
        assertThat(tracks3?.size).isEqualTo(1)

        // test handling publish quality change
        val mediaRequest = VideoMediaRequest()
        val layers = listOf(
            VideoLayerSetting(name = "f", active = false),
            VideoLayerSetting(name = "h", active = true),
            VideoLayerSetting(name = "q", active = false)
        )
        val quality = ChangePublishQuality(
            video_senders = listOf(
                VideoSender(
                    media_request = mediaRequest,
                    layers = layers
                )
            )
        )
        val event = ChangePublishQualityEvent(changePublishQuality = quality)
        call.session?.updatePublishQuality(event)
        call.leave()
    }
}
