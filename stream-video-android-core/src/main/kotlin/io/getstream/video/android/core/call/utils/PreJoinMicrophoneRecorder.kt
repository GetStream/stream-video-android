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

package io.getstream.video.android.core.call.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Mono 16 bit PCM, the format [SoundInputProcessor] expects. */
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

/** Enough for a level meter, and a fraction of the samples a call records. */
private const val SAMPLE_RATE_HZ = 16_000

/**
 * The window WebRTC reads the microphone in once the call is joined, matched here on purpose.
 *
 * The level is smoothed by a ramp that takes about 250ms to rise and fall, so the indicator sags
 * between updates that arrive more slowly than that. Reading in longer windows measures the same
 * peaks but shows a visibly lower bar, because the bar spends its time falling rather than held
 * at the peak.
 */
private const val READ_WINDOW_MS = 10
private const val BYTES_PER_SAMPLE = 2
private const val BYTES_PER_WINDOW = SAMPLE_RATE_HZ * BYTES_PER_SAMPLE * READ_WINDOW_MS / 1000

/**
 * Reads raw microphone samples while the call has no [io.getstream.video.android.core.call.RtcSession].
 *
 * WebRTC only records once a peer connection sends the audio track, so nothing feeds
 * [SoundInputProcessor] before a call is joined and the lobby sound indicator stays at rest. This
 * recorder fills that gap with a plain [AudioRecord] and hands the same 16 bit PCM buffers to
 * [onSamples], the way iOS meters an `AVAudioRecorder` in its pre-join view.
 *
 * Only one recorder may hold the microphone, so the caller must [stop] this one and wait for it to
 * return before the session starts capturing.
 */
internal class PreJoinMicrophoneRecorder(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSamples: (ByteArray) -> Unit,
) {
    private val logger by taggedLogger("Call:PreJoinMicRecorder")

    private var job: Job? = null

    /** Starts reading the microphone. Does nothing when already reading or without permission. */
    fun start() {
        if (job?.isActive == true) return
        if (!hasRecordAudioPermission()) {
            logger.w { "[start] RECORD_AUDIO is not granted, the microphone level stays at zero" }
            return
        }
        logger.d { "[start] reading the microphone before the call is joined" }
        job = scope.launch(DispatcherProvider.IO) { read() }
    }

    /**
     * Stops reading and returns once the microphone has been released, so the caller can hand the
     * device to WebRTC. The read in flight has to return first, which takes at most
     * [READ_WINDOW_MS].
     */
    suspend fun stop() {
        val running = job ?: return
        logger.d { "[stop] releasing the microphone" }
        running.cancelAndJoin()
        // Cleared only after the join: clearing it first would let a [start] in between open a
        // second recorder while this one still holds the microphone. Compared by identity so a
        // recorder started in the meantime is left alone.
        if (job === running) job = null
    }

    /**
     * Stops reading without waiting for the release. For call teardown, which runs after the call
     * scope is cancelled and has nothing waiting to take the microphone.
     */
    fun stopWithoutWaiting() {
        job?.cancel()
        job = null
    }

    private suspend fun read() {
        val recorder = createRecorder() ?: return
        try {
            safeCall { recorder.startRecording() }
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                logger.w { "[read] the microphone did not start recording" }
                return
            }
            val buffer = ByteArray(BYTES_PER_WINDOW)
            while (currentCoroutineContext().isActive) {
                val bytesRead = recorder.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    onSamples(buffer.copyOf(bytesRead))
                } else if (bytesRead < 0) {
                    logger.w { "[read] the microphone returned error $bytesRead, stopping" }
                    break
                }
            }
        } finally {
            safeCall { recorder.stop() }
            safeCall { recorder.release() }
        }
    }

    /**
     * Opens the microphone, preferring the source a call uses so the recorder follows the device
     * the user picked in the lobby, and falling back to the plain one where that is unavailable.
     */
    @SuppressLint("MissingPermission") // checked in start()
    private fun createRecorder(): AudioRecord? {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            CHANNEL_CONFIG,
            AUDIO_ENCODING,
        )
        if (minBufferSize <= 0) {
            logger.w { "[createRecorder] no buffer size for ${SAMPLE_RATE_HZ}Hz: $minBufferSize" }
            return null
        }
        val bufferSize = maxOf(minBufferSize, BYTES_PER_WINDOW)
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC,
        )
        for (source in sources) {
            val recorder = safeCallWithDefault(null) {
                AudioRecord(source, SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_ENCODING, bufferSize)
            } ?: continue
            if (recorder.state == AudioRecord.STATE_INITIALIZED) return recorder
            safeCall { recorder.release() }
        }
        logger.w { "[createRecorder] could not open the microphone" }
        return null
    }

    private fun hasRecordAudioPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
}
