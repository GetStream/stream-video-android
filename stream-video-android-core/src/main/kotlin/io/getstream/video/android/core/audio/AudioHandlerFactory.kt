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
     * Create an AudioHandler configured either with the in-built StreamAudioSwitch or the Twilio AudioSwitch.
     *
     * When `useInBuiltAudioSwitch` is true, returns a StreamAudioSwitchHandler using `preferredStreamAudioDeviceList`
     * and `streamAudioDeviceChangeListener`. When false, returns an AudioSwitchHandler with `preferredStreamAudioDeviceList`
     * mapped to Twilio `AudioDevice` types (unmapped types fall back to speakerphone) and `twilioAudioDeviceChangeListener`.
     *
     * @param context Android context used to initialize the audio handler.
     * @param preferredStreamAudioDeviceList Preferred StreamAudioDevice classes in priority order.
     * @param twilioAudioDeviceChangeListener Callback invoked on Twilio audio device changes (used when not using in-built switch).
     * @param streamAudioDeviceChangeListener Callback invoked on Stream audio device changes (used when using in-built switch).
     * @param useInBuiltAudioSwitch If true, use the in-built StreamAudioSwitch; otherwise use the Twilio AudioSwitch.
     * @return An AudioHandler instance configured according to `useInBuiltAudioSwitch` and the provided preferences/listeners.
     */
    fun create(
        context: Context,
        preferredStreamAudioDeviceList: List<Class<out StreamAudioDevice>>,
        twilioAudioDeviceChangeListener: TwilioAudioDeviceChangeListener,
        streamAudioDeviceChangeListener: StreamAudioDeviceChangeListener,
        useInBuiltAudioSwitch: Boolean,
    ): AudioHandler {
        return if (useInBuiltAudioSwitch) {
            StreamAudioSwitchHandler(
                context = context,
                preferredDeviceList = preferredStreamAudioDeviceList,
                audioDeviceChangeListener = streamAudioDeviceChangeListener,
            )
        } else {
            // Twilio audio switcher
            // Convert StreamAudioDevice types to Twilio AudioDevice types
            val twilioPreferredDevices = preferredStreamAudioDeviceList.map { streamDeviceClass ->
                when (streamDeviceClass) {
                    StreamAudioDevice.BluetoothHeadset::class.java -> AudioDevice.BluetoothHeadset::class.java
                    StreamAudioDevice.WiredHeadset::class.java -> AudioDevice.WiredHeadset::class.java
                    StreamAudioDevice.Earpiece::class.java -> AudioDevice.Earpiece::class.java
                    StreamAudioDevice.Speakerphone::class.java -> AudioDevice.Speakerphone::class.java
                    else -> AudioDevice.Speakerphone::class.java // fallback
                }
            }

            AudioSwitchHandler(
                context = context,
                preferredDeviceList = twilioPreferredDevices,
                audioDeviceChangeListener = twilioAudioDeviceChangeListener,
            )
        }
    }

    /**
     * Map a Twilio `AudioDevice` to the corresponding `StreamAudioDevice`.
     *
     * @param twilioDevice The Twilio `AudioDevice` to convert.
     * @return A `StreamAudioDevice` instance representing the same physical device as the provided Twilio `AudioDevice`.
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