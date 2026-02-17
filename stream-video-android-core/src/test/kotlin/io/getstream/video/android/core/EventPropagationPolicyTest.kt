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

package io.getstream.video.android.core

import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.call.CallBusyHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import kotlin.test.Test

class EventPropagationPolicyTest {
    private lateinit var callBusyHandler: CallBusyHandler
    private lateinit var policy: EventPropagationPolicy

    @Before
    fun setup() {
        callBusyHandler = mockk(relaxed = true)
        policy = EventPropagationPolicy(callBusyHandler)
    }

    @Test
    fun `shouldPropagate delegates to CallBusyHandler for CallRingEvent`() {
        val event = mockk<CallRingEvent>()

        every { callBusyHandler.shouldPropagateEvent(event) } returns false

        val result = policy.shouldPropagate(event)

        assertFalse(result)
        verify(exactly = 1) { callBusyHandler.shouldPropagateEvent(event) }
    }

    @Test
    fun `shouldPropagate returns true when CallBusyHandler allows it`() {
        val event = mockk<CallRingEvent>()

        every { callBusyHandler.shouldPropagateEvent(event) } returns true

        val result = policy.shouldPropagate(event)

        assertTrue(result)
        verify(exactly = 1) { callBusyHandler.shouldPropagateEvent(event) }
    }

    @Test
    fun `shouldPropagate returns true for non CallRingEvent`() {
        val event = mockk<VideoEvent>()

        val result = policy.shouldPropagate(event)

        assertTrue(result)
        verify(exactly = 0) { callBusyHandler.shouldPropagateEvent(any()) }
    }
}
