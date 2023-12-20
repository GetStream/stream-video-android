/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ToggleCameraBroadcastReceiver(coroutineScope: CoroutineScope) : BroadcastReceiver() {
    private val logger by taggedLogger("ToggleCameraBroadcastReceiver")
    private val streamVideo = StreamVideo.instanceOrNull()
    private var call: Call? = null
    private var shouldEnableCameraAgain = false

    init {
        logger.d { "Init" }
        logger.d { "Active call value: " + streamVideo?.state?.activeCall?.value?.id }
        logger.d { "Ringing call value: " + streamVideo?.state?.ringingCall?.value?.id }

        // TODO: active call should be set to ringing call automatically?
        call = streamVideo?.state?.activeCall?.value ?: streamVideo?.state?.ringingCall?.value

        coroutineScope.launch {
            if (call == null) {
                streamVideo?.state?.ringingCall?.collect {
                    if (it != null) {
                        call = it
                        logger.d { "Ringing call changed to ${call?.id}" }
                    }
                }
                streamVideo?.state?.activeCall?.collect {
                    if (it != null) {
                        call = it
                        logger.d { "Active call changed to ${call?.id}" }
                    }
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                logger.d { "Screen is on and locked. Call: ${call?.id}" }
                // TODO:
                // For when the call screen is visible even if the screen is locked.
                // Because of lockscreenVisibility = Notification.VISIBILITY_PUBLIC for channel?
                // To reproduce: answer from locked screen the lock-unlock
                // Problem: answer call while unlocked. Lock then turn screen on by moving phone -> camera is on. Turn screen on by tap or button -> camera is off.
                if (shouldEnableCameraAgain) call?.camera?.enable()
            }
            Intent.ACTION_USER_PRESENT -> {
                logger.d { "Screen is on and unlocked. Call: ${call?.id}" }
                if (shouldEnableCameraAgain) call?.camera?.enable()
            }
            Intent.ACTION_SCREEN_OFF -> {
                // This broadcast action actually means that the device is non-interactive.
                // In a video call scenario, the only way to be non-interactive is when locking the phone manually.
                call?.camera.let { camera ->
                    shouldEnableCameraAgain = camera?.isEnabled?.value ?: false
                    camera?.disable()
                }

                logger.d { "Screen is off. Call: ${call?.id}. Should re-enable camera: $shouldEnableCameraAgain." }
            }
        }
    }
}
