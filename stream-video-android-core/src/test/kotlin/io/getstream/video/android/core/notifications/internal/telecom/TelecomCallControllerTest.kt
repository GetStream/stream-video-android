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

package io.getstream.video.android.core.notifications.internal.telecom

import android.content.Context
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.TelecomCall
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow

// @RunWith(RobolectricTestRunner::class)
class TelecomCallControllerTest {
    private lateinit var context: Context
    private lateinit var call: Call
    private lateinit var telecomCall: TelecomCall.Registered
    private lateinit var repository: JetpackTelecomRepository
    private lateinit var callState: CallState
    private lateinit var telecomPermissions: TelecomPermissions
    private lateinit var telecomHelper: TelecomHelper
    private lateinit var controller: TelecomCallController

//    @Before
    fun setup() {
        mockkConstructor(TelecomPermissions::class)
        mockkConstructor(TelecomHelper::class)

        context = mockk(relaxed = true)
        call = mockk(relaxed = true)
        telecomCall = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        callState = mockk(relaxed = true)
        telecomPermissions = mockk(relaxed = true)
        telecomHelper = mockk(relaxed = true)

        // Construct the system under test
        controller = TelecomCallController(context)

        // Replace internal helper behaviors
        every { anyConstructed<TelecomPermissions>().canUseTelecom(any(), context) } returns true
//        every { anyConstructed<TelecomPermissions>() } returns telecomPermissions
        every { anyConstructed<TelecomHelper>().canUseJetpackTelecom() } returns true

        // Setup repository and call
        every { repository.currentCall } returns MutableStateFlow(telecomCall)
        every { call.state } returns callState
        every { callState.jetpackTelecomRepository } returns repository

        // Setup fake client + config
        val client = mockk<StreamVideoClient>(relaxed = true)
        every { call.client } returns client
        val configRegistry = mockk<CallServiceConfigRegistry>(relaxed = true)
        every { client.callServiceConfigRegistry } returns configRegistry
        every { configRegistry.get(any()) } returns CallServiceConfig(enableTelecom = true)
    }

//    @After
    fun tearDown() {
        unmockkAll()
    }

    // region leaveCall()

//    @Test
    fun `leaveCall should call processAction with Disconnect`() {
        every { anyConstructed<TelecomPermissions>().canUseTelecom(any(), any()) } returns true

        controller.leaveCall(call)

        verify {
            telecomCall.processAction(any())
        }

//        verify {
//            telecomCall.processAction(
//                match {
//                    it is TelecomCallAction.Disconnect &&
//                            it.cause.code == DisconnectCause.LOCAL &&
//                            it.source == InteractionSource.PHONE
//                }
//            )
//        }
    }

//    @Test
    fun `leaveCall should not call processAction if telecom unavailable`() {
        every { anyConstructed<TelecomPermissions>().canUseTelecom(any(), any()) } returns false

        controller.leaveCall(call)

        verify(exactly = 0) { telecomCall.processAction(any()) }
    }

    // endregion

    // region onAnswer()

//    @Test
//    fun `onAnswer should call processAction with Answer video=false when call is video`() {
//        every { call.hasCapability(OwnCapability.SendVideo) } returns true
//
//        controller.onAnswer(call)
//
//        verify {
//            telecomCall.processAction(
//                match {
//                    it is TelecomCallAction.Answer && it.isAudioOnly == false
//                }
//            )
//        }
//    }
//
//    @Test
//    fun `onAnswer should call processAction with Answer video=true when call is audio only`() {
//        every { call.hasCapability(OwnCapability.SendVideo) } returns false
//        every { call.isVideoEnabled() } returns false
//
//        controller.onAnswer(call)
//
//        verify {
//            telecomCall.processAction(
//                match {
//                    it is TelecomCallAction.Answer && it.isAudioOnly
//                }
//            )
//        }
//    }

//    @Test
    fun `onAnswer should not call processAction if telecom not allowed`() {
        every { anyConstructed<TelecomPermissions>().canUseTelecom(any(), any()) } returns false

        controller.onAnswer(call)

        verify(exactly = 0) { telecomCall.processAction(any()) }
    }

    // endregion
}
