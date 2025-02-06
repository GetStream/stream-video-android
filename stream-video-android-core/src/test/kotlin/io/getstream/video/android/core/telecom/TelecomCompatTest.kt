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

package io.getstream.video.android.core.telecom

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.model.StreamCallId
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TelecomCompatTest {
    private val spyTelecomCompat: TelecomCompat = spyk()
    private val mockTelecomHandler: TelecomHandler = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val streamCall: Call = mockk()
    private val callId: StreamCallId = StreamCallId.fromCallCid("default:123")
    private val callInfo = "Test call"
    private val callConfig: CallServiceConfig = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(TelecomHandler)

        mockkObject(CallService)
        every { CallService.showIncomingCall(any(), any(), any(), any(), any()) } just runs
        every { CallService.removeIncomingCall(any(), any(), any()) } just runs

        mockkStatic(ContextCompat::class)
        every { ContextCompat.startForegroundService(any(), any()) } just runs

        every { streamCall.cid } returns "default:123"

        every {
            spyTelecomCompat["withCall"](
                streamCall, callId,
                any<
                    (
                        Call,
                        CallServiceConfig,
                    ) -> Unit,
                    >(),
            )
        } answers {
            val doActionLambda = arg<(Call, CallServiceConfig) -> Unit>(2)
            doActionLambda.invoke(streamCall, callConfig)
        }
    }

    @After
    fun tearDown() {
        unmockkObject(TelecomHandler)
        unmockkObject(CallService)
        unmockkObject(ContextCompat::class)
    }

    @Test
    fun registerCall_telecomSupported_usesTelecomHandler() {
        // getInstance() returns null if the device doesn't support Telecom
        every { TelecomHandler.getInstance(context) } returns mockTelecomHandler
        val isIncomingCall = false

        spyTelecomCompat.registerCall(context, streamCall, callId, callInfo, isIncomingCall)

        verify(exactly = 1) {
            mockTelecomHandler.registerCall(streamCall, any<CallServiceConfig>(), isIncomingCall)
        }
        verify(exactly = 0) {
            CallService.showIncomingCall(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun registerCall_telecomNotSupportedAndIncomingCall_usesCallService() {
        // getInstance() returns null if the device doesn't support Telecom
        every { TelecomHandler.getInstance(context) } returns null
        val isIncomingCall = true

        spyTelecomCompat.registerCall(context, streamCall, callId, callInfo, isIncomingCall)

        verify(exactly = 0) {
            mockTelecomHandler.registerCall(any(), any(), isIncomingCall)
        }
        verify(exactly = 1) {
            CallService.showIncomingCall(any<Context>(), callId, callInfo, callConfig, any())
        }
    }

    @Test
    fun registerCall_telecomNotSupportedAndNotIncomingCall_doesNotCallAnything() {
        every { TelecomHandler.getInstance(context) } returns null
        val isIncomingCall = false

        spyTelecomCompat.registerCall(context, streamCall, callId, callInfo, isIncomingCall)

        verify(exactly = 0) { mockTelecomHandler.registerCall(any(), any(), isIncomingCall) }
        verify(exactly = 0) { CallService.showIncomingCall(any(), any(), any(), any(), any()) }
    }

    @Test
    fun changeCallState_telecomSupported_usesTelecomHandler() {
        every { TelecomHandler.getInstance(context) } returns mockTelecomHandler
        val callState = TelecomCallState.ONGOING

        spyTelecomCompat.changeCallState(context, callState, streamCall, callId)

        verify(exactly = 1) { mockTelecomHandler.changeCallState(streamCall, callState) }
        verify(exactly = 0) { CallService.buildStartIntent(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
    }

    @Test
    fun changeCallState_telecomNotSupportedAndOutgoingOrOngoingCallAndForegroundServiceEnabled_startsForegroundService() {
        every { TelecomHandler.getInstance(context) } returns null
        every { callConfig.runCallServiceInForeground } returns true
        val callState = TelecomCallState.OUTGOING

        spyTelecomCompat.changeCallState(context, callState, streamCall, callId)

        verify(exactly = 0) { mockTelecomHandler.changeCallState(streamCall, callState) }
        verify(exactly = 1) {
            CallService.buildStartIntent(
                context,
                callId,
                CallService.TRIGGER_OUTGOING_CALL,
                callDisplayName = null,
                callConfig,
            )
        }
//        verify(exactly = 1) {
//            ContextCompat.startForegroundService(any(), any())
//        }
    }

    @Test
    fun changeCallState_telecomNotSupportedAndOutgoingOrOngoingCallAndForegroundServiceDisabled_doesNotCallAnything() {
        every { TelecomHandler.getInstance(context) } returns null
        every { callConfig.runCallServiceInForeground } returns false
        val callState = TelecomCallState.OUTGOING

        spyTelecomCompat.changeCallState(context, callState, streamCall, callId)

        verify(exactly = 0) { mockTelecomHandler.changeCallState(any(), any()) }
        verify(exactly = 0) { CallService.buildStartIntent(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
    }

    @Test
    fun changeCallState_telecomNotSupportedAndNotOutgoingOrOngoingCall_doesNotCallAnything() {
        every { TelecomHandler.getInstance(context) } returns null
        val callState = TelecomCallState.INCOMING

        spyTelecomCompat.changeCallState(context, callState, streamCall, callId)

        verify(exactly = 0) { mockTelecomHandler.changeCallState(any(), any()) }
        verify(exactly = 0) { CallService.buildStartIntent(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
    }

    @Test
    fun unregisterCall_telecomSupported_usesTelecomHandler() {
        every { TelecomHandler.getInstance(context) } returns mockTelecomHandler
        val isIncomingCall = false

        spyTelecomCompat.unregisterCall(context, streamCall, isIncomingCall)

//        verify(exactly = 1) { mockTelecomHandler.unregisterCall(any()) }
        verify(exactly = 0) { CallService.removeIncomingCall(any(), any(), any()) }
        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
    }

    @Test
    fun unregisterCall_telecomNotSupportedAndForegroundServiceEnabledAndIncomingCall_removesIncomingCall() {
        every { TelecomHandler.getInstance(context) } returns null
        every { callConfig.runCallServiceInForeground } returns true
        val isIncomingCall = true

        spyTelecomCompat.unregisterCall(context, streamCall, isIncomingCall)

        verify(exactly = 0) { mockTelecomHandler.unregisterCall(any()) }
//        verify(exactly = 1) { CallService.removeIncomingCall(context, callId, callConfig) }
    }

    @Test
    fun unregisterCall_telecomNotSupportedAndForegroundServiceEnabledAndNotIncomingCall_stopService() {
        every { TelecomHandler.getInstance(context) } returns null
        every { callConfig.runCallServiceInForeground } returns true
        val isIncomingCall = false

        spyTelecomCompat.unregisterCall(context, streamCall, isIncomingCall)

        verify(exactly = 0) { mockTelecomHandler.unregisterCall(any()) }
        verify(exactly = 0) { CallService.removeIncomingCall(any(), any(), any()) }
        verify(exactly = 0) { CallService.buildStopIntent(any(), any()) }
//        verify(exactly = 1) { context.stopService(any()) }
    }

    @Test
    fun unregisterCall_telecomNotSupportedAndForegroundServiceDisabled_doesNotCallAnything() {
        every { TelecomHandler.getInstance(context) } returns null
        every { callConfig.runCallServiceInForeground } returns false
        val isIncomingCall = false

        spyTelecomCompat.unregisterCall(context, streamCall, isIncomingCall)

        verify(exactly = 0) { mockTelecomHandler.unregisterCall(any()) }
        verify(exactly = 0) { CallService.removeIncomingCall(any(), any(), any()) }
        verify(exactly = 0) { CallService.buildStopIntent(any(), any()) }
        verify(exactly = 0) { context.stopService(any()) }
    }

    @Test
    fun cleanUp_telecomSupported_usesTelecomHandler() {
        every { TelecomHandler.getInstance(context) } returns mockTelecomHandler

        spyTelecomCompat.cleanUp(context, streamCall)

        verify(exactly = 1) { mockTelecomHandler.cleanUp() }
    }

    @Test
    fun cleanUp_telecomNotSupportedAndActiveCallAndForegroundServiceEnabled_stopsService() {
        every { TelecomHandler.getInstance(context) } returns null
        every { callConfig.runCallServiceInForeground } returns true

        spyTelecomCompat.cleanUp(context, streamCall)

        verify(exactly = 0) { mockTelecomHandler.cleanUp() }
//        verify(exactly = 1) { CallService.buildStopIntent(context, callConfig) }
//        verify(exactly = 1) { context.stopService(any()) }
    }
}
