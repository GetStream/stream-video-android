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

package io.getstream.video.android.core.notifications.internal.service.observers

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.model.RejectReason
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CallRejectionObserverTest {

    @Test
    fun `rejected by all state declines the call`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val callScope = TestScope(dispatcher)
        val clientScope = TestScope(dispatcher)
        val ringingState = MutableStateFlow<RingingState>(RingingState.Incoming())
        val callState = mockk<CallState> {
            every { this@mockk.ringingState } returns ringingState
        }
        val call = mockk<Call> {
            every { scope } returns callScope
            every { state } returns callState
            coEvery { reject(any(), any()) } returns mockk(relaxed = true)
        }
        val streamVideo = mockk<StreamVideoClient> {
            every { scope } returns clientScope
        }
        CallRejectionObserver(call, streamVideo).observe()
        advanceUntilIdle()

        ringingState.value = RingingState.RejectedByAll
        advanceUntilIdle()

        coVerify(exactly = 1) {
            call.reject(
                source = "RingingState.RejectedByAll",
                reason = RejectReason.Decline,
            )
        }
    }

    @Test
    fun `non-rejected state does not decline the call`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val callScope = TestScope(dispatcher)
        val ringingState = MutableStateFlow<RingingState>(RingingState.Incoming())
        val call = mockk<Call>(relaxed = true) {
            every { scope } returns callScope
            every { state.ringingState } returns ringingState
        }
        val streamVideo = mockk<StreamVideoClient>(relaxed = true)
        CallRejectionObserver(call, streamVideo).observe()

        advanceUntilIdle()

        coVerify(exactly = 0) { call.reject(any(), any()) }
    }
}
