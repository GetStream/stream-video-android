/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.service.controllers

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.core.notifications.internal.service.models.ServiceStateSnapshot
import io.getstream.video.android.core.sounds.CallSoundAndVibrationPlayer
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.threeten.bp.OffsetDateTime

internal class ServiceStateController {
    private val _state = MutableStateFlow(ServiceStateSnapshot())
    val state: StateFlow<ServiceStateSnapshot> = _state

    val currentCallId: StreamCallId?
        get() = state.value.currentCallId

    val soundPlayer: CallSoundAndVibrationPlayer?
        get() = state.value.soundPlayer

    val startTime: OffsetDateTime?
        get() = state.value.startTime

    fun setCurrentCallId(callId: StreamCallId?) {
        _state.update { it.copy(currentCallId = callId) }
    }

    fun setSoundPlayer(player: CallSoundAndVibrationPlayer?) {
        _state.update { it.copy(soundPlayer = player) }
    }

    fun setStartTime(time: OffsetDateTime?) {
        _state.update { it.copy(startTime = time) }
    }

    fun registerToggleCameraBroadcastReceiver(
        service: Service,
        scope: CoroutineScope,
    ) {
        val receiver = ToggleCameraBroadcastReceiver(scope)

        var shouldRegister = false

        _state.update { current ->
            if (current.isReceiverRegistered) {
                current
            } else {
                shouldRegister = true
                current.copy(
                    toggleCameraBroadcastReceiver = receiver,
                    isReceiverRegistered = true,
                )
            }
        }

        if (!shouldRegister) return

        try {
            service.registerReceiver(
                receiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_USER_PRESENT)
                },
            )
        } catch (e: Exception) {
            // Roll back state if registration fails
            _state.update {
                it.copy(
                    toggleCameraBroadcastReceiver = null,
                    isReceiverRegistered = false,
                )
            }
        }
    }

    fun unregisterToggleCameraBroadcastReceiver(service: Service) {
        var receiverToUnregister: ToggleCameraBroadcastReceiver? = null

        _state.update { current ->
            if (!current.isReceiverRegistered) {
                receiverToUnregister = null
                current
            } else {
                receiverToUnregister = current.toggleCameraBroadcastReceiver
                current.copy(
                    toggleCameraBroadcastReceiver = null,
                    isReceiverRegistered = false,
                )
            }
        }

        receiverToUnregister ?: return

        try {
            service.unregisterReceiver(receiverToUnregister)
        } catch (e: Exception) {
            // Best-effort cleanup; state is already consistent
        }
    }
}
