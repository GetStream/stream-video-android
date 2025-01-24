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
     * Configures incoming call push notification behavior.
     * @param callId An instance of [StreamCallId] representing the call identifier.
     * @param callerName The name of the caller.
     */
    fun onIncomingCall(callId: StreamCallId, callerName: String)

    /**
     * Configures missed call push notification behavior.
     * @param callId An instance of [StreamCallId] representing the call identifier.
     * @param callerName The name of the caller.
     */
    fun onMissedCall(callId: StreamCallId, callerName: String)

    /**
     * Configures live started push notification behavior.
     * @param callId An instance of [StreamCallId] representing the call identifier.
     * @param callCreatorName The name of the call creator.
     */
    fun onLiveCall(callId: StreamCallId, callCreatorName: String)

    /**
     * Configures generic push notification behavior.
     * @param callId An instance of [StreamCallId] representing the call identifier.
     * @param callCreatorName The name of the call creator.
     */
    fun onNotification(callId: StreamCallId, callCreatorName: String)

    /**
     * Customizes the [Notification] to be displayed for incoming and outgoing ringing calls.
     * @param ringingState The state of the ringing call, see [RingingState.Incoming] and [RingingState.Outgoing].
     * @param callId An instance of [StreamCallId] representing the call identifier.
     * @param callInfo Call information that can be displayed as the primary content of the notification.
     * @param shouldHaveContentIntent If the notification should have a content intent set. Used when the notification is clicked.
     *
     * @return A nullable [Notification].
     */
    fun getRingingCallNotification(
        ringingState: RingingState,
        callId: StreamCallId,
        callInfo: String? = null,
        shouldHaveContentIntent: Boolean = true,
    ): Notification?

    /**
     * Customizes the [Notification] to be displayed for incoming calls.
     * @param fullScreenPendingIntent A high-priority intent that launches an activity in full-screen mode, bypassing the lock screen.
     * @param acceptCallPendingIntent The intent triggered when accepting the call from the notification.
     * @param rejectCallPendingIntent The intent triggered when rejecting the call from the notification.
     * @param callInfo Call information that can be displayed as the primary content of the notification.
     * @param shouldHaveContentIntent If the notification should have a content intent set. Used when the notification is clicked.
     *
     * @return A nullable [Notification].
     */
    fun getIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callInfo: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification?

    /**
     * Customizes the [Notification] to be displayed for ongoing calls. Outgoing calls are also considered ongoing, see the [isOutgoingCall] parameter.
     * @param callId An instance of [StreamCallId] representing the call identifier.
     * @param callInfo Call information that can be displayed as the primary content of the notification.
     * @param isOutgoingCall True if the call is outgoing.
     * @param remoteParticipantCount Number of remote participant. Can be used to further customize the notification.
     *
     * @return A nullable [Notification].
     */
    fun getOngoingCallNotification(
        callId: StreamCallId,
        callInfo: String? = null,
        isOutgoingCall: Boolean = false,
        remoteParticipantCount: Int = 0,
    ): Notification?

    /**
     * Get subsequent updates to notifications.
     * Initially, notifications are posted by one of the other methods, and then this method can be used to re-post them with updated content.
     *
     * @param coroutineScope Coroutine scope used for the updates.
     * @param call The Stream [Call] object.
     * @param localUser The local Stream user.
     * @param onUpdate Callback to be called when the notification is updated. Receives the updated [Notification] object.
     */
    fun getNotificationUpdates(
        coroutineScope: CoroutineScope,
        call: Call,
        localUser: User,
        onUpdate: (Notification) -> Unit,
    )

    /**
     * Customizes the temporary [Notification] that is displayed while setting up the call, i.e. until another notification is shown.
     *
     * @return A nullable [Notification].
     */
    fun getSettingUpCallNotification(): Notification?

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
