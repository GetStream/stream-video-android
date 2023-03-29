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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.notifications.internal.RejectCallBroadcastReceiver.Companion.ACTION_REJECT_CALL
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.Failure
import io.getstream.video.android.core.utils.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.utils.INTENT_EXTRA_NOTIFICATION_ID
import io.getstream.video.android.core.utils.Success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Used to process any pending intents that feature the [ACTION_REJECT_CALL] action. By consuming this
 * event, it rejects a call without starting the application UI, notifying other participants that
 * this user won't join the call. After which it dismisses the originating notification.
 */
internal class RejectCallBroadcastReceiver : BroadcastReceiver() {

    val logger by taggedLogger("RejectCallBroadcastReceiver")

    /**
     * Checks the action to match [ACTION_REJECT_CALL] and then proceeds to reject the call and
     * dismisses any originating notifications.
     *
     * @param context The context of the app.
     * @param intent The pending intent used to trigger the receiver.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        logger.d { "[onReceive] context: $context, intent: $intent" }

        if (context != null && intent?.action == ACTION_REJECT_CALL) {
            val callCid = intent.getStringExtra(INTENT_EXTRA_CALL_CID)

            if (callCid.isNullOrBlank()) {
                return
            }
            val (type, id) = callCid.split(":")
            val preferences = UserPreferencesManager.initialize(context)

            val user = preferences.getUserCredentials()
            val apiKey = preferences.getApiKey()

            if (user != null && apiKey.isNotBlank()) {
                val streamVideo = StreamVideoBuilder(
                    context,
                    apiKey = apiKey,
                    user = user,
                    geo = GEO.GlobalEdgeNetwork
                ).build()

                CoroutineScope(Dispatchers.IO).launch {
                    when (val rejectResult = streamVideo.rejectCall(type, id)) {
                        is Success -> logger.d { "[onReceive] rejectCall, Success: $rejectResult" }
                        is Failure -> logger.d { "[onReceive] rejectCall, Failure: $rejectResult" }
                    }
                }
                val notificationId = intent.getIntExtra(INTENT_EXTRA_NOTIFICATION_ID, 0)
                NotificationManagerCompat.from(context).cancel(notificationId)
            }
        }
    }

    private companion object {
        /**
         * Represents the action used to reject a call.
         */
        private const val ACTION_REJECT_CALL = "io.getstream.video.android.action.REJECT_CALL"
    }
}
