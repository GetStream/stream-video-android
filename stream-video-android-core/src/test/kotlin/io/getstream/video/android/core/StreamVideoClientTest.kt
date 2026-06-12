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
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.android.video.generated.models.CallSessionStartedEvent
import io.getstream.android.video.generated.models.CreateGuestResponse
import io.getstream.android.video.generated.models.UserResponse
import io.getstream.android.video.generated.models.VideoEvent
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
                initialUser = mockk(relaxed = true),
                apiKey = "apikey",
                token = "token",
                lifecycle = lifecycle,
                coordinatorConnectionModule = coordinator,
                tokenRepository = tokenRepo,
                streamNotificationManager = notificationManager,
                enableCallNotificationUpdates = false,
                sounds = sounds,
                vibrationConfig = vibration,
                clientEventReporter = mockk(relaxed = true),
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
        val client = StreamVideoClient(
            context = context,
            initialUser = User(id = "local_input_id", type = UserType.Guest),
            apiKey = "apikey",
            token = "",
            lifecycle = lifecycle,
            coordinatorConnectionModule = coordinator,
            tokenRepository = mockk(relaxed = true),
            streamNotificationManager = mockk(relaxed = true),
            enableCallNotificationUpdates = false,
            sounds = mockk(relaxed = true),
            vibrationConfig = mockk(relaxed = true),
            clientEventReporter = mockk(relaxed = true),
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
}
