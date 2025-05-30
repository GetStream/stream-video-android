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
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CallStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import io.getstream.android.push.permissions.DefaultNotificationPermissionHandler
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ForegroundDetector
import io.getstream.video.android.core.R
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_MISSED_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope

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

    /**
     * [NotificationChannelManager]: Handles all notification channel creation and management
     * [CallNotificationBuilder]: Responsible for building different types of notifications
     * [ForegroundDetector]: Detects if the app is in foreground
     * [NotificationUpdater]: Handles real-time notification updates during calls
     */

    private val intentResolver = DefaultStreamIntentResolver(application)
    private val channelManager = NotificationChannelManager(application)
    private val notificationBuilder = CallNotificationBuilder(application, notificationIconRes)
    private val foregroundDetector = ForegroundDetector()
    private val notificationUpdater = NotificationUpdater(application)

    protected val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(application).also { manager ->
            channelManager.createDefaultChannels(manager)
        }
    }

    override fun onRingingCall(callId: StreamCallId, callDisplayName: String) {
        logger.d { "[onRingingCall] #ringing; callId: ${callId.id}" }
        CallService.showIncomingCall(
            application,
            callId,
            callDisplayName,
            StreamVideo.instance().state.callConfigRegistry.get(callId.type),
            notification = getRingingCallNotification(
                RingingState.Incoming(),
                callId,
                callDisplayName,
                shouldHaveContentIntent = true,
            ),
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
        return when (ringingState) {
            is RingingState.Incoming -> createIncomingCallNotification(
                callId,
                callDisplayName,
                shouldHaveContentIntent,
            )

            is RingingState.Outgoing -> createOutgoingCallNotification(callId, callDisplayName)
            else -> null
        }
    }

    private fun createIncomingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification? {
        val fullScreenIntent = intentResolver.searchIncomingCallPendingIntent(callId)
        val acceptIntent = intentResolver.searchAcceptCallPendingIntent(callId)
        val rejectIntent = intentResolver.searchRejectCallPendingIntent(callId)

        return if (fullScreenIntent != null && acceptIntent != null && rejectIntent != null) {
            getIncomingCallNotification(
                fullScreenPendingIntent = fullScreenIntent,
                acceptCallPendingIntent = acceptIntent,
                rejectCallPendingIntent = rejectIntent,
                callerName = callDisplayName,
                shouldHaveContentIntent = shouldHaveContentIntent,
            )
        } else {
            logger.e { "Ringing call notification not shown, one of the intents is null." }
            null
        }
    }

    private fun createOutgoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
    ): Notification? {
        val outgoingIntent = intentResolver.searchOutgoingCallPendingIntent(callId)
        val endIntent = intentResolver.searchEndCallPendingIntent(callId)

        return if (outgoingIntent != null && endIntent != null) {
            getOngoingCallNotification(
                callId = callId,
                callDisplayName = callDisplayName,
                isOutgoingCall = true,
            )
        } else {
            logger.e { "Ringing call notification not shown, one of the intents is null." }
            null
        }
    }

    override fun getSettingUpCallNotification(): Notification? {
        return notificationBuilder.buildCallSetupNotification()
    }

    override fun getIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification {
        val showAsHighPriority =
            !hideRingingNotificationInForeground || !foregroundDetector.isInForeground()

        return notificationBuilder.buildIncomingCallNotification(
            fullScreenPendingIntent = fullScreenPendingIntent,
            acceptCallPendingIntent = acceptCallPendingIntent,
            rejectCallPendingIntent = rejectCallPendingIntent,
            callerName = callerName,
            shouldHaveContentIntent = shouldHaveContentIntent,
            showAsHighPriority = showAsHighPriority,
        )
    }

    open fun createIncomingCallChannel(channelId: String, showAsHighPriority: Boolean) {
        channelManager.createIncomingCallChannel(channelId, showAsHighPriority)
    }

    override fun onNotification(callId: StreamCallId, callDisplayName: String) {
        val notificationId = callId.hashCode()
        val intent = intentResolver.searchNotificationCallPendingIntent(callId, notificationId)

        if (intent != null) {
            showNotificationCallNotification(intent, callDisplayName, notificationId)
        } else {
            logger.e { "Couldn't find any activity for ACTION_NOTIFICATION" }
        }
    }

    override fun onLiveCall(callId: StreamCallId, callDisplayName: String) {
        val notificationId = callId.hashCode()
        val intent = intentResolver.searchLiveCallPendingIntent(callId, notificationId)

        if (intent != null) {
            showLiveCallNotification(intent, callDisplayName, notificationId)
        } else {
            logger.e { "Couldn't find any activity for ACTION_LIVE_CALL" }
        }
    }

    override fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
    ): Notification? {
        val notificationId = callId.hashCode()

        val onClickIntent = if (isOutgoingCall) {
            intentResolver.searchOutgoingCallPendingIntent(callId, notificationId)
        } else {
            intentResolver.searchOngoingCallPendingIntent(callId, notificationId)
        }

        val hangUpIntent = if (isOutgoingCall) {
            intentResolver.searchRejectCallPendingIntent(callId)
        } else {
            intentResolver.searchEndCallPendingIntent(callId)
        }

        if (hangUpIntent == null) {
            logger.e { "End call intent is null, not showing notification!" }
            return null
        }

        return notificationBuilder.buildOngoingCallNotification(
            callDisplayName = callDisplayName,
            isOutgoingCall = isOutgoingCall,
            remoteParticipantCount = remoteParticipantCount,
            onClickIntent = onClickIntent,
            hangUpIntent = hangUpIntent,
        )
    }

    open fun createOnGoingChannel(ongoingCallsChannelId: String) {
        channelManager.createOngoingCallChannel(ongoingCallsChannelId)
    }

    override fun getNotificationUpdates(
        coroutineScope: CoroutineScope,
        call: Call,
        localUser: User,
        onUpdate: (Notification) -> Unit,
    ) {
        notificationUpdater.startNotificationUpdates(
            coroutineScope = coroutineScope,
            call = call,
            localUser = localUser,
            onUpdate = onUpdate,
            getOngoingNotification = ::getOngoingCallNotification,
        )
    }

    open fun maybeCreateChannel(
        channelId: String,
        context: Context,
        configure: NotificationChannel.() -> Unit = {
        },
    ) {
        channelManager.maybeCreateChannel(channelId, context, configure)
    }

    @SuppressLint("MissingPermission")
    open fun showNotificationCallNotification(
        notificationPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int,
    ) {
        val notification = notificationBuilder.buildSimpleNotification(
            title = "Incoming call",
            text = "$callDisplayName is calling you.",
            intent = notificationPendingIntent,
        )
        notificationManager.notify(notificationId, notification)
    }

    @SuppressLint("MissingPermission")
    open fun showMissedCallNotification(
        notificationPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int,
    ) {
        val notification = notificationBuilder.buildSimpleNotification(
            title = "Missed call from $callDisplayName",
            text = null,
            intent = notificationPendingIntent,
        )
        notificationManager.notify(notificationId, notification)
    }

    @SuppressLint("MissingPermission")
    open fun showLiveCallNotification(
        liveCallPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int,
    ) {
        val notification = notificationBuilder.buildSimpleNotification(
            title = "Live Call",
            text = "$callDisplayName is live now",
            intent = liveCallPendingIntent,
        )
        notificationManager.notify(notificationId, notification)
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
        return notificationBuilder.buildCustomNotification(builder)
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
        return notificationBuilder.createLeaveAction(intent)
    }

    open fun getAcceptAction(intent: PendingIntent): NotificationCompat.Action {
        return notificationBuilder.createAcceptAction(intent)
    }

    open fun getRejectAction(intent: PendingIntent): NotificationCompat.Action {
        return notificationBuilder.createRejectAction(intent)
    }

    open fun isInForeground(): Boolean {
        return foregroundDetector.isInForeground()
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
