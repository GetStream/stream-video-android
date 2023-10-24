/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.notifications.DefaultNotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_LIVE_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_ONGOING_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_REJECT_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.model.StreamCallId

internal class DefaultStreamIntentResolver(val context: Context) {

    private val logger by taggedLogger("IntentResolver")

    /**
     * Search for an activity that can receive incoming calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     */
    internal fun searchIncomingCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = NotificationHandler.INCOMING_CALL_NOTIFICATION_ID,
    ): PendingIntent? =
        searchActivityPendingIntent(
            Intent(NotificationHandler.ACTION_INCOMING_CALL),
            callId,
            notificationId,
        )

    /**
     * Search for an activity that can receive live calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     */
    internal fun searchNotificationCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
    ): PendingIntent? =
        searchActivityPendingIntent(
            Intent(NotificationHandler.ACTION_NOTIFICATION),
            callId,
            notificationId,
        )

    /**
     * Search for an activity that can receive live calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     */
    internal fun searchLiveCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
    ): PendingIntent? =
        searchActivityPendingIntent(
            Intent(NotificationHandler.ACTION_LIVE_CALL),
            callId,
            notificationId,
        )

    /**
     * Search for an activity that can accept call from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @return The [PendingIntent] which can trigger a component to consume accept call events.
     */
    internal fun searchAcceptCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = NotificationHandler.INCOMING_CALL_NOTIFICATION_ID,
    ): PendingIntent? =
        searchActivityPendingIntent(
            Intent(NotificationHandler.ACTION_ACCEPT_CALL),
            callId,
            notificationId,
        )

    /**
     * Searches for a broadcast receiver that can consume the [ACTION_REJECT_CALL] intent to reject
     * a call from the Stream Server.
     *
     * @param callId The ID of the call.
     * @return The [PendingIntent] which can trigger a component to consume the call rejection event.
     */
    internal fun searchRejectCallPendingIntent(
        callId: StreamCallId,
    ): PendingIntent? = searchBroadcastPendingIntent(Intent(ACTION_REJECT_CALL), callId)

    /**
     * Searches for a broadcast receiver that can consume the [ACTION_REJECT_CALL] intent to reject
     * a call from the Stream Server.
     *
     * @param callId The ID of the call.
     * @return The [PendingIntent] which can trigger a component to consume the call rejection event.
     */
    internal fun searchEndCallPendingIntent(
        callId: StreamCallId,
    ): PendingIntent? = searchBroadcastPendingIntent(
        Intent(NotificationHandler.ACTION_LEAVE_CALL),
        callId,
    )

    /**
     * Searches an activity that will accept the [ACTION_ONGOING_CALL] intent and jump right back into the call.
     *
     * @param callId the call id
     * @param notificationId the notification ID.
     */
    fun searchOngoingCallPendingIntent(callId: StreamCallId, notificationId: Int): PendingIntent? {
        val intent = Intent(ACTION_ONGOING_CALL)
        intent.putExtra(INTENT_EXTRA_CALL_CID, callId.cid)
        return searchActivityPendingIntent(
            intent,
            callId,
            notificationId,
        )
    }

    private fun searchBroadcastPendingIntent(
        baseIntent: Intent,
        callId: StreamCallId,
    ): PendingIntent? =
        searchResolveInfo {
            context.packageManager.queryBroadcastReceivers(
                baseIntent,
                0,
            )
        }?.let {
            getBroadcastForIntent(baseIntent, it, callId)
        }

    private fun searchActivityPendingIntent(
        baseIntent: Intent,
        callId: StreamCallId,
        notificationId: Int,
    ): PendingIntent? =
        searchResolveInfo { context.packageManager.queryIntentActivities(baseIntent, 0) }?.let {
            getActivityForIntent(baseIntent, it, callId, notificationId)
        }

    private fun searchResolveInfo(availableComponents: () -> List<ResolveInfo>): ResolveInfo? =
        availableComponents()
            .filter { it.activityInfo.packageName == context.packageName }
            .maxByOrNull { it.priority }

    /**
     * Uses the provided [ResolveInfo] to find an Activity which can consume the intent.
     *
     * @param baseIntent The base intent for the notification.
     * @param resolveInfo Info used to resolve a component matching the action.
     * @param callId The ID of the call.
     * @param flags Any flags required by the component.
     */
    private fun getActivityForIntent(
        baseIntent: Intent,
        resolveInfo: ResolveInfo,
        callId: StreamCallId,
        notificationId: Int,
        flags: Int = DefaultNotificationHandler.PENDING_INTENT_FLAG,
    ): PendingIntent {
        val baseIntentAction =
            requireNotNull(
                baseIntent.action,
            ) { logger.e { "Developer error. Intent action must be set" } }
        val dismissIntent = DismissNotificationActivity
            .createIntent(context, notificationId, baseIntentAction)

        return PendingIntent.getActivities(
            context,
            0,
            arrayOf(buildComponentIntent(baseIntent, resolveInfo, callId), dismissIntent),
            flags,
        )
    }

    /**
     * Uses the provided [ResolveInfo] to find a BroadcastReceiver which can consume the intent.
     *
     * @param baseIntent The base intent for the notification.
     * @param resolveInfo Info used to resolve a component matching the action.
     * @param callId The ID of the call.
     * @param flags Any flags required by the component.
     */
    private fun getBroadcastForIntent(
        baseIntent: Intent,
        resolveInfo: ResolveInfo,
        callId: StreamCallId,
        flags: Int = DefaultNotificationHandler.PENDING_INTENT_FLAG,
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            buildComponentIntent(baseIntent, resolveInfo, callId),
            flags,
        )
    }

    /**
     * Builds an intent used to start the target component for the [PendingIntent].
     *
     * @param baseIntent The base intent with fundamental data and actions.
     * @param resolveInfo Info used to resolve a component matching the action.
     * @param callId The ID of the call.
     */
    private fun buildComponentIntent(
        baseIntent: Intent,
        resolveInfo: ResolveInfo,
        callId: StreamCallId,
    ): Intent {
        return Intent(baseIntent).apply {
            component = ComponentName(
                resolveInfo.activityInfo.applicationInfo.packageName,
                resolveInfo.activityInfo.name,
            )
            putExtra(NotificationHandler.INTENT_EXTRA_CALL_CID, callId)
            putExtra(
                NotificationHandler.INTENT_EXTRA_NOTIFICATION_ID,
                NotificationHandler.INCOMING_CALL_NOTIFICATION_ID,
            )
        }
    }
}
