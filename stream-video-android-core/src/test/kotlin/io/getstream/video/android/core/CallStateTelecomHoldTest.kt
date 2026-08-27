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

package io.getstream.video.android.core

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.base.TestBase
import io.getstream.video.android.core.base.toResponse
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.TelecomCall
import io.getstream.video.android.core.utils.toResponse
import io.getstream.video.android.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * Verifies the telecom hold observer: when Android Telecom puts the call on hold while it is
 * active, the SDK leaves the call with [SdkCause.CALL_ON_HOLD].
 */
@RunWith(RobolectricTestRunner::class)
internal class CallStateTelecomHoldTest : TestBase() {

    private val scope = CoroutineScope(dispatcherRule.testDispatcher)

    private val user = User(id = "caller", createdAt = nowUtc, updatedAt = nowUtc)

    private val activeCall = MutableStateFlow<Call?>(null)
    private val ringingCall = MutableStateFlow<Call?>(null)

    private val clientState = mockk<ClientState>(relaxed = true) {
        every { activeCall } returns this@CallStateTelecomHoldTest.activeCall
        every { ringingCall } returns this@CallStateTelecomHoldTest.ringingCall
    }
    private val client = mockk<StreamVideoClient>(relaxed = true) {
        every { userId } returns this@CallStateTelecomHoldTest.user.id
        every { state } returns clientState
    }
    private val call = mockk<Call>(relaxed = true) {
        every { type } returns "default"
        every { id } returns "telecom-hold-test"
        every { cid } returns "default:telecom-hold-test"
        every { events } returns MutableSharedFlow<VideoEvent>()
    }

    @After
    fun tearDownScope() {
        scope.cancel()
    }

    @Test
    fun `putting an active call on hold leaves the call with CALL_ON_HOLD`() {
        val callState = CallState(client, call, user, scope)
        callState.updateFromResponse(call.toResponse(user.toResponse()))
        activeCall.value = call
        callState.updateRingingState()
        assertTrue(callState.ringingState.value is RingingState.Active)

        val heldCall = mockk<TelecomCall.Registered> { every { isOnHold } returns true }
        callState.jetpackTelecomRepository = mockk<JetpackTelecomRepository> {
            every { currentCall } returns MutableStateFlow<TelecomCall>(heldCall)
        }

        // The observer runs on DispatcherProvider.Default (a real dispatcher here), so the
        // verification has to wait for it.
        verify(timeout = 5_000L) {
            call.leave(
                match<CallLeaveReason> {
                    it is CallLeaveReason.SdkDriven && it.cause == SdkCause.CALL_ON_HOLD
                },
            )
        }
    }
}
