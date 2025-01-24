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
import kotlinx.coroutines.runBlocking
import org.openapitools.client.models.OwnCapability

@RequiresApi(Build.VERSION_CODES.O)
internal class TelecomCall(
    val streamCall: StreamCall,
    var state: TelecomCallState,
    val config: CallServiceConfig,
    val parentScope: CoroutineScope,
) {
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

    private fun publishDevices(callControlScope: CallControlScope) {
        with(callControlScope) {
            combine(availableEndpoints, currentCallEndpoint) { available, current ->
                Pair(available.map { it.toStreamAudioDevice() }, current.toStreamAudioDevice())
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
                .launchIn(this)
        }
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

    fun cleanUp() {
        StreamLog.d(TELECOM_LOG_TAG) { "[TelecomCall#cleanUp]" }

        runBlocking {
            disconnect()
            localScope.cancel()
        }
    }

    suspend fun disconnect() {
        callControlScope?.disconnect(DisconnectCause(DisconnectCause.LOCAL)).let { result ->
            StreamLog.d(TELECOM_LOG_TAG) { "[TelecomCall#disconnect] Disconnect result: $result" }
        }
    }
}
