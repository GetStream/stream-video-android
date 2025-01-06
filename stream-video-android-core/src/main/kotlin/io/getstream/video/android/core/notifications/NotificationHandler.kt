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
import android.app.PendingIntent
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope

public interface NotificationHandler : NotificationPermissionHandler {
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

    fun getSettingUpCallNotification(): Notification?

    /**
     * Get subsequent updates to notifications.
     * Initially, notifications are posted by one of the other methods, and then this method can be used to re-post them with updated content.
     *
     * @param coroutineScope Coroutine scope used for the updates.
     * @param call The Stream call object.
     * @param localUser The local Stream user.
     * @param onUpdate Callback to be called when the notification is updated.
     */
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
        const val INTENT_EXTRA_CALL_DISPLAY_NAME: String = "io.getstream.video.android.intent-extra.call_displayname"

        const val INTENT_EXTRA_NOTIFICATION_ID: String =
            "io.getstream.video.android.intent-extra.notification_id"
        const val INCOMING_CALL_NOTIFICATION_ID = 24756
    }
}
