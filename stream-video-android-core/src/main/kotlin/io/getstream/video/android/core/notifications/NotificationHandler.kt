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

package io.getstream.video.android.core.notifications

import android.app.Notification
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.notifications.dispatchers.NotificationDispatcher
import io.getstream.video.android.core.notifications.handlers.StreamNotificationHandler
import io.getstream.video.android.core.notifications.handlers.StreamNotificationProvider
import io.getstream.video.android.core.notifications.handlers.StreamNotificationUpdatesProvider
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationHandler
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope

public interface NotificationHandler :
    NotificationPermissionHandler,
    StreamNotificationHandler,
    StreamNotificationProvider,
    StreamNotificationUpdatesProvider,
    StreamNotificationDispatcher,
    MediaNotificationHandler {

    /**
     * Get subsequent updates to notifications.
     * Initially, notifications are posted by one of the other methods, and then this method can be used to re-post them with updated content.
     *
     * @param coroutineScope Coroutine scope used for the updates.
     * @param call The Stream call object.
     * @param localUser The local Stream user.
     * @param onUpdate Callback to be called when the notification is updated.
     */
    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "This method is deprecated. Use the getNotificationUpdates method in the NotificationHandler interface instead.",
        replaceWith = ReplaceWith("onCallNotificationUpdate(coroutineScope, call, localUser)"),
    )
    fun getNotificationUpdates(
        coroutineScope: CoroutineScope,
        call: Call,
        localUser: User,
        onUpdate: (Notification) -> Unit,
    )

    companion object {
        const val ACTION_NOTIFICATION = "io.getstream.video.android.action.NOTIFICATION"
        const val ACTION_MISSED_CALL = "io.getstream.video.android.action.MISSED_CALL"
        const val ACTION_LIVE_CALL = "io.getstream.video.android.action.LIVE_CALL"
        const val ACTION_INCOMING_CALL = "io.getstream.video.android.action.INCOMING_CALL"
        const val ACTION_OUTGOING_CALL = "io.getstream.video.android.action.OUTGOING_CALL"
        const val ACTION_ACCEPT_CALL = "io.getstream.video.android.action.ACCEPT_CALL"
        const val ACTION_REJECT_CALL = "io.getstream.video.android.action.REJECT_CALL"
        const val ACTION_LEAVE_CALL = "io.getstream.video.android.action.LEAVE_CALL"
        const val ACTION_ONGOING_CALL = "io.getstream.video.android.action.ONGOING_CALL"
        const val INTENT_EXTRA_CALL_CID: String = "io.getstream.video.android.intent-extra.call_cid"
        const val INTENT_EXTRA_CALL_DISPLAY_NAME: String =
            "io.getstream.video.android.intent-extra.call_displayname"
        const val INTENT_EXTRA_IS_VIDEO: String =
            "io.getstream.video.android.intent-extra.is_video"

        const val INTENT_EXTRA_NOTIFICATION_ID: String =
            "io.getstream.video.android.intent-extra.notification_id"

        @Deprecated(
            message = "Use StreamCallId.getNotificationId(NotificationType.Incoming)",
            replaceWith = ReplaceWith("StreamCallId.getNotificationId(NotificationType.Incoming)"),
        )
        const val INCOMING_CALL_NOTIFICATION_ID = 24756
    }
}

interface StreamNotificationDispatcher {
    fun getStreamNotificationDispatcher(): NotificationDispatcher
}
