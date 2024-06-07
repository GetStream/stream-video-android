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

package io.getstream.android.samples.ringingcall

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.getstream.android.push.firebase.FirebaseMessagingDelegate

class FirebasePushService : FirebaseMessagingService() {

    companion object {
        // This field value should be equal to the configuration name on your stream Dashboard.
        const val PROVIDER_NAME = "firebase"
    }

    override fun onNewToken(token: String) {
        // Update device's token on Stream backend
        try {
            FirebaseMessagingDelegate.registerFirebaseToken(
                token,
                providerName = PROVIDER_NAME,
            )
        } catch (exception: IllegalStateException) {
            // StreamVideo was not initialized
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            if (!FirebaseMessagingDelegate.handleRemoteMessage(message)) {
                // RemoteMessage was not for stream and needs further processing
                Log.d("Firebase", "PN was not for stream")
            }
        } catch (exception: IllegalStateException) {
            // StreamVideo was not initialized, you can do nothing here, about Stream SDK
            // Maybe log some errors
        }
    }
}
