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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.ActivityCompat
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
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.core.notifications.DefaultStreamIntentResolver
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_LIVE_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_MISSED_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_NOTIFICATION
import io.getstream.video.android.core.notifications.StreamIntentResolver
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.model.StreamCallId

/**
 * Default implementation of the [StreamNotificationHandler] interface.
 */
public open class StreamDefaultNotificationHandler(
    private val application: Application,
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(
        application.applicationContext,
    ),
    private val notificationPermissionHandler: NotificationPermissionHandler = DefaultNotificationPermissionHandler.createDefaultNotificationPermissionHandler(
        application,
    ),
    private val intentResolver: StreamIntentResolver = DefaultStreamIntentResolver(application),
    private val hideRingingNotificationInForeground: Boolean,
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
    private val mediaSessionController: StreamMediaSessionController = DefaultStreamMediaSessionController(
        initialNotificationBuilderInterceptor,
        updateNotificationBuilderInterceptor
    ),
    @ExperimentalStreamVideoApi
    private val permissionChecker: (
        context: Context,
        permission: String,
    ) -> Int = { context, permission ->
        ActivityCompat.checkSelfPermission(context, permission)
    },
) : StreamNotificationHandler,
    StreamNotificationProvider,
    StreamNotificationUpdatesProvider,
    NotificationPermissionHandler by notificationPermissionHandler {

    private val logger by taggedLogger("Video:StreamNotificationHandler")

    // START REGION : On push arrived
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

    override fun onLiveCall(callId: StreamCallId, callDisplayName: String) {
        logger.d { "[onLiveCall] callId: ${callId.id}, callDisplayName: $callDisplayName" }
        val notificationId = callId.hashCode()
        val liveCallPendingIntent =
            intentResolver.searchLiveCallPendingIntent(callId, notificationId)
                ?: run {
                    logger.e { "Couldn't find any activity for $ACTION_LIVE_CALL" }
                    intentResolver.getDefaultPendingIntent()
                }

        return ensureChannelAndBuildNotification(notificationChannels.incomingCallChannel) {
            setContentTitle(callDisplayName)
            setChannelId(notificationChannels.incomingCallChannel.id)
            setCategory(NotificationCompat.CATEGORY_STATUS)
            setContentIntent(liveCallPendingIntent)
            setOngoing(false)
        }.showNotification(notificationId)
    }

    override fun onMissedCall(callId: StreamCallId, callDisplayName: String) {
        logger.d { "[onMissedCall] #ringing; callId: ${callId.id}" }
        val notificationId = callId.hashCode()
        val intent = intentResolver.searchMissedCallPendingIntent(callId, notificationId) ?: run {
            logger.e { "Couldn't find any activity for $ACTION_MISSED_CALL" }
            intentResolver.getDefaultPendingIntent()
        }
        getMissedCallNotification(
            callId,
            callDisplayName,
        ).showNotification(notificationId = callId.hashCode())
    }

    override fun onNotification(callId: StreamCallId, callDisplayName: String) {
        logger.d { "[onNotification] callId: ${callId.id}, callDisplayName: $callDisplayName" }
        val notificationId = callId.hashCode()
        val intent = intentResolver.searchNotificationCallPendingIntent(callId, notificationId)
        if (intent == null) {
            logger.e { "Couldn't find any activity for $ACTION_NOTIFICATION" }
        }
        ensureChannelAndBuildNotification(notificationChannels.ongoingCallChannel) {
            setContentTitle(callDisplayName)
            setChannelId(notificationChannels.ongoingCallChannel.id)
            setCategory(NotificationCompat.CATEGORY_STATUS)
            setOngoing(false)
        }.showNotification(notificationId)
    }

    // END REGION : On push arrived

    // START REGION: Notification provider
    override fun getMissedCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
    ): Notification? {
        logger.d { "[getMissedCallNotification] callId: ${callId.id}, callDisplayName: $callDisplayName" }
        val notificationId = callId.hashCode()
        val intent = intentResolver.searchMissedCallPendingIntent(callId, notificationId)
            ?: intentResolver.getDefaultPendingIntent()

        notificationChannels.missedCallChannel.create(notificationManager)

        // Build notification
        val notificationContent = callDisplayName?.let {
            application.getString(
                R.string.stream_video_missed_call_notification_description,
                it,
            )
        }
        return ensureChannelAndBuildNotification(notificationChannels.missedCallChannel) {
            setSmallIcon(R.drawable.stream_video_ic_call)
            setChannelId(notificationChannels.missedCallChannel.id)
            setContentTitle(
                application.getString(R.string.stream_video_missed_call_notification_title),
            )
            setContentText(notificationContent)
            setContentIntent(intent).setAutoCancel(true)
            initialNotificationBuilderInterceptor.onBuildMissedCallNotification(
                this,
                callDisplayName,
            )
        }
    }

    override fun getRingingCallNotification(
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification? {
        logger.d {
            "[getRingingCallNotification] callId: ${callId.id}, ringingState: $ringingState, callDisplayName: $callDisplayName, shouldHaveContentIntent: $shouldHaveContentIntent"
        }
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

    private inline fun getRingingCallNotificationInternal(
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String?,
        shouldHaveContentIntent: Boolean,
        intercept: NotificationCompat.Builder.() -> NotificationCompat.Builder,
    ): Notification? {
        logger.d {
            "[getRingingCallNotificationInternal] callId: ${callId.id}, ringingState: $ringingState, callDisplayName: $callDisplayName, shouldHaveContentIntent: $shouldHaveContentIntent"
        }
        return if (ringingState is RingingState.Incoming) {
            val fullScreenPendingIntent = intentResolver.searchIncomingCallPendingIntent(callId)
            val acceptCallPendingIntent = intentResolver.searchAcceptCallPendingIntent(callId)
            val rejectCallPendingIntent = intentResolver.searchRejectCallPendingIntent(callId)

            if (fullScreenPendingIntent != null && acceptCallPendingIntent != null && rejectCallPendingIntent != null) {
                getIncomingCallNotificationInternal(
                    fullScreenPendingIntent,
                    acceptCallPendingIntent,
                    rejectCallPendingIntent,
                    callDisplayName,
                    shouldHaveContentIntent,
                    intercept,
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

    private inline fun getIncomingCallNotificationInternal(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
        intercept: NotificationCompat.Builder.() -> NotificationCompat.Builder,
    ): Notification {
        logger.d {
            "[getIncomingCallNotificationInternal] callerName: $callerName, shouldHaveContentIntent: $shouldHaveContentIntent"
        }
        return ensureChannelAndBuildNotification(notificationChannels.incomingCallChannel) {
            priority = if (hideRingingNotificationInForeground) {
                NotificationCompat.PRIORITY_LOW
            } else {
                NotificationCompat.PRIORITY_MAX
            }
            setContentTitle(callerName)
            setContentText(
                application.getString(R.string.stream_video_incoming_call_notification_description),
            )
            setSmallIcon(R.drawable.stream_video_ic_call)
            setChannelId(notificationChannels.incomingCallChannel.id)
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
            intercept(this)
        }
    }

    override fun getIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification? {
        logger.d {
            "[getIncomingCallNotification] callerName: $callerName, shouldHaveContentIntent: $shouldHaveContentIntent"
        }
        return getIncomingCallNotificationInternal(
            fullScreenPendingIntent,
            acceptCallPendingIntent,
            rejectCallPendingIntent,
            callerName,
            shouldHaveContentIntent,
        ) {
            initialNotificationBuilderInterceptor.onBuildIncomingCallNotification(
                this,
                fullScreenPendingIntent,
                acceptCallPendingIntent,
                rejectCallPendingIntent,
                callerName,
                shouldHaveContentIntent,
            )
        }
    }

    override fun getSettingUpCallNotification(): Notification? {
        logger.d { "[getSettingUpCallNotification]" }
        val channelId = notificationChannels.outgoingCallChannel.id
        val title = application.getString(R.string.stream_video_call_setup_notification_title)
        val description =
            application.getString(R.string.stream_video_call_setup_notification_description)
        return ensureChannelAndBuildNotification(notificationChannels.outgoingCallChannel) {
            setContentTitle(title)
            setContentText(description)
            setChannelId(channelId)
            setCategory(NotificationCompat.CATEGORY_CALL)
            setOngoing(true)
        }
    }

    @OptIn(ExperimentalStreamVideoApi::class)
    private inline fun getOngoingCallNotificationInternal(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
        mediaNotificationIntercept: NotificationCompat.Builder.() -> NotificationCompat.Builder = { this },
        notificationBuildIntercept: NotificationCompat.Builder.() -> NotificationCompat.Builder = { this },
        playbackStateIntercept: PlaybackStateCompat.Builder.() -> Unit = { },
        metadataIntercept: MediaMetadataCompat.Builder.() -> Unit = { },
        mediaSessionIntercept: () -> MediaSessionCompat = {
            mediaSessionController.provideMediaSession(
                application, callId,
                notificationChannels.ongoingCallChannel.id,
            )
        },
    ): Notification? {
        logger.d {
            "[getOngoingCallNotificationInternal] callId: ${callId.id}, callDisplayName: $callDisplayName, isOutgoingCall: $isOutgoingCall, remoteParticipantCount: $remoteParticipantCount"
        }
        val client = (StreamVideo.instance() as StreamVideoClient)
        val mediaNotificationCallTypes =
            client.streamNotificationManager.notificationConfig.mediaNotificationCallTypes
        return if (mediaNotificationCallTypes.contains(callId.type)) {
            getMinimalMediaStyleNotification(
                callId,
                mediaNotificationIntercept,
                playbackStateIntercept,
                metadataIntercept,
                mediaSessionIntercept,
            )
        } else {
            getSimpleOngoingCallNotification(
                callId,
                callDisplayName,
                isOutgoingCall,
                remoteParticipantCount,
                notificationBuildIntercept,
            )
        }
    }

    override fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
    ): Notification? {
        logger.d {
            "[getOngoingCallNotification] callId: ${callId.id}, callDisplayName: $callDisplayName, isOutgoingCall: $isOutgoingCall, remoteParticipantCount: $remoteParticipantCount"
        }
        return getOngoingCallNotificationInternal(
            callId,
            callDisplayName,
            isOutgoingCall,
            remoteParticipantCount,
            mediaNotificationIntercept = {
                initialNotificationBuilderInterceptor.onBuildOngoingCallMediaNotification(
                    this,
                    callId,
                )
            },
            playbackStateIntercept = {
                val mediaSession = mediaSessionController.provideMediaSession(
                    application, callId,
                    notificationChannels.ongoingCallChannel.id,
                )

                mediaSessionController.initialPlaybackState(
                    application.applicationContext,
                    mediaSession,
                    callId,
                    this
                )
            },
            metadataIntercept = {
                val mediaSession = mediaSessionController.provideMediaSession(
                    application, callId,
                    notificationChannels.ongoingCallChannel.id,
                )
                mediaSessionController.initialMetadata(
                    application.applicationContext,
                    mediaSession,
                    callId,
                    this
                )
            },
            notificationBuildIntercept = {
                initialNotificationBuilderInterceptor.onBuildOngoingCallNotification(
                    this,
                    callId,
                    callDisplayName,
                    isOutgoingCall,
                    remoteParticipantCount,
                )
            },
        )
    }

    // END REGION: Notification provider

    private inline fun getSimpleOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
        intercept: NotificationCompat.Builder.() -> NotificationCompat.Builder,
    ): Notification? {
        logger.d {
            "[getSimpleOngoingCallNotification] callId: ${callId.id}, callDisplayName: $callDisplayName, isOutgoingCall: $isOutgoingCall, remoteParticipantCount: $remoteParticipantCount"
        }
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

        if (hangUpIntent == null) {
            logger.e { "End call intent is null, not showing notification!" }
            return null
        }

        return ensureChannelAndBuildNotification(notificationChannels.ongoingCallChannel) {
            setSmallIcon(R.drawable.stream_video_ic_call).also {
                // If the intent is configured, clicking the notification will return to the call
                if (onClickIntent != null) {
                    it.setContentIntent(onClickIntent)
                } else {
                    logger.w { "Ongoing intent is null click on the ongoing call notification will not work." }
                }
            }
            setContentTitle(
                if (isOutgoingCall) {
                    application.getString(R.string.stream_video_outgoing_call_notification_title)
                } else {
                    application.getString(R.string.stream_video_ongoing_call_notification_title)
                },
            )
            setContentText(
                application.getString(R.string.stream_video_ongoing_call_notification_description),
            )
            setAutoCancel(false).setOngoing(true).addHangUpAction(
                hangUpIntent,
                callDisplayName ?: application.getString(
                    R.string.stream_video_ongoing_call_notification_title,
                ),
                remoteParticipantCount,
            )
            intercept(this)
        }
    }

    // START REGION: Notification updates

    override suspend fun onCallNotificationUpdate(call: Call): Notification? {
        val ringingState = call.state.ringingState.value
        val members = call.state.members.value
        val remoteParticipants = call.state.remoteParticipants.value
        logger.d { "[onCallNotificationUpdate] #ringingState: $ringingState; callId: ${call.cid}" }
        logger.d { "[onCallNotificationUpdate] #members: $members; callId: ${call.cid}" }
        logger.d {
            "[onCallNotificationUpdate] #remoteParticipants: $remoteParticipants; callId: ${call.cid}"
        }

        val notification: Notification? = if (ringingState is RingingState.Outgoing) {
            logger.d { "[onCallNotificationUpdate] Handling outgoing call" }
            val remoteMembersCount = members.size - 1
            val callDisplayName = if (remoteMembersCount != 1) {
                application.getString(
                    R.string.stream_video_outgoing_call_notification_title,
                )
            } else {
                members.firstOrNull { member ->
                    member.user.id != call.state.me.value?.userId?.value
                }?.user?.name ?: "Unknown"
            }

            updateOutgoingCallNotification(
                call = call,
                callDisplayName = callDisplayName,
            )
        } else if (ringingState is RingingState.Incoming) {
            logger.d { "[onCallNotificationUpdate] Handling incoming call" }
            val callDisplayName = members.firstOrNull { member ->
                member.user.id != call.state.me.value?.userId?.value
            }?.user?.name ?: "Unknown"

            logger.d { "[onCallNotificationUpdate] Incoming call from: $callDisplayName" }
            updateIncomingCallNotification(
                call = call,
                callDisplayName = callDisplayName,
            )
        } else if (ringingState is RingingState.Active) {
            logger.d { "[onCallNotificationUpdate] Handling active call" }
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

            updateOngoingCallNotification(
                call = call,
                callDisplayName = callDisplayName,
            )
        } else {
            logger.d { "[onCallNotificationUpdate] Unhandled ringing state: $ringingState" }
            null
        }

        return notification
    }

    override suspend fun updateIncomingCallNotification(
        call: Call,
        callDisplayName: String,
    ): Notification? {
        logger.d {
            "[onUpdateIncomingCallNotification] callId: ${call.cid}, callDisplayName: $callDisplayName"
        }
        val callId = StreamCallId.fromCallCid(call.cid)
        return getRingingCallNotificationInternal(
            ringingState = call.state.ringingState.value,
            callId = callId,
            callDisplayName = callDisplayName,
            shouldHaveContentIntent = true,
            intercept = {
                val fullScreenPendingIntent = intentResolver.searchIncomingCallPendingIntent(callId)
                val acceptCallPendingIntent = intentResolver.searchAcceptCallPendingIntent(callId)
                val rejectCallPendingIntent = intentResolver.searchRejectCallPendingIntent(callId)
                val initial =
                    if (fullScreenPendingIntent != null && acceptCallPendingIntent != null && rejectCallPendingIntent != null) {
                        initialNotificationBuilderInterceptor.onBuildIncomingCallNotification(
                            this,
                            fullScreenPendingIntent,
                            acceptCallPendingIntent,
                            rejectCallPendingIntent,
                            callDisplayName,
                            true,
                        )
                    } else {
                        logger.e { "Ringing call notification not shown, one of the intents is null." }
                        this
                    }
                updateNotificationBuilderInterceptor.onUpdateIncomingCallNotification(
                    initial,
                    callDisplayName,
                    call,
                )
            },
        )
    }

    override suspend fun updateOngoingCallNotification(
        call: Call,
        callDisplayName: String,
    ): Notification? {
        logger.d {
            "[updateOngoingCallNotification] callId: ${call.cid}, callDisplayName: $callDisplayName"
        }
        val callId = StreamCallId.fromCallCid(call.cid)
        return getOngoingCallNotificationInternal(
            callId = StreamCallId.fromCallCid(call.cid),
            callDisplayName = callDisplayName,
            remoteParticipantCount = call.state.remoteParticipants.value.size,
            isOutgoingCall = false,
            playbackStateIntercept = {
                val mediaSession = mediaSessionController.provideMediaSession(
                    application, callId,
                    notificationChannels.ongoingCallChannel.id,
                )
                mediaSessionController.updatePlaybackState(
                    application.applicationContext,
                    mediaSession,
                    call,
                    callDisplayName,
                    this
                )
            },
            metadataIntercept = {
                val mediaSession = mediaSessionController.provideMediaSession(
                    application, callId,
                    notificationChannels.ongoingCallChannel.id,
                )
                mediaSessionController.updateMetadata(
                    application.applicationContext,
                    mediaSession,
                    call,
                    callDisplayName,
                    this
                )
            },
            mediaNotificationIntercept = {
                val initialInterceptor =
                    initialNotificationBuilderInterceptor.onBuildOngoingCallMediaNotification(
                        this,
                        callId,
                    )
                updateNotificationBuilderInterceptor.onUpdateOngoingCallMediaNotification(
                    initialInterceptor,
                    callDisplayName,
                    call,
                )
            },
            notificationBuildIntercept = {
                val initial = initialNotificationBuilderInterceptor.onBuildOngoingCallNotification(
                    this,
                    callId,
                    callDisplayName,
                )
                updateNotificationBuilderInterceptor.onUpdateOngoingCallNotification(
                    initial,
                    callDisplayName,
                    call,
                )
            },
        )
    }

    override suspend fun updateOutgoingCallNotification(
        call: Call,
        callDisplayName: String?,
    ): Notification? {
        logger.d {
            "[updateOutgoingCallNotification] callId: ${call.cid}, callDisplayName: $callDisplayName"
        }
        return getRingingCallNotificationInternal(
            ringingState = call.state.ringingState.value,
            callId = StreamCallId.fromCallCid(call.cid),
            callDisplayName = callDisplayName,
            shouldHaveContentIntent = true,
            intercept = {
                val initial = initialNotificationBuilderInterceptor.onBuildOutgoingCallNotification(
                    this,
                    call.state.ringingState.value,
                    StreamCallId.fromCallCid(call.cid),
                    callDisplayName,
                )
                updateNotificationBuilderInterceptor.onUpdateOutgoingCallNotification(
                    initial,
                    callDisplayName,
                    call,
                )
            },
        )
    }

    @SuppressLint("MissingPermission")
    private fun Notification?.showNotification(notificationId: Int) {
        this?.let { notification ->
            if (permissionChecker(
                    application,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(notificationId, notification)
                logger.d { "[showNotification] with notificationId: $notificationId" }
            } else {
                logger.w { "[showNotification] Permission not granted, not showing notification." }
            }
        }
    }

    private fun NotificationCompat.Builder.addHangUpAction(
        hangUpIntent: PendingIntent,
        callDisplayName: String,
        remoteParticipantCount: Int,
    ): NotificationCompat.Builder = apply {
        logger.d {
            "[addHangUpAction] Adding hang up action for $callDisplayName (remoteParticipantCount=$remoteParticipantCount)"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setStyle(
                CallStyle.forOngoingCall(
                    Person.Builder().setName(callDisplayName).apply {
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
                    }.build(),
                    hangUpIntent,
                ),
            )
        } else {
            addAction(getLeaveAction(hangUpIntent))
        }
    }

    private fun NotificationCompat.Builder.addCallActions(
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callDisplayName: String?,
    ): NotificationCompat.Builder = apply {
        logger.d { "[addCallActions] callDisplayName: $callDisplayName" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setStyle(
                CallStyle.forIncomingCall(
                    Person.Builder().setName(callDisplayName ?: "Unknown").apply {
                        if (callDisplayName == null) {
                            setIcon(
                                IconCompat.createWithResource(
                                    application,
                                    R.drawable.stream_video_ic_user,
                                ),
                            )
                        }
                    }.build(),
                    rejectCallPendingIntent,
                    acceptCallPendingIntent,
                ),
            )
        } else {
            addAction(getAcceptAction(acceptCallPendingIntent))
            addAction(getRejectAction(rejectCallPendingIntent))
        }
    }

    private fun getLeaveAction(intent: PendingIntent): NotificationCompat.Action {
        logger.d { "[getLeaveAction]" }
        return NotificationCompat.Action.Builder(
            null,
            application.getString(R.string.stream_video_call_notification_action_leave),
            intent,
        ).build()
    }

    private fun getAcceptAction(intent: PendingIntent): NotificationCompat.Action {
        logger.d { "[getAcceptAction]" }
        return NotificationCompat.Action.Builder(
            null,
            application.getString(R.string.stream_video_call_notification_action_accept),
            intent,
        ).build()
    }

    private fun getRejectAction(intent: PendingIntent): NotificationCompat.Action {
        logger.d { "[getRejectAction]" }
        return NotificationCompat.Action.Builder(
            null,
            application.getString(R.string.stream_video_call_notification_action_reject),
            intent,
        ).build()
    }

    @OptIn(ExperimentalStreamVideoApi::class)
    private inline fun getMinimalMediaStyleNotification(
        callId: StreamCallId,
        notificationBuildIntercept: NotificationCompat.Builder.() -> NotificationCompat.Builder = { this },
        playbackStateIntercept: PlaybackStateCompat.Builder.() -> Unit = { },
        metadataIntercept: MediaMetadataCompat.Builder.() -> Unit = { },
        mediaSessionIntercept: () -> MediaSessionCompat = {
            mediaSessionController.provideMediaSession(
                application,
                callId,
                notificationChannels.ongoingCallChannel.id
            )
        },
    ): Notification {
        logger.d { "[getMinimalMediaStyleNotification] callId: ${callId.id}" }
        val notificationId = callId.hashCode() // Notification ID
        // Intents
        val onClickIntent = intentResolver.searchOngoingCallPendingIntent(
            callId,
            notificationId,
        )

        // Channel
        notificationChannels.ongoingCallChannel.create(notificationManager)

        // Media session
        val mediaSession = mediaSessionIntercept()


        metadataIntercept.invoke(MediaMetadataCompat.Builder())
        playbackStateIntercept.invoke(PlaybackStateCompat.Builder())

        // 3. Build the notification as usual -----------------------------------
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        // Build notification
        return ensureChannelAndBuildNotification(notificationChannels.ongoingCallChannel) {
            if (onClickIntent != null) {
                setContentIntent(onClickIntent)
            } else {
                logger.w { "Ongoing intent is null click on the ongoing call notification will not work." }
            }
            setContentTitle(
                application.getString(R.string.stream_video_livestream_notification_title),
            )
            setContentText(
                application.getString(R.string.stream_video_livestream_notification_title),
            )
            setAutoCancel(false)
            setShowWhen(false)
            setOngoing(true)
            setSmallIcon(R.drawable.stream_video_ic_live_notification)
            .setStyle(mediaStyle)
            notificationBuildIntercept(this)
        }
    }

    private inline fun ensureChannelAndBuildNotification(
        channelInfo: StreamNotificationChannelInfo,
        builder: NotificationCompat.Builder.() -> NotificationCompat.Builder,
    ): Notification {
        logger.d { "[ensureChannelAndBuildNotification] channelId: ${channelInfo.id}" }
        channelInfo.create(notificationManager)
        return NotificationCompat.Builder(application, channelInfo.id).let(builder).build()
    }

    @OptIn(ExperimentalStreamVideoApi::class)
    internal fun mediaSession(callId: StreamCallId) = mediaSessionController.provideMediaSession(
        application, callId,
        notificationChannels.ongoingCallChannel.id,
    )

    @OptIn(ExperimentalStreamVideoApi::class)
    internal fun clearMediaSession(callId: StreamCallId?) = safeCall {
        callId?.let { mediaSessionController.clear(it) }
    }
}

