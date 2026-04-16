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
import android.media.AudioManager
import android.os.Handler
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDeviceChangeListener
import com.twilio.audioswitch.AudioSwitch
import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient

/**
 * TODO: this class should be merged into the Microphone Manager, should be internal class
 */
public class AudioSwitchHandler(
    private val context: Context,
    private val preferredDeviceList: List<Class<out AudioDevice>>,
    private var audioDeviceChangeListener: AudioDeviceChangeListener,
) : AudioHandler {

    private val logger by taggedLogger(TAG)
    private var audioSwitch: AudioSwitch? = null
    private var isAudioSwitchInitScheduled = false
    private var isActivated = false

    // Resolved once on first use. If StreamVideoClient is not available there is no valid
    // call context — all operations are skipped rather than creating a throwaway thread.
    private val audioSwitchHandler: Handler? by lazy {
        (StreamVideo.instanceOrNull() as? StreamVideoClient)
            ?.getOrCreateAudioHandlerThread()
            ?.handler
    }

    override fun start() {
        val handler = audioSwitchHandler ?: run {
            logger.w { "[start] StreamVideoClient not available, skipping AudioSwitch setup" }
            return
        }
        synchronized(this) {
            if (!isAudioSwitchInitScheduled) {
                isAudioSwitchInitScheduled = true
                handler.removeCallbacksAndMessages(null)

                logger.d { "[start] Posting on audio-switch-thread" }

                handler.post {
                    logger.d { "[start] Running on audio-switch-thread" }

                    val switch = AudioSwitch(
                        context = context,
                        audioFocusChangeListener = onAudioFocusChangeListener,
                        preferredDeviceList = preferredDeviceList,
                    )
                    audioSwitch = switch
                    switch.start(audioDeviceChangeListener)
                }
            }
        }
    }

    override fun stop() {
        logger.d { "[stop] no args" }
        val handler = audioSwitchHandler ?: run {
            logger.w { "[stop] StreamVideoClient not available, skipping" }
            return
        }
        // Remove any pending work and stop AudioSwitch. We do NOT quit the HandlerThread here —
        // it is owned by StreamVideoClient and shared across all concurrent calls.
        handler.removeCallbacksAndMessages(null)
        handler.post {
            audioSwitch?.stop()
            audioSwitch = null
            isActivated = false
        }
    }

    public fun selectDevice(audioDevice: AudioDevice?) {
        logger.i { "[selectDevice] audioDevice: $audioDevice" }
        val handler = audioSwitchHandler ?: run {
            logger.w { "[selectDevice] StreamVideoClient not available, skipping" }
            return
        }
        handler.post {
            audioSwitch?.selectDevice(audioDevice)
            // activate() is only needed for the STARTED → ACTIVATED transition.
            // Once activated, selectDevice() triggers it internally via enumerateDevices().
            if (!isActivated) {
                audioSwitch?.activate()
                isActivated = true
            }
        }
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
