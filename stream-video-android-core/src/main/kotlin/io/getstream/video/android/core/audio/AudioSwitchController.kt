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
import androidx.core.content.getSystemService
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDeviceChangeListener
import com.twilio.audioswitch.AudioSwitch
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.AudioSwitchHandler.Companion.onAudioFocusChangeListener

internal class AudioSwitchController(
    private val context: Context,
    private val preferredDeviceList: List<Class<out AudioDevice>>,
    private val audioDeviceChangeListener: AudioDeviceChangeListener,
) : AudioHandler {
    private val logger by taggedLogger("Audio:SwitchController")

    private var audioSwitch: AudioSwitch? = null
    private var isActivated = false

    /**
     * The audio mode asked for through [setCommunicationModeEnabled], or null while whatever
     * AudioSwitch chose stands. Written from the caller's thread and read from the audio scope
     * that drives [selectDevice].
     */
    @Volatile
    private var requestedAudioMode: Int? = null

    override fun start() {
        if (audioSwitch != null) return

        audioSwitch = getAudioSwitch()
        isActivated = false
        audioSwitch?.start(audioDeviceChangeListener)
    }

    override fun stop() {
        audioSwitch?.stop()
        audioSwitch = null
        isActivated = false
        // AudioSwitch restores the mode it saved before activating, so a request from this session
        // must not be re-applied over the mode the next one starts from.
        requestedAudioMode = null
    }

    fun selectDevice(device: AudioDevice?) {
        val switch = audioSwitch ?: return

        switch.selectDevice(device)

        if (!isActivated) {
            switch.activate()
            isActivated = true
        }
        // activate() puts the device in MODE_IN_COMMUNICATION, and a route change can do it again
        // later, so a request made earlier is re-applied rather than assumed to still hold.
        applyRequestedAudioMode()
    }

    /**
     * Chooses between [AudioManager.MODE_IN_COMMUNICATION] and [AudioManager.MODE_NORMAL] for the
     * running call.
     *
     * Some vendors pick their VoIP capture chain from the audio mode rather than from the
     * requested `MediaRecorder.AudioSource`. Samsung is the one we have measured: in
     * communication mode their HAL reports `needCallRouteDrive: ... for ap call case`, attaches
     * `AudioEffectStage: input_normal_input_voice_changer`, and logs
     * `PreProcess_RA: no solutions for recording` — the AOSP effects are not in the path at all.
     * That chain gates music, and it sits below the `AudioEffect` API, so the audio device module
     * flags, the audio-source constraints and
     * [io.getstream.video.android.core.MicrophoneManager.setHardwareNoiseSuppressorEnabled] all
     * miss it. Leaving communication mode is the only lever that reaches it.
     *
     * Costs the echo cancellation and routing that communication mode brings, and Bluetooth
     * capture with it: SCO carries the headset microphone and only runs in communication mode, so
     * asking for [AudioManager.MODE_NORMAL] on that route gives up the input entirely.
     *
     * The request is remembered and re-applied on every route change for as long as this handler
     * runs, because AudioSwitch sets the mode itself when it activates a device.
     *
     * @return true when the mode was applied, false when there is no [AudioManager] to apply it to.
     */
    fun setCommunicationModeEnabled(enabled: Boolean): Boolean {
        requestedAudioMode = if (enabled) {
            AudioManager.MODE_IN_COMMUNICATION
        } else {
            AudioManager.MODE_NORMAL
        }
        return applyRequestedAudioMode()
    }

    private fun applyRequestedAudioMode(): Boolean {
        val mode = requestedAudioMode ?: return false
        val audioManager = context.getSystemService<AudioManager>() ?: return false

        if (audioManager.mode == mode) return true

        logger.i { "[applyRequestedAudioMode] mode ${audioManager.mode} -> $mode" }
        audioManager.mode = mode
        return true
    }

    fun getAudioSwitch(): AudioSwitch {
        return AudioSwitch(
            context = context,
            audioFocusChangeListener = onAudioFocusChangeListener,
            preferredDeviceList = preferredDeviceList,
        )
    }
}
