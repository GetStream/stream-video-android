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

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.model.RejectReason
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var state: ClientState
    private lateinit var call: Call

    private lateinit var handler: CallBusyHandler

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        streamVideo = mockk(relaxed = true)
        state = mockk(relaxed = true)
        call = mockk(relaxed = true)

        every { streamVideo.state } returns state
        every { streamVideo.scope } returns testScope
        every { streamVideo.call(any(), any()) } returns call

        handler = CallBusyHandler(streamVideo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `rejectIfBusy returns false when rejectCallWhenBusy is false`() = runTest {
        every { state.rejectCallWhenBusy } returns false
        every { state.hasActiveOrRingingCall() } returns true

        val result = handler.rejectIfBusy(call)

        assertFalse(result)
        coVerify(exactly = 0) { call.reject(RejectReason.Busy) }
    }

    @Test
    fun `rejectIfBusy returns false when no active or ringing call`() = runTest {
        every { state.rejectCallWhenBusy } returns true
        every { state.hasActiveOrRingingCall() } returns false

        val result = handler.rejectIfBusy(call)

        assertFalse(result)
        coVerify(exactly = 0) { call.reject(RejectReason.Busy) }
    }

    @Test
    fun `rejectIfBusy returns true and calls reject when busy`() = runTest {
        every { state.rejectCallWhenBusy } returns true
        every { state.hasActiveOrRingingCall() } returns true

        val result = handler.rejectIfBusy(call)

        assertTrue(result)

        // advance coroutine launched inside scope
        testScope.advanceUntilIdle()

        coVerify(exactly = 1) {
            call.reject(RejectReason.Busy)
        }
    }
}
