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

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.utils.Timer
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CallSwitchingTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:AndroidDeviceTest")

    @Test
    fun switch() = runTest {

        val location = clientImpl.getCachedLocation()
        println("location: $location")
        val numberOfCalls = 10
        // create 3 calls
        val calls = (0 until numberOfCalls).map {
            val call = client.call("audio_room", "switch-test-$it")
            val result = call.create(custom = mapOf("switchtest" to "1"))
            assertSuccess(result)
            call
        }

        // loop over the calls, join them and leave
        val t = Timer("switch to location $location")
        (0 until numberOfCalls).map {
            val call = client.call("audio_room", "switch-test-$it")
            val result = call.join()
            assertSuccess(result)
            assertSuccess(result)
            t.split("iteration $it")
            call.leave()
        }
        t.finish()

        t?.let {
            logger.i { "${it.name} took ${it.duration}" }
            it.durations.forEach { (s, t) ->
                logger.i { " - ${it.name}:$s took $t" }
            }
        }
    }
}
