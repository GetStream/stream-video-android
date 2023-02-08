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

package io.getstream.video.android.core.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

internal const val STATE_UNPLUGGED = 0
internal const val STATE_PLUGGED = 1
internal const val INTENT_STATE = "state"

internal class WiredHeadsetReceiver(private val context: Context) : BroadcastReceiver() {

    private var deviceListener: WiredDeviceConnectionListener? = null

    override fun onReceive(context: Context, intent: Intent) {
        intent.getIntExtra(INTENT_STATE, STATE_UNPLUGGED).let { state ->
            if (state == STATE_PLUGGED) {
                deviceListener?.onDeviceConnected()
            } else {
                deviceListener?.onDeviceDisconnected()
            }
        }
    }

    fun start(deviceListener: WiredDeviceConnectionListener) {
        this.deviceListener = deviceListener
        context.registerReceiver(this, IntentFilter(Intent.ACTION_HEADSET_PLUG))
    }

    fun stop() {
        deviceListener = null
        context.unregisterReceiver(this)
    }
}
