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

package io.getstream.video.android.core.notifications

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoProvider
import io.getstream.video.android.core.utils.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.utils.INTENT_EXTRA_NOTIFICATION_ID
import io.getstream.video.android.model.StreamCallGuid
import io.getstream.video.android.model.StreamCallGuid.Companion.toTypeAndId
import io.getstream.video.android.model.streamCallGuid
import kotlinx.coroutines.launch

/**
 * Wrapper around certain logic required to process incoming notification data and payloads.
 *
 * Allows you to easily integrate push notification handling in your app, by extending the Activity.
 */
public abstract class AbstractNotificationActivity :
    ComponentActivity(),
    StreamVideoProvider {

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
        val hasAcceptedCall = intent.action == ACTION_ACCEPT_CALL
        val callCid = intent.streamCallGuid(INTENT_EXTRA_CALL_CID) ?: return

        lifecycleScope.launch {
            if (hasAcceptedCall) {
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
    private suspend fun loadCallData(guid: StreamCallGuid) {
//        when (streamVideo.handlePushMessage(mapOf(INTENT_EXTRA_CALL_CID to callCid))) {
//            is Result.Success -> Unit
//            is Result.Failure -> finish()
//        }
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

    private companion object {
        private const val ACTION_ACCEPT_CALL = "io.getstream.video.android.action.ACCEPT_CALL"
    }
}
