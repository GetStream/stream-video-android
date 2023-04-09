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

/**
 * Things to test in a real android environment
 *
 * * Video decoding: VP8, VP9, H264
 * * Audio decoding: Opus, Opus Red, Opus DTX
 * * Video encoding
 * * Connection/join call flow
 */
class JoinCallTest : IntegrationTestBase() {

    private val logger by taggedLogger("Test:JoinCallTest")

    /**
     *
     */
    @Test
    fun test() = runTest {
        val call = client.call("default", randomUUID())
        val createResult = call.create()
        assertSuccess(createResult)
        val joinResult = call.join()
        assertSuccess(joinResult)
        waitForNextEvent<SFUConnectedEvent>()
        assertThat(call.state.connection.value).isEqualTo(ConnectionState.Connected)
    }

}
