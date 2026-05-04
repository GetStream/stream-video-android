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

package io.getstream.video.android.core.reconnect

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.FastReconnectResult
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

private inline fun <R> Call.use(block: (Call) -> R): R {
    try {
        return block(this)
    } finally {
        cleanup()
    }
}

@RunWith(RobolectricTestRunner::class)
class ReconnectSessionIdTest : IntegrationTestBase() {

    private fun Call.injectMockNetwork(connected: Boolean = true) {
        val mockNetwork = mockk<NetworkStateProvider>(relaxed = true)
        every { mockNetwork.isConnected() } returns connected
        val field = Call::class.java.getDeclaredField("network\$delegate")
        field.isAccessible = true
        field.set(this, lazyOf(mockNetwork))
    }

    @Test
    fun `Rejoin creates a new session`() = runTest(UnconfinedTestDispatcher()) {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.use {
            call.join().getOrNull()
            call.rejoin()
            assertNotEquals(sessionMock, call.session.value)
        }
    }

    @Test
    fun `Fast reconnect does not recreate session`() = runTest {
        // create the call
        val sessionMock = mockk<RtcSession>(relaxed = true)
        coEvery { sessionMock.getPublisherStats() } returns null
        coEvery { sessionMock.getSubscriberStats() } returns null
        every { sessionMock.subscriber } returns MutableStateFlow(null)
        every { sessionMock.publisher } returns MutableStateFlow(null)
        every { sessionMock.currentSfuInfo() } returns Triple(
            "",
            emptyList(),
            emptyList(),
        )
        coEvery { sessionMock.fastReconnect(any()) } returns FastReconnectResult.Connected
        val call = client.call("default", randomUUID())
        call.injectMockNetwork(connected = true)
        call.session.value = sessionMock

        // Fast reconnect
        call.fastReconnect()
        assertEquals(sessionMock, call.session.value)
    }
}
