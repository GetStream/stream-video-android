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

import android.content.Context
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDeviceChangeListener as TwilioAudioDeviceChangeListener

/**
 * Factory for creating AudioHandler instances.
 * Supports both Twilio AudioSwitch (legacy) and custom StreamAudioSwitch implementations.
 */
internal object AudioHandlerFactory {
    /**
     * Creates an AudioHandler instance based on the useCustomAudioSwitch flag.
     *
     * @param context Android context
     * @param preferredDeviceList List of preferred audio device types in priority order
     * @param audioDeviceChangeListener Callback for device changes
     * @param useCustomAudioSwitch If true, uses custom StreamAudioSwitch; if false, uses Twilio AudioSwitch
     * @return An AudioHandler instance
     */
    fun create(
        context: Context,
        preferredDeviceList: List<Class<out StreamAudioDevice>>,
        preferredNativeDeviceList:
        List<Class<out io.getstream.video.android.core.audio.CustomAudioDevice>>,
        audioDeviceChangeListener: TwilioAudioDeviceChangeListener,
        audioNativeDeviceListener: CustomAudioDeviceChangeListener,
        useCustomAudioSwitch: Boolean,
    ): AudioHandler {
        return if (useCustomAudioSwitch) {
            StreamAudioSwitchHandler(
                context = context,
                preferredDeviceList = preferredNativeDeviceList,
                audioDeviceChangeListener = audioNativeDeviceListener,
            )
        } else {
            // Twilio audio switcher
            // Convert StreamAudioDevice types to Twilio AudioDevice types
            val twilioPreferredDevices = preferredDeviceList.map { streamDeviceClass ->
                when (streamDeviceClass) {
                    StreamAudioDevice.BluetoothHeadset::class.java -> AudioDevice.BluetoothHeadset::class.java
                    StreamAudioDevice.WiredHeadset::class.java -> AudioDevice.WiredHeadset::class.java
                    StreamAudioDevice.Earpiece::class.java -> AudioDevice.Earpiece::class.java
                    StreamAudioDevice.Speakerphone::class.java -> AudioDevice.Speakerphone::class.java
                    else -> AudioDevice.Speakerphone::class.java // fallback
                }
            }

            // Convert our AudioDeviceChangeListener to Twilio's AudioDeviceChangeListener
            val twilioListener: TwilioAudioDeviceChangeListener = { devices, selected ->
                // Convert Twilio AudioDevice to StreamAudioDevice
                val streamDevices = devices.map { twilioDevice ->
                    convertTwilioDeviceToStreamDevice(twilioDevice)
                }
                val streamSelected = selected?.let { convertTwilioDeviceToStreamDevice(it) }
                // The callback expects Twilio AudioDevice types, so convert back
                val twilioDevices = streamDevices.map { it.audio }
                val twilioSelected = streamSelected?.audio
                audioDeviceChangeListener(devices, selected)
            }

            AudioSwitchHandler(
                context = context,
                preferredDeviceList = twilioPreferredDevices,
                audioDeviceChangeListener = twilioListener,
            )
        }
    }

    /**
     * Converts a Twilio AudioDevice to StreamAudioDevice.
     */
    private fun convertTwilioDeviceToStreamDevice(twilioDevice: AudioDevice): StreamAudioDevice {
        return when (twilioDevice) {
            is AudioDevice.BluetoothHeadset -> StreamAudioDevice.BluetoothHeadset(
                audio = twilioDevice,
            )
            is AudioDevice.WiredHeadset -> StreamAudioDevice.WiredHeadset(
                audio = twilioDevice,
            )
            is AudioDevice.Earpiece -> StreamAudioDevice.Earpiece(
                audio = twilioDevice,
            )
            is AudioDevice.Speakerphone -> StreamAudioDevice.Speakerphone(
                audio = twilioDevice,
            )
        }
    }
}
