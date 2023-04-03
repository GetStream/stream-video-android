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
import io.getstream.video.android.core.events.*
import io.getstream.video.android.core.model.*
import io.getstream.video.android.core.utils.LatencyResult
import io.getstream.video.android.core.utils.getLatencyMeasurements
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.OwnCapability
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.event.ConnectionQualityInfo
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.Participant
import java.net.SocketTimeoutException
import java.util.*

@RunWith(RobolectricTestRunner::class)
class JoinCallTest : IntegrationTestBase() {

    /**
     *
     */
    @Test
    fun `test joining a call`() = runTest {
        val call = client.call("default", randomUUID())
        assertThat(client.state.connection.value).isEqualTo(ConnectionState.PreConnect())
        call.join()

        assertThat(call.state.connection).isEqualTo(ConnectionState.Connected())


        // call.activeSession.connected

        // TODO: check if the connection is ready
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
        println("test")
        withTimeout(10) {
            val results = clientImpl.measureLatency(urls)
            println(results)
        }
        println("testb")




    }

}
