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

package io.getstream.video.android.core.call.components

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.Subscriber
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.webrtc.PeerConnection

/**
 * Tests [CallIceConnectionMonitor], which watches the publisher / subscriber ICE states
 * and triggers an ICE restart when a connection FAILS or is DISCONNECTED.
 */
class CallIceConnectionMonitorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var session: RtcSession
    private lateinit var publisher: Publisher
    private lateinit var subscriber: Subscriber
    private lateinit var sessionFlow: MutableStateFlow<RtcSession?>
    private lateinit var call: Call

    @Before
    fun setup() {
        session = mockk(relaxed = true)
        publisher = mockk(relaxed = true)
        subscriber = mockk(relaxed = true)
        sessionFlow = MutableStateFlow(session)
        call = mockk(relaxed = true)

        every { call.type } returns "default"
        every { call.id } returns "call-id"
        every { call.scope } returns testScope
        every { call.session } returns sessionFlow
        every { session.publisher } returns MutableStateFlow(publisher)
        every { session.subscriber } returns MutableStateFlow(subscriber)
    }

    private fun monitor() = CallIceConnectionMonitor(call)

    @Test
    fun `failed publisher ice state triggers an ice restart`() = runTest(testDispatcher) {
        every { publisher.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(PeerConnection.IceConnectionState.FAILED)
        every { subscriber.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(PeerConnection.IceConnectionState.CONNECTED)

        val monitor = monitor()
        monitor.start()
        advanceUntilIdle()

        coVerify { publisher.connection.restartIce() }
        monitor.stop()
    }

    @Test
    fun `disconnected subscriber ice state requests a subscriber ice restart`() = runTest(
        testDispatcher,
    ) {
        every { publisher.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(PeerConnection.IceConnectionState.CONNECTED)
        every { subscriber.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(PeerConnection.IceConnectionState.DISCONNECTED)

        val monitor = monitor()
        monitor.start()
        advanceUntilIdle()

        coVerify { session.requestSubscriberIceRestart() }
        monitor.stop()
    }

    @Test
    fun `healthy ice states do not trigger restarts`() = runTest(testDispatcher) {
        every { publisher.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(PeerConnection.IceConnectionState.CONNECTED)
        every { subscriber.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(PeerConnection.IceConnectionState.CONNECTED)

        val monitor = monitor()
        monitor.start()
        advanceUntilIdle()

        coVerify(exactly = 0) { publisher.connection.restartIce() }
        coVerify(exactly = 0) { session.requestSubscriberIceRestart() }
        monitor.stop()
    }
}
