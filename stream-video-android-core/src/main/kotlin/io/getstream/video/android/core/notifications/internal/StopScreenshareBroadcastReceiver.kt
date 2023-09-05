/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_REJECT_CALL
import io.getstream.video.android.core.screenshare.StreamScreenShareService
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Used for handling the "Stop screen sharing" action button on the notification displayed
 * by [StreamScreenShareService]
 */
internal class StopScreenshareBroadcastReceiver : BroadcastReceiver() {

    val logger by taggedLogger("StopScreenshareBroadcastReceiver")

    override fun onReceive(context: Context?, intent: Intent?) {
        logger.d { "[onReceive] context: $context, intent: $intent" }

        if (context != null && intent?.action == StreamScreenShareService.BROADCAST_CANCEL_ACTION) {
            val callCid = StreamCallId.fromCallCid(
                intent.getStringExtra(StreamScreenShareService.INTENT_EXTRA_CALL_ID)!!,
            )

            CoroutineScope(Dispatchers.IO).launch {
                val streamVideo: StreamVideo? = StreamVideo.instanceOrNull()

                if (streamVideo == null) {
                    logger.e {
                        "Received ${ACTION_REJECT_CALL} but StreamVideo is not initialised. " +
                            "Handling notifications requires to initialise StreamVideo in Application.onCreate"
                    }
                    return@launch
                }

                streamVideo.call(callCid.type, callCid.id).stopScreenSharing()
                NotificationManagerCompat.from(
                    context,
                ).cancel(StreamScreenShareService.NOTIFICATION_ID)
            }
        }
    }
}
