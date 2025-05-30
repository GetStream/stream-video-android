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

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat

/**
 * Builds different types of call notifications
 */
internal class CallNotificationBuilder(
    private val application: Application,
    @DrawableRes private val notificationIconRes: Int,
) {
    private val channelManager = NotificationChannelManager(application)

    fun buildCallSetupNotification(): Notification {
        val channelId = application.getString(
            io.getstream.video.android.core.R.string.stream_video_call_setup_notification_channel_id,
        )
        createCallSetupChannel(channelId)

        return buildCustomNotification {
            setContentTitle(
                application.getString(
                    io.getstream.video.android.core.R.string.stream_video_call_setup_notification_title,
                ),
            )
            setContentText(
                application.getString(
                    io.getstream.video.android.core.R.string.stream_video_call_setup_notification_description,
                ),
            )
            setChannelId(channelId)
            setCategory(NotificationCompat.CATEGORY_CALL)
            setOngoing(true)
        }
    }

    fun buildIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
        showAsHighPriority: Boolean,
    ): Notification {
        val channelId = getIncomingCallChannelId(showAsHighPriority)
        channelManager.createIncomingCallChannel(channelId, showAsHighPriority)

        return buildCustomNotification {
            priority = NotificationCompat.PRIORITY_HIGH
            setContentTitle(callerName)
            setContentText(
                application.getString(
                    io.getstream.video.android.core.R.string.stream_video_incoming_call_notification_description,
                ),
            )
            setChannelId(channelId)
            setOngoing(true)
            setCategory(NotificationCompat.CATEGORY_CALL)
            setFullScreenIntent(fullScreenPendingIntent, true)
            configureContentIntent(fullScreenPendingIntent, shouldHaveContentIntent)
            addCallActions(acceptCallPendingIntent, rejectCallPendingIntent, callerName)
        }
    }

    fun buildOngoingCallNotification(
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
        onClickIntent: PendingIntent?,
        hangUpIntent: PendingIntent,
    ): Notification {
        val channelId = application.getString(
            io.getstream.video.android.core.R.string.stream_video_ongoing_call_notification_channel_id,
        )
        channelManager.createOngoingCallChannel(channelId)

        return NotificationCompat.Builder(application, channelId)
            .setSmallIcon(notificationIconRes)
            .also { builder ->
                if (onClickIntent != null) {
                    builder.setContentIntent(onClickIntent)
                }
            }
            .setContentTitle(getOngoingCallTitle(isOutgoingCall))
            .setContentText(
                application.getString(
                    io.getstream.video.android.core.R.string.stream_video_ongoing_call_notification_description,
                ),
            )
            .setAutoCancel(false)
            .setOngoing(true)
            .addHangUpAction(
                hangUpIntent,
                callDisplayName ?: application.getString(io.getstream.video.android.core.R.string.stream_video_ongoing_call_notification_title),
                remoteParticipantCount,
            )
            .build()
    }

    fun buildSimpleNotification(
        title: String,
        text: String?,
        intent: PendingIntent,
    ): Notification {
        return buildCustomNotification {
            setContentTitle(title)
            text?.let { setContentText(it) }
            setContentIntent(intent)
        }
    }

    fun buildCustomNotification(
        builder: NotificationCompat.Builder.() -> Unit,
    ): Notification {
        return NotificationCompat.Builder(application, channelManager.getDefaultChannelId())
            .setSmallIcon(notificationIconRes)
            .setAutoCancel(true)
            .apply(builder)
            .build()
    }

    private fun createCallSetupChannel(channelId: String) {
        channelManager.maybeCreateChannel(
            channelId = channelId,
            context = application,
            configure = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    name = application.getString(io.getstream.video.android.core.R.string.stream_video_call_setup_notification_channel_title)
                    description = application.getString(io.getstream.video.android.core.R.string.stream_video_call_setup_notification_channel_description)
                }
            },
        )
    }

    private fun getIncomingCallChannelId(showAsHighPriority: Boolean): String {
        return application.getString(
            if (showAsHighPriority) {
                io.getstream.video.android.core.R.string.stream_video_incoming_call_notification_channel_id
            } else {
                io.getstream.video.android.core.R.string.stream_video_incoming_call_low_priority_notification_channel_id
            },
        )
    }

    private fun NotificationCompat.Builder.configureContentIntent(
        fullScreenPendingIntent: PendingIntent,
        shouldHaveContentIntent: Boolean,
    ) {
        if (shouldHaveContentIntent) {
            setContentIntent(fullScreenPendingIntent)
        } else {
            val emptyIntent = PendingIntent.getActivity(
                application,
                0,
                android.content.Intent(),
                PendingIntent.FLAG_IMMUTABLE,
            )
            setContentIntent(emptyIntent)
            setAutoCancel(false)
        }
    }

    private fun getOngoingCallTitle(isOutgoingCall: Boolean): String {
        return application.getString(
            if (isOutgoingCall) {
                io.getstream.video.android.core.R.string.stream_video_outgoing_call_notification_title
            } else {
                io.getstream.video.android.core.R.string.stream_video_ongoing_call_notification_title
            },
        )
    }

    private fun NotificationCompat.Builder.addHangUpAction(
        hangUpIntent: PendingIntent,
        callDisplayName: String,
        remoteParticipantCount: Int,
    ): NotificationCompat.Builder = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    androidx.core.app.Person.Builder()
                        .setName(callDisplayName)
                        .apply {
                            if (remoteParticipantCount == 0) {
                                setIcon(
                                    androidx.core.graphics.drawable.IconCompat.createWithResource(
                                        application,
                                        io.getstream.video.android.core.R.drawable.stream_video_ic_user,
                                    ),
                                )
                            } else if (remoteParticipantCount > 1) {
                                setIcon(
                                    androidx.core.graphics.drawable.IconCompat.createWithResource(
                                        application,
                                        io.getstream.video.android.core.R.drawable.stream_video_ic_user_group,
                                    ),
                                )
                            }
                        }
                        .build(),
                    hangUpIntent,
                ),
            )
        } else {
            addAction(createLeaveAction(hangUpIntent))
        }
    }

    private fun NotificationCompat.Builder.addCallActions(
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callDisplayName: String?,
    ): NotificationCompat.Builder = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    androidx.core.app.Person.Builder()
                        .setName(callDisplayName ?: "Unknown")
                        .apply {
                            if (callDisplayName == null) {
                                setIcon(
                                    androidx.core.graphics.drawable.IconCompat.createWithResource(
                                        application,
                                        io.getstream.video.android.core.R.drawable.stream_video_ic_user,
                                    ),
                                )
                            }
                        }
                        .build(),
                    rejectCallPendingIntent,
                    acceptCallPendingIntent,
                ),
            )
        } else {
            addAction(createAcceptAction(acceptCallPendingIntent))
            addAction(createRejectAction(rejectCallPendingIntent))
        }
    }

    fun createLeaveAction(intent: PendingIntent): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            null,
            application.getString(
                io.getstream.video.android.core.R.string.stream_video_call_notification_action_leave,
            ),
            intent,
        ).build()
    }

    fun createAcceptAction(intent: PendingIntent): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            null,
            application.getString(
                io.getstream.video.android.core.R.string.stream_video_call_notification_action_accept,
            ),
            intent,
        ).build()
    }

    fun createRejectAction(intent: PendingIntent): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            null,
            application.getString(
                io.getstream.video.android.core.R.string.stream_video_call_notification_action_reject,
            ),
            intent,
        ).build()
    }
}
