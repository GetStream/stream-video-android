/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

        /**
         * @param baseIntentAction - Important - the action must be set. We are creating multiple
         * dismiss intents (accept, reject, ...) and the system will "think" the Pending intents
         * we create in [DefaultNotificationHandler.getActivityForIntent] are identical. This can
         * lead to a reused intent with a wrong action.
         * See documentation of [PendingIntent.getActivities] - the last intent in the list is the key.
         *
         */
        fun createIntent(context: Context, notificationId: Int, baseIntentAction: String): Intent =
            Intent(context, DismissNotificationActivity::class.java)
                .apply {
                    putExtra(KEY_NOTIFICATION_ID, notificationId)
                    action = baseIntentAction
                }
    }
}
