/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.RtcSession
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ReconnectAttemptsCountTest : IntegrationTestBase() {

    @Test
    fun `Rejoin attempts are correctly updated`() = runTest {
        // create the call
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        // Rejoin
        call.rejoin()
        assertEquals(1, call.reconnectAttepmts)
    }

    @Test
    fun `Fast reconnect does not update the reconnect attempts`() = runTest {
        // create the call
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        // Rejoin
        call.fastReconnect()
        assertEquals(0, call.reconnectAttepmts)
    }

    @Test
    fun `Multiple rejoin calls will increase the reconnect attempts correctly`() = runTest {
        // create the call
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        // Rejoin
        call.rejoin()
        call.rejoin()
        assertEquals(2, call.reconnectAttepmts)
    }
}
