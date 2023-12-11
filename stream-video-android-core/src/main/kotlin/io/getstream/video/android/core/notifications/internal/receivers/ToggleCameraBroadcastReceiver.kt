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
import io.getstream.video.android.core.StreamVideo

class ToggleCameraBroadcastReceiver : BroadcastReceiver() {
    val logger by taggedLogger("ToggleCameraBroadcastReceiver")

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                logger.d { "Screen is on and locked." }
            }
            Intent.ACTION_USER_PRESENT -> {
                logger.d { "Screen is on and unlocked." }
                StreamVideo.instanceOrNull()?.state?.activeCall?.value?.camera?.enable()
            }
            Intent.ACTION_SCREEN_OFF -> {
                // This flag actually means that the device is non-interactive.
                // In a video call scenario, the only way to be non-interactive is when locking the phone manually.
                logger.d { "Screen is off." }
                StreamVideo.instanceOrNull()?.state?.activeCall?.value?.camera?.disable()
            }
        }
    }
}
