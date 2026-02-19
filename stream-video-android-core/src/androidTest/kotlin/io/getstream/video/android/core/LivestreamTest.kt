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

package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test

class LivestreamTest : IntegrationTestBase() {
    /**
     * This test covers the most commonly used endpoints for a livestream
     * Some of these are already covered by the audio room test
     *
     * Create the client without providing the user
     *
     * Go live on a call that's in backstage mode
     *
     * Join a call with token based permissions (either publish or viewer mode)
     *
     * Join a call in viewing only mode
     *
     * Start and stop recording
     *
     * Verify that the following stats are available. (participant count, time running)
     *
     * Check that you can manually change the quality of the call you are receiving
     *
     * Mute the audio of the livestream you are viewing
     *
     * Also see: https://www.notion.so/stream-wiki/Livestream-Tutorial-Android-Brainstorm-1568ee5cb23b4d23b5de69defaa1cc76
     */

    @Test
    fun listRecordings() = runTest {
        val result = call.listRecordings()
        assertSuccess(result)
    }

    @Test
    @Ignore
    fun hls() = runTest {
        val call = client.call("livestream", randomUUID())
        val createResult = call.create()
        assertSuccess(createResult)

        val result = call.startHLS()
        assertSuccess(result)

        assertThat(call.state.egress.value?.hls).isNotNull()

        val result2 = call.stopHLS()
        assertSuccess(result2)
    }

    @Test
    fun RTMPin() = runTest {
        val call = client.call("default", "NnXAIvBKE4Hy")
        val response = call.create()
        assertSuccess(response)

        val rtmp = call.state.ingress.value?.rtmp
        println("client token: ${clientImpl.token}")
        println("rtmp address: ${rtmp?.address}")
        println("rtmp streamKey: ${rtmp?.streamKey}")

        // streamKey should be just the token, not apiKey/token
        assertThat(rtmp?.streamKey?.equals(clientImpl.token)).isTrue()
    }

    @Test
    fun participantCount() = runTest {
        val call = client.call("livestream")
        client.subscribe {
            println("hi123 event: $it")
        }
        call.join(create = true)
        print("debugging: call cid is:  " + call.cid)
        Thread.sleep(1000L)

        // counts and startedAt
        assertThat(call.state.participants.value).isNotEmpty()
        assertThat(call.state.startedAt).isNotNull()
        assertThat(call.state.participantCounts.value?.total).isEqualTo(1)
        assertThat(call.state.participantCounts.value?.anonymous).isEqualTo(0)
    }

    @Test
    fun timeRunning() = runTest {
        println("starting")
        val call = client.call("livestream")
        assertSuccess(call.create())
        val goLiveResponse = call.goLive()
        assertSuccess(goLiveResponse)

        val duration = call.state.durationInMs.value
        println("duration: $duration")
        call.leave()
    }
}
