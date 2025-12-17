/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.service.models

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.core.sounds.CallSoundAndVibrationPlayer
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope

internal class ServiceState {

    var currentCallId: StreamCallId? = null
    var soundPlayer: CallSoundAndVibrationPlayer? = null
    private var toggleCameraBroadcastReceiver: ToggleCameraBroadcastReceiver? = null
    private var isReceiverRegistered = false

    internal fun registerToggleCameraBroadcastReceiver(service: Service, scope: CoroutineScope) {
        if (!isReceiverRegistered) {
            try {
                toggleCameraBroadcastReceiver = ToggleCameraBroadcastReceiver(scope)
                service.registerReceiver(
                    toggleCameraBroadcastReceiver,
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                        addAction(Intent.ACTION_USER_PRESENT)
                    },
                )
                isReceiverRegistered = true
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    internal fun unregisterToggleCameraBroadcastReceiver(service: Service) {
        if (isReceiverRegistered) {
            try {
                toggleCameraBroadcastReceiver?.let { service.unregisterReceiver(it) }
                isReceiverRegistered = false
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}
