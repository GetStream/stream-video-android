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

package io.getstream.video.android

import android.content.Context
import io.getstream.android.push.PushDevice
import io.getstream.android.push.delegate.PushDelegate
import io.getstream.log.StreamLog
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.token.AuthCredentialsProvider
import io.getstream.video.android.user.UserCredentialsManager
import io.getstream.video.android.user.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Class used to handle Push Notifications.
 * It is used by reflection by [io.getstream.android.push.delegate.PushDelegateProvider] class.
 */
internal class VideoPushDelegate(context: Context) : PushDelegate(context) {
    val logger = StreamLog.getLogger("VideoPushDelegate")
    val userPreferences: UserPreferences by lazy {
        UserCredentialsManager.initialize(context)
    }

    /**
     * Handle a push message.
     *
     * @param payload The content of the Push Notification.
     * @return true if the payload was handled properly.
     */
    override fun handlePushMessage(payload: Map<String, Any?>): Boolean {
        logger.d { "[handlePushMessage] payload: $payload" }
        return payload.ifValid {
            // TODO: Show PN Notification
        }
    }

    /**
     * Register a push device in our servers.
     *
     * @param pushDevice Contains info of the push device to be registered.
     */
    override fun registerPushDevice(pushDevice: PushDevice) {
        logger.d { "[registerPushDevice] pushDevice: $pushDevice" }
        userPreferences.getCachedCredentials()?.let { user ->
            userPreferences.getCachedApiKey()?.let { apiKey ->
                AuthCredentialsProvider(
                    user = user,
                    apiKey = apiKey,
                    userToken = user.token
                )
            }
        }?.let { authCredentialsProvider ->
            CoroutineScope(DispatcherProvider.IO).launch {
                StreamVideoBuilder(
                    context = context,
                    credentialsProvider = authCredentialsProvider,
                ).build()
                    .createDevice(
                        token = pushDevice.token,
                        pushProvider = pushDevice.pushProvider.key
                    )
            }
        }
    }

    /**
     * Return if the map is valid.
     * The effect function is only invoked in the case the map is valid.
     *
     * @param effect The function to be invoked on the case the map is valid.
     * @return true if the map is valid.
     */
    private fun Map<String, Any?>.ifValid(effect: () -> Unit): Boolean {
        val isValid = this.isValid()
        effect.takeIf { isValid }?.invoke()
        return isValid
    }

    private fun Map<String, Any?>.isValid(): Boolean =
        isFromStreamServer() && isValidIncomingCall()

    /**
     * Verify if the map contains key/value from Stream Server.
     */
    private fun Map<String, Any?>.isFromStreamServer(): Boolean = this[KEY_SENDER] == VALUE_STREAM_SENDER

    /**
     * Verify if the map contains all keys/values for an incoming call.
     */
    private fun Map<String, Any?>.isValidIncomingCall(): Boolean =
        !(this[KEY_TYPE] as? String).isNullOrBlank() &&
            !(this[KEY_CALL_CID] as? String).isNullOrBlank()

    private companion object {
        private const val KEY_SENDER = "sender"
        private const val KEY_TYPE = "type"
        private const val KEY_CALL_CID = "call_cid"

        private const val VALUE_STREAM_SENDER = "stream.video"
    }
}
