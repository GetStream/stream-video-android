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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import io.getstream.logging.StreamLog

internal class AudioDeviceManager(
    private val context: Context,
    private val audioManager: AudioManager,
    private val audioFocusRequest: AudioFocusRequestWrapper = AudioFocusRequestWrapper(),
    private val audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
) {

    private val logger = StreamLog.getLogger("Call:AudioDeviceManager")

    private var savedAudioMode = 0
    private var savedIsMicrophoneMuted = false
    private var savedSpeakerphoneEnabled = false
    private var audioRequest: AudioFocusRequest? = null

    init {
        logger.i { "<init> audioFocusChangeListener: $audioFocusChangeListener" }
    }

    fun hasEarpiece(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    @SuppressLint("NewApi")
    fun hasSpeakerphone(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            context.packageManager
                .hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)
        ) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return true
                }
            }
            false
        } else {
            true
        }
    }

    @SuppressLint("NewApi")
    fun setAudioFocus() {
        // Request audio focus before making any device switch.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioRequest = audioFocusRequest.buildRequest(audioFocusChangeListener)
            audioRequest?.let {
                val result = audioManager.requestAudioFocus(it)
                logger.i { "[setAudioFocus] #new; completed: ${result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED}" }
            }
        } else {
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
            logger.i { "[setAudioFocus] #old; completed: ${result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED}" }
        }
        /*
         * Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
         * required to be in this mode when playout and/or recording starts for
         * best possible VoIP performance. Some devices have difficulties with speaker mode
         * if this is not set.
         */
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    fun enableBluetoothSco(enable: Boolean) {
        logger.i { "[enableBluetoothSco] enable: $enable" }
        audioManager.run { if (enable) startBluetoothSco() else stopBluetoothSco() }
    }

    fun enableSpeakerphone(enable: Boolean) {
        logger.i { "[enableSpeakerphone] enable: $enable" }
        audioManager.isSpeakerphoneOn = enable
    }

    fun mute(mute: Boolean) {
        logger.i { "[mute] mute: $mute" }
        audioManager.isMicrophoneMute = mute
    }

    // TODO Consider persisting audio state in the event of process death
    fun cacheAudioState() {
        logger.i { "[cacheAudioState] no args" }
        savedAudioMode = audioManager.mode
        savedIsMicrophoneMuted = audioManager.isMicrophoneMute
        savedSpeakerphoneEnabled = audioManager.isSpeakerphoneOn
    }

    @SuppressLint("NewApi")
    fun restoreAudioState() {
        logger.i { "[cacheAudioState] no args" }
        audioManager.mode = savedAudioMode
        mute(savedIsMicrophoneMuted)
        enableSpeakerphone(savedSpeakerphoneEnabled)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioRequest?.let {
                logger.d { "[cacheAudioState] abandonAudioFocusRequest: $it" }
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            logger.d { "[cacheAudioState] audioFocusChangeListener: $audioFocusChangeListener" }
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }
}
