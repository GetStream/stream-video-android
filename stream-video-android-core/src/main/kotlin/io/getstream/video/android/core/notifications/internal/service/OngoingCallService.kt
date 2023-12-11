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
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId

/**
 * A foreground service that is running when there is an active call.
 */
internal class OngoingCallService : Service() {
    private val logger by taggedLogger("OngoingCallService")
    private var callId: StreamCallId? = null
    private val toggleCameraBroadcastReceiver = ToggleCameraBroadcastReceiver()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        val streamVideo = StreamVideo.instanceOrNull()
        val started = if (callId != null && streamVideo != null) {
            val notification = streamVideo.getOngoingCallNotification(callId!!)
            if (notification != null) {
                startForeground(callId.hashCode(), notification)
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

        if (started) {
            registerToggleCameraBroadcastReceiver()
        } else {
            logger.w { "Foreground service did not start!" }
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun registerToggleCameraBroadcastReceiver() {
        try {
            registerReceiver(
                toggleCameraBroadcastReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_USER_PRESENT)
                },
            )
        } catch (e: Exception) {
            logger.e(e) { "Unable to register ToggleCameraBroadcastReceiver." }
        }
    }

    override fun onDestroy() {
        callId?.let {
            val notificationId = callId.hashCode()
            NotificationManagerCompat.from(this).cancel(notificationId)
        }

        unregisterToggleCameraBroadcastReceiver()

        super.onDestroy()
    }

    private fun unregisterToggleCameraBroadcastReceiver() {
        try {
            unregisterReceiver(toggleCameraBroadcastReceiver)
        } catch (e: Exception) {
            logger.e(e) { "Unable to unregister ToggleCameraBroadcastReceiver." }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        callId?.let {
            StreamVideo.instanceOrNull()?.call(it.type, it.id)?.leave()
            logger.i { "Left ongoing call." }
        }
    }

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null
}
