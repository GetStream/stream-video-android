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
import io.getstream.result.Result
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_REJECT_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_NOTIFICATION_ID
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        val userDataStore: StreamUserDataStore by lazy {
            StreamUserDataStore.install(context!!)
        }
        if (context != null && intent?.action == ACTION_REJECT_CALL) {
            val callCid = intent.streamCallId(INTENT_EXTRA_CALL_CID)!!

            CoroutineScope(Dispatchers.IO).launch {
                // TODO: Running asynchronous code in a BroadcastReceiver isn't safe,
                // we should use wake locks.
                val streamVideo: StreamVideo = if (StreamVideo.isInstalled) {
                    StreamVideo.instance()
                } else {
                    userDataStore.user.firstOrNull()?.let { user ->
                        userDataStore.userToken.firstOrNull()?.let { userToken ->
                            StreamVideoBuilder(
                                context = context,
                                user = user,
                                token = userToken,
                                apiKey = userDataStore.apiKey.first(),
                            ).build().also { StreamVideo.removeClient() }
                        }
                    }!!
                }

                when (val rejectResult = streamVideo.call(callCid.type, callCid.id).reject()) {
                    is Result.Success -> logger.d { "[onReceive] rejectCall, Success: $rejectResult" }
                    is Result.Failure -> logger.d { "[onReceive] rejectCall, Failure: $rejectResult" }
                }
            }
            val notificationId = intent.getIntExtra(INTENT_EXTRA_NOTIFICATION_ID, 0)
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }
}
