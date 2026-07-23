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

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.GetCallResponse
import io.getstream.android.video.generated.models.GetOrCreateCallResponse
import io.getstream.android.video.generated.models.GoLiveResponse
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.android.video.generated.models.RingCallRequest
import io.getstream.android.video.generated.models.StartHLSBroadcastingResponse
import io.getstream.android.video.generated.models.StopLiveResponse
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.recording.RecordingType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CallApiClient], the stateless façade over the coordinator (REST)
 * endpoints. Every method must delegate to [StreamVideoClient] and, where relevant,
 * update [CallState] from the response.
 */
class CallApiClientTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var clientImpl: StreamVideoClient
    private lateinit var state: CallState
    private lateinit var call: Call
    private lateinit var apiClient: CallApiClient

    @Before
    fun setup() {
        clientImpl = mockk(relaxed = true)
        state = mockk(relaxed = true)
        call = mockk(relaxed = true)

        every { call.type } returns "default"
        every { call.id } returns "call-id"
        every { call.clientImpl } returns clientImpl
        every { call.state } returns state
        every { call.scope } returns testScope
        every { call.sessionId } returns "session-id"

        // Response-processing endpoints return Success so the onSuccess/state-update
        // branches are exercised.
        coEvery {
            clientImpl.getCall(any(), any())
        } returns Result.Success(mockk<GetCallResponse>(relaxed = true))
        coEvery {
            clientImpl.getOrCreateCall(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.Success(mockk<GetOrCreateCallResponse>(relaxed = true))
        coEvery {
            clientImpl.getOrCreateCallFullMembers(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.Success(mockk<GetOrCreateCallResponse>(relaxed = true))
        coEvery {
            clientImpl.updateCall(any(), any(), any())
        } returns Result.Success(mockk<UpdateCallResponse>(relaxed = true))
        coEvery {
            clientImpl.goLive(any(), any(), any(), any(), any())
        } returns Result.Success(mockk<GoLiveResponse>(relaxed = true))
        coEvery {
            clientImpl.stopLive(any(), any())
        } returns Result.Success(mockk<StopLiveResponse>(relaxed = true))
        coEvery {
            clientImpl.startBroadcasting(any(), any())
        } returns Result.Success(mockk<StartHLSBroadcastingResponse>(relaxed = true))

        apiClient = CallApiClient(call)
    }

    @Test
    fun `get delegates and updates state`() = runTest(testDispatcher) {
        val result = apiClient.get()

        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify { clientImpl.getCall("default", "call-id") }
        verify { state.updateFromResponse(any<GetCallResponse>()) }
    }

    @Test
    fun `create without members uses getOrCreateCall`() = runTest(testDispatcher) {
        apiClient.create(memberIds = listOf("u1"), ring = false, notify = false)

        coVerify {
            clientImpl.getOrCreateCall(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        verify { state.updateFromResponse(any<GetOrCreateCallResponse>()) }
    }

    @Test
    fun `create with members uses full-members endpoint`() = runTest(testDispatcher) {
        apiClient.create(members = listOf(MemberRequest(userId = "u1")))

        coVerify {
            clientImpl.getOrCreateCallFullMembers(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `create with ring registers an outgoing ringing call`() = runTest(testDispatcher) {
        apiClient.create(ring = true)

        coVerify { call.client.state.addRingingCall(any(), any()) }
    }

    @Test
    fun `update delegates and refreshes state`() = runTest(testDispatcher) {
        apiClient.update(custom = mapOf("k" to "v"))

        coVerify { clientImpl.updateCall(eq("default"), eq("call-id"), any()) }
        verify { state.updateFromResponse(any<UpdateCallResponse>()) }
    }

    @Test
    fun `pin and unpin for everyone delegate`() = runTest(testDispatcher) {
        apiClient.pinForEveryone("s1", "u1")
        apiClient.unpinForEveryone("s1", "u1")

        coVerify { clientImpl.pinForEveryone("default", "call-id", "s1", "u1") }
        coVerify { clientImpl.unpinForEveryone("default", "call-id", "s1", "u1") }
    }

    @Test
    fun `sendReaction delegates`() = runTest(testDispatcher) {
        apiClient.sendReaction(type = "reaction", emoji = ":like:")
        coVerify { clientImpl.sendReaction("default", "call-id", "reaction", ":like:", null) }
    }

    @Test
    fun `queryMembers delegates`() = runTest(testDispatcher) {
        apiClient.queryMembers(
            filter = mapOf("k" to "v"),
            sort = listOf(SortField.Desc("created_at")),
        )
        coVerify {
            clientImpl.queryMembersInternal(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `mute helpers build the correct requests`() = runTest(testDispatcher) {
        apiClient.muteAllUsers()
        apiClient.muteUser("u1")
        apiClient.muteUsers(listOf("u1", "u2"))

        coVerify(exactly = 3) { clientImpl.muteUsers("default", "call-id", any()) }
    }

    @Test
    fun `live helpers delegate and update state`() = runTest(testDispatcher) {
        apiClient.goLive(startRecording = true)
        apiClient.stopLive()

        coVerify { clientImpl.goLive("default", "call-id", any(), any(), any()) }
        coVerify { clientImpl.stopLive("default", "call-id") }
        verify { state.updateFromResponse(any<GoLiveResponse>()) }
        verify { state.updateFromResponse(any<StopLiveResponse>()) }
    }

    @Test
    fun `hls helpers delegate`() = runTest(testDispatcher) {
        apiClient.startHLS()
        apiClient.stopHLS()

        coVerify { clientImpl.startBroadcasting("default", "call-id") }
        coVerify { clientImpl.stopBroadcasting("default", "call-id") }
    }

    @Test
    fun `event and permission helpers delegate`() = runTest(testDispatcher) {
        apiClient.sendCustomEvent(mapOf("k" to "v"))
        apiClient.requestPermissions("send-audio", "send-video")

        coVerify { clientImpl.sendCustomEvent("default", "call-id", any()) }
        coVerify {
            clientImpl.requestPermissions(
                "default",
                "call-id",
                listOf("send-audio", "send-video"),
            )
        }
    }

    @Test
    fun `recording helpers delegate`() = runTest(testDispatcher) {
        apiClient.startRecording(RecordingType.Composite)
        apiClient.stopRecording(RecordingType.Composite)
        apiClient.listRecordings("s1")

        coVerify {
            clientImpl.startRecording("default", "call-id", recordingType = RecordingType.Composite)
        }
        coVerify { clientImpl.stopRecording("default", "call-id", RecordingType.Composite) }
        coVerify { clientImpl.listRecordings("default", "call-id", "s1") }
    }

    @Test
    fun `moderation helpers delegate`() = runTest(testDispatcher) {
        apiClient.blockUser("u1")
        apiClient.kickUser("u2", block = true)
        apiClient.removeMembers(listOf("u3"))
        apiClient.updateMembers(listOf(MemberRequest(userId = "u4")))

        coVerify { clientImpl.blockUser("default", "call-id", "u1") }
        coVerify { clientImpl.kickUser("default", "call-id", "u2", true) }
        coVerify(exactly = 2) { clientImpl.updateMembers("default", "call-id", any()) }
    }

    @Test
    fun `permission grant and revoke delegate`() = runTest(testDispatcher) {
        apiClient.grantPermissions("u1", listOf("send-audio"))
        apiClient.revokePermissions("u1", listOf("send-audio"))

        coVerify(exactly = 2) { clientImpl.updateUserPermissions("default", "call-id", any()) }
    }

    @Test
    fun `ringing helpers delegate`() = runTest(testDispatcher) {
        apiClient.ring()
        apiClient.notify()
        apiClient.accept()
        apiClient.reject(RejectReason.Cancel)

        coVerify { clientImpl.ring("default", "call-id") }
        coVerify { clientImpl.notify("default", "call-id") }
        coVerify { clientImpl.accept("default", "call-id") }
        coVerify { clientImpl.reject("default", "call-id", RejectReason.Cancel) }
    }

    @Test
    fun `ring with an explicit request delegates`() = runTest(testDispatcher) {
        val request = RingCallRequest(video = true)
        apiClient.ring(request)
        coVerify { clientImpl.ring("default", "call-id", request) }
    }

    @Test
    fun `create with members and ring registers an outgoing ringing call`() = runTest(
        testDispatcher,
    ) {
        apiClient.create(members = listOf(MemberRequest(userId = "u1")), ring = true)

        coVerify {
            clientImpl.getOrCreateCallFullMembers(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        coVerify { call.client.state.addRingingCall(any(), any()) }
    }

    @Test
    fun `accept marks the call accepted on this device`() = runTest(testDispatcher) {
        apiClient.accept()
        verify { state.acceptedOnThisDevice = true }
    }

    @Test
    fun `transcription and closed-caption helpers delegate`() = runTest(testDispatcher) {
        apiClient.startTranscription()
        apiClient.stopTranscription()
        apiClient.listTranscription()
        apiClient.startClosedCaptions()
        apiClient.stopClosedCaptions()

        coVerify { clientImpl.startTranscription("default", "call-id") }
        coVerify { clientImpl.stopTranscription("default", "call-id") }
        coVerify { clientImpl.listTranscription("default", "call-id") }
        coVerify { clientImpl.startClosedCaptions("default", "call-id") }
        coVerify { clientImpl.stopClosedCaptions("default", "call-id") }
    }

    @Test
    fun `collectUserFeedback delegates on the call scope`() = runTest(testDispatcher) {
        apiClient.collectUserFeedback(rating = 5, reason = "great")
        advanceUntilIdle()

        coVerify {
            clientImpl.collectFeedback(
                callType = "default",
                id = "call-id",
                sessionId = "session-id",
                rating = 5,
                reason = "great",
                custom = null,
            )
        }
    }
}
