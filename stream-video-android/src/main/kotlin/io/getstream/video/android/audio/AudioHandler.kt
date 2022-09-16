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

package io.getstream.video.android.audio

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

public interface AudioHandler {
    /**
     * Called when a room is started.
     */
    public fun start()

    /**
     * Called when a room is disconnected.
     */
    public fun stop()
}

public class AudioSwitchHandler constructor(private val context: Context) : AudioHandler {
    private var audioDeviceChangeListener: AudioDeviceChangeListener? = null
    private var onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null
    private var preferredDeviceList: List<Class<out AudioDevice>>? = null

    private var audioSwitch: AudioSwitch? = null

    // AudioSwitch is not threadsafe, so all calls should be done on the main thread.
    private val handler = Handler(Looper.getMainLooper())

    override fun start() {
        if (audioSwitch == null) {
            handler.removeCallbacksAndMessages(null)
            handler.post {
                val switch = AudioSwitch(
                    context = context,
                    audioFocusChangeListener = onAudioFocusChangeListener
                        ?: defaultOnAudioFocusChangeListener,
                    preferredDeviceList = preferredDeviceList ?: defaultPreferredDeviceList
                )
                audioSwitch = switch
                switch.start(audioDeviceChangeListener ?: defaultAudioDeviceChangeListener)
                switch.activate()
            }
        }
    }

    override fun stop() {
        handler.removeCallbacksAndMessages(null)
        handler.post {
            audioSwitch?.stop()
            audioSwitch = null
        }
    }

    public val selectedAudioDevice: AudioDevice?
        get() = audioSwitch?.selectedAudioDevice

    public val availableAudioDevices: List<AudioDevice>
        get() = audioSwitch?.availableAudioDevices ?: listOf()

    public fun selectDevice(audioDevice: AudioDevice?) {
        audioSwitch?.selectDevice(audioDevice)
    }

    public companion object {
        private val defaultOnAudioFocusChangeListener by lazy(LazyThreadSafetyMode.NONE) {
            AudioManager.OnAudioFocusChangeListener { }
        }
        private val defaultAudioDeviceChangeListener by lazy(LazyThreadSafetyMode.NONE) {
            object : AudioDeviceChangeListener {
                override fun invoke(
                    audioDevices: List<AudioDevice>,
                    selectedAudioDevice: AudioDevice?
                ) {
                    Log.d("AudioChanging", selectedAudioDevice.toString())
                }
            }
        }
        private val defaultPreferredDeviceList by lazy(LazyThreadSafetyMode.NONE) {
            listOf(
                AudioDevice.BluetoothHeadset::class.java,
                AudioDevice.WiredHeadset::class.java,
                AudioDevice.Earpiece::class.java,
                AudioDevice.Speakerphone::class.java
            )
        }
    }
}
