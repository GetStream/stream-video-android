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

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CallStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import io.getstream.android.push.permissions.DefaultNotificationPermissionHandler
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.R
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_LIVE_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_MISSED_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_NOTIFICATION
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.DefaultStreamIntentResolver
import io.getstream.video.android.core.telecom.TelecomCompat
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

public open class DefaultNotificationHandler(
    private val application: Application,
    private val notificationPermissionHandler: NotificationPermissionHandler =
        DefaultNotificationPermissionHandler
            .createDefaultNotificationPermissionHandler(application),
    /**
     * Set this to true if you want to make the ringing notifications as low-priority
     * in case the application is in foreground. This will prevent the notification from
     * interrupting the user while he is in the app. In this case you need to make sure to
     * handle this call state and display an incoming call screen.
     */
    val hideRingingNotificationInForeground: Boolean = false,

    /**
     * The notification icon for call notifications.
     */
    @DrawableRes val notificationIconRes: Int = android.R.drawable.ic_menu_call,
) : NotificationHandler,
    NotificationPermissionHandler by notificationPermissionHandler {

    private val logger by taggedLogger("Call:NotificationHandler")
    val intentResolver = DefaultStreamIntentResolver(application)
    protected val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(application).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.createNotificationChannel(
                    NotificationChannelCompat
                        .Builder(
                            getChannelId(),
                            NotificationManager.IMPORTANCE_HIGH,
                        )
                        .setName(getChannelName())
                        .setDescription(getChannelDescription())
                        .build(),
                )
            }
        }
    }

    override fun onRingingCall(callId: StreamCallId, callDisplayName: String) {
        logger.d { "[onRingingCall] #ringing; callId: ${callId.id}" }

        TelecomCompat.registerCall(
            application,
            callId = callId,
            callDisplayName = callDisplayName,
            isIncomingCall = true,
        )
    }

    override fun onMissedCall(callId: StreamCallId, callDisplayName: String) {
        logger.d { "[onMissedCall] #ringing; callId: ${callId.id}" }
        val notificationId = callId.hashCode()
        val intent = intentResolver.searchMissedCallPendingIntent(callId, notificationId)
            ?: run {
                logger.e { "Couldn't find any activity for $ACTION_MISSED_CALL" }
                intentResolver.getDefaultPendingIntent()
            }
        showMissedCallNotification(
            intent,
            callDisplayName,
            notificationId,
        )
    }

    override fun getRingingCallNotification(
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification? {
        return if (ringingState is RingingState.Incoming) {
            val fullScreenPendingIntent = intentResolver.searchIncomingCallPendingIntent(callId)
            val acceptCallPendingIntent = intentResolver.searchAcceptCallPendingIntent(callId)
            val rejectCallPendingIntent = intentResolver.searchRejectCallPendingIntent(callId)

            if (fullScreenPendingIntent != null && acceptCallPendingIntent != null && rejectCallPendingIntent != null) {
                getIncomingCallNotification(
                    fullScreenPendingIntent,
                    acceptCallPendingIntent,
                    rejectCallPendingIntent,
                    callDisplayName,
                    shouldHaveContentIntent,
                )
            } else {
                logger.e { "Ringing call notification not shown, one of the intents is null." }
                null
            }
        } else if (ringingState is RingingState.Outgoing) {
            val outgoingCallPendingIntent = intentResolver.searchOutgoingCallPendingIntent(callId)
            val endCallPendingIntent = intentResolver.searchEndCallPendingIntent(callId)

            if (outgoingCallPendingIntent != null && endCallPendingIntent != null) {
                getOngoingCallNotification(
                    callId,
                    callDisplayName,
                    isOutgoingCall = true,
                )
            } else {
                logger.e { "Ringing call notification not shown, one of the intents is null." }
                null
            }
        } else {
            null
        }
    }

    override fun getSettingUpCallNotification(): Notification? {
        val channelId = application.getString(
            R.string.stream_video_call_setup_notification_channel_id,
        )

        maybeCreateChannel(
            channelId = channelId,
            context = application,
            configure = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    name = application.getString(
                        R.string.stream_video_call_setup_notification_channel_title,
                    )
                    description = application.getString(
                        R.string.stream_video_call_setup_notification_channel_description,
                    )
                }
            },
        )

        return getNotification {
            setContentTitle(
                application.getString(R.string.stream_video_call_setup_notification_title),
            )
            setContentText(
                application.getString(R.string.stream_video_call_setup_notification_description),
            )
            setChannelId(channelId)
            setCategory(NotificationCompat.CATEGORY_CALL)
            setOngoing(true)
        }
    }

    override fun getIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification {
        // if the app is in foreground then don't interrupt the user with a high priority
        // notification (popup). The application will display an incoming ringing call
        // screen instead - but this needs to be handled by the application.
        // The default behaviour is that all notification are high priority
        val showAsHighPriority = !hideRingingNotificationInForeground || !isInForeground()
        val channelId = application.getString(
            if (showAsHighPriority) {
                R.string.stream_video_incoming_call_notification_channel_id
            } else {
                R.string.stream_video_incoming_call_low_priority_notification_channel_id
            },
        )

        createIncomingCallChannel(channelId, showAsHighPriority)

        return getNotification {
            priority = NotificationCompat.PRIORITY_HIGH
            setContentTitle(callerName)
            setContentText(
                application.getString(R.string.stream_video_incoming_call_notification_description),
            )
            setChannelId(channelId)
            setOngoing(true)
            setCategory(NotificationCompat.CATEGORY_CALL)
            setFullScreenIntent(fullScreenPendingIntent, true)
            if (shouldHaveContentIntent) {
                setContentIntent(fullScreenPendingIntent)
            } else {
                val emptyIntent = PendingIntent.getActivity(
                    application,
                    0,
                    Intent(),
                    PendingIntent.FLAG_IMMUTABLE,
                )
                setContentIntent(emptyIntent)
                setAutoCancel(false)
            }
            addCallActions(acceptCallPendingIntent, rejectCallPendingIntent, callerName)
        }
    }

    open fun createIncomingCallChannel(channelId: String, showAsHighPriority: Boolean) {
        maybeCreateChannel(
            channelId = channelId,
            context = application,
            configure = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    name = application.getString(
                        R.string.stream_video_incoming_call_notification_channel_title,
                    )
                    description = application.getString(
                        if (showAsHighPriority) {
                            R.string.stream_video_incoming_call_notification_channel_description
                        } else {
                            R.string.stream_video_incoming_call_low_priority_notification_channel_description
                        },
                    )
                    importance = if (showAsHighPriority) {
                        NotificationManager.IMPORTANCE_HIGH
                    } else {
                        NotificationManager.IMPORTANCE_LOW
                    }
                    this.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    this.setShowBadge(true)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.setAllowBubbles(true)
                }
            },
        )
    }

    override fun onNotification(callId: StreamCallId, callDisplayName: String) {
        val notificationId = callId.hashCode()
        intentResolver.searchNotificationCallPendingIntent(callId, notificationId)
            ?.let { notificationPendingIntent ->
                showNotificationCallNotification(
                    notificationPendingIntent,
                    callDisplayName,
                    notificationId,
                )
            } ?: logger.e { "Couldn't find any activity for $ACTION_NOTIFICATION" }
    }

    override fun onLiveCall(callId: StreamCallId, callDisplayName: String) {
        val notificationId = callId.hashCode()
        intentResolver.searchLiveCallPendingIntent(callId, notificationId)
            ?.let { liveCallPendingIntent ->
                showLiveCallNotification(
                    liveCallPendingIntent,
                    callDisplayName,
                    notificationId,
                )
            } ?: logger.e { "Couldn't find any activity for $ACTION_LIVE_CALL" }
    }

    override fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
    ): Notification? {
        val notificationId = callId.hashCode() // Notification ID

        // Intents
        val onClickIntent = if (isOutgoingCall) {
            intentResolver.searchOutgoingCallPendingIntent(
                callId,
                notificationId,
            )
        } else {
            intentResolver.searchOngoingCallPendingIntent(
                callId,
                notificationId,
            )
        }
        val hangUpIntent = if (isOutgoingCall) {
            intentResolver.searchRejectCallPendingIntent(callId)
        } else {
            intentResolver.searchEndCallPendingIntent(callId)
        }

        // Channel preparation
        val ongoingCallsChannelId = application.getString(
            R.string.stream_video_ongoing_call_notification_channel_id,
        )

        createOnGoingChannel(ongoingCallsChannelId)

        if (hangUpIntent == null) {
            logger.e { "End call intent is null, not showing notification!" }
            return null
        }

        // Build notification
        return NotificationCompat.Builder(application, ongoingCallsChannelId)
            .setSmallIcon(notificationIconRes)
            .also {
                // If the intent is configured, clicking the notification will return to the call
                if (onClickIntent != null) {
                    it.setContentIntent(onClickIntent)
                } else {
                    logger.w { "Ongoing intent is null. Clicking on the ongoing call notification will not work." }
                }
            }
            .setContentTitle(
                if (isOutgoingCall) {
                    application.getString(R.string.stream_video_outgoing_call_notification_title)
                } else {
                    application.getString(R.string.stream_video_ongoing_call_notification_title)
                },
            )
            .setContentText(
                application.getString(R.string.stream_video_ongoing_call_notification_description),
            )
            .setAutoCancel(false)
            .setOngoing(true)
            .addHangUpAction(
                hangUpIntent,
                callDisplayName ?: application.getString(
                    R.string.stream_video_ongoing_call_notification_title,
                ),
                remoteParticipantCount,
            )
            .build()
    }

    open fun createOnGoingChannel(ongoingCallsChannelId: String) {
        maybeCreateChannel(
            channelId = ongoingCallsChannelId,
            context = application,
            configure = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    name = application.getString(
                        R.string.stream_video_ongoing_call_notification_channel_title,
                    )
                    description =
                        application.getString(R.string.stream_video_ongoing_call_notification_channel_description)
                }
            },
        )
    }

    override fun getNotificationUpdates(
        coroutineScope: CoroutineScope,
        call: Call,
        localUser: User,
        onUpdate: (Notification) -> Unit,
    ) {
        coroutineScope.launch {
            var latestRemoteParticipantCount = -1

            // Monitor call state and remote participants
            combine(
                call.state.ringingState,
                call.state.members,
                call.state.remoteParticipants,
            ) { ringingState, members, remoteParticipants ->
                Triple(ringingState, members, remoteParticipants)
            }
                .distinctUntilChanged()
                .filter { it.first is RingingState.Active || it.first is RingingState.Outgoing }
                .collectLatest { state ->
                    val ringingState = state.first
                    val members = state.second
                    val remoteParticipants = state.third

                    if (ringingState is RingingState.Outgoing) {
                        val remoteMembersCount = members.size - 1

                        val callDisplayName = if (remoteMembersCount != 1) {
                            application.getString(
                                R.string.stream_video_outgoing_call_notification_title,
                            )
                        } else {
                            members.firstOrNull { member ->
                                member.user.id != localUser.id
                            }?.user?.name ?: "Unknown"
                        }

                        getOngoingCallNotification(
                            callId = StreamCallId.fromCallCid(call.cid),
                            callDisplayName = callDisplayName,
                            isOutgoingCall = true,
                            remoteParticipantCount = remoteMembersCount,
                        )?.let {
                            onUpdate(it)
                        }
                    } else if (ringingState is RingingState.Active) {
                        // If number of remote participants increased or decreased
                        if (remoteParticipants.size != latestRemoteParticipantCount) {
                            latestRemoteParticipantCount = remoteParticipants.size

                            val callDisplayName = if (remoteParticipants.isEmpty()) {
                                // If no remote participants, get simple call notification title
                                application.getString(
                                    R.string.stream_video_ongoing_call_notification_title,
                                )
                            } else {
                                if (remoteParticipants.size > 1) {
                                    // If more than 1 remote participant, get group call notification title
                                    application.getString(
                                        R.string.stream_video_ongoing_group_call_notification_title,
                                    )
                                } else {
                                    // If 1 remote participant, get the name of the remote participant
                                    remoteParticipants.firstOrNull()?.name?.value ?: "Unknown"
                                }
                            }

                            // Use latest call display name in notification
                            getOngoingCallNotification(
                                callId = StreamCallId.fromCallCid(call.cid),
                                callDisplayName = callDisplayName,
                                remoteParticipantCount = remoteParticipants.size,
                            )?.let {
                                onUpdate(it)
                            }
                        }
                    }
                }
        }
    }

    open fun maybeCreateChannel(
        channelId: String,
        context: Context,
        configure: NotificationChannel.() -> Unit = {
        },
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                application.getString(
                    R.string.stream_video_ongoing_call_notification_channel_title,
                ),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply(configure)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    open fun showNotificationCallNotification(
        notificationPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int,
    ) {
        showNotification(notificationId) {
            setContentTitle("Incoming call")
            setContentText("$callDisplayName is calling you.")
            setContentIntent(notificationPendingIntent)
        }
    }

    open fun showMissedCallNotification(
        notificationPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int,
    ) {
        showNotification(notificationId) {
            setContentTitle("Missed call from $callDisplayName")
            setContentIntent(notificationPendingIntent)
        }
    }

    open fun showLiveCallNotification(
        liveCallPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int,
    ) {
        showNotification(notificationId) {
            setContentTitle("Live Call")
            setContentText("$callDisplayName is live now")
            setContentIntent(liveCallPendingIntent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        notificationId: Int,
        builder: NotificationCompat.Builder.() -> Unit,
    ) {
        val notification = getNotification(builder)
        notificationManager.notify(notificationId, notification)
    }

    open fun getNotification(
        builder: NotificationCompat.Builder.() -> Unit,
    ): Notification {
        return NotificationCompat.Builder(application, getChannelId())
            .setSmallIcon(notificationIconRes)
            .setAutoCancel(true)
            .apply(builder)
            .build()
    }

    open fun NotificationCompat.Builder.addHangUpAction(
        hangUpIntent: PendingIntent,
        callDisplayName: String,
        remoteParticipantCount: Int,
    ): NotificationCompat.Builder = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setStyle(
                CallStyle.forOngoingCall(
                    Person.Builder()
                        .setName(callDisplayName)
                        .apply {
                            if (remoteParticipantCount == 0) {
                                // Just one user in the call
                                setIcon(
                                    IconCompat.createWithResource(
                                        application,
                                        R.drawable.stream_video_ic_user,
                                    ),
                                )
                            } else if (remoteParticipantCount > 1) {
                                // More than one user in the call
                                setIcon(
                                    IconCompat.createWithResource(
                                        application,
                                        R.drawable.stream_video_ic_user_group,
                                    ),
                                )
                            }
                        }
                        .build(),
                    hangUpIntent,
                ),
            )
        } else {
            addAction(getLeaveAction(hangUpIntent))
        }
    }

    open fun NotificationCompat.Builder.addCallActions(
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callDisplayName: String?,
    ): NotificationCompat.Builder = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setStyle(
                CallStyle.forIncomingCall(
                    Person.Builder()
                        .setName(callDisplayName ?: "Unknown")
                        .apply {
                            if (callDisplayName == null) {
                                setIcon(
                                    IconCompat.createWithResource(
                                        application,
                                        R.drawable.stream_video_ic_user,
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
            addAction(getAcceptAction(acceptCallPendingIntent))
            addAction(getRejectAction(rejectCallPendingIntent))
        }
    }

    open fun getLeaveAction(intent: PendingIntent): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            null,
            application.getString(R.string.stream_video_call_notification_action_leave),
            intent,
        ).build()
    }

    open fun getAcceptAction(intent: PendingIntent): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            null,
            application.getString(R.string.stream_video_call_notification_action_accept),
            intent,
        ).build()
    }

    open fun getRejectAction(intent: PendingIntent): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            null,
            application.getString(R.string.stream_video_call_notification_action_reject),
            intent,
        ).build()
    }

    open fun isInForeground(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return (
            appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
            )
    }

    open fun getChannelId(): String = application.getString(
        R.string.stream_video_incoming_call_notification_channel_id,
    )

    open fun getChannelName(): String = application.getString(
        R.string.stream_video_incoming_call_notification_channel_title,
    )

    open fun getChannelDescription(): String = application.getString(
        R.string.stream_video_incoming_call_notification_channel_description,
    )

    companion object {

        internal val PENDING_INTENT_FLAG: Int by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        }
    }
}
