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
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.model.RejectReason
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallBusyHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var streamVideo: StreamVideoClient
    private lateinit var clientState: ClientState
    private lateinit var call: Call

    private lateinit var handler: CallBusyHandler
    private val activeCallFlow = MutableStateFlow<Call?>(null)
    private val ringingCallFlow = MutableStateFlow<Call?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        streamVideo = mockk(relaxed = true)
        clientState = mockk(relaxed = true) {
            every { rejectCallWhenBusy } returns true
            every { activeCall } returns activeCallFlow
            every { ringingCall } returns ringingCallFlow
        }
        call = mockk(relaxed = true)

        every { streamVideo.state } returns clientState
        every { streamVideo.scope } returns testScope
        every { streamVideo.call(any(), any()) } returns call

        handler = CallBusyHandler(streamVideo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    private fun mockCall(id: String): Call {
        return mockk {
            every { this@mockk.id } returns id
        }
    }

    @Test
    fun `returns false when rejectCallWhenBusy is disabled`() {
        every { clientState.rejectCallWhenBusy } returns false

        val result = handler.isBusyWithAnotherCall("video:123")

        assertFalse(result)
    }

    @Test
    fun `returns false when same call is active`() {
        every { clientState.rejectCallWhenBusy } returns true
        every { clientState.activeCall } returns MutableStateFlow(mockCall("123"))
        every { clientState.ringingCall } returns MutableStateFlow(null)

        val result = handler.isBusyWithAnotherCall("video:123")

        assertFalse(result)
    }

    @Test
    fun `returns true when busy with another active call but does not reject if rejectViaApi false`() {
        every { clientState.rejectCallWhenBusy } returns true
        every { clientState.activeCall } returns MutableStateFlow(mockCall("999"))
        every { clientState.ringingCall } returns MutableStateFlow(null)

        val result = handler.isBusyWithAnotherCall("video:123", rejectViaApi = false)

        assertTrue(result)
        verify(exactly = 0) { streamVideo.call(any(), any()) }
    }

    @Test
    fun `rejects call when busy and rejectViaApi true`() = runTest {
        every { clientState.rejectCallWhenBusy } returns true
        every { clientState.activeCall } returns MutableStateFlow(mockCall("999"))
        every { clientState.ringingCall } returns MutableStateFlow(null)

        val callMock = mockk<Call>(relaxed = true)
        every { streamVideo.call("video", "123") } returns callMock

        val result = handler.isBusyWithAnotherCall("video:123", rejectViaApi = true)

        advanceUntilIdle()

        assertTrue(result)
        coVerify { callMock.reject(RejectReason.Busy) }
    }

    @Test
    fun `shouldPropagateEvent returns false when busy`() {
        every { clientState.rejectCallWhenBusy } returns true
        every { clientState.activeCall } returns MutableStateFlow(mockCall("999"))
        every { clientState.ringingCall } returns MutableStateFlow(null)

        val event = mockk<CallRingEvent> {
            every { callCid } returns "video:123"
        }

        val result = handler.shouldPropagateEvent(event)

        assertFalse(result)
    }

    @Test
    fun `shouldShowIncomingCallNotification returns false when busy`() {
        every { clientState.rejectCallWhenBusy } returns true
        every { clientState.activeCall } returns MutableStateFlow(mockCall("999"))
        every { clientState.ringingCall } returns MutableStateFlow(null)

        val result = handler.shouldShowIncomingCallNotification("video:123")

        assertFalse(result)
    }
}
