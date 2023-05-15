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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * - Join failure should reconnect
 * -- Either to the same SFU
 * -- Or to a different SFU
 * - Ice connection failure should reconnect
 * -- Either to the same SFU
 * -- Or to a different SFU
 * - Sockets should reconnect
 * - Camera should restart
 */
class ReconnectTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:AndroidDeviceTest")
    @Test
    fun switchSfuTest() = runTest {
        call.join()

        // TODO: exclude the SFU that failed...
        // TODO: can we remove any API calls here or resuse latency measurements
        // TODO: add loading/status indicators

        call.switchSfu()
    }

    @Test
    fun reconnectPeers() = runTest {
        call.join()
        Thread.sleep(2000)
        val a = call.session?.subscriber?.connection?.connectionState()
        val b = call.session?.publisher?.connection?.connectionState()
        println("yyyzzz $a and $b ${call.session?.subscriber}")

        // the socket and rtc connection disconnect...,
        // or ice candidate don't arrive due to temporary network failure
        call.session?.reconnect()
        Thread.sleep(2000)
        // reconnect recreates the peer connections
        val sub = call.session?.subscriber?.connection?.connectionState()
        val pub = call.session?.publisher?.connection?.connectionState()

        println("yyyzzz $sub and $pub ${call.session?.subscriber?.state?.value}")
    }

    @Test
    fun switchSfuQuickly() = runTest {
        call.join()
        Thread.sleep(2000)

        // connect to the new socket
        // do an ice restart
        call.session?.let {
            it.switchSfu(it.sfuUrl, it.sfuToken)
        }

    }

}
