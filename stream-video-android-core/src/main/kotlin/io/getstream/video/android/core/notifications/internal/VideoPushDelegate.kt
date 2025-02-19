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

package io.getstream.video.android.core.notifications.internal

import io.getstream.android.push.PushDevice
import io.getstream.android.push.delegate.AndroidPushDelegateProvider
import io.getstream.android.push.delegate.PushDelegate
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.mapper.toTypeAndId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Class used to handle Push Notifications.
 *
 * It is used by reflection by [AndroidPushDelegateProvider] class.
 */
internal class VideoPushDelegate : PushDelegate() {
    private val logger by taggedLogger("Call:PushDelegate")
    private val DEFAULT_CALL_TEXT = "Unknown caller"

    /**
     * Handle a push message.
     *
     * @param metadata The metadata of the Push Notification.
     * @param payload The content of the Push Notification.
     * @return true if the payload was handled properly.
     */
    override fun handlePushMessage(
        metadata: Map<String, Any?>,
        payload: Map<String, Any?>,
    ): Boolean {
        logger.d { "[handlePushMessage] payload: $payload, metadata: $metadata" }
        return payload.ifValid {
            val callId = (payload[KEY_CALL_CID] as String).toTypeAndId()
                .let { StreamCallId(it.first, it.second) }
            CoroutineScope(DispatcherProvider.IO).launch {
                when (payload[KEY_TYPE]) {
                    KEY_TYPE_RING -> handleRingType(callId, payload)
                    KEY_TYPE_MISSED -> handleMissedType(callId, payload)
                    KEY_TYPE_NOTIFICATION -> handleNotificationType(callId, payload)
                    KEY_TYPE_LIVE_STARTED -> handleLiveStartedType(callId, payload)
                }
            }
        }
    }

    private suspend fun handleRingType(callId: StreamCallId, payload: Map<String, Any?>) {
        val callDisplayName = (payload[KEY_CREATED_BY_DISPLAY_NAME] as String).ifEmpty { DEFAULT_CALL_TEXT }
        getStreamVideo("ring-type-notification")?.onIncomingCall(callId, callDisplayName)
    }

    private suspend fun handleMissedType(callId: StreamCallId, payload: Map<String, Any?>) {
        val callDisplayName = (payload[KEY_CREATED_BY_DISPLAY_NAME] as String).ifEmpty { DEFAULT_CALL_TEXT }
        getStreamVideo("missed-type-notification")?.onMissedCall(callId, callDisplayName)
    }

    private suspend fun handleNotificationType(callId: StreamCallId, payload: Map<String, Any?>) {
        val callDisplayName = (payload[KEY_CREATED_BY_DISPLAY_NAME] as String).ifEmpty { DEFAULT_CALL_TEXT }
        getStreamVideo("generic-notification")?.onNotification(callId, callDisplayName)
    }

    private suspend fun handleLiveStartedType(callId: StreamCallId, payload: Map<String, Any?>) {
        val callDisplayName = (payload[KEY_CREATED_BY_DISPLAY_NAME] as String).ifEmpty { DEFAULT_CALL_TEXT }
        getStreamVideo("live-started-notification")?.onLiveCall(callId, callDisplayName)
    }

    /**
     * Register a push device in our servers.
     *
     * @param pushDevice Contains info of the push device to be registered.
     */
    override fun registerPushDevice(pushDevice: PushDevice) {
        logger.d { "[registerPushDevice] pushDevice: $pushDevice" }
        CoroutineScope(DispatcherProvider.IO).launch {
            getStreamVideo("register-push-device")?.createDevice(pushDevice)
        }
    }

    private fun getStreamVideo(requestReason: String) =
        StreamVideo.instanceOrNull().also {
            if (it == null) {
                logger.e {
                    "Ignoring a push notification ($requestReason) StreamVideo is not initialised. " +
                        "Handling notifications requires to initialise StreamVideo in Application.onCreate"
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
        KEY_TYPE_MISSED -> isValidMissedType()
        KEY_TYPE_NOTIFICATION -> isValidNotificationType()
        KEY_TYPE_LIVE_STARTED -> isValidLiveStarted()
        else -> false
    }

    /**
     * Verify if the map contains all keys/values for a Ring Type.
     */
    private fun Map<String, Any?>.isValidRingType(): Boolean =
        // TODO: KEY_CALL_DISPLAY_NAME can be empty. Are there any other important key/values?
        // !(this[KEY_CALL_DISPLAY_NAME] as? String).isNullOrBlank()
        true

    /**
     * Verify if the map contains all keys/values for a Missed Type.
     */
    private fun Map<String, Any?>.isValidMissedType(): Boolean =
        // TODO: KEY_CALL_DISPLAY_NAME can be empty. Are there any other important key/values?
        // !(this[KEY_CALL_DISPLAY_NAME] as? String).isNullOrBlank()
        true

    /**
     * Verify if the map contains all keys/values for a Notification Type.
     */
    private fun Map<String, Any?>.isValidNotificationType(): Boolean =
        // TODO: KEY_CALL_DISPLAY_NAME can be empty. Are there any other important key/values?
        // !(this[KEY_CALL_DISPLAY_NAME] as? String).isNullOrBlank()
        true

    /**
     * Verify if the map contains all keys/values for a Live Started Type.
     */
    private fun Map<String, Any?>.isValidLiveStarted(): Boolean =
        // TODO: KEY_CALL_DISPLAY_NAME can be empty. Are there any other important key/values?
        // !(this[KEY_CALL_DISPLAY_NAME] as? String).isNullOrBlank()
        true

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
        private const val KEY_TYPE_MISSED = "call.missed"
        private const val KEY_TYPE_NOTIFICATION = "call.notification"
        private const val KEY_TYPE_LIVE_STARTED = "call.live_started"
        private const val KEY_CALL_CID = "call_cid"
        private const val KEY_CALL_DISPLAY_NAME = "call_display_name"
        private const val KEY_CREATED_BY_DISPLAY_NAME = "created_by_display_name"
        private const val VALUE_STREAM_SENDER = "stream.video"
    }
}
