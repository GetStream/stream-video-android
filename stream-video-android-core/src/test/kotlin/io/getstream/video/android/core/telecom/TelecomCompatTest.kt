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
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.model.StreamCallId
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
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
    private val callConfig: CallServiceConfig = mockk()

    @Before
    fun setUp() {
        mockkObject(TelecomHandler)
        mockkObject(CallService)

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

        every { CallService.showIncomingCall(any(), any(), any(), any(), any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkObject(TelecomHandler)
        unmockkObject(CallService)
    }

    @Test
    fun registerCall_whenTelecomSupported_usesTelecomHandler() {
        every { TelecomHandler.getInstance(context) } returns mockTelecomHandler
        val isIncomingCall = false

        spyTelecomCompat.registerCall(context, streamCall, callId, callInfo, isIncomingCall)

        verify(exactly = 1) {
            mockTelecomHandler.registerCall(streamCall, any<CallServiceConfig>(), isIncomingCall)
        }
        verify(exactly = 0) { CallService.showIncomingCall(any(), any(), any(), any(), any()) }
    }

    @Test
    fun registerCall_withTelecomNotSupportedAndIncomingCall_usesCallService() {
        every { TelecomHandler.getInstance(context) } returns null
        val isIncomingCall = true

        spyTelecomCompat.registerCall(context, streamCall, callId, callInfo, isIncomingCall)

        verify(exactly = 0) { mockTelecomHandler.registerCall(any(), any(), isIncomingCall) }
        verify(exactly = 1) {
            CallService.showIncomingCall(any<Context>(), callId, callInfo, callConfig, any())
        }
    }

    @Test
    fun registerCall_withTelecomNotSupportedAndWithoutIncomingCall_doesNotCallAnything() {
        every { TelecomHandler.getInstance(context) } returns null
        val isIncomingCall = false

        spyTelecomCompat.registerCall(context, streamCall, callId, callInfo, isIncomingCall)

        verify(exactly = 0) { mockTelecomHandler.registerCall(any(), any(), isIncomingCall) }
        verify(exactly = 0) { CallService.showIncomingCall(any(), any(), any(), any(), any()) }
    }
}
