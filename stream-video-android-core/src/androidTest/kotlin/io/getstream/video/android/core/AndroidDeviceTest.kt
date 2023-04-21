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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.AudioSwitchHandler
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.utils.buildAudioConstraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CapturerObserver
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoFrame

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

    @Test
    fun camera() = runTest {

        val camera = call.mediaManager.camera
        assertThat(camera).isNotNull()
        camera.startCapture()
    }

    @Test
    fun microphone() = runTest {
        val mic = call.mediaManager.microphone
        assertThat(mic).isNotNull()
        mic.startCapture()
    }

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule
        .grant(Manifest.permission.BLUETOOTH_CONNECT)

    @Test
    fun tempAudioTest() = runTest {
        var audioManager = context.getSystemService<AudioManager>()
        val audioHandler = AudioSwitchHandler(context)
        logger.d { "[setupAudio] #sfu; no args" }
        audioHandler.start()
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

        val speakerOn = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices ?: throw java.lang.IllegalStateException("No devices found")
            val deviceType = if (speakerOn) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }

            val device = devices.firstOrNull { it.type == deviceType } ?: throw java.lang.IllegalStateException("No devices found")

            val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
            logger.d { "[setupAudio] #sfu; isCommunicationDeviceSet: $isCommunicationDeviceSet" }
        }

        // listing devices..
        val allDevices =  audioHandler.availableAudioDevices
        // switching device: device: io.getstream.video.android.core.audio.AudioDevice
        audioHandler.selectDevice(allDevices.first())
        // speaker on

        // alternative way to find the speaker device
        val speakerDevice: AudioDeviceInfo?
        val devices = audioManager!!.availableCommunicationDevices
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                speakerDevice = device
                break
            }
        }

        val activeDevice = allDevices.firstOrNull {
            if (true) {
                it.name.contains("speaker", true)
            } else {
                !it.name.contains("speaker", true)
            }
        }
        audioHandler.selectDevice(activeDevice)

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
    }

    @Test
    fun localTrack() = runTest {
        // join will automatically start the audio and video capture
        // based on the call settings
        val joinResult = call.join()
        assertSuccess(joinResult)

        // verify the video track is present and working
        val videoWrapper = call.state.me.value?.videoTrackWrapped
        assertThat(videoWrapper?.video?.enabled()).isTrue()
        assertThat(videoWrapper?.video?.state()).isEqualTo(MediaStreamTrack.State.LIVE)

        // verify the audio track is present and working
        val audioWrapper = call.state.me.value?.audioTrackWrapped
        assertThat(audioWrapper?.audio?.enabled()).isTrue()
        assertThat(audioWrapper?.audio?.state()).isEqualTo(MediaStreamTrack.State.LIVE)
    }

    @Test
    @Ignore
    fun publishing() = runTest {
        // join will automatically start the audio and video capture
        // based on the call settings
        val joinResult = call.join()
        assertSuccess(joinResult)
        delay(500)

        // see if the ice connection is ok
        val iceConnectionState = call.session?.publisher?.connection?.iceConnectionState()
        assertThat(iceConnectionState).isEqualTo(PeerConnection.IceConnectionState.CONNECTED)
        // verify the stats are being tracked
        val report = call.session?.getPublisherStats()?.value
        assertThat(report).isNotNull()

        // verify we are sending data to the SFU

        // TODO: PeerConnection.IceConnectionState.CONNECTED isn't reached
        // it is RTCOutboundRtpStreamStats && it.bytesSent > 0
        report?.statsMap?.values?.any { it is Throwable }
    }

    @Test
    fun logging() = runTest {
        logger.e { "androidTest logging e " }
        logger.w { "androidTest logging w " }
        logger.i { "androidTest logging i " }
        logger.d { "androidTest logging d " }
    }

    @Test
    fun receiving() = runTest {
        // TODO: have a specific SFU setting to send back fake data
        // TODO: replace the id with your active call
        val call = client.call("default", "AYIh97yMpCXV")
        val joinResult = call.join()
        assertSuccess(joinResult)
        delay(1000)
        delay(100000)

        call.state.participants.collect()
        // see if the ice connection is ok on the subscriber
        val iceConnectionState = call.session?.subscriber?.connection?.iceConnectionState()
        assertThat(iceConnectionState).isEqualTo(PeerConnection.IceConnectionState.CONNECTED)
        // verify the stats are being tracked
        val report = call.session?.getSubscriberStats()?.value
        assertThat(report).isNotNull()

        // loop over the participants
        call.state.participants.value.forEach { participant ->
            val videoTrack = participant.videoTrackWrapped?.video
            assertThat(videoTrack).isNotNull()
            assertThat(videoTrack?.enabled()).isTrue()
            assertThat(videoTrack?.state()).isEqualTo(MediaStreamTrack.State.LIVE)

            val audioTrack = participant.audioTrackWrapped?.audio
            assertThat(audioTrack).isNotNull()
            assertThat(audioTrack?.enabled()).isTrue()
            assertThat(audioTrack?.state()).isEqualTo(MediaStreamTrack.State.LIVE)
        }
    }
}
