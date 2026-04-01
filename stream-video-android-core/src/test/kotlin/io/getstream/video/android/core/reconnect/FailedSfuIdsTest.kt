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
import io.getstream.video.android.core.call.RtcSession
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class FailedSfuIdsTest : IntegrationTestBase(connectCoordinatorWS = false) {

    @Suppress("UNCHECKED_CAST")
    private fun Call.getFailedSfuIds(): MutableSet<String> {
        val field = Call::class.java.getDeclaredField("failedSfuIds")
        field.isAccessible = true
        return field.get(this) as MutableSet<String>
    }

    private fun Call.invokeAddFailedSfuId(sfuId: String) {
        val method = Call::class.java.getDeclaredMethod("addFailedSfuId", String::class.java)
        method.isAccessible = true
        method.invoke(this, sfuId)
    }

    private fun Call.invokeGetFailedSfuIdsSnapshot(): List<String> {
        val method = Call::class.java.getDeclaredMethod("getFailedSfuIdsSnapshot")
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(this) as List<String>
    }

    @Test
    fun `addFailedSfuId adds unique SFU IDs`() = runTest {
        val call = client.call("default", randomUUID())

        call.invokeAddFailedSfuId("sfu-edge-1")
        call.invokeAddFailedSfuId("sfu-edge-2")

        val ids = call.getFailedSfuIds()
        assertEquals(2, ids.size)
        assertTrue(ids.contains("sfu-edge-1"))
        assertTrue(ids.contains("sfu-edge-2"))
    }

    @Test
    fun `addFailedSfuId does not add duplicates`() = runTest {
        val call = client.call("default", randomUUID())

        call.invokeAddFailedSfuId("sfu-edge-1")
        call.invokeAddFailedSfuId("sfu-edge-1")
        call.invokeAddFailedSfuId("sfu-edge-1")

        assertEquals(1, call.getFailedSfuIds().size)
    }

    @Test
    fun `addFailedSfuId ignores blank strings`() = runTest {
        val call = client.call("default", randomUUID())

        call.invokeAddFailedSfuId("")
        call.invokeAddFailedSfuId("   ")

        assertTrue(call.getFailedSfuIds().isEmpty())
    }

    @Test
    fun `getFailedSfuIdsSnapshot returns a copy`() = runTest {
        val call = client.call("default", randomUUID())

        call.invokeAddFailedSfuId("sfu-edge-1")
        val snapshot = call.invokeGetFailedSfuIdsSnapshot()

        call.invokeAddFailedSfuId("sfu-edge-2")

        assertEquals(1, snapshot.size)
        assertEquals(2, call.getFailedSfuIds().size)
    }

    @Test
    fun `onSfuConnectionEstablished clears failed SFU IDs`() = runTest {
        val call = client.call("default", randomUUID())

        call.invokeAddFailedSfuId("sfu-edge-1")
        call.invokeAddFailedSfuId("sfu-edge-2")
        assertEquals(2, call.getFailedSfuIds().size)

        call.onSfuConnectionEstablished()

        assertTrue(call.getFailedSfuIds().isEmpty())
    }

    @Test
    fun `migrate adds current session sfuName to failed list`() = runTest {
        val call = client.call("default", randomUUID())
        val sessionMock = mockk<RtcSession>(relaxed = true)
        every { sessionMock.sfuName } returns "sfu-edge-old"
        call.session.value = sessionMock
        call.location = "test-location"

        call.migrate()

        assertTrue(call.getFailedSfuIds().contains("sfu-edge-old"))
    }

    @Test
    fun `failed SFU IDs accumulate across multiple migrate calls`() = runTest {
        val call = client.call("default", randomUUID())

        val session1 = mockk<RtcSession>(relaxed = true)
        every { session1.sfuName } returns "sfu-edge-1"
        call.session.value = session1
        call.location = "test-location"
        call.migrate()

        val session2 = mockk<RtcSession>(relaxed = true)
        every { session2.sfuName } returns "sfu-edge-2"
        call.session.value = session2
        call.migrate()

        val ids = call.getFailedSfuIds()
        assertTrue(ids.contains("sfu-edge-1"))
        assertTrue(ids.contains("sfu-edge-2"))
    }

    @Test
    fun `onSfuConnectionEstablished after migrate clears accumulated IDs`() = runTest {
        val call = client.call("default", randomUUID())

        call.invokeAddFailedSfuId("sfu-edge-1")
        call.invokeAddFailedSfuId("sfu-edge-2")
        call.invokeAddFailedSfuId("sfu-edge-3")
        assertFalse(call.getFailedSfuIds().isEmpty())

        call.onSfuConnectionEstablished()

        assertTrue(call.getFailedSfuIds().isEmpty())
    }
}
