/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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
