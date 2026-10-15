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

package io.getstream.video.android.tutorial.livestream

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.getstream.android.push.permissions.DefaultNotificationPermissionHandler
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.video.android.core.notifications.handlers.CompatibilityStreamNotificationHandler
import io.getstream.video.android.core.notifications.handlers.StreamNotificationBuilderInterceptors
import io.getstream.video.android.model.StreamCallId

public open class LiveStreamNotificationHandler(
    application: Application,
    notificationPermissionHandler: NotificationPermissionHandler =
        DefaultNotificationPermissionHandler
            .createDefaultNotificationPermissionHandler(application),
    hideRingingNotificationInForeground: Boolean = false,
) : CompatibilityStreamNotificationHandler(
    application = application,
    notificationPermissionHandler = notificationPermissionHandler,
    hideRingingNotificationInForeground = hideRingingNotificationInForeground,
    initialNotificationBuilderInterceptor = LiveStreamNotificationInterceptors(application),
)

/**
 * Customizes the media-style notification shown for livestream calls (call types listed in
 * `NotificationConfig.mediaNotificationCallTypes`, which includes `livestream` by default).
 */
private class LiveStreamNotificationInterceptors(
    private val application: Application,
) : StreamNotificationBuilderInterceptors() {

    override fun onBuildOngoingCallMediaNotification(
        builder: NotificationCompat.Builder,
        callId: StreamCallId,
        payload: Map<String, Any?>,
    ): NotificationCompat.Builder {
        return builder
            .setContentTitle("Livestream on progress ")
            .setContentText("Tap to go back the livestream call")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(
                getBitmapFromDrawable(
                    application.applicationContext,
                    R.drawable.getstream_mountain,
                ),
            )
    }

    private fun getBitmapFromDrawable(context: Context, drawableResId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableResId)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
