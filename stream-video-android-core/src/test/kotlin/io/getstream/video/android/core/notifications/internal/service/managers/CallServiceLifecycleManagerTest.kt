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

package io.getstream.video.android.core.notifications.internal.service.managers

import io.getstream.android.video.generated.models.GetCallResponse
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test

class CallServiceLifecycleManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var sut: CallServiceLifecycleManager

    private val streamVideo: StreamVideo = mockk(relaxed = true)
    private val call: Call = mockk(relaxed = true)
    private val callState: CallState = mockk(relaxed = true)
    private val ringingStateFlow = MutableStateFlow<RingingState>(RingingState.Idle)
    private val membersFlow = MutableStateFlow<List<MemberState>>(emptyList())

    private val callId = StreamCallId("default", "call-123")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sut = CallServiceLifecycleManager()

        every { streamVideo.call(any(), any()) } returns call
        every { call.state } returns callState
        every { callState.ringingState } returns ringingStateFlow
        every { callState.members } returns membersFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initializeCallAndSocket calls onError when call get fails`() = testScope.runTest {
        val onError = mockk<(Error?) -> Unit>(relaxed = true)

        coEvery { call.get() } returns Result.Failure(Error.GenericError("boom"))

        sut.initializeCallAndSocket(
            scope = this,
            streamVideo = streamVideo,
            call = call,
            onError = onError,
        )

        advanceUntilIdle()

        verify { onError.invoke(any()) }
        coVerify { streamVideo.connectIfNotAlreadyConnected() }
    }

    @Test
    fun `initializeCallAndSocket does not call onError on success`() = testScope.runTest {
        val onError = mockk<(Error?) -> Unit>(relaxed = true)
        val getCallResponseSuccess = mockk<Result.Success<GetCallResponse>>(relaxed = true)
        coEvery { call.get() } returns getCallResponseSuccess

        sut.initializeCallAndSocket(this, streamVideo, call, onError)

        advanceUntilIdle()

        verify(exactly = 0) { onError.invoke(any()) }
        coVerify { streamVideo.connectIfNotAlreadyConnected() }
    }

    @Test
    fun `updateRingingCall adds ringing call to state`() = testScope.runTest {
        val ringingState = RingingState.Incoming()

        sut.updateRingingCall(
            scope = this,
            streamVideo = streamVideo,
            call = call,
            ringingState = ringingState,
        )

        advanceUntilIdle()

        verify {
            streamVideo.state.addRingingCall(call, ringingState)
        }
    }

    @Test
    fun `endCall rejects outgoing call`() = testScope.runTest {
        ringingStateFlow.value = RingingState.Outgoing()

        mockkObject(StreamVideo.Companion)
        every { StreamVideo.instanceOrNull() } returns streamVideo

        sut.endCall(this, callId)

        advanceUntilIdle()

        coVerify {
            call.reject(any(), any())
        }
    }

    @Test
    fun `endCall rejects incoming call when member count is 2`() = testScope.runTest {
        ringingStateFlow.value = RingingState.Incoming()
        membersFlow.value = listOf(mockk(), mockk())

        mockkObject(StreamVideo.Companion)
        every { StreamVideo.instanceOrNull() } returns streamVideo

        sut.endCall(this, callId)

        advanceUntilIdle()

        coVerify { call.reject(source = "memberCount == 2") }
    }

    @Test
    fun `endCall leaves incoming call when member count is not 2`() = testScope.runTest {
        ringingStateFlow.value = RingingState.Incoming()
        membersFlow.value = listOf(mockk())

        mockkObject(StreamVideo.Companion)
        every { StreamVideo.instanceOrNull() } returns streamVideo

        sut.endCall(this, callId)

        advanceUntilIdle()

        verify { call.leave("call-service-end-call-incoming") }
    }

    @Test
    fun `endCall leaves call for unknown ringing state`() = testScope.runTest {
        ringingStateFlow.value = RingingState.Active

        mockkObject(StreamVideo.Companion)
        every { StreamVideo.instanceOrNull() } returns streamVideo

        sut.endCall(this, callId)

        advanceUntilIdle()

        verify { call.leave("call-service-end-call-unknown") }
    }

    @Test
    fun `endCall does nothing when callId is null`() = testScope.runTest {
        mockkObject(StreamVideo.Companion)
        every { StreamVideo.instanceOrNull() } returns streamVideo
        sut.endCall(this, null)

        advanceUntilIdle()

        verify { streamVideo wasNot Called }
    }
}
