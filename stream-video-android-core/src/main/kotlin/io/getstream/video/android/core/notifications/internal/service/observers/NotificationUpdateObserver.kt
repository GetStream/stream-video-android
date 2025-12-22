/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.service.observers

import android.app.Notification
import android.content.Context
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.permissions.ForegroundServicePermissionManager
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class NotificationUpdateObserver(
    private val call: Call,
    private val streamVideo: StreamVideoClient,
    private val scope: CoroutineScope,
    private val permissionManager: ForegroundServicePermissionManager,
    val onStartService: (
        notificationId: Int,
        notification: Notification,
        trigger: String,
        foregroundServiceType: Int,
    ) -> Unit,
) {

    private val logger by taggedLogger("NotificationUpdateObserver")

    /**
     * Starts observing notification update triggers.
     */
    @OptIn(ExperimentalStreamVideoApi::class)
    fun observe(context: Context) {
        scope.launch {
            logger.d { "Observing notification updates for call: ${call.cid}" }

            val updateTriggers = getUpdateTriggers()

            updateTriggers.collectLatest { _ ->
                updateNotification(context)
            }
        }
    }

    /**
     * Gets the flow that triggers notification updates.
     */
    @OptIn(ExperimentalStreamVideoApi::class)
    private fun getUpdateTriggers() =
        streamVideo.streamNotificationManager
            .notificationConfig
            .notificationUpdateTriggers(call)
            ?: createDefaultUpdateTriggers()

    /**
     * Creates default update triggers from call state.
     */
    private fun createDefaultUpdateTriggers(): Flow<List<Any>> {
        return combine(
            call.state.ringingState,
            call.state.members,
            call.state.remoteParticipants,
            call.state.backstage,
        ) { ringingState, members, remoteParticipants, backstage ->
            listOf(ringingState, members, remoteParticipants, backstage)
        }.distinctUntilChanged()
    }

    /**
     * Updates the notification based on current call state.
     */
    private suspend fun updateNotification(context: Context) {
        val ringingState = call.state.ringingState.value
        logger.d { "[updateNotification] ringingState: $ringingState" }

        val notification = streamVideo.onCallNotificationUpdate(call)
        logger.d { "[updateNotification] notification: ${notification != null}" }

        if (notification != null) {
            showNotificationForState(context, ringingState, notification)
        } else {
            logger.w { "[updateNotification] No notification generated" }
        }
    }

    /**
     * Shows the appropriate notification based on ringing state.
     */
    private fun showNotificationForState(
        context: Context,
        ringingState: RingingState,
        notification: Notification,
    ) {
        val callId = StreamCallId(call.type, call.id)

        when (ringingState) {
            is RingingState.Active -> {
                showActiveCallNotification(context, callId, notification)
            }
            is RingingState.Outgoing -> {
                showOutgoingCallNotification(context, callId, notification)
            }
            is RingingState.Incoming -> {
                showIncomingCallNotification(context, callId, notification)
            }
            else -> {
                logger.d { "[updateNotification] Unhandled ringing state: $ringingState" }
            }
        }
    }

    private fun showActiveCallNotification(
        context: Context,
        callId: StreamCallId,
        notification: Notification,
    ) {
        logger.d { "[updateNotification] Showing active call notification" }
//        val notificationId = callId.hashCode()
        val notificationId = callId.getNotificationId(NotificationType.Ongoing)
        startForegroundWithServiceType(
            notificationId, // todo rahul - correct this
            notification,
            CallService.Companion.TRIGGER_ONGOING_CALL,
            permissionManager.getServiceType(context, CallService.Companion.TRIGGER_ONGOING_CALL),
        )
    }

    private fun showOutgoingCallNotification(
        context: Context,
        callId: StreamCallId,
        notification: Notification,
    ) {
        logger.d { "[updateNotification] Showing outgoing call notification" }
        startForegroundWithServiceType(
            callId.getNotificationId(NotificationType.Outgoing), // todo rahul - correct this
            notification,
            CallService.Companion.TRIGGER_OUTGOING_CALL,
            permissionManager.getServiceType(context, CallService.Companion.TRIGGER_OUTGOING_CALL),
        )
    }

    private fun showIncomingCallNotification(
        context: Context,
        callId: StreamCallId,
        notification: Notification,
    ) {
        logger.d { "[updateNotification] Showing incoming call notification" }
        startForegroundWithServiceType(
            callId.getNotificationId(NotificationType.Incoming),
            notification,
            CallService.Companion.TRIGGER_INCOMING_CALL,
            permissionManager.getServiceType(context, CallService.Companion.TRIGGER_INCOMING_CALL),
        )
    }

    fun startForegroundWithServiceType(
        notificationId: Int,
        notification: Notification,
        trigger: String,
        foregroundServiceType: Int,
    ) {
        onStartService(notificationId, notification, trigger, foregroundServiceType)
    }
}
