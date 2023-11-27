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

package io.getstream.video.android.core.notifications.internal.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId

/**
 * A foreground service that is running when there is an active call.
 */
internal class OngoingCallService : Service() {
    private val logger by taggedLogger("OngoingCallService")
    private var callId: StreamCallId? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        val streamVideo = StreamVideo.instanceOrNull()
        val started = if (callId != null && streamVideo != null) {
            val notification = streamVideo.getOngoingCallNotification(callId!!)
            if (notification != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceCompat.startForeground(this, callId.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } else {
                    startForeground(callId.hashCode(), notification)
                }
                true
            } else {
                // Service not started no notification
                logger.e { "Could not get notification for ongoing call" }
                false
            }
        } else {
            // Service not started, no call Id or stream video
            logger.e { "Call id or streamVideo are not available." }
            false
        }

        if (!started) {
            logger.w { "Foreground service did not start!" }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        callId?.let {
            val notificationId = callId.hashCode()
            NotificationManagerCompat.from(this).cancel(notificationId)
        }
        super.onDestroy()
    }

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null
}
