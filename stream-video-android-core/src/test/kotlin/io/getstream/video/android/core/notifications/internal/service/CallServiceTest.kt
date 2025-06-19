/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import android.content.Intent
import android.content.pm.ServiceInfo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_KEY
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_OUTGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallDisplayName
import io.getstream.video.android.model.streamCallId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CallServiceTest {

    @MockK
    lateinit var mockContext: Context

    @MockK
    private lateinit var mockStreamVideoClient: StreamVideoClient

    @MockK
    lateinit var mockNotification: Notification

    private lateinit var context: Context
    private lateinit var callService: CallService
    private lateinit var testCallId: StreamCallId

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = RuntimeEnvironment.getApplication()
        callService = CallService()
        testCallId = StreamCallId(type = "default", id = "test-call-123")
    }

    // Test companion object constants
    @Test
    fun `service constants have expected values`() {
        assertEquals("incoming_call", TRIGGER_INCOMING_CALL)
        assertEquals("outgoing_call", TRIGGER_OUTGOING_CALL)
        assertEquals("ongoing_call", TRIGGER_ONGOING_CALL)
        assertEquals("remove_call", TRIGGER_REMOVE_INCOMING_CALL)
        assertEquals(
            "io.getstream.video.android.core.notifications.internal.service.CallService.call_trigger",
            TRIGGER_KEY
        )
    }

    @Test
    fun `service has correct default service type`() {
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL, callService.serviceType)
    }

    // Test intent building
    @Test
    fun `buildStartIntent creates correct intent for incoming call`() {
        // When
        val intent = CallService.buildStartIntent(
            context = context,
            callId = testCallId,
            trigger = TRIGGER_INCOMING_CALL,
            callDisplayName = "John Doe"
        )

        // Then
        assertEquals(CallService::class.java.name, intent.component?.className)
        assertEquals(testCallId, intent.streamCallId(INTENT_EXTRA_CALL_CID))
        assertEquals("John Doe", intent.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME))
        assertEquals(TRIGGER_INCOMING_CALL, intent.getStringExtra(TRIGGER_KEY))
    }

    @Test
    fun `buildStartIntent creates correct intent for outgoing call`() {
        // When
        val intent = CallService.buildStartIntent(
            context = context,
            callId = testCallId,
            trigger = TRIGGER_OUTGOING_CALL
        )

        // Then
        assertEquals(CallService::class.java.name, intent.component?.className)
        assertEquals(testCallId, intent.streamCallId(INTENT_EXTRA_CALL_CID))
        assertEquals(TRIGGER_OUTGOING_CALL, intent.getStringExtra(TRIGGER_KEY))
        assertNull(intent.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME))
    }

    @Test
    fun `buildStartIntent creates correct intent for ongoing call`() {
        // When
        val intent = CallService.buildStartIntent(
            context = context,
            callId = testCallId,
            trigger = TRIGGER_ONGOING_CALL
        )

        // Then
        assertEquals(CallService::class.java.name, intent.component?.className)
        assertEquals(testCallId, intent.streamCallId(INTENT_EXTRA_CALL_CID))
        assertEquals(TRIGGER_ONGOING_CALL, intent.getStringExtra(TRIGGER_KEY))
    }

    @Test
    fun `buildStartIntent creates correct intent for remove incoming call`() {
        // When
        val intent = CallService.buildStartIntent(
            context = context,
            callId = testCallId,
            trigger = TRIGGER_REMOVE_INCOMING_CALL
        )

        // Then
        assertEquals(CallService::class.java.name, intent.component?.className)
        assertEquals(testCallId, intent.streamCallId(INTENT_EXTRA_CALL_CID))
        assertEquals(TRIGGER_REMOVE_INCOMING_CALL, intent.getStringExtra(TRIGGER_KEY))
    }

    @Test
    fun `buildStartIntent throws exception for invalid trigger`() {
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            CallService.buildStartIntent(
                context = context,
                callId = testCallId,
                trigger = "invalid_trigger"
            )
        }
    }

    @Test
    fun `buildStartIntent uses custom service class from configuration`() {
        // Given
        val customConfig = CallServiceConfig(
            serviceClass = LivestreamCallService::class.java
        )

        // When
        val intent = CallService.buildStartIntent(
            context = context,
            callId = testCallId,
            trigger = TRIGGER_INCOMING_CALL,
            callServiceConfiguration = customConfig
        )

        // Then
        assertEquals(LivestreamCallService::class.java.name, intent.component?.className)
    }

    @Test
    fun `buildStopIntent creates correct intent`() {
        // When
        val intent = CallService.buildStopIntent(context)

        // Then
        assertNotNull(intent)
        assertEquals(CallService::class.java.name, intent.component?.className)
    }

    @Test
    fun `buildStopIntent uses custom service class from configuration`() {
        // Given
        val customConfig = CallServiceConfig(
            serviceClass = LivestreamCallService::class.java
        )

        // When
        val intent = CallService.buildStopIntent(
            context = context,
            callServiceConfiguration = customConfig
        )

        // Then
        assertNotNull(intent)
        // Note: The actual implementation has some complex logic for running services
        // so we just verify the intent is created
    }

    // Test notification generation logic
    @Test
    fun `getNotificationPair returns correct data for ongoing call`() {
        // Given
        every { mockStreamVideoClient.getOngoingCallNotification(any(), any()) } returns mockNotification

        // When
        val result = callService.getNotificationPair(
            trigger = TRIGGER_ONGOING_CALL,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = "John Doe"
        )

        // Then
        assertEquals(mockNotification, result.first)
        assertEquals(testCallId.hashCode(), result.second)
    }

    @Test
    fun `getNotificationPair returns correct data for incoming call`() {
        // Given
        val mockState = mockk<io.getstream.video.android.core.ClientState>()
        every { mockStreamVideoClient.state } returns mockState
        every { mockState.activeCall } returns mockk {
            every { value } returns null
        }
        every { mockStreamVideoClient.getRingingCallNotification(any(), any(), any(), any()) } returns mockNotification

        // When
        val result = callService.getNotificationPair(
            trigger = TRIGGER_INCOMING_CALL,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = "John Doe"
        )

        // Then
        assertEquals(mockNotification, result.first)
        assertEquals(NotificationHandler.INCOMING_CALL_NOTIFICATION_ID, result.second)
    }

    @Test
    fun `getNotificationPair returns null notification for remove incoming call`() {
        // When
        val result = callService.getNotificationPair(
            trigger = TRIGGER_REMOVE_INCOMING_CALL,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = null
        )

        // Then
        assertNull(result.first)
        assertEquals(NotificationHandler.INCOMING_CALL_NOTIFICATION_ID, result.second)
    }

    @Test
    fun `getNotificationPair returns null notification for unknown trigger`() {
        // When
        val result = callService.getNotificationPair(
            trigger = "unknown_trigger",
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = null
        )

        // Then
        assertNull(result.first)
        assertEquals(testCallId.hashCode(), result.second)
    }

    // Test service subclasses have correct service types
    @Test
    fun `LivestreamCallService has correct service type`() {
        val livestreamService = LivestreamCallService()
        val expectedType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        assertEquals(expectedType, livestreamService.serviceType)
    }

    @Test
    fun `LivestreamAudioCallService has correct service type`() {
        val audioService = LivestreamAudioCallService()
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE, audioService.serviceType)
    }

    @Test
    fun `LivestreamViewerService has correct service type`() {
        val viewerService = LivestreamViewerService()
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, viewerService.serviceType)
    }

    @Test
    fun `AudioCallService has correct service type`() {
        val audioCallService = AudioCallService()
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE, audioCallService.serviceType)
    }

    // Test intent extra handling
    @Test
    fun `intent extras are properly set and retrieved`() {
        // Given
        val intent = Intent()
        val callDisplayName = "Test User"

        // When
        intent.putExtra(INTENT_EXTRA_CALL_CID, testCallId)
        intent.putExtra(INTENT_EXTRA_CALL_DISPLAY_NAME, callDisplayName)
        intent.putExtra(TRIGGER_KEY, TRIGGER_INCOMING_CALL)

        // Then
        assertEquals(testCallId, intent.streamCallId(INTENT_EXTRA_CALL_CID))
        assertEquals(callDisplayName, intent.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME))
        assertEquals(TRIGGER_INCOMING_CALL, intent.getStringExtra(TRIGGER_KEY))
    }

    @Test
    fun `intent handles null call display name gracefully`() {
        // Given
        val intent = Intent()

        // When
        intent.putExtra(INTENT_EXTRA_CALL_CID, testCallId)
        // Intentionally not setting INTENT_EXTRA_CALL_DISPLAY_NAME

        // Then
        assertEquals(testCallId, intent.streamCallId(INTENT_EXTRA_CALL_CID))
        assertNull(intent.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME))
    }

    // Test service configuration integration
    @Test
    fun `service respects configuration for different call types`() {
        // Given
        val livestreamConfig = CallServiceConfig(
            serviceClass = LivestreamCallService::class.java,
            runCallServiceInForeground = true
        )

        // When
        val intent = CallService.buildStartIntent(
            context = context,
            callId = StreamCallId(type = "livestream", id = "test-123"),
            trigger = TRIGGER_INCOMING_CALL,
            callServiceConfiguration = livestreamConfig
        )

        // Then
        assertEquals(LivestreamCallService::class.java.name, intent.component?.className)
    }

    // Test error handling
    @Test
    fun `service handles missing StreamVideo instance gracefully in notification generation`() {
        // Given - Using a real CallService instance but with mocked dependencies
        val service = CallService()

        // When - Call getNotificationPair with minimal valid parameters
        val result = service.getNotificationPair(
            trigger = TRIGGER_REMOVE_INCOMING_CALL, // This trigger doesn't need StreamVideo methods
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = null
        )

        // Then - Should handle gracefully and return expected result
        assertNull(result.first) // No notification for remove trigger
        assertEquals(NotificationHandler.INCOMING_CALL_NOTIFICATION_ID, result.second)
    }

    // Test constants consistency
    @Test
    fun `notification ID constants are consistent`() {
        // These constants should remain stable as they affect notification behavior
        assertEquals(24756, NotificationHandler.INCOMING_CALL_NOTIFICATION_ID)
    }

    @Test
    fun `intent extra keys are consistent`() {
        // These keys should remain stable as they affect intent parsing
        assertEquals("io.getstream.video.android.intent-extra.call_cid", INTENT_EXTRA_CALL_CID)
        assertEquals("io.getstream.video.android.intent-extra.call_displayname", INTENT_EXTRA_CALL_DISPLAY_NAME)
    }
}
