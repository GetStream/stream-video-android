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
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDeviceChangeListener
import com.twilio.audioswitch.AudioSwitch
import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger

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

/**
 * TODO: this class should be merged into the Microphone Manager
 */
public class AudioSwitchHandler(
    private val context: Context,
    private val preferredDeviceList: List<Class<out AudioDevice>>,
    private var audioDeviceChangeListener: AudioDeviceChangeListener,
) : AudioHandler {

    private val logger by taggedLogger(TAG)
    private var audioSwitch: AudioSwitch? = null
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var isAudioSwitchInitScheduled = false

    override fun start() {
        synchronized(this) {
            if (!isAudioSwitchInitScheduled) {
                isAudioSwitchInitScheduled = true
                mainThreadHandler.removeCallbacksAndMessages(null)

                logger.d { "[start] Posting on main" }

                mainThreadHandler.post {
                    logger.d { "[start] Running on main" }

                    val switch = AudioSwitch(
                        context = context,
                        audioFocusChangeListener = onAudioFocusChangeListener,
                        preferredDeviceList = preferredDeviceList,
                        loggingEnabled = true, //TODO Rahul, remove before release or better enable with sdk logger
                    )
                    audioSwitch = switch
                    switch.start(audioDeviceChangeListener)
                }
            }
        }
    }

    override fun stop() {
        logger.d { "[stop] no args" }
        mainThreadHandler.removeCallbacksAndMessages(null)
        mainThreadHandler.post {
            audioSwitch?.stop()
            audioSwitch = null
        }
    }

    public fun selectDevice(audioDevice: AudioDevice?) {
        logger.i { "[selectDevice] audioDevice: $audioDevice" }
        audioSwitch?.selectDevice(audioDevice)
        audioSwitch?.activate()
    }

    public companion object {
        private const val TAG = "AudioSwitchHandler"
        private val onAudioFocusChangeListener by lazy(LazyThreadSafetyMode.NONE) {
            DefaultOnAudioFocusChangeListener()
        }

        private class DefaultOnAudioFocusChangeListener : AudioManager.OnAudioFocusChangeListener {
            override fun onAudioFocusChange(focusChange: Int) {
                val typeOfChange: String = when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> "AUDIOFOCUS_GAIN"
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> "AUDIOFOCUS_GAIN_TRANSIENT"
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE"
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK"
                    AudioManager.AUDIOFOCUS_LOSS -> "AUDIOFOCUS_LOSS"
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "AUDIOFOCUS_LOSS_TRANSIENT"
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK"
                    else -> "AUDIOFOCUS_INVALID"
                }
                StreamLog.i(TAG) { "[onAudioFocusChange] focusChange: $typeOfChange" }
            }
        }
    }
}
