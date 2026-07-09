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
import io.getstream.android.core.api.StreamClient
import io.getstream.android.core.api.model.connection.StreamConnectionState
import io.getstream.android.core.api.socket.listeners.StreamClientListener
import io.getstream.android.core.api.subscribe.StreamSubscription
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.android.video.generated.models.CallSessionStartedEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.call.CallBusyHandler
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.notifications.internal.StreamNotificationManager
import io.getstream.video.android.core.socket.common.token.TokenRepository
import io.getstream.video.android.core.sounds.RingingCallVibrationConfig
import io.getstream.video.android.core.sounds.Sounds
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamVideoClientTest {
    private lateinit var client: StreamVideoClient
    private lateinit var streamClient: StreamClient
    private lateinit var state: ClientState

    @Before
    fun setup() {
        val prepared = prepareClient()
        client = prepared.client
        streamClient = prepared.streamClient

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

    private data class ClientHarness(
        val client: StreamVideoClient,
        val streamClient: StreamClient,
        val tokenRepository: TokenRepository,
    )

    private fun prepareClient(user: User = mockk(relaxed = true)): ClientHarness {
        val context = mockk<Context>(relaxed = true)
        val lifecycle = mockk<Lifecycle>(relaxed = true)
        val coordinator = mockk<CoordinatorConnectionModule>(relaxed = true)
        val tokenRepo = mockk<TokenRepository>(relaxed = true)
        val notificationManager = mockk<StreamNotificationManager>(relaxed = true)
        val sounds = mockk<Sounds>(relaxed = true)
        val vibration = mockk<RingingCallVibrationConfig>(relaxed = true)
        val streamClientMock = mockk<StreamClient>(relaxed = true)
        val subscription = mockk<StreamSubscription>(relaxed = true)
        every { streamClientMock.subscribe(any()) } returns Result.success(subscription)
        every { streamClientMock.connectionState } returns MutableStateFlow(StreamConnectionState.Idle)

        val client = spyk(
            StreamVideoClient(
                context = context,
                user = user,
                apiKey = "apikey",
                token = "token",
                lifecycle = lifecycle,
                coordinatorConnectionModule = coordinator,
                streamClient = streamClientMock,
                tokenRepository = tokenRepo,
                streamNotificationManager = notificationManager,
                enableCallNotificationUpdates = false,
                sounds = sounds,
                vibrationConfig = vibration,
            ),
            recordPrivateCalls = true,
        )
        return ClientHarness(client, streamClientMock, tokenRepo)
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
        val rtcSession = mockk<RtcSession>(relaxed = true)
        val sessionFlow: MutableStateFlow<RtcSession?> = MutableStateFlow(rtcSession)
        val call = mockk<Call>(relaxed = true) {
            every { session } returns sessionFlow
        }

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
        val client = prepareClient().client
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
        val client = prepareClient().client
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

    @Test
    fun `init subscribes to streamClient`() {
        // The subscribe call is exercised inside prepareClient() via the ctor. Re-verify
        // by asserting `streamClient.subscribe(...)` was invoked exactly once when the
        // StreamVideoClient was constructed.
        verify(exactly = 1) { streamClient.subscribe(any()) }
    }

    @Test
    fun `connectAsync delegates to streamClient connect`() = runTest {
        coEvery { streamClient.connect() } returns Result.success(mockk(relaxed = true))

        val result = client.connectAsync().await()

        coVerify(exactly = 1) { streamClient.connect() }
        assertTrue(result is Success)
    }

    @Test
    fun `cleanup disconnects streamClient and does not touch tokenRepository`() {
        val harness = prepareClient()

        harness.client.cleanup()

        coVerify(exactly = 1) { harness.streamClient.disconnect() }
        // Invariant: the 401 path no longer routes through tokenRepository.updateToken
        // from inside cleanup(). (Cleanup itself never touched tokenRepository, but this
        // assertion guards against a future regression that reintroduces the coupling.)
        verify(exactly = 0) { harness.tokenRepository.updateToken(any()) }
    }

    @Test
    fun `connectAsync fails fast for anonymous users without touching streamClient`() = runTest {
        // iOS parity: anonymous users are REST-only; connect attempts fail before
        // reaching the network.
        val harness = prepareClient(
            user = User(id = "anon-1", type = UserType.Anonymous),
        )

        val result = harness.client.connectAsync().await()

        assertTrue(result is Failure)
        coVerify(exactly = 0) { harness.streamClient.connect() }
    }

    @Test
    fun `connectIfNotAlreadyConnected is a no-op for anonymous users`() = runTest {
        val harness = prepareClient(
            user = User(id = "anon-1", type = UserType.Anonymous),
        )

        harness.client.connectIfNotAlreadyConnected()

        coVerify(exactly = 0) { harness.streamClient.connect() }
    }

    @Test
    fun `connectIfNotAlreadyConnected connects when the socket is not connected`() = runTest {
        val harness = prepareClient(
            user = User(id = "auth-1", type = UserType.Authenticated),
        )

        harness.client.connectIfNotAlreadyConnected()

        coVerify(exactly = 1) { harness.streamClient.connect() }
    }

    @Test
    fun `streamClientListener forwards VideoEvents into the event pipeline`() {
        // The listener belongs to the underlying instance, not the spyk copy, so verify
        // through the shared subscriptions set instead of spy recording.
        val harness = prepareClient()
        val listener = slot<StreamClientListener>()
        verify { harness.streamClient.subscribe(capture(listener)) }
        val received = mutableListOf<VideoEvent>()
        harness.client.subscribe { received.add(it) }
        // CallSessionStartedEvent has no ClientState.handleEvent branch, so the dispatch
        // reaches client subscriptions without side effects.
        val event = mockk<CallSessionStartedEvent>(relaxed = true)

        listener.captured.onEvent(event)

        assertEquals(listOf<VideoEvent>(event), received)
    }

    @Test
    fun `streamClientListener ignores non-VideoEvent payloads`() {
        val harness = prepareClient()
        val listener = slot<StreamClientListener>()
        verify { harness.streamClient.subscribe(capture(listener)) }
        val received = mutableListOf<VideoEvent>()
        harness.client.subscribe { received.add(it) }

        listener.captured.onEvent("not-a-video-event")

        assertTrue(received.isEmpty())
    }

    @Test
    fun `streamClientListener onError does not throw`() {
        val harness = prepareClient()
        val listener = slot<StreamClientListener>()
        verify { harness.streamClient.subscribe(capture(listener)) }

        listener.captured.onError(RuntimeException("socket error"))
    }
}
