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

package io.getstream.video.android.core.telecom

import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import io.getstream.log.StreamLog
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.openapitools.client.models.OwnCapability

@RequiresApi(Build.VERSION_CODES.O)
internal class TelecomCall(
    val streamCall: StreamCall,
    val config: CallServiceConfig,
    val parentScope: CoroutineScope, // Used to collect devices before having a CallControlScope
) {
    var state = TelecomCallState.IDLE
        private set

    val notificationId = streamCall.cid.hashCode()

    val attributes: CallAttributesCompat
        get() = CallAttributesCompat(
            displayName = streamCall.state.createdBy.value?.userNameOrId ?: "Unknown",
            address = Uri.parse("https://getstream.io/video/join/${streamCall.cid}"),
            direction = if (state == TelecomCallState.INCOMING) {
                CallAttributesCompat.DIRECTION_INCOMING
            } else {
                CallAttributesCompat.DIRECTION_OUTGOING
            },
            callType = mediaType,
        )

    val mediaType: Int
        get() = if (streamCall.hasCapability(OwnCapability.SendVideo)) {
            CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        } else {
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL
        }

    var callControlScope: CallControlScope? = null
        set(value) {
            field = value
            value?.let(::publishDevices)
        }

    var deviceListener: DeviceListener? = null
        set(value) {
            field = value
            value?.let(::collectDevices)
        }

    private val localScope = CoroutineScope(parentScope.coroutineContext + Job())

    private val devices = MutableStateFlow<Pair<List<StreamAudioDevice>, StreamAudioDevice>?>(null)

    fun updateState(newState: TelecomCallState) {
        val joined = TelecomCallState.IDLE to TelecomCallState.ONGOING
        val answered = TelecomCallState.INCOMING to TelecomCallState.ONGOING
        val accepted = TelecomCallState.OUTGOING to TelecomCallState.ONGOING
        val transition = state to newState
        state = newState

        StreamLog.d(TELECOM_LOG_TAG) { "[updateState] New state: $state" }

        callControlScope?.let {
            it.launch {
                when (transition) {
                    joined, accepted -> it.setActive().let { result ->
                        StreamLog.d(TELECOM_LOG_TAG) { "[updateState] Set active: $result" }
                    }

                    answered -> it.answer(mediaType).let { result ->
                        StreamLog.d(TELECOM_LOG_TAG) { "[updateState] Answered: $result" }
                    }
                }
            }
        }
    }

    suspend fun handleTelecomEvent(event: TelecomEvent) {
        StreamLog.d(TELECOM_LOG_TAG) { "[TelecomCall#handleTelecomEvent] event: $event" }

        when (event) {
            TelecomEvent.ANSWER -> {
                streamCall.accept()
                streamCall.join()
            }
            TelecomEvent.DISCONNECT -> {
                streamCall.leave()
            }
            TelecomEvent.SET_ACTIVE -> {
                streamCall.join()
            }
            TelecomEvent.SET_INACTIVE -> {
                streamCall.leave()
            }
        }
    }

    fun cleanUp() {
        StreamLog.d(TELECOM_LOG_TAG) { "[TelecomCall#cleanUp]" }

        localScope.cancel()
        callControlScope?.let {
            it.launch {
                it.disconnect(DisconnectCause(DisconnectCause.LOCAL)).let { result ->
                    StreamLog.d(
                        TELECOM_LOG_TAG,
                    ) { "[TelecomCall#cleanUp] Disconnect result: $result" }
                }
            }
        }
    }

    private fun publishDevices(callControlScope: CallControlScope) {
        combine(
            callControlScope.availableEndpoints,
            callControlScope.currentCallEndpoint,
        ) { availableDevices, currentDevice ->
            availableDevices.map { it.toStreamAudioDevice() } to currentDevice.toStreamAudioDevice()
        }
            .distinctUntilChanged()
            .onEach { devicePair ->
                devices.value = devicePair

                StreamLog.d(TELECOM_LOG_TAG) {
                    with(devicePair) {
                        "[TelecomCall#publishDevices] Published devices. Available: ${first.map { it.name }}, selected: ${second.name}"
                    }
                }
            }
            .launchIn(callControlScope)
    }

    private fun collectDevices(listener: DeviceListener) {
        devices
            .onEach { devicePair ->
                devicePair?.let { pair ->
                    with(pair) {
                        listener(first, second)

                        StreamLog.d(TELECOM_LOG_TAG) {
                            "[TelecomCall#collectDevices] Collected devices. Available: ${first.map { it.name }}, selected: ${second.name}"
                        }
                    }
                }
            }
            .launchIn(localScope)
    }
}
