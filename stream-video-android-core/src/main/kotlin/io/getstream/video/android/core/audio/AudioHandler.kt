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
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDeviceChangeListener
import io.getstream.log.StreamLog
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// TODO it should be internal and its function should return Result<>
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
 * TODO: this class should be merged into the Microphone Manager, should be internal class
 */
public class AudioSwitchHandler(
    context: Context,
    preferredDeviceList: List<Class<out AudioDevice>>,
    audioDeviceChangeListener: AudioDeviceChangeListener,
) : AudioHandler {

    private val controller = AudioSwitchController(
        context,
        preferredDeviceList,
        audioDeviceChangeListener,
    )
    private var scope: CoroutineScope? = null

    private fun ensureScope(): CoroutineScope? {
        val ctx = (StreamVideo.instanceOrNull() as? StreamVideoClient)
            ?.getAudioContext() ?: return null

        if (scope == null) {
            scope = ctx.createChildScope()
        }

        return scope
    }

    override fun start() {
        val scope = ensureScope() ?: return
        scope.launch { controller.start() }
    }

    override fun stop() {
        val scope = ensureScope() ?: return
        scope.launch { controller.stop() }
    }

    fun selectDevice(device: AudioDevice?) {
        val scope = ensureScope() ?: return
        scope.launch { controller.selectDevice(device) }
    }

    public companion object {
        private const val TAG = "AudioSwitchHandler"
        internal val onAudioFocusChangeListener by lazy(LazyThreadSafetyMode.NONE) {
            DefaultOnAudioFocusChangeListener()
        }

        internal class DefaultOnAudioFocusChangeListener : AudioManager.OnAudioFocusChangeListener {
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
