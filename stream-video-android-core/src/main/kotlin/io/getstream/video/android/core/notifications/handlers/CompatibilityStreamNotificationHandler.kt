/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.handlers

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.android.push.permissions.DefaultNotificationPermissionHandler
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.R
import io.getstream.video.android.core.notifications.DefaultStreamIntentResolver
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.StreamIntentResolver
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationConfig
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationContent
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationVisuals
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope

/**
 * This class is for compatibility with the old notification handler.
 */
open class CompatibilityStreamNotificationHandler(
    private val application: Application,
    notificationPermissionHandler: NotificationPermissionHandler = DefaultNotificationPermissionHandler.createDefaultNotificationPermissionHandler(
        application,
    ),
    notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(
        application.applicationContext,
    ),
    intentResolver: StreamIntentResolver = DefaultStreamIntentResolver(application),
    hideRingingNotificationInForeground: Boolean = false,
    initialNotificationBuilderInterceptor: StreamNotificationBuilderInterceptors =
        StreamNotificationBuilderInterceptors(),
    updateNotificationBuilderInterceptor: StreamNotificationUpdateInterceptors =
        StreamNotificationUpdateInterceptors(),
    notificationChannels: StreamNotificationChannels = StreamNotificationChannels(
        incomingCallChannel = createChannelInfoFromResIds(
            application.applicationContext,
            R.string.stream_video_incoming_call_notification_channel_id,
            R.string.stream_video_incoming_call_notification_channel_title,
            R.string.stream_video_incoming_call_notification_channel_description,
            NotificationManager.IMPORTANCE_HIGH,
        ),
        ongoingCallChannel = createChannelInfoFromResIds(
            application.applicationContext,
            R.string.stream_video_ongoing_call_notification_channel_id,
            R.string.stream_video_ongoing_call_notification_channel_title,
            R.string.stream_video_ongoing_call_notification_channel_description,
            NotificationManager.IMPORTANCE_DEFAULT,
        ),
        outgoingCallChannel = createChannelInfoFromResIds(
            application.applicationContext,
            R.string.stream_video_outgoing_call_notification_channel_id,
            R.string.stream_video_outgoing_call_notification_channel_title,
            R.string.stream_video_outgoing_call_notification_channel_description,
            NotificationManager.IMPORTANCE_DEFAULT,
        ),
        missedCallChannel = createChannelInfoFromResIds(
            application.applicationContext,
            R.string.stream_video_missed_call_notification_channel_id,
            R.string.stream_video_missed_call_notification_channel_title,
            R.string.stream_video_missed_call_notification_channel_description,
            NotificationManager.IMPORTANCE_HIGH,
        ),
    ),
) : NotificationHandler, StreamDefaultNotificationHandler(
    application,
    notificationManager,
    notificationPermissionHandler,
    intentResolver,
    hideRingingNotificationInForeground,
    initialNotificationBuilderInterceptor,
    updateNotificationBuilderInterceptor,
    notificationChannels,
) {

    // Deprecated methods for compatibility
    @Deprecated(
        message = "Deprecated method. Use the onCallNotificationUpdate method instead.",
        level = DeprecationLevel.ERROR,
        replaceWith =
        ReplaceWith("onCallNotificationUpdate(call)"),
    )
    override fun getNotificationUpdates(
        coroutineScope: CoroutineScope,
        call: Call,
        localUser: User,
        onUpdate: (Notification) -> Unit,
    ) {
        // Do nothing, deprecated
    }

    @Deprecated(
        "This interface is deprecated. Use the notification interceptors instead.",
        level = DeprecationLevel.ERROR,
    )
    override fun createMinimalMediaStyleNotification(
        callId: StreamCallId,
        mediaNotificationConfig: MediaNotificationConfig,
        remoteParticipantCount: Int,
    ): NotificationCompat.Builder? = null

    @Deprecated(
        "This interface is deprecated. Use the notification interceptors instead.",
        level = DeprecationLevel.ERROR,
    )
    override fun getMediaNotificationConfig(): MediaNotificationConfig {
        return MediaNotificationConfig(
            MediaNotificationContent("", ""),
            MediaNotificationVisuals(android.R.drawable.ic_media_play, null),
            null,
        )
    }
}
