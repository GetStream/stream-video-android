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

package io.getstream.video.android.core.notifications.internal.service.observers

import android.app.Notification
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.dispatchers.NotificationDispatcher
import io.getstream.video.android.model.StreamCallId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test

class TelecomNotificationUpdateObserverTest {

    private val callId = StreamCallId("default", "call-id")
    private val call = mockk<Call>()
    private val callState = mockk<CallState>()
    private val streamVideo = mockk<StreamVideoClient>()
    private val dispatcher = mockk<NotificationDispatcher>(relaxed = true)
    private val notification = mockk<Notification>()
    private val notificationId = MutableStateFlow<Int?>(null)
    private val observer = TelecomNotificationUpdateObserver(
        call,
        streamVideo,
        TestScope(UnconfinedTestDispatcher()),
    )

    init {
        every { call.type } returns callId.type
        every { call.id } returns callId.id
        every { call.state } returns callState
        every { callState.notificationIdFlow } returns notificationId
        every { streamVideo.getStreamNotificationDispatcher() } returns dispatcher
    }

    @Test
    fun `incoming update reuses existing notification ID`() {
        notificationId.value = 42

        observer.showIncomingCallNotification(callId, notification)

        verify { dispatcher.notify(callId, 42, notification) }
    }

    @Test
    fun `incoming update uses incoming ID when no notification was posted`() {
        observer.showIncomingCallNotification(callId, notification)

        verify {
            dispatcher.notify(
                callId,
                callId.getNotificationId(NotificationType.Incoming),
                notification,
            )
        }
    }

    @Test
    fun `outgoing update uses outgoing ID when no notification was posted`() {
        observer.showOutgoingCallNotification(callId, notification)

        verify {
            dispatcher.notify(
                callId,
                callId.getNotificationId(NotificationType.Outgoing),
                notification,
            )
        }
    }
}
