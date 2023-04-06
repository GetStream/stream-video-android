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

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
    fun `start recording a call`() = runTest {
        val call = client.call("livestream", randomUUID())
        val createResult = call.create()
        assertSuccess(createResult)
        val result = call.startRecording()
        assertSuccess(result)
        val result2 = call.stopRecording()
        assertSuccess(result2)
    }

    @Test
    fun `advanced recording`() = runTest {
        // TODO: server support
        // simple layouts to start, roadmap for more options after 1.0
        // see notion
    }

    @Test
    fun `start and stop broadcasting to HLS`() = runTest {
        val call = client.call("livestream", randomUUID())
        call.create()
        val result = call.startBroadcasting()
        assertSuccess(result)
        val result2 = call.stopBroadcasting()
        assertSuccess(result2)
        // TODO: where is the HLS url?
    }

    @Test
    fun `calls should support RTMP in`() = runTest {
        val call = client.call("livestream", randomUUID())
        // TODO: not implemented on the server
        // Create a publishing token
        // TODO: do we ask the coordinator for it? or generate it locally?
    }

    @Test
    fun `join a call with a viewing only token, that's not authenticated`() = runTest {
        // TODO: not implemented on the server
        val call = client.call("livestream", randomUUID(), "mytoken")
    }

    @Test
    fun `call should expose participant count, time running stats`() = runTest {
        val call = client.call("livestream", randomUUID())
        // TODO: not implemented on the server
    }

    @Test
    fun `manually specify the video quality you want to receive`() = runTest {
        val call = client.call("livestream", randomUUID())
        // TODO: not implemented on the server
    }

    @Test
    fun `mute the audio of the call you are receiving`() = runTest {
        val call = client.call("livestream", randomUUID())
        // TODO: This we can implement client side, should be disabled by default
        // TODO: Call type setting to mute incoming audio by default
    }
}
