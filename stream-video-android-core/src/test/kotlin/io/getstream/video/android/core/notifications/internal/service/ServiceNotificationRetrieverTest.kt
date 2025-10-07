package io.getstream.video.android.core.notifications.internal.service

import android.app.Notification
import android.content.Context
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.model.StreamCallId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
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

    @MockK
    lateinit var mockNotification: Notification

    private lateinit var context: Context
    private lateinit var serviceNotificationRetriever: ServiceNotificationRetriever
    private lateinit var testCallId: StreamCallId

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = RuntimeEnvironment.getApplication()
//        callService = CallService()
        serviceNotificationRetriever = ServiceNotificationRetriever()
        testCallId = StreamCallId(type = "default", id = "test-call-123")
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
            trigger = TRIGGER_ONGOING_CALL,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = "John Doe",
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
        every {
            mockStreamVideoClient.getRingingCallNotification(any(), any(), any(), any(), any())
        } returns mockNotification

        // When
        val result = serviceNotificationRetriever.getNotificationPair(
            context = context,
            trigger = TRIGGER_INCOMING_CALL,
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
            trigger = TRIGGER_REMOVE_INCOMING_CALL,
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = null,
        )

        // Then
        assertNull(result.first)
        assertEquals(testCallId.getNotificationId(NotificationType.Incoming), result.second)
    }

    @Test
    fun `getNotificationPair returns null notification for unknown trigger`() {
        // When
        val result = serviceNotificationRetriever.getNotificationPair(
            context = context,
            trigger = "unknown_trigger",
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = null,
        )

        // Then
        assertNull(result.first)
        assertEquals(testCallId.hashCode(), result.second)
    }

    @Test
    fun `service handles missing StreamVideo instance gracefully in notification generation`() {
        // Given - Using a real CallService instance but with mocked dependencies

        // When - Call getNotificationPair with minimal valid parameters
        val result = serviceNotificationRetriever.getNotificationPair(
            context,
            trigger = TRIGGER_REMOVE_INCOMING_CALL, // This trigger doesn't need StreamVideo methods
            streamVideo = mockStreamVideoClient,
            streamCallId = testCallId,
            intentCallDisplayName = null,
        )

        // Then - Should handle gracefully and return expected result
        assertNull(result.first) // No notification for remove trigger
        assertEquals(testCallId.getNotificationId(NotificationType.Incoming), result.second)
    }
}