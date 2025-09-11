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

package io.getstream.video.android.core.sounds

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import io.getstream.log.taggedLogger

internal class CallSoundPlayer(private val context: Context) {
    private val logger by taggedLogger("CallSoundPlayer")
    private val mediaPlayer: MediaPlayer by lazy { MediaPlayer() }
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var ringtone: Ringtone? = null

    fun playCallSound(soundUri: Uri?, playIfMuted: Boolean = false) {
        try {
            synchronized(this) {
                requestAudioFocus(playIfMuted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        playWithRingtoneManager(soundUri, playIfMuted)
                    } else {
                        playWithMediaPlayer(soundUri, playIfMuted)
                    }
                }
            }
        } catch (e: Exception) {
            logger.d { "[playCallSound] Error playing call sound: ${e.message}" }
        }
    }

    private fun requestAudioFocus(playIfMuted: Boolean = false, onGranted: () -> Unit) {
        if (audioManager == null) {
            (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let {
                audioManager = it
            } ?: run {
                logger.d { "[requestAudioFocus] Error getting AudioManager system service" }
                return
            }
        }

        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    if (playIfMuted) {
                        AudioAttributes.Builder().setVoiceCommunicationAttributes().build()
                    } else {
                        AudioAttributes.Builder().setNotificationRingtoneAttributes().build()
                    },
                )
                .setAcceptsDelayedFocusGain(false)
                .build()

            audioFocusRequest?.let {
                audioManager?.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } ?: false
        } else {
            audioManager?.requestAudioFocus(
                null,
                if (playIfMuted) AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_RING,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        logger.d { "[requestAudioFocus] Audio focus " + if (isGranted) "granted" else "not granted" }
        if (isGranted) onGranted()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun playWithRingtoneManager(soundUri: Uri?, playIfMuted: Boolean = false) {
        soundUri?.let {
            if (ringtone?.isPlaying == true) ringtone?.stop()

            ringtone = RingtoneManager.getRingtone(context, soundUri)

            if (playIfMuted) {
                ringtone?.setAudioAttributes(
                    AudioAttributes.Builder().setVoiceCommunicationAttributes().build(),
                )
            } else {
                ringtone?.setAudioAttributes(
                    AudioAttributes.Builder().setNotificationRingtoneAttributes().build(),
                )
            }

            if (ringtone?.isPlaying == false) {
                ringtone?.isLooping = true
                ringtone?.play()

                logger.d { "[playWithRingtoneManager] Sound playing" }
            }
        }
    }

    private fun playWithMediaPlayer(soundUri: Uri?, playIfMuted: Boolean = false) {
        soundUri?.let {
            mediaPlayer.let { mediaPlayer ->
                if (!mediaPlayer.isPlaying) {
                    setMediaPlayerDataSource(mediaPlayer, soundUri, playIfMuted)
                    mediaPlayer.start()

                    logger.d { "[playWithMediaPlayer] Sound playing" }
                }
            }
        }
    }

    private fun setMediaPlayerDataSource(mediaPlayer: MediaPlayer, uri: Uri, playIfMuted: Boolean = false) {
        mediaPlayer.reset()
        mediaPlayer.setDataSource(context, uri)
        mediaPlayer.isLooping = true

        if (playIfMuted) {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder().setVoiceCommunicationAttributes().build(),
            )
        } else {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder().setNotificationRingtoneAttributes().build(),
            )
        }

        mediaPlayer.prepare()
    }

    fun stopCallSound() {
        synchronized(this) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    logger.d { "[stopCallSound] Stopping RingtoneManager sound" }
                    if (ringtone?.isPlaying == true) ringtone?.stop()
                } else {
                    logger.d { "[stopCallSound] Stopping MediaPlayer sound" }
                    if (mediaPlayer.isPlaying == true) mediaPlayer.stop()
                }
            } catch (e: Exception) {
                logger.d { "[stopCallSound] Error stopping call sound: ${e.message}" }
            } finally {
                abandonAudioFocus()
            }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            audioManager?.abandonAudioFocus(null)
        }
    }

    fun cleanUpAudioResources() {
        synchronized(this) {
            logger.d { "[cleanAudioResources] Cleaning audio resources" }

            if (ringtone?.isPlaying == true) ringtone?.stop()
            ringtone = null

            mediaPlayer.release()

            audioManager = null
            audioFocusRequest = null
        }
    }
}

private fun AudioAttributes.Builder.setVoiceCommunicationAttributes(): AudioAttributes.Builder {
    setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)

    return this
}

private fun AudioAttributes.Builder.setNotificationRingtoneAttributes(): AudioAttributes.Builder {
    setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
    setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)

    return this
}
