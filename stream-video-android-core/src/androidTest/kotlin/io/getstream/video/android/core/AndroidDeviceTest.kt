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

import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.SFUConnectedEvent
import io.getstream.video.android.core.utils.buildAudioConstraints
import kotlinx.coroutines.test.runTest
import org.junit.Test

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
class AndroidDeviceTest : IntegrationTestBase() {

    private val logger by taggedLogger("Test:JoinCallTest")

    @Test
    fun camera() = runTest {
        val manager = MediaManagerImpl(context)
        val camera = manager.camera
        assertThat(camera).isNotNull()
        camera.startCapture()
    }

    @Test
    fun microphone() = runTest {
        val manager = MediaManagerImpl(context)
        val mic = manager.microphone
        assertThat(mic).isNotNull()
        mic.startCapture()
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
        waitForNextEvent<SFUConnectedEvent>()
        assertThat(call.state.connection.value).isEqualTo(ConnectionState.Connected)
//        val joinResponse = waitForNextEvent<JoinCallResponseEvent>()
//        val participantsResponse = joinResponse.callState.participants
//        println(participantsResponse)
//        assertThat(participantsResponse.size).isEqualTo(1)
//        val participants = call.state.participants
//        println(participants)
//        assertThat(participants.value.size).isEqualTo(1)
    }

}
