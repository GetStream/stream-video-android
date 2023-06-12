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

package io.getstream.video.android.core.notifications.internal

import android.content.Context
import io.getstream.android.push.PushDevice
import io.getstream.android.push.delegate.PushDelegate
import io.getstream.android.push.delegate.PushDelegateProvider
import io.getstream.log.StreamLog
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.mapper.toTypeAndId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Class used to handle Push Notifications.
 *
 * It is used by reflection by [PushDelegateProvider] class.
 */
internal class VideoPushDelegate(
    context: Context
) : PushDelegate(context) {
    private val logger = StreamLog.getLogger("VideoPushDelegate")
    private val userDataStore: StreamUserDataStore by lazy {
        StreamUserDataStore.install(context)
    }
    private val streamVideo: StreamVideo?
        get() = if (StreamVideo.isInstalled) {
            StreamVideo.instance()
        } else {
            userDataStore.user.value?.let { user ->
                userDataStore.userToken.value?.let { userToken ->
                    StreamVideoBuilder(
                        context = context,
                        user = user,
                        token = userToken,
                        apiKey = userDataStore.apiKey.value,
                    ).build().also { StreamVideo.unInstall() }
                }
            }
        }

    /**
     * Handle a push message.
     *
     * @param payload The content of the Push Notification.
     * @return true if the payload was handled properly.
     */
    override fun handlePushMessage(payload: Map<String, Any?>): Boolean {
        println("JcLog: handlePushMessage($payload)")
        logger.d { "[handlePushMessage] payload: $payload" }
        return payload.ifValid {
            println("JcLog: Is valid payload")

            val callId = (payload[KEY_CALL_CID] as String).toTypeAndId()
                .let { StreamCallId(it.first, it.second) }
            when (payload[KEY_TYPE]) {
                KEY_TYPE_RING -> handleRingType(callId, payload)
                KEY_TYPE_NOTIFICATION -> handleNotificationType(callId, payload)
                KEY_TYPE_LIVE_STARTED -> handleLiveStartedType(callId, payload)
            }
        }
    }

    private fun handleRingType(callId: StreamCallId, payload: Map<String, Any?>) {
        val callDisplayName = payload[KEY_CALL_DISPLAY_NAME] as String
        println("JcLog: calling streamVideo: $streamVideo")
        streamVideo?.onRiningCall(callId, callDisplayName)
    }

    private fun handleNotificationType(callId: StreamCallId, payload: Map<String, Any?>) {
        streamVideo?.onNotification(callId)
    }

    private fun handleLiveStartedType(callId: StreamCallId, payload: Map<String, Any?>) {
        streamVideo?.onLivestream(callId)
    }

    /**
     * Register a push device in our servers.
     *
     * @param pushDevice Contains info of the push device to be registered.
     */
    override fun registerPushDevice(pushDevice: PushDevice) {
        logger.d { "[registerPushDevice] pushDevice: $pushDevice" }
        CoroutineScope(DispatcherProvider.IO).launch {
            streamVideo?.createDevice(pushDevice)
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

    /**
     * Verify if the map contains all keys/values for a notification.
     */
    private fun Map<String, Any?>.isValid(): Boolean =
        isFromStreamServer() &&
            containsCallId() &&
            containsKnownType()

    /**
     * Verify if the map contains a CallId.
     */
    private fun Map<String, Any?>.containsCallId(): Boolean =
        !(this[KEY_CALL_CID] as? String).isNullOrBlank()

    /**
     * Verify if the map contains a known type.
     */
    private fun Map<String, Any?>.containsKnownType(): Boolean = when (this[KEY_TYPE]) {
        KEY_TYPE_RING -> isValidRingType()
        KEY_TYPE_NOTIFICATION -> isValidNotificationType()
        KEY_TYPE_LIVE_STARTED -> isValidLiveStarted()
        else -> false
    }

    /**
     * Verify if the map contains all keys/values for a Ring Type.
     */
    private fun Map<String, Any?>.isValidRingType(): Boolean =
        !(this[KEY_CALL_DISPLAY_NAME] as? String).isNullOrBlank()

    /**
     * Verify if the map contains all keys/values for a Notification Type.
     */
    private fun Map<String, Any?>.isValidNotificationType(): Boolean = true

    /**
     * Verify if the map contains all keys/values for a Live Started Type.
     */
    private fun Map<String, Any?>.isValidLiveStarted(): Boolean = true
    /**
     * Verify if the map contains key/value from Stream Server.
     */
    private fun Map<String, Any?>.isFromStreamServer(): Boolean =
        this[KEY_SENDER] == VALUE_STREAM_SENDER

    /**
     * Verify if the map contains all keys/values for an incoming call.
     */
    private fun Map<String, Any?>.isValidIncomingCall(): Boolean =
        !(this[KEY_TYPE] as? String).isNullOrBlank() && !(this[KEY_CALL_CID] as? String).isNullOrBlank()

    private companion object {
        private const val KEY_SENDER = "sender"
        private const val KEY_TYPE = "type"
        private const val KEY_TYPE_RING = "call.ring"
        private const val KEY_TYPE_NOTIFICATION = "call.notification"
        private const val KEY_TYPE_LIVE_STARTED = "call.live_started"
        private const val KEY_CALL_CID = "call_cid"
        private const val KEY_CALL_DISPLAY_NAME = "call_display_name"
        private const val VALUE_STREAM_SENDER = "stream.video"
    }
}
