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

import android.content.Context
import androidx.lifecycle.Lifecycle
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.android.video.generated.models.CallSessionStartedEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.call.CallBusyHandler
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.notifications.internal.StreamNotificationManager
import io.getstream.video.android.core.socket.common.token.TokenRepository
import io.getstream.video.android.core.sounds.RingingCallVibrationConfig
import io.getstream.video.android.core.sounds.Sounds
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamVideoClientTest {
    private lateinit var client: StreamVideoClient
    private lateinit var state: ClientState

    @Before
    fun setup() {
        client = prepareClient()

        state = mockk(relaxed = true)

        // Inject mocked state via reflection
        client::class.java.getDeclaredField("state").apply {
            isAccessible = true
            set(client, state)
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun prepareClient(): StreamVideoClient {
        val context = mockk<Context>(relaxed = true)
        val lifecycle = mockk<Lifecycle>(relaxed = true)
        val coordinator = mockk<CoordinatorConnectionModule>(relaxed = true)
        val tokenRepo = mockk<TokenRepository>(relaxed = true)
        val notificationManager = mockk<StreamNotificationManager>(relaxed = true)
        val sounds = mockk<Sounds>(relaxed = true)
        val vibration = mockk<RingingCallVibrationConfig>(relaxed = true)

        return spyk(
            StreamVideoClient(
                context = context,
                user = mockk(relaxed = true),
                apiKey = "apikey",
                token = "token",
                lifecycle = lifecycle,
                coordinatorConnectionModule = coordinator,
                tokenRepository = tokenRepo,
                streamNotificationManager = notificationManager,
                enableCallNotificationUpdates = false,
                sounds = sounds,
                vibrationConfig = vibration,
            ),
            recordPrivateCalls = true,
        )
    }

    @Test
    fun `resolveSelectedCid returns explicit cid when provided`() {
        val event = mockk<VideoEvent>()

        val result = client.resolveSelectedCid(event, "video:123")

        assertEquals("video:123", result)
    }

    @Test
    fun `resolveSelectedCid extracts cid from WSCallEvent`() {
        val event = mockk<CallSessionStartedEvent>()
        every { event.getCallCID() } returns "video:999"

        val result = client.resolveSelectedCid(event, "")

        assertEquals("video:999", result)
    }

    @Test
    fun `notifyClientSubscriptions triggers listener when no filter`() {
        val event = mockk<VideoEvent>()
        val listener = mockk<VideoEventListener<VideoEvent>>(relaxed = true)

        val sub = EventSubscription(listener)

        client::class.java.getDeclaredField("subscriptions").apply {
            isAccessible = true
            set(client, mutableSetOf(sub))
        }

        client.notifyClientSubscriptions(event)

        verify { listener.onEvent(event) }
    }

    @Test
    fun `notifyClientSubscriptions triggers only when filter matches`() {
        val event = mockk<VideoEvent>()
        val listener = mockk<VideoEventListener<VideoEvent>>(relaxed = true)

        val sub = EventSubscription(listener) { false }

        client::class.java.getDeclaredField("subscriptions").apply {
            isAccessible = true
            set(client, mutableSetOf(sub))
        }

        client.notifyClientSubscriptions(event)

        verify(exactly = 0) { listener.onEvent(any()) }
    }

    @Test
    fun `shouldProcessCallAcceptedEvent returns false when accepted event not for outgoing call`() {
        val event = mockk<CallAcceptedEvent>()
        every { event.callCid } returns "video:999"

        val ringingCall = mockk<Call>(relaxed = true)
        every { ringingCall.cid } returns "video:123"

        val ringingStateFlow = MutableStateFlow<RingingState>(RingingState.Outgoing(false))

        val callState = mockk<CallState>(relaxed = true)
        every { callState.ringingState } returns ringingStateFlow
        every { ringingCall.state } returns callState

        every { state.ringingCall } returns MutableStateFlow(ringingCall)

        val result = client.shouldProcessCallAcceptedEvent(event)

        assertFalse(result)
    }

    @Test
    fun `shouldProcessCallAcceptedEvent returns true when same cid`() {
        val event = mockk<CallAcceptedEvent>()
        every { event.callCid } returns "video:123"

        val ringingCall = mockk<Call>(relaxed = true)
        every { ringingCall.cid } returns "video:123"

        val ringingStateFlow = MutableStateFlow<RingingState>(RingingState.Outgoing(false))

        val callState = mockk<CallState>(relaxed = true)
        every { callState.ringingState } returns ringingStateFlow
        every { ringingCall.state } returns callState

        every { state.ringingCall } returns MutableStateFlow(ringingCall)

        val result = client.shouldProcessCallAcceptedEvent(event)

        assertTrue(result)
    }

    @Test
    fun `propagateEventToCall updates call components`() {
        val event = mockk<VideoEvent>()
        val call = mockk<Call>(relaxed = true)

        client::class.java.getDeclaredField("calls").apply {
            isAccessible = true
            set(client, mutableMapOf("video:123" to call))
        }

        client.propagateEventToCall("video:123", event)

        verify { call.state.handleEvent(event) }
        verify { call.handleEvent(event) }
    }

    @Test
    fun `fireEvent full flow executes in order when callBusyHandler allows`() {
        val event = mockk<CallRingEvent>(relaxed = true)

        every { event.callCid } returns "video:999"
        every { client.callBusyHandler.shouldPropagateEvent(event) } returns true

        client.fireEvent(event)

        verify { state.handleEvent(event) }
    }

    @Test
    fun `fireEvent won't fully flow executes in when callBusyHandler returns false`() {
        val event = mockk<CallRingEvent>(relaxed = true)

        every { event.callCid } returns "video:999"
        val client = prepareClient()
        val clientState = mockk<ClientState>(relaxed = true)
        client::class.java.getDeclaredField("state").apply {
            isAccessible = true
            set(client, clientState)
        }
        val mockCallBusyHandler = mockk<CallBusyHandler>(relaxed = true)
        every { mockCallBusyHandler.shouldPropagateEvent(event) } returns false
        client::class.java.getDeclaredField("callBusyHandler").apply {
            isAccessible = true
            set(client, mockCallBusyHandler)
        }

        client.fireEvent(event)

        verify(exactly = 0) { clientState.handleEvent(event) }
        unmockkAll()
    }

    @Test
    fun `fireEvent won't fully flow executes in when callBusyHandler returns true`() {
        val event = mockk<CallRingEvent>(relaxed = true)

        every { event.callCid } returns "video:999"
        val client = prepareClient()
        val clientState = mockk<ClientState>(relaxed = true)
        client::class.java.getDeclaredField("state").apply {
            isAccessible = true
            set(client, clientState)
        }
        val mockCallBusyHandler = mockk<CallBusyHandler>(relaxed = true)
        every { mockCallBusyHandler.shouldPropagateEvent(event) } returns true
        client::class.java.getDeclaredField("callBusyHandler").apply {
            isAccessible = true
            set(client, mockCallBusyHandler)
        }

        client.fireEvent(event)

        verify(exactly = 1) { clientState.handleEvent(event) }
        unmockkAll()
    }
}
