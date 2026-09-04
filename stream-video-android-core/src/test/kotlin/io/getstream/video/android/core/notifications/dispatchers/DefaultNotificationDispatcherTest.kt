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

package io.getstream.video.android.core.notifications.dispatchers

import android.app.Notification
import androidx.core.app.NotificationManagerCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DefaultNotificationDispatcherTest {

    @Test
    fun `notify updates the call owned by the injected client`() {
        val notificationManager = mockk<NotificationManagerCompat>(relaxed = true)
        val streamVideo = mockk<StreamVideo>()
        val call = mockk<Call>()
        val callState = mockk<CallState>(relaxed = true)
        val notification = mockk<Notification>()
        val callId = StreamCallId(type = "default", id = "call-id")
        every { streamVideo.call(callId.type, callId.id) } returns call
        every { call.state } returns callState

        DefaultNotificationDispatcher(notificationManager, streamVideo)
            .notify(callId, 42, notification)

        verify {
            callState.updateNotification(42, notification)
            notificationManager.notify(42, notification)
        }
    }
}
