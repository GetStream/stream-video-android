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

package io.getstream.video.android.core.notifications

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CallStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import io.getstream.android.push.permissions.DefaultNotificationPermissionHandler
import io.getstream.android.push.permissions.NotificationPermissionHandler
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.R
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_ACCEPT_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_INCOMING_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_LIVE_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_NOTIFICATION
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_REJECT_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.internal.DismissNotificationActivity
import io.getstream.video.android.core.utils.stringOrDefault
import io.getstream.video.android.model.StreamCallId

public open class DefaultNotificationHandler(
    private val application: Application,
    private val notificationPermissionHandler: NotificationPermissionHandler =
        DefaultNotificationPermissionHandler
            .createDefaultNotificationPermissionHandler(application),
) : NotificationHandler,
    NotificationPermissionHandler by notificationPermissionHandler {

    private val logger: TaggedLogger by taggedLogger("Video:DefaultNotificationHandler")
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(application).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.createNotificationChannel(
                    NotificationChannelCompat
                        .Builder(
                            getChannelId(),
                            NotificationManager.IMPORTANCE_HIGH,
                        )
                        .setName(getChannelName())
                        .setDescription(getChannelDescription())
                        .build(),
                )
            }
        }
    }

    override fun onRingingCall(callId: StreamCallId, callDisplayName: String) {
        searchIncomingCallPendingIntent(callId)?.let { fullScreenPendingIntent ->
            searchAcceptCallPendingIntent(callId)?.let { acceptCallPendingIntent ->
                searchRejectCallPendingIntent(callId)?.let { rejectCallPendingIntent ->
                    showIncomingCallNotification(
                        fullScreenPendingIntent,
                        acceptCallPendingIntent,
                        rejectCallPendingIntent,
                        callDisplayName,
                    )
                }
            } ?: logger.e { "Couldn't find any activity for $ACTION_ACCEPT_CALL" }
        } ?: logger.e { "Couldn't find any activity for $ACTION_INCOMING_CALL" }
    }

    override fun onNotification(callId: StreamCallId, callDisplayName: String) {
        val notificationId = callId.hashCode()
        searchNotificationCallPendingIntent(callId, notificationId)
            ?.let { notificationPendingIntent ->
                showNotificationCallNotification(
                    notificationPendingIntent,
                    callDisplayName,
                    notificationId,
                )
            } ?: logger.e { "Couldn't find any activity for $ACTION_NOTIFICATION" }
    }

    override fun onLiveCall(callId: StreamCallId, callDisplayName: String) {
        val notificationId = callId.hashCode()
        searchLiveCallPendingIntent(callId, notificationId)?.let { liveCallPendingIntent ->
            showLiveCallNotification(
                liveCallPendingIntent,
                callDisplayName,
                notificationId,
            )
        } ?: logger.e { "Couldn't find any activity for $ACTION_LIVE_CALL" }
    }

    /**
     * Search for an activity that can receive incoming calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     */
    private fun searchIncomingCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = INCOMING_CALL_NOTIFICATION_ID,
    ): PendingIntent? =
        searchActivityPendingIntent(Intent(ACTION_INCOMING_CALL), callId, notificationId)

    /**
     * Search for an activity that can receive live calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     */
    private fun searchNotificationCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
    ): PendingIntent? =
        searchActivityPendingIntent(Intent(ACTION_NOTIFICATION), callId, notificationId)

    /**
     * Search for an activity that can receive live calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     */
    private fun searchLiveCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
    ): PendingIntent? =
        searchActivityPendingIntent(Intent(ACTION_LIVE_CALL), callId, notificationId)

    /**
     * Search for an activity that can accept call from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @return The [PendingIntent] which can trigger a component to consume accept call events.
     */
    private fun searchAcceptCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = INCOMING_CALL_NOTIFICATION_ID,
    ): PendingIntent? =
        searchActivityPendingIntent(Intent(ACTION_ACCEPT_CALL), callId, notificationId)

    /**
     * Searches for a broadcast receiver that can consume the [ACTION_REJECT_CALL] intent to reject
     * a call from the Stream Server.
     *
     * @param callId The ID of the call.
     * @return The [PendingIntent] which can trigger a component to consume the call rejection event.
     */
    private fun searchRejectCallPendingIntent(
        callId: StreamCallId,
    ): PendingIntent? = searchBroadcastPendingIntent(Intent(ACTION_REJECT_CALL), callId)

    private fun searchBroadcastPendingIntent(
        baseIntent: Intent,
        callId: StreamCallId,
    ): PendingIntent? =
        searchResolveInfo {
            application.packageManager.queryBroadcastReceivers(
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
        searchResolveInfo { application.packageManager.queryIntentActivities(baseIntent, 0) }?.let {
            getActivityForIntent(baseIntent, it, callId, notificationId)
        }

    private fun searchResolveInfo(availableComponents: () -> List<ResolveInfo>): ResolveInfo? =
        availableComponents()
            .filter { it.activityInfo.packageName == application.packageName }
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
        flags: Int = PENDING_INTENT_FLAG,
    ): PendingIntent {
        val baseIntentAction =
            requireNotNull(
                baseIntent.action,
            ) { logger.e { "Developer error. Intent action must be set" } }
        val dismissIntent = DismissNotificationActivity
            .createIntent(application, notificationId, baseIntentAction)

        return PendingIntent.getActivities(
            application,
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
        flags: Int = PENDING_INTENT_FLAG,
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            application,
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
            putExtra(INTENT_EXTRA_CALL_CID, callId)
            putExtra(INTENT_EXTRA_NOTIFICATION_ID, INCOMING_CALL_NOTIFICATION_ID)
        }
    }

    private fun showNotificationCallNotification(
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

    private fun showLiveCallNotification(
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

    private fun showIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callDisplayName: String,
        notificationId: Int = INCOMING_CALL_NOTIFICATION_ID,
    ) {
        showNotification(notificationId) {
            priority = NotificationCompat.PRIORITY_HIGH
            setContentTitle("Incoming call")
            setContentText(callDisplayName)
            setOngoing(false)
            setContentIntent(fullScreenPendingIntent)
            setFullScreenIntent(fullScreenPendingIntent, true)
            setCategory(NotificationCompat.CATEGORY_CALL)
            addCallActions(acceptCallPendingIntent, rejectCallPendingIntent, callDisplayName)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        notificationId: Int,
        builder: NotificationCompat.Builder.() -> Unit,
    ) {
        val notification = NotificationCompat.Builder(application, getChannelId())
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setAutoCancel(true)
            .apply(builder)
            .build()
        notificationManager.notify(notificationId, notification)
    }
    private fun NotificationCompat.Builder.addCallActions(
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callDisplayName: String,
    ): NotificationCompat.Builder = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setStyle(
                CallStyle.forIncomingCall(
                    Person.Builder()
                        .setName(callDisplayName)
                        .build(),
                    rejectCallPendingIntent,
                    acceptCallPendingIntent,
                ),
            )
        } else {
            addAction(
                NotificationCompat.Action.Builder(
                    null,
                    application.getString(R.string.stream_video_call_notification_action_accept),
                    acceptCallPendingIntent,
                ).build(),
            )
            addAction(
                NotificationCompat.Action.Builder(
                    null,
                    application.getString(R.string.stream_video_call_notification_action_reject),
                    rejectCallPendingIntent,
                ).build(),
            )
        }
    }

    open fun getChannelId(): String = stringOrDefault(
        application.applicationContext,
        R.string.stream_video_incoming_call_notification_channel_id,
        CHANNEL_ID,
    )

    open fun getChannelName(): String = stringOrDefault(
        application.applicationContext,
        R.string.stream_video_incoming_call_notification_channel_title,
        CHANNEL_NAME,
    )
    open fun getChannelDescription(): String = stringOrDefault(
        application.applicationContext,
        R.string.stream_video_incoming_call_notification_channel_description,
        CHANNEL_DESCRIPTION,
    )

    companion object {
        private const val CHANNEL_ID = "incoming_calls"
        private const val CHANNEL_NAME = "Incoming Calls"
        private const val CHANNEL_DESCRIPTION = "Incoming audio and video call alerts"

        private val PENDING_INTENT_FLAG: Int by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        }
    }
}
