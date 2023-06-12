package io.getstream.video.android.core.notifications.internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat

internal class DismissNotificationActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationManagerCompat.from(this)
            .cancel(intent.getIntExtra(KEY_NOTIFICATION_ID, 0))
        finish()
    }

    companion object {
        private const val KEY_NOTIFICATION_ID = "KEY_NOTIFICATION_ID"
        fun createIntent(context: Context, notificationId: Int): Intent =
            Intent(context, DismissNotificationActivity::class.java)
                .apply {
                    putExtra(KEY_NOTIFICATION_ID, notificationId)
                }
    }
}