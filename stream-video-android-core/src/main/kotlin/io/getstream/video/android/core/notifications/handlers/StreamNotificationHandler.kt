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
import android.app.PendingIntent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.model.StreamCallId

interface StreamNotificationHandler {
    /**
     * Customize the notification when you receive a push notification for ringing call,
     * which has further two types [RingingState.Incoming] and [RingingState.Outgoing]
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    fun onRingingCall(callId: StreamCallId, callDisplayName: String)

    /**
     * Customize the notification when you receive a push notification for Missed Call
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    fun onMissedCall(callId: StreamCallId, callDisplayName: String)

    /**
     * Customize the notification when you receive a push notification for general usage
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    fun onNotification(callId: StreamCallId, callDisplayName: String)

    /**
     * Customize the notification when you receive a push notification for Live Call
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    fun onLiveCall(callId: StreamCallId, callDisplayName: String)
}

interface StreamNotificationProvider {

    /**
     * Customize the notification when you receive a push notification for ringing call with type [RingingState.Incoming]
     * @param fullScreenPendingIntent A high-priority intent that launches an activity in full-screen mode, bypassing the lock screen.
     * @param acceptCallPendingIntent The intent triggered when accepting the call from the notification.
     * @param rejectCallPendingIntent The intent triggered when rejecting the call from the notification.
     * @param callerName The name of the caller to display in the notification
     * @param shouldHaveContentIntent If true, clicking the notification triggers [fullScreenPendingIntent].
     * @return A [Notification] object customized for the incoming call.
     */
    fun getIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification?

    /**
     * Customize the notification when you receive a push notification for ringing call with type [RingingState.Outgoing] and [RingingState.Active]
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     * @param isOutgoingCall True if the call is outgoing [RingingState.Outgoing], false if it is an active call [RingingState.Active].
     * @param remoteParticipantCount Count of remote participant
     * @return A [Notification] object customized for the ongoing call.
     */
    fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String? = null,
        isOutgoingCall: Boolean = false,
        remoteParticipantCount: Int = 0,
    ): Notification?

    /**
     * Customize the notification when you receive a push notification for ringing call
     * @param ringingState The current state of ringing call, represented by [RingingState]
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     * @param shouldHaveContentIntent If set to true then it will launch a screen when the user will click on the notification
     * @return A [Notification] object customized for the ongoing call.
     */
    fun getRingingCallNotification(
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String? = null,
        shouldHaveContentIntent: Boolean = true,
    ): Notification?

    /**
     * Customize the notification when you receive a push notification for Missed Call
     *
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     * @return A [Notification] object customized for the missed call.
     */
    fun getMissedCallNotification(
        callId: StreamCallId,
        callDisplayName: String? = null,
    ): Notification?

    /**
     * Temporary notification. Sometimes the system needs to show a notification while the call is not ready.
     * This is the notification that will be shown.
     *
     * @return A [Notification] object.
     */
    fun getSettingUpCallNotification(): Notification?
}

interface StreamNotificationUpdatesProvider {

    /**
     * Get subsequent updates to notifications.
     * Initially, notifications are posted by one of the other methods, and then this method can be used to re-post them with updated content.
     *
     * @param call The Stream call object.
     * @return A [Notification] object customized for the ongoing call.
     */
    suspend fun onCallNotificationUpdate(call: Call): Notification?

    /**
     * Update the ongoing call notification.
     *
     * @param call The Stream call object.
     * @return A [Notification] object customized for the ongoing call.
     */
    suspend fun updateOngoingCallNotification(
        call: Call,
        callDisplayName: String,
    ): Notification?

    /**
     * Update the ringing call notification.
     *
     * @param call The Stream call object.
     * @return A [Notification] object customized for the ringing call.
     */
    suspend fun updateOutgoingCallNotification(
        call: Call,
        callDisplayName: String?,
    ): Notification?

    /**
     * Update the ringing call notification.
     *
     * @param call The Stream call object.
     * @return A [Notification] object customized for the ringing call.
     */
    suspend fun updateIncomingCallNotification(
        call: Call,
        callDisplayName: String,
    ): Notification?
}

/**
 * Interceptor for notification builders.
 */
open class StreamNotificationBuilderInterceptors {

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param fullScreenPendingIntent A high-priority intent that launches an activity in full-screen mode, bypassing the lock screen.
     * @param acceptCallPendingIntent The intent triggered when accepting the call from the notification.
     * @param rejectCallPendingIntent The intent triggered when rejecting the call from the notification.
     * @param callerName The name of the caller to display in the notification.
     * @param shouldHaveContentIntent If true, clicking the notification triggers [fullScreenPendingIntent].
     */
    open fun onBuildIncomingCallNotification(
        builder: NotificationCompat.Builder,
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
    ): NotificationCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param callDisplayName The name of the caller to display in the notification.
     * @param isOutgoingCall True if the call is outgoing, false if it is an active call.
     * @param remoteParticipantCount Count of remote participant.
     */
    open fun onBuildOngoingCallNotification(
        builder: NotificationCompat.Builder,
        callId: StreamCallId,
        callDisplayName: String? = null,
        isOutgoingCall: Boolean = false,
        remoteParticipantCount: Int = 0,
    ): NotificationCompat.Builder {
        return builder
    }

    open fun onBuildOngoingCallMediaNotification(
        builder: NotificationCompat.Builder,
        callId: StreamCallId,
    ): NotificationCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param callDisplayName The name of the caller to display in the notification.
     */
    open fun onBuildMissedCallNotification(
        builder: NotificationCompat.Builder,
        callDisplayName: String?,
    ): NotificationCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param ringingState The current state of ringing call, represented by [RingingState]
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     * @param shouldHaveContentIntent If set to true then it will launch a screen when the user will click on the notification
     */
    open fun onBuildOutgoingCallNotification(
        builder: NotificationCompat.Builder,
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String? = null,
        shouldHaveContentIntent: Boolean = true,
    ): NotificationCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify the media metadata before it is posted.
     *
     * @param builder The media metadata builder.
     * @param callId An instance of [StreamCallId] representing the call identifier
     */
    fun onBuildMediaNotificationMetadata(
        builder: MediaMetadataCompat.Builder,
        callId: StreamCallId,
    ): MediaMetadataCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify the media playback state before it is posted.
     *
     * @param builder The media playback state builder.
     * @param callId An instance of [StreamCallId] representing the call identifier
     */
    fun onBuildMediaNotificationPlaybackState(
        builder: PlaybackStateCompat.Builder,
        callId: StreamCallId,
    ): PlaybackStateCompat.Builder {
        return builder
    }

    /**
     * Intercept the media session creation and modify it before it is posted.
     *
     * @param application The application context.
     * @param channelId The channel id.
     */
    fun onCreateMediaSessionCompat(
        application: Application,
        channelId: String,
    ): MediaSessionCompat? {
        return null
    }
}

/**
 * Interceptor for notification updates.
 */
open class StreamNotificationUpdateInterceptors {

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param callDisplayName The name of the caller to display in the notification.
     * @param call the stream call
     */
    open suspend fun onUpdateOngoingCallNotification(
        builder: NotificationCompat.Builder,
        callDisplayName: String? = null,
        call: Call,
    ): NotificationCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param callDisplayName The name of the caller to display in the notification.
     * @param call the stream call
     */
    open suspend fun onUpdateOngoingCallMediaNotification(
        builder: NotificationCompat.Builder,
        callDisplayName: String? = null,
        call: Call,
    ): NotificationCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param callDisplayName The name of the caller to display in the notification
     * @param call the stream call
     */
    open suspend fun onUpdateOutgoingCallNotification(
        builder: NotificationCompat.Builder,
        callDisplayName: String? = null,
        call: Call,
    ): NotificationCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param callDisplayName The name of the caller to display in the notification
     * @param call the stream call
     */
    open suspend fun onUpdateIncomingCallNotification(
        builder: NotificationCompat.Builder,
        callDisplayName: String? = null,
        call: Call,
    ): NotificationCompat.Builder {
        return builder
    }

    /**
     * Intercept the media notification metadata builder and modify it before it is posted for updating.
     *
     * @param builder The media metadata builder.
     * @param call The Stream call object.
     * @param callDisplayName The name of the caller to display in the notification.
     */
    open fun onUpdateMediaNotificationMetadata(
        builder: MediaMetadataCompat.Builder,
        call: Call,
        callDisplayName: String? = null,
    ): MediaMetadataCompat.Builder {
        return builder
    }

    /**
     * Intercept the media notification playback state builder and modify it before it is posted for updating.
     *
     * @param builder The media playback state builder.
     * @param call The Stream call object.
     * @param callDisplayName The name of the caller to display in the notification.
     */
    open fun onUpdateMediaNotificationPlaybackState(
        builder: PlaybackStateCompat.Builder,
        call: Call,
        callDisplayName: String? = null,
    ): PlaybackStateCompat.Builder {
        return builder
    }

    /**
     * Intercept the media session creation and modify it before it is posted.
     *
     * @param application The application context.
     * @param channelId The channel id.
     */
    fun onUpdateMediaSessionCompat(
        application: Application,
        channelId: String,
    ): MediaSessionCompat? {
        return null
    }
}
