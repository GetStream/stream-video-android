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

package io.getstream.video.android.core.audio

import android.telecom.CallEndpoint
import com.twilio.audioswitch.AudioDevice

sealed class StreamAudioDevice {

    /** The friendly name of the device.*/
    abstract val name: String

    abstract val audioSwitchDevice: AudioDevice?

    abstract val telecomDevice: CallEndpoint?

    /** An [StreamAudioDevice] representing a Bluetooth Headset.*/
    data class BluetoothHeadset constructor(
        override val name: String = "Bluetooth",
        override val audioSwitchDevice: AudioDevice? = null,
        override val telecomDevice: CallEndpoint? = null,
    ) : StreamAudioDevice()

    /** An [StreamAudioDevice] representing a Wired Headset.*/
    data class WiredHeadset constructor(
        override val name: String = "Wired Headset",
        override val audioSwitchDevice: AudioDevice? = null,
        override val telecomDevice: CallEndpoint? = null,
    ) : StreamAudioDevice()

    /** An [StreamAudioDevice] representing the Earpiece.*/
    data class Earpiece constructor(
        override val name: String = "Earpiece",
        override val audioSwitchDevice: AudioDevice? = null,
        override val telecomDevice: CallEndpoint? = null,
    ) : StreamAudioDevice()

    /** An [StreamAudioDevice] representing the Speakerphone.*/
    data class Speakerphone constructor(
        override val name: String = "Speakerphone",
        override val audioSwitchDevice: AudioDevice? = null,
        override val telecomDevice: CallEndpoint? = null,
    ) : StreamAudioDevice()

    companion object {

        @JvmStatic
        fun StreamAudioDevice.toAudioDevice(): AudioDevice? {
            return this.audioSwitchDevice
        }

        @JvmStatic
        fun AudioDevice.fromAudio(): StreamAudioDevice {
            return when (this) {
                is AudioDevice.BluetoothHeadset -> BluetoothHeadset(audioSwitchDevice = this)
                is AudioDevice.WiredHeadset -> WiredHeadset(audioSwitchDevice = this)
                is AudioDevice.Earpiece -> Earpiece(audioSwitchDevice = this)
                is AudioDevice.Speakerphone -> Speakerphone(audioSwitchDevice = this)
            }
        }
    }
}
