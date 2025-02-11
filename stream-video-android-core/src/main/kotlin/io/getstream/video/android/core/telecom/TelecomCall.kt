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

import android.content.Context
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallAttributesCompat.Companion.DIRECTION_INCOMING
import androidx.core.telecom.CallAttributesCompat.Companion.DIRECTION_OUTGOING
import androidx.core.telecom.CallAttributesCompat.Companion.SUPPORTS_SET_INACTIVE
import androidx.core.telecom.CallAttributesCompat.Companion.SUPPORTS_STREAM
import androidx.core.telecom.CallControlScope
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.DefaultStreamIntentResolver
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.models.OwnCapability
import kotlin.getValue

@RequiresApi(Build.VERSION_CODES.O)
internal class TelecomCall(
    val context: Context,
    val streamCall: StreamCall,
    val config: CallServiceConfig,
    val parentScope: CoroutineScope, // Used to collect devices before having a CallControlScope
) {
    private val logger by taggedLogger("StreamVideo:TelecomCall")

    var state = TelecomCallState.IDLE
        set(value) {
            previousState = field
            field = value
        }

    var previousState = TelecomCallState.IDLE

    val notificationId = streamCall.cid.hashCode()

    var notificationUpdatesJob: Job? = null

    val attributes: CallAttributesCompat
        get() = CallAttributesCompat(
            displayName = streamCall.state.createdBy.value?.userNameOrId ?: "Unknown",
            address = Uri.parse("https://getstream.io/video/join/${streamCall.cid}"),
            direction = if (state == TelecomCallState.INCOMING) {
                DIRECTION_INCOMING
            } else {
                DIRECTION_OUTGOING // The only other state is OUTGOING
            },
            callType = mediaType,
            callCapabilities = SUPPORTS_SET_INACTIVE or SUPPORTS_STREAM,
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

    val localScope = CoroutineScope(parentScope.coroutineContext + Job())

    private val devices = MutableStateFlow<Pair<List<StreamAudioDevice>, StreamAudioDevice>?>(null)

    private var activeCallSettings: ActiveCallSettings? = null

    data class ActiveCallSettings(
        val cameraEnabled: Boolean,
        val microphoneEnabled: Boolean,
        val speakerVolume: Int,
    )

    fun updateTelecomState() {
        val joined = TelecomCallState.IDLE to TelecomCallState.ONGOING
        val answered = TelecomCallState.INCOMING to TelecomCallState.ONGOING
        val accepted = TelecomCallState.OUTGOING to TelecomCallState.ONGOING
        val transition = previousState to state

        logger.d { "[updateTelecomState] #telecom; Transition: $transition" }

        callControlScope?.let {
            it.launch {
                when (transition) {
                    joined, accepted -> it.setActive().let { result ->
                        logger.d { "[updateTelecomState] #telecom; Set active: $result" }
                    }

                    answered -> it.answer(mediaType).let { result ->
                        logger.d { "[updateTelecomState] #telecom; Answered: $result" }
                    }
                }
            }
        }
    }

    suspend fun handleTelecomEvent(event: TelecomEvent) {
        logger.d { "[handleTelecomEvent] #telecom; event: $event" }

        when (event) {
            TelecomEvent.ANSWER -> {
                withContext(Dispatchers.IO) {
                    streamCall.accept().map {
                        DefaultStreamIntentResolver(context)
                            .searchAcceptCallPendingIntent(streamCall.buildStreamCallId())?.send()
                        streamCall.join()
                    }
                }
            }
            TelecomEvent.DISCONNECT -> {
                if (state == TelecomCallState.INCOMING) {
                    withContext(Dispatchers.IO) { streamCall.reject(RejectReason.Decline) }
                    streamCall.leave() // Will trigger TelecomHandler#unregisterCall
                } else {
                    streamCall.leave()
                }
            }
            TelecomEvent.SET_ACTIVE -> {
                activeCallSettings?.let {
                    streamCall.camera.setEnabled(it.cameraEnabled, true)
                    streamCall.microphone.setEnabled(it.microphoneEnabled, true)
                    streamCall.speaker.setVolume(it.speakerVolume)
                }
            }
            TelecomEvent.SET_INACTIVE -> {
                activeCallSettings = ActiveCallSettings(
                    cameraEnabled = streamCall.camera.isEnabled.value,
                    microphoneEnabled = streamCall.microphone.isEnabled.value,
                    speakerVolume = streamCall.speaker.volume.value ?: 0,
                )

                streamCall.camera.disable()
                streamCall.microphone.disable()
                streamCall.speaker.setVolume(0)
            }
        }
    }

    fun cleanUp() {
        localScope.cancel()
        callControlScope?.let {
            it.launch {
                it.disconnect(DisconnectCause(DisconnectCause.LOCAL)).let { result ->
                    logger.d { "[cleanUp] #telecom; Disconnect result: $result" }
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

                logger.d {
                    with(devicePair) {
                        "[publishDevices] #telecom; Published devices. Available: ${first.map { it.name }}, selected: ${second.name}"
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

                        logger.d {
                            "[collectDevices] #telecom; Collected devices. Available: ${first.map { it.name }}, selected: ${second.name}"
                        }
                    }
                }
            }
            .launchIn(localScope)
    }
}
