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

import android.content.Context
import io.getstream.video.android.core.Call
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
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ServiceIntentBuilderTest {

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

    @Test
    fun `buildStartIntent creates correct intent for outgoing call`() {
        val intent = ServiceIntentBuilder().buildStartIntent(
            context,
            StartServiceParam(
                callId = testCallId,
                trigger = TRIGGER_OUTGOING_CALL,
            ),
        )

        assertEquals(CallService::class.java.name, intent.component?.className)
        assertEquals(testCallId, intent.streamCallId(INTENT_EXTRA_CALL_CID))
        assertEquals(TRIGGER_OUTGOING_CALL, intent.getStringExtra(TRIGGER_KEY))
        assertNull(intent.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME))
    }

    @Test
    fun `buildStartIntent creates correct intent for ongoing call`() {
        val intent = ServiceIntentBuilder().buildStartIntent(
            context,
            StartServiceParam(
                callId = testCallId,
                trigger = TRIGGER_ONGOING_CALL,
            ),
        )

        assertEquals(CallService::class.java.name, intent.component?.className)
        assertEquals(testCallId, intent.streamCallId(INTENT_EXTRA_CALL_CID))
        assertEquals(TRIGGER_ONGOING_CALL, intent.getStringExtra(TRIGGER_KEY))
    }

    @Test
    fun `buildStartIntent creates correct intent for remove incoming call`() {
        val intent = ServiceIntentBuilder().buildStartIntent(
            context = context,
            StartServiceParam(testCallId, TRIGGER_REMOVE_INCOMING_CALL),
        )

        assertEquals(CallService::class.java.name, intent.component?.className)
        assertEquals(testCallId, intent.streamCallId(INTENT_EXTRA_CALL_CID))
        assertEquals(TRIGGER_REMOVE_INCOMING_CALL, intent.getStringExtra(TRIGGER_KEY))
    }

    @Test
    fun `buildStartIntent throws exception for invalid trigger`() {
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            ServiceIntentBuilder().buildStartIntent(
                context,
                StartServiceParam(testCallId, "invalid_trigger"),
            )
        }
    }

    @Test
    fun `buildStartIntent uses custom service class from configuration`() {
        val customConfig = CallServiceConfig(
            serviceClass = LivestreamCallService::class.java,
        )

        val intent = ServiceIntentBuilder().buildStartIntent(
            context,
            StartServiceParam(
                callId = testCallId,
                trigger = TRIGGER_INCOMING_CALL,
                callServiceConfiguration = customConfig,
            ),
        )

        assertEquals(LivestreamCallService::class.java.name, intent.component?.className)
    }

    @Test
    fun `buildStopIntent returns null when service is not running`() {
        val builder = spyk(ServiceIntentBuilder())

        val serviceClass = CallService::class.java
        val config = CallServiceConfig(serviceClass = serviceClass)
        val param = StopServiceParam(
            callServiceConfiguration = config,
            call = null,
        )

        every {
            builder.isServiceRunning(context, serviceClass)
        } returns false

        val intent = builder.buildStopIntent(context, param)

        assertNull(intent)
    }

    @Test
    fun `buildStopIntent returns intent with stop flag when service is running`() {
        val builder = spyk(ServiceIntentBuilder())

        val serviceClass = CallService::class.java
        val config = CallServiceConfig(serviceClass = serviceClass)
        val param = StopServiceParam(
            callServiceConfiguration = config,
            call = null,
        )

        every {
            builder.isServiceRunning(context, serviceClass)
        } returns true

        val intent = builder.buildStopIntent(context, param)

        assertNotNull(intent)
        assertEquals(serviceClass.name, intent!!.component?.className)
        assertTrue(intent.getBooleanExtra(CallService.EXTRA_STOP_SERVICE, false))
    }

    @Test
    fun `buildStopIntent attaches call cid when call is present`() {
        val builder = spyk(ServiceIntentBuilder())

        val serviceClass = CallService::class.java
        val config = CallServiceConfig(serviceClass = serviceClass)

        val call = mockk<Call> {
            every { type } returns "default"
            every { id } returns "123"
            every { cid } returns "default:123"
        }

        val param = StopServiceParam(
            callServiceConfiguration = config,
            call = call,
        )

        every {
            builder.isServiceRunning(context, serviceClass)
        } returns true

        val intent = builder.buildStopIntent(context, param)

        assertNotNull(intent)

        val streamCallId =
            intent!!.getParcelableExtra<StreamCallId>(INTENT_EXTRA_CALL_CID)

        assertNotNull(streamCallId)
        assertEquals("default", streamCallId!!.type)
        assertEquals("123", streamCallId.id)
        assertEquals("default:123", streamCallId.cid)

        assertTrue(intent.getBooleanExtra(CallService.EXTRA_STOP_SERVICE, false))
    }

    @Test
    fun `service respects configuration for different call types`() {
        val livestreamConfig = CallServiceConfig(
            serviceClass = LivestreamCallService::class.java,
            runCallServiceInForeground = true,
        )

        val intent = ServiceIntentBuilder().buildStartIntent(
            context,
            StartServiceParam(
                callId = StreamCallId(type = "livestream", id = "test-123"),
                trigger = TRIGGER_INCOMING_CALL,
                callServiceConfiguration = livestreamConfig,
            ),
        )

        assertEquals(LivestreamCallService::class.java.name, intent.component?.className)
    }
}
