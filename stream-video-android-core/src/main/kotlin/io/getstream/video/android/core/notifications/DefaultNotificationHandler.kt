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
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import io.getstream.video.android.core.R
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.handlers.StreamDefaultNotificationHandler
import io.getstream.video.android.core.notifications.handlers.StreamNotificationBuilderInterceptors
import io.getstream.video.android.core.notifications.handlers.StreamNotificationChannels
import io.getstream.video.android.core.notifications.handlers.StreamNotificationUpdateInterceptors
import io.getstream.video.android.core.notifications.handlers.create
import io.getstream.video.android.core.notifications.handlers.createChannelInfoFromResIds
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationConfig
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationContent
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationVisuals
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

    private val initialNotificationBuilderInterceptor: StreamNotificationBuilderInterceptors =
        StreamNotificationBuilderInterceptors(),
    private val updateNotificationBuilderInterceptor: StreamNotificationUpdateInterceptors =
        StreamNotificationUpdateInterceptors(),
    private val notificationChannels: StreamNotificationChannels = StreamNotificationChannels(
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
    private val delegate: StreamDefaultNotificationHandler = StreamDefaultNotificationHandler(
        application = application,
        hideRingingNotificationInForeground = hideRingingNotificationInForeground,
        initialNotificationBuilderInterceptor = initialNotificationBuilderInterceptor,
        updateNotificationBuilderInterceptor = updateNotificationBuilderInterceptor,
        notificationChannels = notificationChannels,
    ),
) : NotificationHandler,
    NotificationPermissionHandler by notificationPermissionHandler {

    private val logger by taggedLogger("Call:NotificationHandler")
    val intentResolver = DefaultStreamIntentResolver(application)
    protected val notificationManager: NotificationManagerCompat by lazy {
        delegate.notificationManager
    }

    override fun onRingingCall(callId: StreamCallId, callDisplayName: String) =
        delegate.onRingingCall(callId, callDisplayName)

    override fun onMissedCall(callId: StreamCallId, callDisplayName: String) =
        delegate.onMissedCall(callId, callDisplayName)

    override fun getRingingCallNotification(
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification? =
        delegate.getRingingCallNotification(
            ringingState,
            callId,
            callDisplayName,
            shouldHaveContentIntent,
        )

    override fun getMissedCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
    ): Notification? = delegate.getMissedCallNotification(callId, callDisplayName)

    override fun getIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification? =
        delegate.getIncomingCallNotification(
            fullScreenPendingIntent,
            acceptCallPendingIntent,
            rejectCallPendingIntent,
            callerName,
            shouldHaveContentIntent,
        )

    override fun getSettingUpCallNotification(): Notification? =
        delegate.getSettingUpCallNotification()

    override fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
    ): Notification? =
        delegate.getOngoingCallNotification(
            callId,
            callDisplayName,
            isOutgoingCall,
            remoteParticipantCount,
        )

    override fun onNotification(callId: StreamCallId, callDisplayName: String) =
        delegate.onNotification(callId, callDisplayName)

    override fun onLiveCall(callId: StreamCallId, callDisplayName: String) =
        delegate.onLiveCall(callId, callDisplayName)

    @Deprecated(
        "Use notificationChannels and delegate for channel management instead.",
        level = DeprecationLevel.WARNING,
    )
    open fun createOnGoingChannel(ongoingCallsChannelId: String) {
        notificationChannels.ongoingCallChannel.create(notificationManager)
    }

    override suspend fun onCallNotificationUpdate(call: Call): Notification? =
        delegate.onCallNotificationUpdate(call)

    override suspend fun updateOngoingCallNotification(
        call: Call,
        callDisplayName: String,
    ): Notification? =
        delegate.updateOngoingCallNotification(call, callDisplayName)

    override suspend fun updateOutgoingCallNotification(
        call: Call,
        callDisplayName: String?,
    ): Notification? =
        delegate.updateOutgoingCallNotification(call, callDisplayName)

    override suspend fun updateIncomingCallNotification(
        call: Call,
        callDisplayName: String,
    ): Notification? = delegate.updateIncomingCallNotification(call, callDisplayName)

    @Deprecated(
        "Use notificationChannels and delegate for channel management instead.",
        level = DeprecationLevel.WARNING,
    )
    open fun maybeCreateChannel(
        channelId: String,
        context: Context,
        configure: NotificationChannel.() -> Unit = {},
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

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "This method is deprecated. Use the getNotificationUpdates method in the NotificationHandler interface instead.",
    )
    override fun getNotificationUpdates(
        coroutineScope: CoroutineScope,
        call: Call,
        localUser: User,
        onUpdate: (Notification) -> Unit,
    ) {
        val streamVideoClient = StreamVideo.instanceOrNull() as? StreamVideoClient

        if (streamVideoClient?.enableCallNotificationUpdates != true) return
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
                        val currentRemoteParticipantCount = remoteParticipants.size
                        // If number of remote participants increased or decreased
                        if (currentRemoteParticipantCount != latestRemoteParticipantCount) {
                            val isSameCase =
                                currentRemoteParticipantCount > 1 && latestRemoteParticipantCount > 1
                            latestRemoteParticipantCount = currentRemoteParticipantCount

                            if (!isSameCase) {
                                val callDisplayName = if (remoteParticipants.isEmpty()) {
                                    // If no remote participants, get simple call notification title
                                    application.getString(
                                        R.string.stream_video_ongoing_call_notification_title,
                                    )
                                } else {
                                    if (currentRemoteParticipantCount > 1) {
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
                                    remoteParticipantCount = currentRemoteParticipantCount,
                                )?.let {
                                    onUpdate(it)
                                }
                            }
                        }
                    }
                }
        }
    }

    @Deprecated(
        "This function is unused and will be removed in a future release.",
        level = DeprecationLevel.WARNING,
    )
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

    @Deprecated(
        "This function is unused and will be removed in a future release.",
        level = DeprecationLevel.WARNING,
    )
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

    @Deprecated(
        "This function is unused and will be removed in a future release.",
        level = DeprecationLevel.WARNING,
    )
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
        logger.d { "[showNotification] with notificationId: $notificationId" }
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

    override fun createMinimalMediaStyleNotification(
        callId: StreamCallId,
        mediaNotificationConfig: MediaNotificationConfig,
        remoteParticipantCount: Int,
    ): NotificationCompat.Builder? {
        val notificationId = callId.hashCode() // Notification ID
        // Intents
        val onClickIntent = mediaNotificationConfig.contentIntent
            ?: intentResolver.searchOngoingCallPendingIntent(
                callId,
                notificationId,
            )

        // Channel preparation
        val ongoingCallsChannelId = application.getString(
            R.string.stream_video_ongoing_call_notification_channel_id,
        )

        createOnGoingChannel(ongoingCallsChannelId)
        val mediaSession = MediaSessionCompat(application, ongoingCallsChannelId)

        val liveMetadata = MediaMetadataCompat.Builder()
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1L).apply {
                val bannerBitmap = mediaNotificationConfig.mediaNotificationVisuals.bannerBitmap
                if (bannerBitmap != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bannerBitmap)
                }
            }
            .build()
        mediaSession.setMetadata(liveMetadata)

        val liveState = PlaybackStateCompat.Builder()
            .setState(
                PlaybackStateCompat.STATE_PLAYING,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1f,
            ).build()
        mediaSession.setPlaybackState(liveState)

        // 3. Build the notification as usual -----------------------------------
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)

        // Build notification
        return NotificationCompat.Builder(application, ongoingCallsChannelId)
            .also {
                // If the intent is configured, clicking the notification will return to the call
                if (onClickIntent != null) {
                    it.setContentIntent(onClickIntent)
                } else {
                    logger.w { "Ongoing intent is null click on the ongoing call notification will not work." }
                }
            }
            .setContentTitle(mediaNotificationConfig.mediaNotificationContent.contentTitle)
            .setContentText(mediaNotificationConfig.mediaNotificationContent.contentText)
            .setLargeIcon(mediaNotificationConfig.mediaNotificationVisuals.bannerBitmap)
            .setAutoCancel(false)
            .setShowWhen(false)
            .setOngoing(true)
            .setStyle(mediaStyle).apply {
                if (mediaNotificationConfig.mediaNotificationVisuals.smallIcon != null) {
                    setSmallIcon(mediaNotificationConfig.mediaNotificationVisuals.smallIcon)
                }
            }
    }

    override fun getMediaNotificationConfig(): MediaNotificationConfig {
        return MediaNotificationConfig(
            MediaNotificationContent(
                application.getString(R.string.stream_video_livestream_notification_title),
                application.getString(R.string.stream_video_livestream_notification_description),
            ),
            MediaNotificationVisuals(android.R.drawable.ic_media_play, null),
            null,
        )
    }

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
