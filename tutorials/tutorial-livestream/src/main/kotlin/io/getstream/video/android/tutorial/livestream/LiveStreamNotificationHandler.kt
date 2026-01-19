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
import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import io.getstream.android.push.permissions.DefaultNotificationPermissionHandler
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.notifications.DefaultNotificationHandler
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationConfig
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationContent
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationVisuals
import io.getstream.video.android.model.StreamCallId

public open class LiveStreamNotificationHandler(

    private val application: Application,
    notificationPermissionHandler: NotificationPermissionHandler =
        DefaultNotificationPermissionHandler
            .createDefaultNotificationPermissionHandler(application),
    hideRingingNotificationInForeground: Boolean = false,
    @DrawableRes notificationIconRes: Int = android.R.drawable.ic_menu_call,
) : DefaultNotificationHandler(
    application,
    notificationPermissionHandler,
    hideRingingNotificationInForeground,
    notificationIconRes,
) {

    private val logger by taggedLogger("LiveStreamNotificationHandler")

    override fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
    ): Notification? {
        return if (callId.type.contains("livestream")) {
            createMinimalMediaStyleNotification(
                callId,
                getMediaNotificationConfig(),
                remoteParticipantCount,
            )?.build()
        } else {
            super.getOngoingCallNotification(
                callId,
                callDisplayName,
                isOutgoingCall,
                remoteParticipantCount,
            )
        }
    }

    override fun getMediaNotificationConfig(): MediaNotificationConfig {
        return MediaNotificationConfig(
            MediaNotificationContent(
                "Livestream on progress ",
                "Tap to go back the livestream call",
            ),
            MediaNotificationVisuals(
                android.R.drawable.ic_media_play,
                getBitmapFromDrawable(
                    application.applicationContext,
                    io.getstream.video.android.tutorial.livestream.R.drawable.getstream_mountain,
                ),
            ),
            null,
        )
    }
    fun getBitmapFromDrawable(context: Context, drawableResId: Int): Bitmap {
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
