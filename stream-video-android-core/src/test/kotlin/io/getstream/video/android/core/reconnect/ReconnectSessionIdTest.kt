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

import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.RtcSession
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ReconnectSessionIdTest : IntegrationTestBase() {

    @Test
    fun `Rejoin creates a new session`() = runTest(UnconfinedTestDispatcher()) {
        // create the call
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.join().getOrNull()

        // Rejoin
        call.rejoin()
        assertNotEquals(sessionMock, call.session)
    }

    @Test
    fun `Fast reconnect does not recreate session`() = runTest {
        // create the call
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        // Rejoin
        call.fastReconnect()
        assertEquals(sessionMock, call.session)
    }
}
