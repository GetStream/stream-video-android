package io.getstream.video.android.core.notifications

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.video.android.R
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_ACCEPT_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_INCOMING_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_REJECT_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.internal.DismissNotificationActivity
import io.getstream.video.android.core.utils.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.utils.INTENT_EXTRA_NOTIFICATION_ID
import io.getstream.video.android.model.StreamCallId

open public class DefaultNotificationHandler(
    private val context: Context,
) : NotificationHandler {

    private val logger: TaggedLogger by taggedLogger("Video:DefaultNotificationHandler")
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.createNotificationChannel(
                    NotificationChannelCompat
                        .Builder(
                            getChannelId(),
                            NotificationManager.IMPORTANCE_HIGH
                        )
                        .setName(getChannelName())
                        .setDescription(getChannelDescription())
                        .build()
                )
            }
        }
    }

    override fun onRiningCall(callId: StreamCallId, callDisplayName: String) {
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
            } ?: logger.e { "Couldn't find any activity for ${ACTION_ACCEPT_CALL}" }
        } ?: logger.e { "Couldn't find any activity for ${ACTION_INCOMING_CALL}" }
    }

    override fun onNotification(callId: StreamCallId) {
        TODO("Not yet implemented")
    }

    override fun onLivestream(callId: StreamCallId) {
        TODO("Not yet implemented")
    }

    /**
     * Search for an activity that can receive incoming calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     */
    private fun searchIncomingCallPendingIntent(
        callId: StreamCallId
    ): PendingIntent? = searchActivityPendingIntent(Intent(ACTION_INCOMING_CALL), callId)

    /**
     * Search for an activity that can accept call from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @return The [PendingIntent] which can trigger a component to consume accept call events.
     */
    private fun searchAcceptCallPendingIntent(
        callId: StreamCallId,
    ): PendingIntent? = searchActivityPendingIntent(Intent(ACTION_ACCEPT_CALL), callId)

    /**
     * Searches for a broadcast receiver that can consume the [ACTION_REJECT_CALL] intent to reject
     * a call from the Stream Server.
     *
     * @param callId The ID of the call.
     * @return The [PendingIntent] which can trigger a component to consume the call rejection event.
     */
    private fun searchRejectCallPendingIntent(
        callId: StreamCallId
    ): PendingIntent? = searchBroadcastPendingIntent(Intent(ACTION_REJECT_CALL), callId)

    private fun searchBroadcastPendingIntent(
        baseIntent: Intent,
        callId: StreamCallId,
    ): PendingIntent? =
        searchResolveInfo { context.packageManager.queryBroadcastReceivers(baseIntent, 0) }?.let {
            getBroadcastForIntent(baseIntent, it, callId)
        }

    private fun searchActivityPendingIntent(
        baseIntent: Intent,
        callId: StreamCallId,
    ): PendingIntent? =
        searchResolveInfo { context.packageManager.queryIntentActivities(baseIntent, 0) }?.let {
            getActivityForIntent(baseIntent, it, callId)
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
        flags: Int = PENDING_INTENT_FLAG,
    ): PendingIntent {
        val dismissIntent = DismissNotificationActivity
            .createIntent(context, INCOMING_CALL_NOTIFICATION_ID)
        return PendingIntent.getActivities(
            context,
            0,
            arrayOf(buildComponentIntent(baseIntent, resolveInfo, callId), dismissIntent),
            flags
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
            context, 0,
            buildComponentIntent(baseIntent, resolveInfo, callId),
            flags
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
        callId: StreamCallId
    ): Intent {
        return Intent(baseIntent).apply {
            component = ComponentName(
                resolveInfo.activityInfo.applicationInfo.packageName,
                resolveInfo.activityInfo.name
            )
            putExtra(INTENT_EXTRA_CALL_CID, callId)
            putExtra(INTENT_EXTRA_NOTIFICATION_ID, INCOMING_CALL_NOTIFICATION_ID)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showIncomingCallNotification(
        fullScreenPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
        rejectCallPendingIntent: PendingIntent,
        callDisplayName: String,
    ) {
        println("JcLog: [showIncomingCallNotification]")
        val notification = NotificationCompat.Builder(context, getChannelId())
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("Incoming call")
            .setContentText(callDisplayName)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(R.string.stream_video_call_notification_action_accept),
                    acceptCallPendingIntent,
                ).build()
            ).addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(R.string.stream_video_call_notification_action_reject),
                    rejectCallPendingIntent
                ).build()
            ).build()
        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)
    }

    open fun getChannelId(): String = CHANNEL_ID
    open fun getChannelName(): String = CHANNEL_NAME
    open fun getChannelDescription(): String = CHANNEL_DESCRIPTION

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