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
    fun sessionRetry() = runTest {
        call.join()
        // the socket and rtc connection disconnect...,
        // or ice candidate don't arrive due to temporary network failure
        call.session?.reconnect()
        // reconnect recreates the peer connections
    }

    @Test
    fun reconnect() = runTest {
        call.state.connection // pre connect
        backgroundScope.launch {
            call.join()
            call.state.connection // loading
        }
        // show a loading icon while loading
        call.state.connection // temporary error/ reconnecting
        // permanent error -> Failed to join call. Mention this call id to tech support
        // happy connection

        /**
         * From a UI Perspective, the first joinRequest already sets up state
         * Then the JoinEventResponse gives more state
         *
         * Video is only available after peer connections are ready.
         * And after updateSubscriptions is called
         * And the track is received
         *
         *
         * -- we check for disconnected, closed or failed. if failed, closed or disconnected for more than 3 seconds
         * -- call the coordinator and ask if we should switch SFU
         */

        /**
         * From a UI Perspective, the first joinRequest already sets up state
         * Then the JoinEventResponse gives more state
         *
         * Video is only available after peer connections are ready.
         * And after updateSubscriptions is called
         * And the track is received
         *
         *
         * -- we check for disconnected, closed or failed. if failed, closed or disconnected for more than 3 seconds
         * -- call the coordinator and ask if we should switch SFU
         */
    }
}
