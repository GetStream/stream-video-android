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
import io.getstream.video.android.core.events.SFUConnectedEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JoinCallTest : IntegrationTestBase() {

    private val logger by taggedLogger("Test:JoinCallTest")

    /**
     *
     */
    @Test
    fun `test joining a call`() = runTest {
        val call = client.call("default", randomUUID())
        val createResult = call.create()
        assertSuccess(createResult)
        val joinResult = call.join()
        assertSuccess(joinResult)
        waitForNextEvent<SFUConnectedEvent>()
        assertThat(call.state.connection.value).isEqualTo(ConnectionState.Connected)
    }

    @Test
    fun `test latency measurements`() = runTest {
        // latency measurements for different urls
        // for each urls we measure 3 times sequentially (so you cache the connection)
        // average is calculated using the second and third measurement
        // sorted by average
        // timeout in place, ensure that if 1 of the urls is broken it still works
        // TODO: ideally if it takes more than 3 seconds, just collect what we have and move on

        val urls = mutableListOf(
            "https://sfu-9c0dc03.ovh-lim1.stream-io-video.com/latency_test.png",
            "https://sfu-a69b58a.blu-tal1.stream-io-video.com/latency_test.png",
            "https://latency-test.aws-sin1.stream-io-video.com/latency_test.png",
            "http://kibana.us-east.gtstrm.com/" // This url is blocked, and will hang
        )

        // TODO: with timeout doesn't fully work on latency measurements
        // related to java threading and coroutine compatibility
        val results = clientImpl.measureLatency(urls)
        assertThat(results).isNotEmpty()
        logger.d { results.toString() }
    }

    @Test
    fun `test session id`() = runTest {
        TODO()
    }
}
