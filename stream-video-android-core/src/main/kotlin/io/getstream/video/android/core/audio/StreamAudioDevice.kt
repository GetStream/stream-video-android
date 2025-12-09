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

import com.twilio.audioswitch.AudioDevice
import kotlin.DeprecationLevel
import kotlin.ReplaceWith

/**
 * Represents an audio device for Twilio's AudioSwitch implementation.
 *
 * @deprecated This class is deprecated. Use [AudioDevice] when [useCustomAudioSwitch] is true.
 * This class will be removed in a future version. For new code, use [AudioDevice] instead.
 *
 * @see AudioDevice
 */
@Deprecated(
    message = "StreamAudioDevice is deprecated. Use NativeStreamAudioDevice when useCustomAudioSwitch is true. " +
        "This class is kept for backward compatibility with Twilio's AudioSwitch.",
    replaceWith = ReplaceWith(
        "NativeStreamAudioDevice",
        "io.getstream.video.android.core.audio.NativeStreamAudioDevice",
    ),
    level = DeprecationLevel.WARNING,
)
public sealed class StreamAudioDevice {

    /** The friendly name of the device.*/
    abstract val name: String

    /**
     * The Twilio AudioDevice instance.
     * This is always non-null since StreamAudioDevice is only used with Twilio's AudioSwitch.
     * For custom audio switch implementation, use [AudioDevice] instead.
     * @see com.twilio.audioswitch.AudioDevice
     */
    abstract val audio: AudioDevice

    /** An [StreamAudioDevice] representing a Bluetooth Headset.*/
    data class BluetoothHeadset constructor(
        override val name: String = "Bluetooth",
        /**
         * The Twilio AudioDevice instance.
         * This is always non-null since StreamAudioDevice is only used with Twilio's AudioSwitch.
         * @see com.twilio.audioswitch.AudioDevice
         */
        override val audio: AudioDevice,
    ) : StreamAudioDevice()

    /** An [StreamAudioDevice] representing a Wired Headset.*/
    data class WiredHeadset constructor(
        override val name: String = "Wired Headset",
        /**
         * The Twilio AudioDevice instance.
         * This is always non-null since StreamAudioDevice is only used with Twilio's AudioSwitch.
         * @see com.twilio.audioswitch.AudioDevice
         */
        override val audio: AudioDevice,
    ) : StreamAudioDevice()

    /** An [StreamAudioDevice] representing the Earpiece.*/
    data class Earpiece constructor(
        override val name: String = "Earpiece",
        /**
         * The Twilio AudioDevice instance.
         * This is always non-null since StreamAudioDevice is only used with Twilio's AudioSwitch.
         * @see com.twilio.audioswitch.AudioDevice
         */
        override val audio: AudioDevice,
    ) : StreamAudioDevice()

    /** An [StreamAudioDevice] representing the Speakerphone.*/
    data class Speakerphone constructor(
        override val name: String = "Speakerphone",
        /**
         * The Twilio AudioDevice instance.
         * This is always non-null since StreamAudioDevice is only used with Twilio's AudioSwitch.
         * @see com.twilio.audioswitch.AudioDevice
         */
        override val audio: AudioDevice,
    ) : StreamAudioDevice()

    companion object {

        @JvmStatic
        fun StreamAudioDevice.toAudioDevice(): AudioDevice {
            return this.audio
        }

        @JvmStatic
        fun AudioDevice.fromAudio(): StreamAudioDevice {
            return when (this) {
                is AudioDevice.BluetoothHeadset -> BluetoothHeadset(audio = this)
                is AudioDevice.WiredHeadset -> WiredHeadset(audio = this)
                is AudioDevice.Earpiece -> Earpiece(audio = this)
                is AudioDevice.Speakerphone -> Speakerphone(audio = this)
            }
        }
    }
}
