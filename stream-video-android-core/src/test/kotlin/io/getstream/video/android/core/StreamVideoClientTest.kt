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
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.android.video.generated.models.CallSessionStartedEvent
import io.getstream.android.video.generated.models.CreateGuestResponse
import io.getstream.android.video.generated.models.UserResponse
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.threeten.bp.OffsetDateTime
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

    private fun prepareClient(
        user: User = User(id = "user-1", type = UserType.Authenticated),
    ): ClientHarness {
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
                initialUser = user,
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
                analytics = mockk(relaxed = true),
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

    // Regression: a guest user's createGuest call runs on a background `guestUserJob`.
    // If an authenticated API request (e.g. createDevice) fires before that job completes,
    // it leaves the SDK with no Authorization header and stream-auth-type "anonymous",
    // so the backend silently associates the request with the wrong identity.
    // apiCall must block until the guest setup is done. AND-1202.
    @Test
    fun `apiCall waits for guestUserJob to complete before invoking the block`() = runTest {
        val guestJob = CompletableDeferred<Unit>()
        client::class.java.getDeclaredField("guestUserJob").apply {
            isAccessible = true
            set(client, guestJob)
        }

        var blockRan = false
        val apiCallJob = launch {
            client.apiCall {
                blockRan = true
                "ok"
            }
        }

        runCurrent()
        assertFalse(blockRan, "apiCall must not run while guestUserJob is still pending")

        guestJob.complete(Unit)
        apiCallJob.join()
        assertTrue(blockRan, "apiCall must run once guestUserJob completes")
    }

    // The guard inside apiCall must skip the await when apiCall is itself running inside
    // the guest setup's coroutine — otherwise createGuestUser, which goes through apiCall,
    // would await its own enclosing job and deadlock.
    @Test
    fun `apiCall does not deadlock when invoked from within guestUserJob`() = runTest {
        var blockRan = false
        val guestJob: Deferred<Unit> = async(start = CoroutineStart.LAZY) {
            client.apiCall {
                blockRan = true
                "ok"
            }
            Unit
        }
        client::class.java.getDeclaredField("guestUserJob").apply {
            isAccessible = true
            set(client, guestJob)
        }

        guestJob.await()
        assertTrue(blockRan, "apiCall inside the guest setup must run without deadlocking")
    }

    // If setupGuestUser fails the SDK has no valid guest session, so subsequent API
    // calls must NOT proceed under anonymous/empty-token state. The bare await on
    // guestUserJob lets the failure propagate; safeSuspendingCallWithResult then
    // turns it into Result.Failure rather than silently re-issuing as anonymous.
    @Test
    fun `apiCall surfaces guestUserJob failure instead of swallowing it`() = runTest {
        val failed = CompletableDeferred<Unit>().apply {
            completeExceptionally(IllegalStateException("Failed to create guest user"))
        }
        client::class.java.getDeclaredField("guestUserJob").apply {
            isAccessible = true
            set(client, failed)
        }

        var blockRan = false
        val result = client.apiCall {
            blockRan = true
            "should-not-run"
        }

        assertFalse(blockRan, "apiCall must not invoke the request block when guest setup failed")
        assertTrue(
            result is io.getstream.result.Result.Failure,
            "expected Result.Failure, got $result",
        )
    }

    // Regression: StreamNotificationManager.createDevice() calls api.createDevice() directly
    // instead of going through apiCall {}, so it doesn't inherit the guestUserJob await guard.
    // registerPushDevice() must await guestUserJob itself — otherwise the push generator can
    // kick off and fire createDevice() before the coordinator's auth headers flip from
    // anonymous to JWT. AND-1202.
    @Test
    fun `registerPushDevice waits for guestUserJob to complete before delegating`() = runTest {
        val notificationManager = client.streamNotificationManager
        val guestJob = CompletableDeferred<Unit>()
        client::class.java.getDeclaredField("guestUserJob").apply {
            isAccessible = true
            set(client, guestJob)
        }

        val registerJob = launch { client.registerPushDevice() }

        runCurrent()
        coVerify(exactly = 0) { notificationManager.registerPushDevice() }

        guestJob.complete(Unit)
        registerJob.join()
        coVerify(exactly = 1) { notificationManager.registerPushDevice() }
    }

    // userId used to be captured at construction. After AND-1202 it reads through the
    // UserRepository so the server-issued guest identity (adopted on createGuest success)
    // is reflected everywhere the SDK reads client.userId.
    @Test
    fun `userId tracks the current user reference`() {
        client.userRepository.setUser(User(id = "server_issued_guest", type = UserType.Guest))
        assertEquals("server_issued_guest", client.userId)

        client.userRepository.setUser(User(id = "another_user", type = UserType.Authenticated))
        assertEquals("another_user", client.userId)
    }

    // setupGuestUser must adopt response.user from createGuest so the SDK's local user.id
    // matches the JWT's user_id claim. JS does this via connectUser(response.user, ...).
    // Without it the socket auth payload and the device-registration JWT could disagree.
    @Test
    fun `setupGuestUser adopts the server-issued user identity on success`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val lifecycle = mockk<Lifecycle>(relaxed = true)
        val coordinator = mockk<CoordinatorConnectionModule>(relaxed = true)
        val api = mockk<ProductvideoApi>(relaxed = true)
        every { coordinator.api } returns api
        coEvery { api.createGuest(any()) } returns CreateGuestResponse(
            accessToken = "guest-jwt",
            duration = "1ms",
            user = UserResponse(
                createdAt = OffsetDateTime.MIN,
                id = "server_normalized_id",
                language = "en",
                role = "guest",
                updatedAt = OffsetDateTime.MIN,
                name = "Guest",
            ),
        )
        val streamClientMock = mockk<StreamClient>(relaxed = true)
        every {
            streamClientMock.subscribe(any())
        } returns Result.success(mockk<StreamSubscription>(relaxed = true))
        every { streamClientMock.connectionState } returns MutableStateFlow(StreamConnectionState.Idle)
        val client = StreamVideoClient(
            context = context,
            initialUser = User(id = "local_input_id", type = UserType.Guest),
            apiKey = "apikey",
            token = "",
            lifecycle = lifecycle,
            coordinatorConnectionModule = coordinator,
            streamClient = streamClientMock,
            tokenRepository = mockk(relaxed = true),
            streamNotificationManager = mockk(relaxed = true),
            enableCallNotificationUpdates = false,
            sounds = mockk(relaxed = true),
            vibrationConfig = mockk(relaxed = true),
            analytics = mockk(relaxed = true),
        )

        client.setupGuestUser(client.user)
        client.guestUserJob?.await()

        assertEquals("server_normalized_id", client.user.id)
        assertEquals("server_normalized_id", client.userId)
        assertEquals(UserType.Guest, client.user.type)
        // state.user is sourced from the UserRepository, so it should reflect the
        // adopted identity automatically — no separate mirror to keep in sync.
        assertEquals("server_normalized_id", client.state.user.value?.id)
        assertEquals(UserType.Guest, client.state.user.value?.type)
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
    fun `streamClientListener routes connection state into ClientState`() {
        val harness = prepareClient()
        val listener = slot<StreamClientListener>()
        verify { harness.streamClient.subscribe(capture(listener)) }
        val reported = StreamConnectionState.Connecting.Opening("user-1")

        listener.captured.onState(reported)

        assertEquals(reported, harness.client.state.connection.value)
    }

    @Test
    fun `streamClientListener onError does not throw`() {
        val harness = prepareClient()
        val listener = slot<StreamClientListener>()
        verify { harness.streamClient.subscribe(capture(listener)) }

        listener.captured.onError(RuntimeException("socket error"))
    }
}
