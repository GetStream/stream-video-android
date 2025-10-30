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

package io.getstream.video.android.core.notifications.internal.receivers

import android.content.Context
import android.content.Intent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ExternalCallRejectionHandler
import io.getstream.video.android.core.ExternalCallRejectionSource
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_REJECT_CALL
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RejectCallBroadcastReceiverTest {

    private lateinit var context: Context
    private lateinit var intent: Intent
    private lateinit var call: Call
    private lateinit var rejectionHandler: ExternalCallRejectionHandler
    private lateinit var receiver: RejectCallBroadcastReceiver

    @Before
    fun setup() {
        mockkConstructor(ExternalCallRejectionHandler::class)

        context = mockk(relaxed = true)
        intent = mockk(relaxed = true)
        call = mockk(relaxed = true)
        rejectionHandler = mockk(relaxed = true)

        // When a new ExternalCallRejectionHandler() is created, return our mock
        coEvery {
            anyConstructed<ExternalCallRejectionHandler>().onRejectCall(any(), any(), any(), any())
        } just Runs

        receiver = RejectCallBroadcastReceiver()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `action constant should be ACTION_REJECT_CALL`() {
        assertEquals(ACTION_REJECT_CALL, receiver.action)
    }

    @Test
    fun `onReceive should call ExternalCallRejectionHandler with NOTIFICATION source`() = runTest {
        receiver.onReceive(call, context, intent)

        coVerify {
            anyConstructed<ExternalCallRejectionHandler>().onRejectCall(
                ExternalCallRejectionSource.NOTIFICATION,
                call,
                context,
                intent,
            )
        }
    }
}
