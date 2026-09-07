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

package io.getstream.video.android.core.notifications.internal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class GenericCallActionBroadcastReceiverTest {

    private val testAction = "io.getstream.video.android.TEST_ACTION"

    /**
     * Concrete receiver used to observe how [GenericCallActionBroadcastReceiver] dispatches
     * to the actual action handler.
     */
    private class TestReceiver(override val action: String) : GenericCallActionBroadcastReceiver() {
        @Volatile
        var handledCall: Call? = null
        val invocationCount = AtomicInteger(0)

        override suspend fun onReceive(call: Call, context: Context, intent: Intent) {
            invocationCount.incrementAndGet()
            handledCall = call
        }
    }

    private lateinit var context: Context
    private lateinit var receiver: TestReceiver
    private lateinit var pendingResult: BroadcastReceiver.PendingResult

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        pendingResult = mockk(relaxed = true)
        // Spy so we can observe goAsync() and stub it, since a real broadcast context is absent.
        receiver = spyk(TestReceiver(testAction))
        every { receiver.goAsync() } returns pendingResult
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun intentWithCallId(callId: StreamCallId?, action: String = testAction): Intent =
        Intent(action).apply {
            if (callId != null) {
                putExtra(NotificationHandler.INTENT_EXTRA_CALL_CID, callId)
            }
        }

    @Test
    fun `when stream call id is missing goAsync is not called so the broadcast is not left pending`() {
        receiver.onReceive(context, intentWithCallId(null))

        // The core of the fix: without a pending result there is nothing to leak.
        verify(exactly = 0) { receiver.goAsync() }
        assertEquals(0, receiver.invocationCount.get())
    }

    @Test
    fun `when action does not match the intent goAsync is not called and handler is not invoked`() {
        receiver.onReceive(
            context,
            intentWithCallId(StreamCallId("default", "123"), action = "some.other.action"),
        )

        verify(exactly = 0) { receiver.goAsync() }
        assertEquals(0, receiver.invocationCount.get())
    }

    @Test
    fun `when stream call id is present the broadcast is kept alive and finished`() {
        mockkObject(StreamVideo.Companion)
        val streamVideo = mockk<StreamVideo>(relaxed = true)
        val call = mockk<Call>(relaxed = true)
        every { StreamVideo.instanceOrNull() } returns streamVideo
        every { streamVideo.call(any(), any()) } returns call

        receiver.onReceive(context, intentWithCallId(StreamCallId("default", "123")))

        verify(exactly = 1) { receiver.goAsync() }
        // The launched coroutine finishes the pending broadcast once the handler completes.
        verify(timeout = 5_000) { pendingResult.finish() }
    }

    @Test
    fun `when stream call id is present the action handler receives the resolved call`() {
        mockkObject(StreamVideo.Companion)
        val streamVideo = mockk<StreamVideo>(relaxed = true)
        val call = mockk<Call>(relaxed = true)
        every { StreamVideo.instanceOrNull() } returns streamVideo
        every { streamVideo.call("default", "123") } returns call

        receiver.onReceive(context, intentWithCallId(StreamCallId("default", "123")))

        verify(timeout = 5_000) { pendingResult.finish() }
        assertEquals(call, receiver.handledCall)
    }

    @Test
    fun `when stream video is not initialised the broadcast is still finished`() {
        mockkObject(StreamVideo.Companion)
        every { StreamVideo.instanceOrNull() } returns null

        receiver.onReceive(context, intentWithCallId(StreamCallId("default", "123")))

        verify(exactly = 1) { receiver.goAsync() }
        verify(timeout = 5_000) { pendingResult.finish() }
        assertEquals(0, receiver.invocationCount.get())
    }
}
