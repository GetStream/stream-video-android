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

package io.getstream.video.android.core.notifications.handlers

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.notifications.StreamIntentResolver
import io.getstream.video.android.model.StreamCallId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Skeleton test for StreamDefaultNotificationHandler.
 * Mocks all constructor dependencies to make it testable.
 */
@RunWith(RobolectricTestRunner::class)
class StreamDefaultNotificationHandlerTest : IntegrationTestBase() {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockNotificationManager: NotificationManagerCompat

    @MockK
    lateinit var mockNotificationPermissionHandler: NotificationPermissionHandler

    @MockK
    lateinit var mockIntentResolver: StreamIntentResolver

    @MockK
    lateinit var mockInitialInterceptor: StreamNotificationBuilderInterceptors

    @MockK
    lateinit var mockUpdateInterceptor: StreamNotificationUpdateInterceptors

    @MockK
    lateinit var mockCall: Call

    @MockK
    lateinit var mockCallState: CallState

    @MockK
    lateinit var mockPendingIntent: PendingIntent

    private lateinit var testCallId: StreamCallId
    private lateinit var testHandler: StreamDefaultNotificationHandler

    @Before
    fun setUp2() {
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)

        testCallId = StreamCallId(type = "default", id = "test-call-123")

        // Mock NotificationCompat.Builder to avoid Android framework issues
        mockkConstructor(NotificationCompat.Builder::class)
        val mockNotificationBuilder = mockk<NotificationCompat.Builder>(relaxed = true)
        val mockNotification = mockk<Notification>(relaxed = true)

        every { anyConstructed<NotificationCompat.Builder>().build() } returns mockNotification
        every {
            anyConstructed<NotificationCompat.Builder>().setContentTitle(any())
        } returns mockNotificationBuilder
        every {
            anyConstructed<NotificationCompat.Builder>().setContentText(any())
        } returns mockNotificationBuilder
        every {
            anyConstructed<NotificationCompat.Builder>().setSmallIcon(any<Int>())
        } returns mockNotificationBuilder
        every {
            anyConstructed<NotificationCompat.Builder>().setContentIntent(any())
        } returns mockNotificationBuilder
        every {
            anyConstructed<NotificationCompat.Builder>().addAction(
                any(),
                any(),
                any(),
            )
        } returns mockNotificationBuilder
        every {
            anyConstructed<NotificationCompat.Builder>().setCategory(any())
        } returns mockNotificationBuilder
        every {
            anyConstructed<NotificationCompat.Builder>().setPriority(any())
        } returns mockNotificationBuilder
        every {
            anyConstructed<NotificationCompat.Builder>().setAutoCancel(any())
        } returns mockNotificationBuilder
        every {
            anyConstructed<NotificationCompat.Builder>().setOngoing(any())
        } returns mockNotificationBuilder
        every {
            anyConstructed<NotificationCompat.Builder>().setChannelId(any())
        } returns mockNotificationBuilder
        every { anyConstructed<NotificationCompat.Builder>().build() } returns mockk(relaxed = true)

        // Basic mocks
        every { mockApplication.applicationContext } returns mockApplication
        every { mockApplication.getString(any()) } returns "Test String"
        every { mockApplication.getString(any(), any()) } returns "Test String with param"
        every { mockApplication.applicationInfo } returns mockk(relaxed = true)

        // Mock call state
        every { mockCall.cid } returns "default:test-call-123"
        every { mockCall.state } returns mockCallState
        every { mockCallState.ringingState } returns MutableStateFlow(RingingState.Incoming())
        every { mockCallState.members } returns MutableStateFlow(emptyList())
        every { mockCallState.remoteParticipants } returns MutableStateFlow(emptyList())

        mockkStatic(NotificationCompat.Builder::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onRingingCall shows incoming call notification with comprehensive verification`() {
        // Given
        val callDisplayName = "John Doe"
        val mockkApp = mockk<Application>(relaxed = true)
        val mockIncomingChannelInfo = mockk<StreamNotificationChannelInfo>(relaxed = true)

        // Mock intent resolver calls
        every { mockIntentResolver.searchIncomingCallPendingIntent(testCallId) } returns mockPendingIntent
        every { mockIntentResolver.searchAcceptCallPendingIntent(testCallId) } returns mockPendingIntent
        every { mockIntentResolver.searchRejectCallPendingIntent(testCallId) } returns mockPendingIntent

        // Mock interceptor call
        every {
            mockInitialInterceptor.onBuildIncomingCallNotification(
                any(), any(), any(), any(), any(), any(),
            )
        } returns mockk<NotificationCompat.Builder>(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
        )

        // When
        testHandler.onRingingCall(testCallId, callDisplayName)

        // Then - Verify all intent resolver calls
        verify { mockIntentResolver.searchIncomingCallPendingIntent(testCallId) }
        verify { mockIntentResolver.searchAcceptCallPendingIntent(testCallId) }
        verify { mockIntentResolver.searchRejectCallPendingIntent(testCallId) }

        // Verify interceptor is called with correct parameters
        verify {
            mockInitialInterceptor.onBuildIncomingCallNotification(
                any(), // context
                mockPendingIntent, // content intent
                mockPendingIntent, // accept intent
                mockPendingIntent, // reject intent
                callDisplayName, // caller name
                true, // with actions
            )
        }

        // Verify notification manager is called to show notification
        verify {
            mockNotificationManager.createNotificationChannel(
                any<NotificationChannelCompat>(),
            )
        }
    }

    @Test
    fun `onMissedCall creates and shows missed call notification`() {
        // Given
        val callDisplayName = "Jane Smith"
        val mockkApp = mockk<Application>(relaxed = true)
        val mockMissedChannelInfo = mockk<StreamNotificationChannelInfo>(relaxed = true)

        // Mock intent resolver calls
        every {
            mockIntentResolver.searchMissedCallPendingIntent(
                testCallId,
                any(),
            )
        } returns mockPendingIntent

        // Mock interceptor call
        every {
            mockInitialInterceptor.onBuildMissedCallNotification(any(), any())
        } returns mockk<NotificationCompat.Builder>(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        testHandler.onMissedCall(testCallId, callDisplayName)

        // Then - Verify intent resolver call with correct notification ID
        verify {
            mockIntentResolver.searchMissedCallPendingIntent(
                testCallId,
                testCallId.hashCode(),
            )
        }

        // Verify interceptor is called with correct parameters
        verify {
            mockInitialInterceptor.onBuildMissedCallNotification(
                any(), // context
                callDisplayName, // caller name
            )
        }

        // Verify notification manager is called to show notification
        verify { mockNotificationManager.notify(testCallId.hashCode(), any()) }
    }

    @Test
    fun `onMissedCall falls back to default intent when specific intent not found`() {
        // Given
        val callDisplayName = "Bob Wilson"
        val mockkApp = mockk<Application>(relaxed = true)
        val mockMissedChannelInfo = mockk<StreamNotificationChannelInfo>(relaxed = true)

        // Mock intent resolver calls - return null for specific intent, then default
        every { mockIntentResolver.searchMissedCallPendingIntent(testCallId, any()) } returns null
        every { mockIntentResolver.getDefaultPendingIntent() } returns mockPendingIntent

        // Mock notification channels
        every { mockMissedChannelInfo.id } returns "missed_call_channel"

        // Mock interceptor call
        every {
            mockInitialInterceptor.onBuildMissedCallNotification(any(), any())
        } returns mockk<NotificationCompat.Builder>(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        testHandler.onMissedCall(testCallId, callDisplayName)

        // Then - Verify fallback to default intent
        verify {
            mockIntentResolver.searchMissedCallPendingIntent(
                testCallId,
                testCallId.hashCode(),
            )
        }
        verify { mockIntentResolver.getDefaultPendingIntent() }

        // Verify interceptor is called with default intent
        verify {
            mockInitialInterceptor.onBuildMissedCallNotification(
                any(), // context
                callDisplayName, // caller name
            )
        }

        // Verify notification manager is called
        verify { mockNotificationManager.notify(testCallId.hashCode(), any()) }
    }

    @Test
    fun `onNotification creates general notification`() {
        // Given
        val callDisplayName = "Alice Johnson"
        val mockkApp = mockk<Application>(relaxed = true)

        // Mock intent resolver calls
        every {
            mockIntentResolver.searchNotificationCallPendingIntent(
                testCallId,
                any(),
            )
        } returns mockPendingIntent

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        testHandler.onNotification(testCallId, callDisplayName)

        // Then - Verify intent resolver call with correct notification ID
        verify {
            mockIntentResolver.searchNotificationCallPendingIntent(
                testCallId,
                testCallId.hashCode(),
            )
        }

        // Verify notification manager is called to show notification
        verify { mockNotificationManager.notify(testCallId.hashCode(), any()) }
    }

    @Test
    fun `onLiveCall creates live call notification`() {
        // Given
        val callDisplayName = "Charlie Brown"
        val mockkApp = mockk<Application>(relaxed = true)

        // Mock intent resolver calls
        every {
            mockIntentResolver.searchLiveCallPendingIntent(
                testCallId,
                any(),
            )
        } returns mockPendingIntent

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        testHandler.onLiveCall(testCallId, callDisplayName)

        // Then - Verify intent resolver call with correct notification ID
        verify { mockIntentResolver.searchLiveCallPendingIntent(testCallId, testCallId.hashCode()) }

        // Verify notification manager is called to show notification
        verify { mockNotificationManager.notify(testCallId.hashCode(), any()) }
    }

    // ========== onUpdate Tests (Suspending Methods) ==========

    @Test
    fun `onCallNotificationUpdate handles incoming ringing state`() = runTest {
        // Given
        val mockkApp = mockk<Application>(relaxed = true)
        every { mockCallState.ringingState } returns MutableStateFlow(RingingState.Incoming())
        every { mockIntentResolver.searchIncomingCallPendingIntent(any()) } returns mockPendingIntent
        every { mockIntentResolver.searchAcceptCallPendingIntent(any()) } returns mockPendingIntent
        every { mockIntentResolver.searchRejectCallPendingIntent(any()) } returns mockPendingIntent
        every {
            mockInitialInterceptor.onBuildIncomingCallNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)
        coEvery {
            mockUpdateInterceptor.onUpdateIncomingCallNotification(
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.onCallNotificationUpdate(mockCall)

        // Then
        coVerify { mockUpdateInterceptor.onUpdateIncomingCallNotification(any(), any(), mockCall) }
    }

    @Test
    fun `onCallNotificationUpdate handles Active state with participants`() = runTest {
        // Given
        val mockkApp = mockk<Application>(relaxed = true)
        every { mockCallState.ringingState } returns MutableStateFlow(RingingState.Active)
        every { mockCallState.remoteParticipants } returns MutableStateFlow(
            listOf(
                mockk(),
                mockk(),
            ),
        ) // 2 participants
        every { mockIntentResolver.searchOutgoingCallPendingIntent(any()) } returns mockPendingIntent
        every { mockIntentResolver.searchEndCallPendingIntent(any()) } returns mockPendingIntent
        every {
            mockIntentResolver.searchOngoingCallPendingIntent(
                any(),
                any(),
            )
        } returns mockPendingIntent
        every { mockIntentResolver.searchRejectCallPendingIntent(any()) } returns mockPendingIntent
        every {
            mockInitialInterceptor.onBuildOngoingCallNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)
        coEvery {
            mockUpdateInterceptor.onUpdateOngoingCallNotification(
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.onCallNotificationUpdate(mockCall)

        // Then
        coVerify { mockUpdateInterceptor.onUpdateOngoingCallNotification(any(), any(), any()) }
    }

    @Test
    fun `onCallNotificationUpdate returns null for idle state with no participants`() = runTest {
        // Given
        val mockkApp = mockk<Application>(relaxed = true)
        every { mockCallState.ringingState } returns MutableStateFlow(RingingState.Idle)
        every { mockCallState.remoteParticipants } returns MutableStateFlow(emptyList()) // No participants
        every { mockCallState.members } returns MutableStateFlow(emptyList())

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.onCallNotificationUpdate(mockCall)

        // Then
        assertNull(result)
    }

    @Test
    fun `updateIncomingCallNotification calls interceptor with correct parameters`() = runTest {
        // Given
        val callDisplayName = "Henry Wilson"
        val mockkApp = mockk<Application>(relaxed = true)
        every { mockIntentResolver.searchIncomingCallPendingIntent(any()) } returns mockPendingIntent
        every { mockIntentResolver.searchAcceptCallPendingIntent(any()) } returns mockPendingIntent
        every { mockIntentResolver.searchRejectCallPendingIntent(any()) } returns mockPendingIntent
        coEvery {
            mockUpdateInterceptor.onUpdateIncomingCallNotification(
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)
        every {
            mockInitialInterceptor.onBuildIncomingCallNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.updateIncomingCallNotification(mockCall, callDisplayName)

        // Then
        coVerify {
            mockUpdateInterceptor.onUpdateIncomingCallNotification(
                any(),
                callDisplayName,
                mockCall,
            )
        }
    }

    @Test
    fun `updateOngoingCallNotification calls interceptor with correct parameters`() = runTest {
        // Given
        val callDisplayName = "Ivy Chen"
        val mockkApp = mockk<Application>(relaxed = true)
        every { mockIntentResolver.searchOutgoingCallPendingIntent(any()) } returns mockPendingIntent
        every { mockIntentResolver.searchEndCallPendingIntent(any()) } returns mockPendingIntent
        every {
            mockIntentResolver.searchOngoingCallPendingIntent(
                any(),
                any(),
            )
        } returns mockPendingIntent
        coEvery {
            mockUpdateInterceptor.onUpdateOngoingCallNotification(
                any(),
                any(),
                any(),
            )
        } returns mockk()
        every {
            mockIntentResolver.searchOngoingCallPendingIntent(
                any(),
                any(),
            )
        } returns mockPendingIntent
        every { mockIntentResolver.searchRejectCallPendingIntent(any()) } returns mockPendingIntent
        every {
            mockInitialInterceptor.onBuildOngoingCallNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)
        coEvery {
            mockUpdateInterceptor.onUpdateOngoingCallNotification(
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.updateOngoingCallNotification(mockCall, callDisplayName)

        // Then
        coVerify {
            mockUpdateInterceptor.onUpdateOngoingCallNotification(
                any(),
                callDisplayName,
                mockCall,
            )
        }
    }

    // ========== getXYZNotification Tests ==========

    @Test
    fun `getMissedCallNotification returns notification with correct properties`() {
        // Given
        val callDisplayName = "Carol Davis"
        val mockkApp = mockk<Application>(relaxed = true)
        val mockMissedChannelInfo = mockk<StreamNotificationChannelInfo>(relaxed = true)

        // Mock intent resolver calls
        every {
            mockIntentResolver.searchMissedCallPendingIntent(testCallId, any())
        } returns mockPendingIntent

        // Mock interceptor call
        every {
            mockInitialInterceptor.onBuildMissedCallNotification(any(), any())
        } returns mockk<NotificationCompat.Builder>(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.getMissedCallNotification(testCallId, callDisplayName)

        // Then
        assertNotNull(result)
        verify {
            mockInitialInterceptor.onBuildMissedCallNotification(
                any(), // context
                callDisplayName, // caller name
            )
        }
    }

    @Test
    fun `getMissedCallNotification handles null callDisplayName`() {
        // Given
        val mockkApp = mockk<Application>(relaxed = true)
        val mockMissedChannelInfo = mockk<StreamNotificationChannelInfo>(relaxed = true)

        // Mock intent resolver calls
        every {
            mockIntentResolver.searchMissedCallPendingIntent(testCallId, any())
        } returns mockPendingIntent

        // Mock interceptor call
        every {
            mockInitialInterceptor.onBuildMissedCallNotification(any(), any())
        } returns mockk<NotificationCompat.Builder>(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.getMissedCallNotification(testCallId, null)

        // Then
        assertNotNull(result)
        verify {
            mockInitialInterceptor.onBuildMissedCallNotification(
                any(), // context
                null, // null caller name
            )
        }
    }

    @Test
    fun `getRingingCallNotification returns incoming call notification for incoming state`() {
        // Given
        val callDisplayName = "David Brown"
        val ringingState = RingingState.Incoming()
        val mockkApp = mockk<Application>(relaxed = true)
        val mockIncomingChannelInfo = mockk<StreamNotificationChannelInfo>(relaxed = true)

        every { mockIntentResolver.searchIncomingCallPendingIntent(testCallId) } returns mockPendingIntent
        every { mockIntentResolver.searchAcceptCallPendingIntent(testCallId) } returns mockPendingIntent
        every { mockIntentResolver.searchRejectCallPendingIntent(testCallId) } returns mockPendingIntent

        // Mock interceptor call
        every {
            mockInitialInterceptor.onBuildIncomingCallNotification(any(), any(), any(), any(), any(), any())
        } returns mockk<NotificationCompat.Builder>(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.getRingingCallNotification(
            ringingState,
            testCallId,
            callDisplayName,
            true,
        )

        // Then
        assertNotNull(result)
        verify { mockIntentResolver.searchIncomingCallPendingIntent(testCallId) }
        verify { mockIntentResolver.searchAcceptCallPendingIntent(testCallId) }
        verify { mockIntentResolver.searchRejectCallPendingIntent(testCallId) }
        verify {
            mockInitialInterceptor.onBuildIncomingCallNotification(
                any(), // context
                mockPendingIntent, // content intent
                mockPendingIntent, // accept intent
                mockPendingIntent, // reject intent
                callDisplayName, // caller name
                true, // with actions
            )
        }
    }

    @Test
    fun `getRingingCallNotification returns null when intents are missing for incoming call`() {
        // Given
        val callDisplayName = "Eva Green"
        val ringingState = RingingState.Incoming()
        val mockkApp = mockk<Application>(relaxed = true)

        every { mockIntentResolver.searchIncomingCallPendingIntent(testCallId) } returns null
        every { mockIntentResolver.searchAcceptCallPendingIntent(testCallId) } returns mockPendingIntent
        every { mockIntentResolver.searchRejectCallPendingIntent(testCallId) } returns mockPendingIntent

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.getRingingCallNotification(
            ringingState,
            testCallId,
            callDisplayName,
            true,
        )

        // Then
        assertNull(result)
    }

    @Test
    fun `getRingingCallNotification handles outgoing call state`() {
        // Given
        val callDisplayName = "Frank Miller"
        val ringingState = RingingState.Outgoing()
        val mockkApp = mockk<Application>(relaxed = true)

        every { mockIntentResolver.searchOutgoingCallPendingIntent(testCallId) } returns mockPendingIntent
        every {
            mockIntentResolver.searchOutgoingCallPendingIntent(testCallId, any())
        } returns mockPendingIntent
        every { mockIntentResolver.searchRejectCallPendingIntent(testCallId) } returns mockPendingIntent
        every { mockIntentResolver.searchEndCallPendingIntent(testCallId) } returns mockPendingIntent

        // Mock interceptor call
        every {
            mockInitialInterceptor.onBuildOutgoingCallNotification(any(), any(), any(), any())
        } returns mockk<NotificationCompat.Builder>(relaxed = true)
        every {
            mockInitialInterceptor.onBuildOngoingCallNotification(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockk<NotificationCompat.Builder>(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.getRingingCallNotification(
            ringingState,
            testCallId,
            callDisplayName,
            true,
        )

        // Then
        assertNotNull(result)
        verify { mockIntentResolver.searchOutgoingCallPendingIntent(testCallId) }
        verify { mockIntentResolver.searchEndCallPendingIntent(testCallId) }
        verify {
            mockInitialInterceptor.onBuildOngoingCallNotification(
                any(), // builder
                any(), // ringing state
                any(), // call id
                any(),
                any(),
            )
        }
    }

    @Test
    fun `getIncomingCallNotification calls interceptor with correct parameters`() {
        // Given
        val callerName = "Grace Lee"
        val mockkApp = mockk<Application>(relaxed = true)

        // Mock interceptor call
        every {
            mockInitialInterceptor.onBuildIncomingCallNotification(any(), any(), any(), any(), any(), any())
        } returns mockk<NotificationCompat.Builder>(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.getIncomingCallNotification(
            mockPendingIntent,
            mockPendingIntent,
            mockPendingIntent,
            callerName,
            true,
        )

        // Then
        assertNotNull(result)
        verify {
            mockInitialInterceptor.onBuildIncomingCallNotification(
                any(), // context
                mockPendingIntent, // content intent
                mockPendingIntent, // accept intent
                mockPendingIntent, // reject intent
                callerName, // caller name
                true, // with actions
            )
        }
    }

    @Test
    fun `getIncomingCallNotification handles withActions false`() {
        // Given
        val callerName = "Helen Smith"
        val mockkApp = mockk<Application>(relaxed = true)

        // Mock interceptor call
        every {
            mockInitialInterceptor.onBuildIncomingCallNotification(any(), any(), any(), any(), any(), any())
        } returns mockk<NotificationCompat.Builder>(relaxed = true)

        testHandler = StreamDefaultNotificationHandler(
            application = mockkApp,
            notificationManager = mockNotificationManager,
            notificationPermissionHandler = mockNotificationPermissionHandler,
            intentResolver = mockIntentResolver,
            hideRingingNotificationInForeground = false,
            initialNotificationBuilderInterceptor = mockInitialInterceptor,
            updateNotificationBuilderInterceptor = mockUpdateInterceptor,
            permissionChecker = { _, _ -> PackageManager.PERMISSION_GRANTED },
        )

        // When
        val result = testHandler.getIncomingCallNotification(
            mockPendingIntent,
            mockPendingIntent,
            mockPendingIntent,
            callerName,
            false, // withActions = false
        )

        // Then
        assertNotNull(result)
        verify {
            mockInitialInterceptor.onBuildIncomingCallNotification(
                any(), // context
                mockPendingIntent, // content intent
                mockPendingIntent, // accept intent
                mockPendingIntent, // reject intent
                callerName, // caller name
                false, // with actions = false
            )
        }
    }
}
