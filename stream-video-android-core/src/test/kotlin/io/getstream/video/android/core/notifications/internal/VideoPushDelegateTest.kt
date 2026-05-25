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

package io.getstream.video.android.core.notifications.internal

import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.KEY_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.KEY_CREATED_BY_DISPLAY_NAME
import io.getstream.video.android.model.StreamCallId
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPushDelegateTest {

    private val delegate = VideoPushDelegate()
    private val streamVideo: StreamVideo = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(StreamVideo)
    }

    @After
    fun tearDown() {
        unmockkObject(StreamVideo)
    }

    @Test
    fun `check getCallDisplayName() with empty payload`() {
        assertEquals(VideoPushDelegate.DEFAULT_CALL_TEXT, delegate.getCallDisplayName(emptyMap()))
    }

    @Test
    fun `check getCallDisplayName() with non-String value`() {
        assertEquals(
            VideoPushDelegate.DEFAULT_CALL_TEXT,
            delegate.getCallDisplayName(mapOf(KEY_CALL_DISPLAY_NAME to 1)),
        )
    }

    @Test
    fun `check getCallDisplayName() with String value`() {
        val createdBy = "createdBy"
        assertEquals(
            createdBy,
            delegate.getCallDisplayName(mapOf(KEY_CREATED_BY_DISPLAY_NAME to createdBy)),
        )
    }

    @Test
    fun `check getCallDisplayName() pick KEY_CALL_DISPLAY_NAME`() {
        val callDisplayName = "callDisplayName"
        val createdBy = "createdBy"
        assertEquals(
            callDisplayName,
            delegate.getCallDisplayName(
                mapOf(
                    KEY_CALL_DISPLAY_NAME to callDisplayName,
                    KEY_CREATED_BY_DISPLAY_NAME to createdBy,
                ),
            ),
        )
    }

    @Test
    fun `handlePushMessage routes ring payload to onRingingCall`() {
        every { StreamVideo.instanceOrNull() } returns streamVideo
        val payload = validPayload(type = "call.ring", callDisplayName = "Daily sync")

        val handled = delegate.handlePushMessage(metadata = emptyMap(), payload = payload)

        assertTrue(handled)
        verify {
            streamVideo.onRingingCall(
                callId = StreamCallId("default", "123"),
                callDisplayName = "Daily sync",
                payload = payload,
            )
        }
    }

    @Test
    fun `handlePushMessage routes missed payload to onMissedCall using created by fallback`() {
        every { StreamVideo.instanceOrNull() } returns streamVideo
        val payload = validPayload(
            type = "call.missed",
            callDisplayName = "   ",
            createdByDisplayName = "Nina",
        )

        val handled = delegate.handlePushMessage(metadata = emptyMap(), payload = payload)

        assertTrue(handled)
        verify {
            streamVideo.onMissedCall(
                callId = StreamCallId("default", "123"),
                callDisplayName = "Nina",
                payload = payload,
            )
        }
    }

    @Test
    fun `handlePushMessage routes notification payload to onNotification`() {
        every { StreamVideo.instanceOrNull() } returns streamVideo
        val payload = validPayload(type = "call.notification", callDisplayName = "Town hall")

        val handled = delegate.handlePushMessage(metadata = emptyMap(), payload = payload)

        assertTrue(handled)
        verify {
            streamVideo.onNotification(
                callId = StreamCallId("default", "123"),
                callDisplayName = "Town hall",
                payload = payload,
            )
        }
    }

    @Test
    fun `handlePushMessage routes live started payload to onLiveCall`() {
        every { StreamVideo.instanceOrNull() } returns streamVideo
        val payload = validPayload(type = "call.live_started", callDisplayName = "Livestream")

        val handled = delegate.handlePushMessage(metadata = emptyMap(), payload = payload)

        assertTrue(handled)
        verify {
            streamVideo.onLiveCall(
                callId = StreamCallId("default", "123"),
                callDisplayName = "Livestream",
                payload = payload,
            )
        }
    }

    @Test
    fun `handlePushMessage returns false when sender is not stream`() {
        every { StreamVideo.instanceOrNull() } returns streamVideo

        val handled = delegate.handlePushMessage(
            metadata = emptyMap(),
            payload = validPayload(type = "call.ring") + ("sender" to "other"),
        )

        assertFalse(handled)
        verify(exactly = 0) { streamVideo.onRingingCall(any(), any(), any()) }
    }

    @Test
    fun `handlePushMessage returns false when call cid is blank`() {
        every { StreamVideo.instanceOrNull() } returns streamVideo

        val handled = delegate.handlePushMessage(
            metadata = emptyMap(),
            payload = validPayload(type = "call.ring") + ("call_cid" to " "),
        )

        assertFalse(handled)
        verify(exactly = 0) { streamVideo.onRingingCall(any(), any(), any()) }
    }

    private fun validPayload(
        type: String,
        callCid: String = "default:123",
        callDisplayName: String = "Call display name",
        createdByDisplayName: String = "Created by",
    ): Map<String, Any?> {
        return mapOf(
            "sender" to "stream.video",
            "type" to type,
            "call_cid" to callCid,
            KEY_CALL_DISPLAY_NAME to callDisplayName,
            KEY_CREATED_BY_DISPLAY_NAME to createdByDisplayName,
        )
    }
}
