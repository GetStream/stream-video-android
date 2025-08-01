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

interface StreamNotificationHandler : StreamNotificationHandlerWithPayload {
    /**
     * Customize the notification when you receive a push notification for ringing call,
     * which has further two types [RingingState.Incoming] and [RingingState.Outgoing]
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("onRingingCall(callId, callDisplayName, emptyMap())"),
        level = DeprecationLevel.WARNING,
    )
    fun onRingingCall(callId: StreamCallId, callDisplayName: String) {
        onRingingCall(callId, callDisplayName, emptyMap())
    }

    /**
     * Customize the notification when you receive a push notification for Missed Call
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("onMissedCall(callId, callDisplayName, emptyMap())"),
        level = DeprecationLevel.WARNING,
    )
    fun onMissedCall(callId: StreamCallId, callDisplayName: String) {
        onMissedCall(callId, callDisplayName, emptyMap())
    }

    /**
     * Customize the notification when you receive a push notification for general usage
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("onNotification(callId, callDisplayName, emptyMap())"),
        level = DeprecationLevel.WARNING,
    )
    fun onNotification(callId: StreamCallId, callDisplayName: String) {
        onNotification(callId, callDisplayName, emptyMap())
    }

    /**
     * Customize the notification when you receive a push notification for Live Call
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("onLiveCall(callId, callDisplayName, emptyMap())"),
        level = DeprecationLevel.WARNING,
    )
    fun onLiveCall(callId: StreamCallId, callDisplayName: String) {
        onLiveCall(callId, callDisplayName, emptyMap())
    }
}

interface StreamNotificationHandlerWithPayload {
    /**
     * Customize the notification when you receive a push notification for ringing call,
     * which has further two types [RingingState.Incoming] and [RingingState.Outgoing]
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */

    fun onRingingCall(callId: StreamCallId, callDisplayName: String, payload: Map<String, Any?>)

    /**
     * Customize the notification when you receive a push notification for Missed Call
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    fun onMissedCall(callId: StreamCallId, callDisplayName: String, payload: Map<String, Any?>)

    /**
     * Customize the notification when you receive a push notification for general usage
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    fun onNotification(callId: StreamCallId, callDisplayName: String, payload: Map<String, Any?>)

    /**
     * Customize the notification when you receive a push notification for Live Call
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     */
    fun onLiveCall(callId: StreamCallId, callDisplayName: String, payload: Map<String, Any?>)
}

interface StreamNotificationProviderWithPayload {
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
        payload: Map<String, Any?>,
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
        payload: Map<String, Any?>,
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
        payload: Map<String, Any?>,
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
        payload: Map<String, Any?>,
    ): Notification?
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

interface StreamNotificationProvider : StreamNotificationProviderWithPayload {

    /**
     * Customize the notification when you receive a push notification for ringing call with type [RingingState.Incoming]
     * @param fullScreenPendingIntent A high-priority intent that launches an activity in full-screen mode, bypassing the lock screen.
     * @param acceptCallPendingIntent The intent triggered when accepting the call from the notification.
     * @param rejectCallPendingIntent The intent triggered when rejecting the call from the notification.
     * @param callerName The name of the caller to display in the notification
     * @param shouldHaveContentIntent If true, clicking the notification triggers [fullScreenPendingIntent].
     * @return A [Notification] object customized for the incoming call.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "getIncomingCallNotification(fullScreenPendingIntent,acceptCallPendingIntent,rejectCallPendingIntent,callerName,shouldHaveContentIntent,emptyMap()",
        ),
        level = DeprecationLevel.WARNING,
    )
    fun getIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification? {
        return getIncomingCallNotification(
            fullScreenPendingIntent,
            acceptCallPendingIntent,
            rejectCallPendingIntent,
            callerName,
            shouldHaveContentIntent,
            emptyMap(),
        )
    }

    /**
     * Customize the notification when you receive a push notification for ringing call with type [RingingState.Outgoing] and [RingingState.Active]
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     * @param isOutgoingCall True if the call is outgoing [RingingState.Outgoing], false if it is an active call [RingingState.Active].
     * @param remoteParticipantCount Count of remote participant
     * @return A [Notification] object customized for the ongoing call.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "getOngoingCallNotification(callId,callDisplayName,isOutgoingCall,remoteParticipantCount)",
        ),
        level = DeprecationLevel.WARNING,
    )
    fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String? = null,
        isOutgoingCall: Boolean = false,
        remoteParticipantCount: Int = 0,
    ): Notification? {
        return getOngoingCallNotification(
            callId,
            callDisplayName,
            isOutgoingCall,
            remoteParticipantCount,
            emptyMap(),
        )
    }

    /**
     * Customize the notification when you receive a push notification for ringing call
     * @param ringingState The current state of ringing call, represented by [RingingState]
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     * @param shouldHaveContentIntent If set to true then it will launch a screen when the user will click on the notification
     * @return A [Notification] object customized for the ongoing call.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "getRingingCallNotification(ringingState,callId,callDisplayName,shouldHaveContentIntent)",
        ),
        level = DeprecationLevel.WARNING,
    )
    fun getRingingCallNotification(
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String? = null,
        shouldHaveContentIntent: Boolean = true,
    ): Notification? {
        return getRingingCallNotification(
            ringingState,
            callId,
            callDisplayName,
            shouldHaveContentIntent,
            emptyMap(),
        )
    }

    /**
     * Customize the notification when you receive a push notification for Missed Call
     *
     * @param callId An instance of [StreamCallId] representing the call identifier
     * @param callDisplayName The name of the caller to display in the notification
     * @return A [Notification] object customized for the missed call.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("getMissedCallNotification(callId,callDisplayName,emptyMap())"),
        level = DeprecationLevel.WARNING,
    )
    fun getMissedCallNotification(
        callId: StreamCallId,
        callDisplayName: String? = null,
    ): Notification? {
        return getMissedCallNotification(callId, callDisplayName, emptyMap())
    }

    /**
     * Temporary notification. Sometimes the system needs to show a notification while the call is not ready.
     * This is the notification that will be shown.
     *
     * @return A [Notification] object.
     */
    fun getSettingUpCallNotification(): Notification?
}

/**
 * Interceptor for notification builders.
 */
open class StreamNotificationBuilderInterceptors :
    StreamNotificationBuilderInterceptorsWithPayload() {

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
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "onBuildIncomingCallNotification(builder,fullScreenPendingIntent,acceptCallPendingIntent,rejectCallPendingIntent,callerName,shouldHaveContentIntent,emptyMap())",
        ),
        level = DeprecationLevel.WARNING,
    )
    open fun onBuildIncomingCallNotification(
        builder: NotificationCompat.Builder,
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
    ): NotificationCompat.Builder {
        return onBuildIncomingCallNotification(
            builder,
            fullScreenPendingIntent,
            acceptCallPendingIntent,
            rejectCallPendingIntent,
            callerName,
            shouldHaveContentIntent,
            emptyMap(),
        )
    }

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param callDisplayName The name of the caller to display in the notification.
     * @param isOutgoingCall True if the call is outgoing, false if it is an active call.
     * @param remoteParticipantCount Count of remote participant.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "onBuildOngoingCallNotification(builder,callId,callDisplayName,isOutgoingCall,remoteParticipantCount,emptyMap())",
        ),
        level = DeprecationLevel.WARNING,
    )
    open fun onBuildOngoingCallNotification(
        builder: NotificationCompat.Builder,
        callId: StreamCallId,
        callDisplayName: String? = null,
        isOutgoingCall: Boolean = false,
        remoteParticipantCount: Int = 0,
    ): NotificationCompat.Builder {
        return onBuildOngoingCallNotification(
            builder,
            callId,
            callDisplayName,
            isOutgoingCall,
            remoteParticipantCount,
            emptyMap(),
        )
    }

    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("onBuildOngoingCallMediaNotification(builder,callId,emptyMap())"),
        level = DeprecationLevel.WARNING,
    )
    open fun onBuildOngoingCallMediaNotification(
        builder: NotificationCompat.Builder,
        callId: StreamCallId,
    ): NotificationCompat.Builder {
        return onBuildOngoingCallMediaNotification(builder, callId, emptyMap())
    }

    /**
     * Intercept the notification builder and modify it before it is posted.
     *
     * @param builder The notification builder.
     * @param callDisplayName The name of the caller to display in the notification.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "onBuildMissedCallNotification(builder,callDisplayName,emptyMap())",
        ),
        level = DeprecationLevel.WARNING,
    )
    open fun onBuildMissedCallNotification(
        builder: NotificationCompat.Builder,
        callDisplayName: String?,
    ): NotificationCompat.Builder {
        return onBuildMissedCallNotification(builder, callDisplayName, emptyMap())
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
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "onBuildOutgoingCallNotification(builder,ringingState,callId,callDisplayName,shouldHaveContentIntent,emptyMap())",
        ),
        level = DeprecationLevel.WARNING,
    )
    open fun onBuildOutgoingCallNotification(
        builder: NotificationCompat.Builder,
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String? = null,
        shouldHaveContentIntent: Boolean = true,
    ): NotificationCompat.Builder {
        return onBuildOutgoingCallNotification(
            builder,
            ringingState,
            callId,
            callDisplayName,
            shouldHaveContentIntent,
            emptyMap(),
        )
    }

    /**
     * Intercept the notification builder and modify the media metadata before it is posted.
     *
     * @param builder The media metadata builder.
     * @param callId An instance of [StreamCallId] representing the call identifier
     */
    open fun onBuildMediaNotificationMetadata(
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
    open fun onBuildMediaNotificationPlaybackState(
        builder: PlaybackStateCompat.Builder,
        callId: StreamCallId,
    ): PlaybackStateCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify the media style before it is posted.
     *
     * @param style The media style.
     * @param callId An instance of [StreamCallId] representing the call identifier
     */
    open fun onBuildMediaNotificationStyle(
        style: androidx.media.app.NotificationCompat.MediaStyle,
        callId: StreamCallId,
    ): androidx.media.app.NotificationCompat.MediaStyle {
        return onBuildMediaNotificationStyle(style, callId, emptyMap())
    }

    /**
     * Intercept the media session creation and modify it before it is posted.
     *
     * @param application The application context.
     * @param channelId The channel id.
     */
    open fun onCreateMediaSessionCompat(
        application: Application,
        channelId: String,
    ): MediaSessionCompat? {
        return null
    }
}

open class StreamNotificationBuilderInterceptorsWithPayload {

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
        payload: Map<String, Any?>,
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
        payload: Map<String, Any?>,
    ): NotificationCompat.Builder {
        return builder
    }

    open fun onBuildOngoingCallMediaNotification(
        builder: NotificationCompat.Builder,
        callId: StreamCallId,
        payload: Map<String, Any?>,
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
        payload: Map<String, Any?>,
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
        payload: Map<String, Any?>,
    ): NotificationCompat.Builder {
        return builder
    }

    /**
     * Intercept the notification builder and modify the media style before it is posted.
     *
     * @param style The media style.
     * @param callId An instance of [StreamCallId] representing the call identifier
     */
    open fun onBuildMediaNotificationStyle(
        style: androidx.media.app.NotificationCompat.MediaStyle,
        callId: StreamCallId,
        payload: Map<String, Any?>,
    ): androidx.media.app.NotificationCompat.MediaStyle {
        return style
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
    open suspend fun onUpdateMediaNotificationMetadata(
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
    open suspend fun onUpdateMediaNotificationPlaybackState(
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
    open suspend fun onUpdateMediaSessionCompat(
        application: Application,
        channelId: String,
    ): MediaSessionCompat? {
        return null
    }
}
