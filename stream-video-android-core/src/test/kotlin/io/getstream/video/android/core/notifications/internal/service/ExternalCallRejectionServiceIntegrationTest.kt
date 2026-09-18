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

package io.getstream.video.android.core.notifications.internal.service

import android.app.Application
import android.app.Notification
import android.os.Build
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.ExternalCallRejectionHandler
import io.getstream.video.android.core.ExternalCallRejectionSource
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_KEY
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.core.permission.android.StreamPermissionCheck
import io.getstream.video.android.model.StreamCallId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class ExternalCallRejectionServiceIntegrationTest {

    @MockK(relaxed = true)
    private lateinit var client: StreamVideoClient

    @MockK(relaxed = true)
    private lateinit var clientState: ClientState

    @MockK(relaxed = true)
    private lateinit var call: Call

    @MockK(relaxed = true)
    private lateinit var callState: CallState

    private lateinit var application: Application
    private lateinit var callScope: CoroutineScope

    private val callId = StreamCallId(type = "audio_call", id = "incoming-call")
    private val notificationIdFlow = MutableStateFlow<Int?>(null)
    private val ringingStateFlow = MutableStateFlow<RingingState>(RingingState.Incoming())
    private val connectionFlow = MutableStateFlow<RealtimeConnection>(RealtimeConnection.PreJoin)
    private val callEvents = MutableSharedFlow<VideoEvent>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        application = RuntimeEnvironment.getApplication()
        callScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher())

        val configRegistry = CallServiceConfigRegistry().apply {
            register(callId.type, DefaultCallConfigurations.audioCall)
        }
        val permissionCheck = mockk<StreamPermissionCheck> {
            every { checkAndroidPermissionsGroup(any(), any()) } returns Pair(true, emptySet())
        }

        every { client.context } returns application
        every { client.userId } returns "callee"
        every { client.state } returns clientState
        every { client.callServiceConfigRegistry } returns configRegistry
        every { client.permissionCheck } returns permissionCheck
        every { client.enableCallNotificationUpdates } returns false
        every { client.call(any(), any()) } returns call
        every { client.getSettingUpCallNotification(any(), any()) } returns null
        every {
            client.getRingingCallNotification(any(), any(), any(), any(), any())
        } returns Notification()

        every { clientState.callConfigRegistry } returns configRegistry
        every { clientState.activeCall } returns MutableStateFlow(null)
        every { clientState.ringingCall } returns MutableStateFlow(call)

        every { call.client } returns client
        every { call.type } returns callId.type
        every { call.id } returns callId.id
        every { call.cid } returns callId.cid
        every { call.scope } returns callScope
        every { call.state } returns callState
        every { call.events } returns callEvents

        every { callState.notificationIdFlow } returns notificationIdFlow
        every { callState.ringingState } returns ringingStateFlow
        every { callState.connection } returns connectionFlow

        coEvery { call.get() } returns Result.Success(mockk(relaxed = true))
        coEvery { call.reject(source = any(), reason = any()) } returns
            Result.Success(mockk(relaxed = true))

        StreamVideo.install(client)
    }

    @After
    fun tearDown() {
        StreamVideo.removeClient()
        callScope.cancel()
        unmockkAll()
    }

    @Test
    fun `rejecting incoming audio call stops service and unregisters receivers`() = runTest {
        val incomingIntent = ServiceIntentBuilder().buildStartIntent(
            application,
            StartServiceParam(
                callId = callId,
                trigger = TRIGGER_INCOMING_CALL,
                callServiceConfiguration = DefaultCallConfigurations.audioCall,
            ),
        )
        val serviceController = Robolectric.buildService(
            AudioCallService::class.java,
            incomingIntent,
        )
            .create()
            .startCommand(0, 1)
        val service = serviceController.get()
        val shadowApplication = shadowOf(application)
        val receiver = service.serviceStateController.state.value.toggleCameraBroadcastReceiver

        assertNotNull(receiver)
        assertTrue(service.serviceStateController.state.value.isReceiverRegistered)
        assertTrue(shadowApplication.registeredReceivers.any { it.broadcastReceiver === receiver })

        ShadowSystemClock.advanceBy(3, TimeUnit.SECONDS)
        ExternalCallRejectionHandler().onRejectCall(
            ExternalCallRejectionSource.NOTIFICATION,
            call,
            application,
        )

        val removeIncomingCallIntent = shadowApplication.nextStartedService
        assertNotNull(removeIncomingCallIntent)
        assertEquals(
            AudioCallService::class.java.name,
            removeIncomingCallIntent.component?.className,
        )
        assertEquals(
            TRIGGER_REMOVE_INCOMING_CALL,
            removeIncomingCallIntent.getStringExtra(TRIGGER_KEY),
        )

        service.onStartCommand(removeIncomingCallIntent, 0, 2)
        assertTrue(shadowOf(service).isStoppedBySelf)

        serviceController.destroy()

        assertFalse(service.serviceStateController.state.value.isReceiverRegistered)
        assertNull(service.serviceStateController.state.value.toggleCameraBroadcastReceiver)
        assertFalse(shadowApplication.registeredReceivers.any { it.broadcastReceiver === receiver })
    }
}
