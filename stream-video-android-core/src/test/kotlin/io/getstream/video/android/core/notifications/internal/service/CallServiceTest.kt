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

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
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
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class CallServiceTest {

    @MockK
    lateinit var mockContext: Context

    @MockK
    private lateinit var mockStreamVideoClient: StreamVideoClient

    @MockK
    lateinit var mockNotification: Notification

    @MockK
    lateinit var payload: Map<String, Any?>

    private lateinit var context: Context
    private lateinit var callService: CallService
    private lateinit var testCallId: StreamCallId

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = RuntimeEnvironment.getApplication()
        callService = CallService()
        testCallId = StreamCallId(type = "default", id = "test-call-123")
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        clearAllMocks()
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
            TRIGGER_KEY,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `service has correct service type for Android R`() {
        callService.attachContext(context)

        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
            callService.serviceType,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `service has correct service type for Android Q`() {
        callService.attachContext(context)

        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED

        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL, callService.serviceType)
    }

    @Test
    fun `service has correct service type for pre Android Q`() {
        callService.attachContext(context)

        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED

        assertEquals(0, callService.serviceType)
    }

    @Test
    fun `service has correct service type without permission granted`() {
        callService.attachContext(context)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL, callService.serviceType)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `LivestreamAudioCallService has correct service type without permission for Android UPSIDE_DOWN_CAKE`() {
        val audioService = LivestreamAudioCallService()
        audioService.attachContext(context)

        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE, audioService.serviceType)
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
        assertEquals(
            "io.getstream.video.android.intent-extra.call_displayname",
            INTENT_EXTRA_CALL_DISPLAY_NAME,
        )
    }
}

internal fun CallService.attachContext(context: Context) {
    val method = ContextWrapper::class.java.getDeclaredMethod(
        "attachBaseContext",
        Context::class.java,
    )
    method.isAccessible = true
    method.invoke(this, context)
}
