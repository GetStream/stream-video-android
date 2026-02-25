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

import android.app.Notification
import android.content.Context
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.model.StreamCallId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ServiceNotificationRetrieverTest {

    @MockK
    private lateinit var mockStreamVideoClient: StreamVideoClient

    private lateinit var state: ClientState

    @MockK
    lateinit var mockNotification: Notification

    private lateinit var context: Context
    private lateinit var serviceNotificationRetriever: ServiceNotificationRetriever
    private lateinit var testCallId: StreamCallId
    private lateinit var call: Call

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        state = mockk(relaxed = true)
        context = RuntimeEnvironment.getApplication()
        serviceNotificationRetriever = ServiceNotificationRetriever()
        testCallId = StreamCallId(type = "default", id = "test-call-123")
        every { mockStreamVideoClient.scope } returns TestScope()

        call = Call(mockStreamVideoClient, "default", "test-call-123", mockk())
        every { mockStreamVideoClient.call(testCallId.type, testCallId.id) } returns call
        mockStreamVideoClient::class.java.getDeclaredField("state").apply {
            isAccessible = true
            set(mockStreamVideoClient, state)
        }
        every { mockStreamVideoClient.state } returns state
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Test notification generation logic
    @Test
    fun `getNotificationPair returns correct data for ongoing call`() {
        // Given
        every {
            mockStreamVideoClient.getOngoingCallNotification(any(), any(), payload = any())
        } returns mockNotification

        // When
        val result = serviceNotificationRetriever.getNotificationPair(
            context = context,
            trigger = CallService.Companion.Trigger.OnGoingCall,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = "John Doe",
        )

        // Then
        assertEquals(mockNotification, result.first)
        assertEquals(testCallId.getNotificationId(NotificationType.Ongoing), result.second)
    }

    @Test
    fun `getNotificationPair returns correct data for incoming call`() {
        // Given
        val mockState = mockk<io.getstream.video.android.core.ClientState>()
        every { mockStreamVideoClient.state } returns mockState
        every { mockState.activeCall } returns mockk {
            every { value } returns null
        }
        every {
            mockStreamVideoClient.getRingingCallNotification(any(), any(), any(), any(), any())
        } returns mockNotification

        // When
        val result = serviceNotificationRetriever.getNotificationPair(
            context = context,
            trigger = CallService.Companion.Trigger.IncomingCall,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = "John Doe",
        )

        // Then
        assertEquals(mockNotification, result.first)
        assertEquals(testCallId.getNotificationId(NotificationType.Incoming), result.second)
    }

    @Test
    fun `getNotificationPair returns null notification for remove incoming call`() {
        // When
        val result = serviceNotificationRetriever.getNotificationPair(
            context = context,
            trigger = CallService.Companion.Trigger.RemoveIncomingCall,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = null,
        )

        // Then
        assertNull(result.first)
        assertEquals(testCallId.getNotificationId(NotificationType.Incoming), result.second)
    }

    @Test
    fun `getNotificationPair returns correct notification for outgoing call`() {
        val mockState = mockk<io.getstream.video.android.core.ClientState>()
        every { mockStreamVideoClient.state } returns mockState
        every { mockState.activeCall } returns mockk {
            every { value } returns null
        }
        every {
            mockStreamVideoClient.getRingingCallNotification(any(), any(), any(), any(), any())
        } returns mockNotification

        // When
        val result = serviceNotificationRetriever.getNotificationPair(
            context = context,
            trigger = CallService.Companion.Trigger.OutgoingCall,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = "John Doe",
        )

        // Then
        assertEquals(mockNotification, result.first)
        assertEquals(testCallId.getNotificationId(NotificationType.Outgoing), result.second)
    }

    @Test
    fun `getNotificationPair returns null notification for unknown trigger`() {
        // When
        val result = serviceNotificationRetriever.getNotificationPair(
            context = context,
            trigger = CallService.Companion.Trigger.None,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = null,
        )

        // Then
        assertNull(result.first)
        assertEquals(testCallId.hashCode(), result.second)
    }
}
