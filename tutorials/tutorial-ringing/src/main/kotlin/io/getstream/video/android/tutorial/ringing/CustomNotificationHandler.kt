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

package io.getstream.video.android.tutorial.ringing

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.getstream.android.samples.ringingcall.R
import io.getstream.video.android.core.notifications.handlers.CompatibilityStreamNotificationHandler
import io.getstream.video.android.core.notifications.handlers.StreamNotificationBuilderInterceptors

class CustomNotificationHandler(
    application: Application,
) : CompatibilityStreamNotificationHandler(
    application = application,
    hideRingingNotificationInForeground = true,
    initialNotificationBuilderInterceptor = CustomNotificationInterceptors(application),
)

private class CustomNotificationInterceptors(
    private val application: Application,
) : StreamNotificationBuilderInterceptors() {

    override fun onBuildMissedCallNotification(
        builder: NotificationCompat.Builder,
        callDisplayName: String?,
        payload: Map<String, Any?>,
    ): NotificationCompat.Builder {
        return builder
            .setSmallIcon(R.drawable.round_call_missed_24)
            .setContentIntent(buildContentIntent())
            .setContentTitle("Tutorial Missed Call from $callDisplayName")
            .setAutoCancel(true)
    }

    private fun buildContentIntent() = PendingIntent.getActivity(
        application,
        0,
        Intent(application, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
