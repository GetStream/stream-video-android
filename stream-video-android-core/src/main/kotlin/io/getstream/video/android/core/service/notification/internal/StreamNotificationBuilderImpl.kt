/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.service.notification.internal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.R
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.service.notification.IdentifiedNotification
import io.getstream.video.android.core.service.notification.StreamNotificationBuilder
import io.getstream.video.android.core.utils.notificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.getstream.video.android.core.model.state.StreamCallState as State

internal class StreamNotificationBuilderImpl(
    private val context: Context,
    private val streamVideo: StreamVideo,
    private val scope: CoroutineScope,
    private val getNotificationId: () -> Int
) : StreamNotificationBuilder {

    private val logger by taggedLogger("Call:NtfBuilder")

    private val actionBuilder: NotificationActionBuilder by lazy {
        NotificationActionBuilderImpl(
            context
        )
    }
    private val actionReceiver: NotificationActionReceiver by lazy {
        NotificationActionReceiverImpl(
            context
        )
    }

    init {
        initNotificationChannel()
        observeAction()
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = getDefaultNotificationChannel(context)
            context.notificationManager.createNotificationChannel(notificationChannel())
        }
    }

    private fun observeAction() {
        scope.launch {
            actionReceiver.registerAsFlow().collect {
                logger.v { "[observeAction] action: $it" }
//                when (it) {
// //                    is NotificationAction.Accept -> streamVideo.acceptCall(it.guid.type, it.guid.id)
// //                    is NotificationAction.Reject -> streamVideo.rejectCall(it.guid.type, it.guid.id)
// //                    is NotificationAction.Cancel -> streamVideo.cancelCall(it.guid.type, it.guid.id)
//                }
            }
        }
    }

    override fun build(state: State.Active): IdentifiedNotification {
        val notificationId = getNotificationId()
        val notification = getNotificationBuilder(
            contentTitle = getContentTitle(state),
            contentText = getContentText(state),
            groupKey = state.callGuid.cid,
            intent = Intent(Intent.ACTION_CALL)
        ).apply {
            buildNotificationActions(notificationId, state).forEach {
                addAction(it)
            }
        }.build()
        return IdentifiedNotification(notificationId, notification)
    }

    private fun buildNotificationActions(
        notificationId: Int,
        state: State.Active
    ): Array<NotificationCompat.Action> {
        return when (state) {
            is State.Incoming -> arrayOf(
                actionBuilder.createRejectAction(notificationId, state.callGuid),
                actionBuilder.createAcceptAction(notificationId, state.callGuid)
            )
            is State.Outgoing,
            is State.Joining,
            is State.InCall -> arrayOf(
                actionBuilder.createCancelAction(notificationId, state.callGuid)
            )
            is State.Drop -> emptyArray()
        }
    }

    private fun getContentTitle(state: State.Active): String {
        return "${state.callGuid.id}: " + when (state) {
            is State.Outgoing -> "Outgoing Call"
            is State.Incoming -> "Incoming"
            is State.Joining -> "Joining"
            is State.Joined -> "Joined"
            is State.Connecting -> "Connecting"
            is State.Connected -> "Connected"
            is State.Drop -> "Drop"
        }
    }

    private fun getContentText(state: State.Active): String {
        return when (state) {
            is State.Started -> state.users.values.filter { it.id != state.createdByUserId }
                .joinToString { it.name }
            is State.Drop -> ""
        }
    }

    private fun getNotificationBuilder(
        contentTitle: String,
        contentText: String,
        groupKey: String,
        intent: Intent,
    ): NotificationCompat.Builder {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            flags,
        )

        return NotificationCompat.Builder(context, getNotificationChannelId())
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(false)
            .setSmallIcon(R.drawable.stream_video_ic_call)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(contentIntent)
            .setGroup(groupKey)
    }

    private fun getNotificationChannelId(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getDefaultNotificationChannel(context)().id
        } else {
            ""
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDefaultNotificationChannel(context: Context): (() -> NotificationChannel) {
        return {
            NotificationChannel(
                context.getString(R.string.stream_video_call_notification_channel_id),
                context.getString(R.string.stream_video_call_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        }
    }
}
