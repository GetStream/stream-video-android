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

package io.getstream.video.android.core.screenshare

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.video.android.core.R
import io.getstream.video.android.core.notifications.internal.receivers.StopScreenshareBroadcastReceiver
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.utils.startForegroundWithServiceType

/**
 * Screen-sharing in Android requires a ForegroundService (with type foregroundServiceType set to "mediaProjection").
 * The Stream SDK will start this [StreamScreenShareService] once screen-sharing is enabled and then
 * will stop it when screen-sharing it's either stopped by the user or we get a callback that the
 * screen-sharing was stopped by the system.
 *
 * This Service isn't doing any long-running operations. It's just an empty Service to meet the platform
 * requirement (https://developer.android.com/reference/android/media/projection/MediaProjectionManager).
 */
internal class StreamScreenShareService : Service() {

    private val channelId = "StreamScreenShareService"

    private val binder = LocalBinder()

    /**
     * This Binder is only used to be able to wait for the service until it's started
     * in [ScreenShareManager]
     */
    inner class LocalBinder : Binder()

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callId = intent?.getStringExtra(EXTRA_CALL_ID)!!
        val cancelScreenShareIntent = Intent(
            this,
            StopScreenshareBroadcastReceiver::class.java,
        ).apply {
            action = BROADCAST_CANCEL_ACTION
            putExtra(INTENT_EXTRA_CALL_ID, callId)
        }
        val cancelScreenSharePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                cancelScreenShareIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val builder = NotificationCompat.Builder(applicationContext, channelId).apply {
            priority = NotificationCompat.PRIORITY_HIGH
            setAutoCancel(false)
            setOngoing(true)
            setSmallIcon(R.drawable.stream_video_ic_screenshare)
            setContentTitle(getString(R.string.stream_video_screen_sharing_notification_title))
            setContentText(getString(R.string.stream_video_screen_sharing_notification_description))
            setAllowSystemGeneratedContextualActions(false)
            addAction(
                R.drawable.stream_video_ic_cancel_screenshare,
                getString(R.string.stream_video_screen_sharing_notification_action_stop),
                cancelScreenSharePendingIntent,
            )
        }

        NotificationManagerCompat.from(application).also {
            it.createNotificationChannel(
                NotificationChannelCompat
                    .Builder(channelId, NotificationManager.IMPORTANCE_DEFAULT)
                    .setName(
                        getString(R.string.stream_video_screen_sharing_notification_channel_title),
                    )
                    .setDescription(
                        getString(
                            R.string.stream_video_screen_sharing_notification_channel_description,
                        ),
                    )
                    .build(),
            )
        }

        startForegroundWithServiceType(
            NOTIFICATION_ID,
            builder.build(),
            CallService.Companion.Trigger.ShareScreen,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        internal const val NOTIFICATION_ID = 43534
        internal const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        internal const val BROADCAST_CANCEL_ACTION =
            "io.getstream.video.android.action.CANCEL_SCREEN_SHARE"
        internal const val INTENT_EXTRA_CALL_ID = "io.getstream.video.android.intent-extra.call_cid"

        fun createIntent(context: Context, callId: String) =
            Intent(context, StreamScreenShareService::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
            }
    }
}
