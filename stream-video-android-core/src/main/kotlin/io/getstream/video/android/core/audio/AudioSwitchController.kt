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

package io.getstream.video.android.core.audio

import android.content.Context
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDeviceChangeListener
import com.twilio.audioswitch.AudioSwitch
import io.getstream.video.android.core.audio.AudioSwitchHandler.Companion.onAudioFocusChangeListener

internal class AudioSwitchController(
    private val context: Context,
    private val preferredDeviceList: List<Class<out AudioDevice>>,
    private val listener: AudioDeviceChangeListener,
) {
    private var audioSwitch: AudioSwitch? = null
    private var isActivated = false

    fun start() {
        if (audioSwitch != null) return

        val switch = AudioSwitch(
            context = context,
            audioFocusChangeListener = onAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
        )

        audioSwitch = switch
        isActivated = false
        switch.start(listener)
    }

    fun stop() {
        audioSwitch?.stop()
        audioSwitch = null
        isActivated = false
    }

    fun selectDevice(device: AudioDevice?) {
        val switch = audioSwitch ?: return

        switch.selectDevice(device)

        if (!isActivated) {
            switch.activate()
            isActivated = true
        }
    }
}
