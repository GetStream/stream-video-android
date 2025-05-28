/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.push.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.getstream.android.push.firebase.FirebaseMessagingDelegate
import io.getstream.log.StreamLog

internal class DemoAppFirebaseService : FirebaseMessagingService() {
    private val logger = StreamLog.getLogger("Push:DemoAppFirebaseService")

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        logger.d { "[onFirebaseMessageReceived] remoteMessage: $remoteMessage" }
        try {
            FirebaseMessagingDelegate.handleRemoteMessage(remoteMessage)
                .also {
                    when (it) {
                        true -> logger.d { "[onFirebaseMessageReceived] message handled successfully" }
                        false -> logger.d { "[onFirebaseMessageReceived] message was not handled" }
                    }
                }
        } catch (exception: IllegalStateException) {
            logger.e(
                exception,
            ) { "[onFirebaseMessageReceived] error while handling remote message" }
        } finally {
            stopSelf()
        }
    }

    override fun onNewToken(token: String) {
        try {
            FirebaseMessagingDelegate.registerFirebaseToken(token, "")
        } catch (exception: IllegalStateException) {
            logger.e(exception) { "[onFirebaseNewToken] error while registering Firebase Token" }
        }
    }
}
