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

package io.getstream.video.android.core.notifications.internal

import android.app.Notification
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope

internal object NoOpNotificationHandler : NotificationHandler {
    override fun onRingingCall(callId: StreamCallId, callDisplayName: String) { /* NoOp */ }
    override fun onMissedCall(callId: StreamCallId, callDisplayName: String) { /* NoOp */ }
    override fun onNotification(callId: StreamCallId, callDisplayName: String) { /* NoOp */ }
    override fun onLiveCall(callId: StreamCallId, callDisplayName: String) { /* NoOp */ }
    override fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int,
    ): Notification? = null
    override fun getRingingCallNotification(
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String?,
        shouldHaveContentIntent: Boolean,
    ): Notification? = null
    override fun getSettingUpCallNotification(): Notification? = null
    override fun getNotificationUpdates(
        coroutineScope: CoroutineScope,
        call: Call,
        localUser: User,
        onUpdate: (Notification) -> Unit,
    ) { /* NoOp */ }

    override fun onPermissionDenied() { /* NoOp */ }
    override fun onPermissionGranted() { /* NoOp */ }
    override fun onPermissionRationale() { /* NoOp */ }
    override fun onPermissionRequested() { /* NoOp */ }
}
