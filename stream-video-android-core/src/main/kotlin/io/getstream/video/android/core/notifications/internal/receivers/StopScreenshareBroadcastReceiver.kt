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

package io.getstream.video.android.core.notifications.internal.receivers

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.screenshare.StreamScreenShareService

/**
 * Used for handling the "Stop screen sharing" action button on the notification displayed
 * by [StreamScreenShareService]
 */
internal class StopScreenshareBroadcastReceiver : GenericCallActionBroadcastReceiver() {

    val logger by taggedLogger("StopScreenshareBroadcastReceiver")
    override val action: String = StreamScreenShareService.BROADCAST_CANCEL_ACTION

    override suspend fun onReceive(call: Call, context: Context, intent: Intent) {
        call.stopScreenSharing()
        NotificationManagerCompat.from(
            context,
        ).cancel(StreamScreenShareService.NOTIFICATION_ID)
    }
}
