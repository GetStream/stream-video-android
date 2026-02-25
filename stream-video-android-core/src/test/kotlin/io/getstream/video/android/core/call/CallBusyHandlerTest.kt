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

package io.getstream.video.android.core.call

import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.video.android.core.Call
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallBusyHandlerTest {

    private fun mockCall(cid: String): Call {
        return mockk {
            every { this@mockk.id } returns cid.split(":")[1]
            every { this@mockk.cid } returns cid
        }
    }

    @Test
    fun `returns false when rejectCallWhenBusy is disabled`() {
        val handler =
            CallBusyHandler(false, MutableStateFlow<Call?>(null), MutableStateFlow<Call?>(null))
        val result = handler.isBusyWithAnotherCall("video:123")

        assertFalse(result)
    }

    @Test
    fun `returns false when same call is active`() {
        val activeCallFlow = MutableStateFlow(mockCall("video:123"))
        val ringingCallFlow = MutableStateFlow<Call?>(null)

        val handler = CallBusyHandler(true, activeCallFlow, ringingCallFlow)
        val result = handler.isBusyWithAnotherCall("video:123")

        assertFalse(result)
    }

    @Test
    fun `returns true when busy with another active call`() {
        val activeCallFlow = MutableStateFlow(mockCall("video:999"))
        val ringingCallFlow = MutableStateFlow<Call?>(null)

        val handler = CallBusyHandler(true, activeCallFlow, ringingCallFlow)
        val result = handler.isBusyWithAnotherCall("video:123")

        assertTrue(result)
        assertTrue(handler.callBusyHandlerState.value != null)
    }

    @Test
    fun `returns true when busy with another ringing call`() {
        val ringingCallFlow = MutableStateFlow(mockCall("video:999"))
        val activeCallFlow = MutableStateFlow<Call?>(null)

        val handler = CallBusyHandler(true, activeCallFlow, ringingCallFlow)
        val result = handler.isBusyWithAnotherCall("video:123")

        assertTrue(result)
        assertTrue(handler.callBusyHandlerState.value != null)
    }

    @Test
    fun `updates reject state flow when busy with another call`() = runTest {
        val activeCallFlow = MutableStateFlow(mockCall("video:999"))
        val ringingCallFlow = MutableStateFlow<Call?>(null)

        val handler = CallBusyHandler(true, activeCallFlow, ringingCallFlow)
        val result = handler.isBusyWithAnotherCall("video:123")

        advanceUntilIdle()

        assertTrue(result)
        assertTrue(handler.callBusyHandlerState.value?.streamCallId?.cid == "video:123")
    }

    @Test
    fun `shouldPropagateEvent returns false when busy`() {
        val activeCallFlow = MutableStateFlow(mockCall("video:999"))
        val ringingCallFlow = MutableStateFlow<Call?>(null)

        val event = mockk<CallRingEvent> {
            every { callCid } returns "video:123"
        }
        val handler = CallBusyHandler(true, activeCallFlow, ringingCallFlow)
        val result = handler.shouldPropagateEvent(event)

        assertFalse(result)
    }
}
