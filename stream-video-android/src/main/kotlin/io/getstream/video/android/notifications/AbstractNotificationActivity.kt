package io.getstream.video.android.notifications

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.StreamVideoProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.INTENT_EXTRA_CALL_ACCEPTED
import io.getstream.video.android.utils.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.utils.INTENT_EXTRA_NOTIFICATION_ID
import io.getstream.video.android.utils.Success
import kotlinx.coroutines.launch

/**
 * Wrapper around certain logic required to process incoming notification data and payloads.
 *
 * Allows you to easily integrate push notification handling in your app, by extending the Activity.
 */
public abstract class AbstractNotificationActivity : AppCompatActivity(), StreamVideoProvider {

    private val streamVideo: StreamVideo by lazy { getStreamVideo(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeVideoIfNeeded()
        processNotificationData()
    }

    /**
     * Used to load the notification data, sent by our push provider delegate which is automatically
     * included and serves ringing use cases out of the box.
     *
     * If the user accepted the call through push, we automatically accept it and update the BE.
     *
     * Otherwise, we load the call data for the internal state.
     */
    private fun processNotificationData() {
        val hasAcceptedCall = intent.getBooleanExtra(INTENT_EXTRA_CALL_ACCEPTED, false)
        val callCid = intent.getStringExtra(INTENT_EXTRA_CALL_CID)

        if (callCid.isNullOrBlank()) {
            return
        }

        lifecycleScope.launch {
            if (hasAcceptedCall) {
                streamVideo.acceptCall(callCid)
                dismissIncomingCallNotifications()
            } else {
                loadCallData(callCid)
            }
        }
    }

    /**
     * Loads the call data and upserts it internally to the engine, that allows the automatic call
     * flow to trigger.
     *
     * @param callCid The CID containing the call ID and type.
     */
    private suspend fun loadCallData(callCid: String) {
        when (streamVideo.handlePushMessage(mapOf(INTENT_EXTRA_CALL_CID to callCid))) {
            is Success -> Unit
            is Failure -> finish()
        }
        dismissIncomingCallNotifications()
    }

    /**
     * Dismisses any notifications that might be active with a given notification ID.
     * Used to clear up the notification state if the call has been accepted or rejected.
     */
    private fun dismissIncomingCallNotifications() {
        val notificationId = intent.getIntExtra(INTENT_EXTRA_NOTIFICATION_ID, 0)
        NotificationManagerCompat.from(this).cancel(notificationId)
        finish()
    }

    /**
     * Used to initialize the [StreamVideo] client in case it hasn't been initialized yet.
     * This is required when starting the Activity from push notifications, while on the lock
     * screen and similar.
     */
    protected abstract fun initializeVideoIfNeeded()
}