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

package io.getstream.video.android.core.rtc

import com.google.common.truth.Truth.assertThat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.base.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
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
    @Ignore
    fun `test joining a call`() = runTest {
        val call = client.call("default", randomUUID())
        val createResult = call.create()
        assertSuccess(createResult)
        // TODO: we need more mocking and improvements to RtcSession to make this work
    }

    @Test
    fun `test latency measurements`() = runTest {
        // latency measurements for different urls
        // for each urls we measure 3 times sequentially (so you cache the connection)
        // average is calculated using the second and third measurement
        // sorted by average
        // timeout in place, ensure that if 1 of the urls is broken it still works

        val urls = mutableListOf(
            "https://sfu-9c0dc03.ovh-lim1.stream-io-video.com/latency_test.png",
            "https://sfu-a69b58a.blu-tal1.stream-io-video.com/latency_test.png",
            "https://latency-test.aws-sin1.stream-io-video.com/latency_test.png",
            "http://kibana.us-east.gtstrm.com/", // This url is blocked, and will hang
        )

        // related to java threading and coroutine compatibility
        val results = clientImpl.measureLatency(urls)
        assertThat(results).isNotEmpty()
        logger.d { results.toString() }
    }
}
