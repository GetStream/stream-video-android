package io.getstream.video.android.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.getstream.video.android.R
import io.getstream.video.android.model.state.StreamCallGuid
import io.getstream.video.android.utils.registerReceiverAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal object StreamCallReceiver {
    private const val ACTION_ACCEPT = "io.getstream.video.android.ACCEPT"
    private const val ACTION_REJECT = "io.getstream.video.android.REJECT"
    private const val ACTION_CANCEL = "io.getstream.video.android.CANCEL"

    private const val KEY_TYPE = "type"
    private const val KEY_ID = "id"
    private const val KEY_CID = "cid"

    private val IMMUTABLE_PENDING_INTENT_FLAGS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    internal suspend fun registerAsFlow(context: Context): Flow<Action> {
        return context.registerReceiverAsFlow(ACTION_ACCEPT, ACTION_REJECT, ACTION_CANCEL)
            .map {
                val callGuid = it.extractCallGuid()
                when (val action = it.action) {
                    ACTION_ACCEPT -> Action.Accept(callGuid)
                    ACTION_REJECT -> Action.Reject(callGuid)
                    ACTION_CANCEL -> Action.Cancel(callGuid)
                    else -> error("unexpected action: $action")
                }
            }
    }

    internal fun createAcceptAction(
        context: Context,
        notificationId: Int,
        guid: StreamCallGuid
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_call,
            context.getString(R.string.stream_call_notification_action_accept),
            createAcceptPendingIntent(context, notificationId, guid),
        ).build()
    }

    internal fun createRejectAction(
        context: Context,
        notificationId: Int,
        guid: StreamCallGuid
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_delete,
            context.getString(R.string.stream_call_notification_action_accept),
            createRejectPendingIntent(context, notificationId, guid),
        ).build()
    }

    internal fun createCancelAction(
        context: Context,
        notificationId: Int,
        guid: StreamCallGuid
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_delete,
            context.getString(R.string.stream_call_notification_action_accept),
            createRejectPendingIntent(context, notificationId, guid),
        ).build()
    }

    private fun createAcceptPendingIntent(
        context: Context,
        notificationId: Int,
        guid: StreamCallGuid
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        createNotifyIntent(context, guid, ACTION_ACCEPT),
        IMMUTABLE_PENDING_INTENT_FLAGS,
    )

    private fun createRejectPendingIntent(
        context: Context,
        notificationId: Int,
        guid: StreamCallGuid
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        createNotifyIntent(context, guid, ACTION_REJECT),
        IMMUTABLE_PENDING_INTENT_FLAGS,
    )

    private fun createNotifyIntent(context: Context, guid: StreamCallGuid, action: String) =
        Intent(context, StreamCallReceiver::class.java).apply {
            putExtra(KEY_TYPE, guid.type)
            putExtra(KEY_CID, guid.id)
            putExtra(KEY_TYPE, guid.cid)
            this.action = action
        }

    private fun Intent.extractCallGuid() = StreamCallGuid(
        type = getStringExtra(KEY_TYPE) ?: error("no type found"),
        id = getStringExtra(KEY_ID) ?: error("no id found"),
        cid = getStringExtra(KEY_CID) ?: error("no cid found")
    )

    internal sealed class Action {
        abstract val guid: StreamCallGuid
        data class Accept(override val guid: StreamCallGuid) : Action()
        data class Reject(override val guid: StreamCallGuid) : Action()
        data class Cancel(override val guid: StreamCallGuid) : Action()
    }

}