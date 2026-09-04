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

package io.getstream.video.android.core.notifications.internal.telecom.jetpack

import android.net.Uri
import android.os.Build
import android.os.ParcelUuid
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import io.getstream.video.android.core.notifications.internal.telecom.IncomingCallTelecomAction
import io.getstream.video.android.model.StreamCallId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class JetpackTelecomRepositoryTest {

    private lateinit var callsManager: CallsManager
    private lateinit var repository: JetpackTelecomRepository

    @Before
    fun setup() {
        callsManager = mockk()
        repository = JetpackTelecomRepository(
            callsManager = callsManager,
            callId = StreamCallId("default", "call-id"),
            incomingCallTelecomAction = mockk<IncomingCallTelecomAction>(relaxed = true),
        )
    }

    @Test
    fun `registerCall invokes callback after Telecom enters call scope`() = runTest {
        val attributes = slot<CallAttributesCompat>()
        val callControlScope = mockk<CallControlScope>(relaxed = true) {
            every { coroutineContext } returns EmptyCoroutineContext
            every { getCallId() } returns ParcelUuid(UUID.randomUUID())
            every { currentCallEndpoint } returns emptyFlow()
            every { availableEndpoints } returns emptyFlow()
            every { isMuted } returns emptyFlow()
        }
        coEvery {
            callsManager.addCall(
                capture(attributes),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } coAnswers {
            arg<CallControlScope.() -> Unit>(5).invoke(callControlScope)
        }
        var registeredStateSeen = false

        repository.registerCall(
            displayName = "Caller",
            address = Uri.parse("stream:call-id"),
            isIncoming = true,
            isVideoCall = true,
            onRegistered = {
                registeredStateSeen = repository.currentCall.value is TelecomCall.Registered
            },
            onException = { throw AssertionError("Unexpected registration failure", it) },
        )

        assertTrue(registeredStateSeen)
        assertEquals(CallAttributesCompat.DIRECTION_INCOMING, attributes.captured.direction)
        assertEquals(CallAttributesCompat.CALL_TYPE_VIDEO_CALL, attributes.captured.callType)
        assertTrue(repository.currentCall.value is TelecomCall.None)
    }

    @Test
    fun `registerCall reports non-cancellation failures`() = runTest {
        val failure = IllegalStateException("Telecom rejected call")
        coEvery {
            callsManager.addCall(any(), any(), any(), any(), any(), any())
        } throws failure
        val onException = mockk<(Exception) -> Unit>(relaxed = true)

        repository.registerCall(
            "Caller",
            Uri.parse("stream:call-id"),
            true,
            false,
            onRegistered = { error("Registration must not succeed") },
            onException = onException,
        )

        verify { onException(failure) }
        assertTrue(repository.currentCall.value is TelecomCall.None)
    }

    @Test
    fun `registerCall rethrows coroutine cancellation without reporting failure`() = runTest {
        val cancellation = CancellationException("test cancelled")
        coEvery {
            callsManager.addCall(any(), any(), any(), any(), any(), any())
        } throws cancellation
        val onException = mockk<(Exception) -> Unit>(relaxed = true)

        assertFailsWith<CancellationException> {
            repository.registerCall(
                "Caller",
                Uri.parse("stream:call-id"),
                true,
                false,
                onRegistered = {},
                onException = onException,
            )
        }

        verify(exactly = 0) { onException(any()) }
    }
}
