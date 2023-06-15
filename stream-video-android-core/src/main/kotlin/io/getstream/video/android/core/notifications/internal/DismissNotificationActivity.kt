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
