package io.getstream.video.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.getstream.logging.StreamLog
import io.getstream.video.android.R
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.service.StreamCallReceiver.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import io.getstream.video.android.model.state.StreamCallState as State

public abstract class StreamCallService : Service() {

    private val logger = StreamLog.getLogger(TAG)

    private val scope = CoroutineScope(DispatcherProvider.Default)

    private val streamCalls: StreamCalls by lazy { getStreamCalls(this) }

    private val notificationManager: NotificationManager by lazy {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).also { notificationManager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = getDefaultNotificationChannel(this)
                notificationManager.createNotificationChannel(notificationChannel())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            StreamCallReceiver.registerAsFlow(application).collect {
                logger.v { "[observeAction] action: $it" }
                when (it) {
                    is Action.Accept -> streamCalls.acceptCall(it.guid.type, it.guid.id)
                    is Action.Reject -> streamCalls.rejectCall(it.guid.cid)
                    is Action.Cancel -> streamCalls.cancelCall(it.guid.cid)
                }
            }
        }
        scope.launch {
            streamCalls.callState.collect {
                logger.v { "[observeState] state: $it" }
                updateNotification(it)
            }
        }
        startForeground(streamCalls.callState.value)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    protected abstract fun getStreamCalls(context: Context): StreamCalls

    protected open fun getNotificationId(context: Context): Int = R.id.stream_call_notification

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground(state: State) {
        if (state !is State.Active) {
            logger.w { "[startForeground] rejected (state is not Active): $state" }
            stopSelf()
            return
        }
        logger.v { "[startForeground] state: $state" }
        val (notificationId, notification) = buildNotification(state)
        startForeground(notificationId, notification)
    }

    private fun updateNotification(state: State) {
        if (state !is State.Active) {
            logger.w { "[updateNotification] rejected (state is not Active): $state" }
            stopSelf()
            return
        }
        logger.v { "[updateNotification] state: $state" }
        val (notificationId, notification) = buildNotification(state)
        notificationManager.notify(notificationId, notification)
    }

    private fun buildNotification(state: State.Active): Pair<Int, Notification> {
        val notificationId = getNotificationId(this)
        val notification = getNotificationBuilder(
            contentTitle = getContentTitle(state),
            contentText = getContentText(state),
            groupKey = state.callGuid.cid,
            intent = Intent(Intent.ACTION_CALL)
        ).apply {
            buildNotificationActions(notificationId, state).forEach {
                addAction(it)
            }
        }.build()
        return Pair(notificationId, notification)
    }

    private fun buildNotificationActions(notificationId: Int, state: State.Active): Array<NotificationCompat.Action> {
        return when (state) {
            is State.Incoming -> arrayOf(
                StreamCallReceiver.createRejectAction(application, notificationId, state.callGuid),
                StreamCallReceiver.createAcceptAction(application, notificationId, state.callGuid)
            )
            is State.Starting,
            is State.Outgoing,
            is State.Joining,
            is State.Connecting,
            is State.Connected -> arrayOf(
                StreamCallReceiver.createCancelAction(application, notificationId, state.callGuid)
            )
            is State.Drop -> emptyArray()
        }
    }

    private fun getContentTitle(state: State.Active): String {
        return when (state) {
            is State.Starting -> "Starting call of ${state.memberUserIds.size} people"
            is State.Outgoing -> "Outgoing Call"
            is State.Incoming -> "Incoming"
            is State.Joining -> "Joining"
            is State.Connected -> "Connected"
            is State.Connecting -> "Connecting"
            is State.Drop -> "Drop"
        }
    }

    private fun getContentText(state: State.Active): String {
        return when (state) {
            is State.Starting -> "Starting"
            is State.Outgoing -> "Outgoing"
            is State.Incoming -> "Incoming"
            is State.Joining -> "Joining"
            is State.Connected -> "Connected"
            is State.Connecting -> "Connecting"
            is State.Drop -> "Drop"
        }
    }

    private fun getNotificationBuilder(
        contentTitle: String,
        contentText: String,
        groupKey: String,
        intent: Intent,
    ): NotificationCompat.Builder {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            flags,
        )

        return NotificationCompat.Builder(this, getNotificationChannelId())
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(false)
            .setSmallIcon(R.drawable.baseline_call_stream_24dp)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(contentIntent)
            .setGroup(groupKey)
    }

    private fun getNotificationChannelId(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getDefaultNotificationChannel(this)().id
        } else {
            ""
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDefaultNotificationChannel(context: Context): (() -> NotificationChannel) {
        return {
            NotificationChannel(
                context.getString(R.string.stream_call_notification_channel_id),
                context.getString(R.string.stream_call_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        }
    }

    public companion object {
        @PublishedApi
        internal const val TAG: String = "Call:StreamService"

        public inline fun <reified T : StreamCallService> start(context: Context) {
            StreamLog.v(TAG) { "/start/ service: ${T::class}" }
            ContextCompat.startForegroundService(
                context, Intent(context, T::class.java)
            )
        }

        public inline fun <reified T : StreamCallService> stop(context: Context) {
            StreamLog.v(TAG) { "/stop/ service: ${T::class}" }
            context.stopService(
                Intent(context, T::class.java)
            )
        }
    }
}