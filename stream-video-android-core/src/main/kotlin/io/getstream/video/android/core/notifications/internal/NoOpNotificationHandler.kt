/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal

import android.app.Notification
import android.app.PendingIntent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.dispatchers.NotificationDispatcher
import io.getstream.video.android.model.StreamCallId

internal object NoOpNotificationHandler : NotificationHandler {

    override fun onRingingCall(
        callId: StreamCallId,
        callDisplayName: String,
        payload: Map<String, Any?>,
    ) {
        /* NoOp */
    }

    override fun onMissedCall(
        callId: StreamCallId,
        callDisplayName: String,
        payload: Map<String, Any?>,
    ) {
        /* NoOp */
    }

    override fun onNotification(
        callId: StreamCallId,
        callDisplayName: String,
        payload: Map<String, Any?>,
    ) {
        /* NoOp */
    }

    override fun onLiveCall(
        callId: StreamCallId,
        callDisplayName: String,
        payload: Map<String, Any?>,
    ) {
        /* NoOp */
    }

    override fun getIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callerName: String?,
        shouldHaveContentIntent: Boolean,
        payload: Map<String, Any?>,
    ): Notification? = null

    override fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
        payload: Map<String, Any?>,
    ): Notification? = null
    override suspend fun onCallNotificationUpdate(call: Call): Notification? = null

    override suspend fun updateOngoingCallNotification(
        call: Call,
        callDisplayName: String,
    ): Notification? = null

    override suspend fun updateOutgoingCallNotification(
        call: Call,
        callDisplayName: String?,
    ): Notification? = null

    override suspend fun updateIncomingCallNotification(
        call: Call,
        callDisplayName: String,
    ): Notification? = null

    override fun getStreamNotificationDispatcher(): NotificationDispatcher {
        return (null as NotificationDispatcher)
    }

    override fun getRingingCallNotification(
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String?,
        shouldHaveContentIntent: Boolean,
        payload: Map<String, Any?>,
    ): Notification? = null

    override fun getMissedCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        payload: Map<String, Any?>,
    ): Notification? = null

    override fun getSettingUpCallNotification(
        trigger: String,
        callId: StreamCallId,
    ): Notification? = null

    override fun onPermissionDenied() {
        /* NoOp */
    }

    override fun onPermissionGranted() {
        /* NoOp */
    }

    override fun onPermissionRationale() {
        /* NoOp */
    }

    override fun onPermissionRequested() {
        /* NoOp */
    }
}
