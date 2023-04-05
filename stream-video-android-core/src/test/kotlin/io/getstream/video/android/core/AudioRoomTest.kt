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

import io.getstream.video.android.core.model.QueryCallsData
import io.getstream.video.android.core.model.SendReactionData
import io.getstream.video.android.core.utils.mapSuspend
import io.getstream.video.android.core.utils.onSuccess
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioRoomTest : IntegrationTestBase() {
    /**
     * The Swift tutorial is good inspiration
     * https://github.com/GetStream/stream-video-swift/blob/main/docusaurus/docs/iOS/guides/quickstart/audio-room.md
     *
     * Create a call
     * Go live in that call
     *
     * Join a call as a listener
     * Request permission to speak
     *
     * Errors
     * - Trying to publish while not having permissions to do so should raise an error
     *
     */
    @Test
    fun `query calls should succeed`() = runTest {
        /**
         * To test:
         * - Filter on custom fields
         * - Filter on about to start in X hours
         * - Filter on currently live
         */
        val filters = mutableMapOf("active" to true)
        val result = client.queryCalls(QueryCallsData(filters, limit=1))
        assertSuccess(result)
    }

    @Test
    fun `creating a new call should work`() = runTest {
        val result = client.call("default", randomUUID()).create()
        assertSuccess(result)
    }

    @Test
    fun `next we want to go live in a call`() = runTest {
        val call = client.call("audio_room", randomUUID())
        val result = call.create()
        assertSuccess(result)
        val goLiveResult = call.goLive()
        assertSuccess(goLiveResult)
        val stopLiveResult = call.stopLive()
        assertSuccess(stopLiveResult)
    }

    @Test
    fun `maybe send a reaction`() = runTest {
        val response = call.sendReaction(SendReactionData("raise-hand"))
        assertSuccess(response)
    }

    @Test
    fun `for audio rooms it's common to request permissions`() = runTest {
        val result = call.requestPermissions(mutableListOf("screenshare", "send-audio"))
        assertSuccess(result)
    }

    @Test
    fun `sometimes listeners will join as an anonymous user`() = runTest {
        // TODO support and test this
    }

    @Test
    fun `sometimes listeners will join with a token`() = runTest {

        // TODO: server support
        val call = client.call("default", randomUUID(), "token")
        val result = call.get()
        assertSuccess(result)
    }

    @Test
    fun `publishing audio or video as a listener should raise an error`() = runTest {
        // TODO

    }


}
