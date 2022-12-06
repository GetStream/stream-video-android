package io.getstream.video.android.dogfooding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.INTENT_EXTRA_CALL_ACCEPTED
import io.getstream.video.android.utils.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.utils.INTENT_EXTRA_NOTIFICATION_ID
import io.getstream.video.android.utils.Success
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeVideoIfNeeded()
        processNotificationData()
    }

    private fun processNotificationData() {
        val hasAcceptedCall = intent.getBooleanExtra(INTENT_EXTRA_CALL_ACCEPTED, false)
        val callCid = intent.getStringExtra(INTENT_EXTRA_CALL_CID)

        if (callCid.isNullOrBlank()) {
            return
        }

        lifecycleScope.launch {
            if (hasAcceptedCall) {
                dogfoodingApp.streamVideo.acceptCall(callCid)
                dismissIncomingCallNotifications()
            } else {
                loadCallData(callCid)
            }
        }
    }

    private suspend fun loadCallData(callCid: String) {
        when (dogfoodingApp.streamVideo.handlePushMessage(mapOf(INTENT_EXTRA_CALL_CID to callCid))) {
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

    private fun initializeVideoIfNeeded() {
        if (!dogfoodingApp.isInitialized()) {
            val hasInitialized = dogfoodingApp.initializeFromCredentials()

            if (!hasInitialized) {
                finish()
                return
            }
        }
    }
}