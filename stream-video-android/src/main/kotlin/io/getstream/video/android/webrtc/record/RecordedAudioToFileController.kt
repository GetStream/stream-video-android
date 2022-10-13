/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package io.getstream.video.android.webrtc.record

import android.media.AudioFormat
import android.os.Environment
import io.getstream.logging.StreamLog
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.webrtc.utils.stringify
import org.webrtc.audio.JavaAudioDeviceModule
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Implements the AudioRecordSamplesReadyCallback interface and writes
 * recorded raw audio samples to an output file.
 */
internal class RecordedAudioToFileController : JavaAudioDeviceModule.SamplesReadyCallback {

    private val logger = StreamLog.getLogger("Call:RecAudioCallback")

    private val lock = Any()
    private val executor: ExecutorService
    private var rawAudioFileOutputStream: OutputStream? = null
    private var isRunning = false
    private var fileSizeInBytes: Long = 0

    private val isEnabled = false

    init {
        logger.d { "<init> ctor" }
        executor = Executors.newSingleThreadExecutor()
    }

    /**
     * Should be called on the same executor thread as the one provided at
     * construction.
     */
    fun start() {
        if (!isEnabled) return
        executor.execute {
            logger.d { "[start] no args" }
            if (!isExternalStorageWritable) {
                logger.e { "[start] failed; writing to external media is not possible" }
                return@execute
            }
            synchronized(lock) { isRunning = true }
        }
    }

    /**
     * Should be called on the same executor thread as the one provided at
     * construction.
     */
    fun stop() {
        if (!isEnabled) return
        executor.execute {
            logger.d { "[stop] no args" }
            synchronized(lock) {
                isRunning = false
                try {
                    rawAudioFileOutputStream?.close()
                } catch (e: IOException) {
                    logger.e { "[stop] failed; unable to close file with saved input audio: $e" }
                }
                rawAudioFileOutputStream = null
                fileSizeInBytes = 0
            }
        }
    }

    // Checks if external storage is available for read and write.
    private val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    // Utilizes audio parameters to create a file name which contains sufficient
    // information so that the file can be played using an external file player.
    // Example: /sdcard/recorded_audio_16bits_48000Hz_mono.pcm.
    private fun openRawAudioOutputFile(sampleRate: Int, channelCount: Int) {
        val fileName = (Environment.getExternalStorageDirectory().path + File.separator
            + "stream_recorded_audio_16bits_${sampleRate}Hz"
            + (if (channelCount == 1) "_mono" else "_stereo") + ".pcm")
        val outputFile = File(fileName)
        try {
            rawAudioFileOutputStream = FileOutputStream(outputFile)
        } catch (e: FileNotFoundException) {
            logger.e { "[openRawAudioOutputFile] failed: $e" }
        }
        logger.i { "[openRawAudioOutputFile] fileName: $fileName" }
    }

    // Called when new audio samples are ready.
    override fun onWebRtcAudioRecordSamplesReady(samples: JavaAudioDeviceModule.AudioSamples) {
        if (!isEnabled) return
        logger.v { "[onWebRtcAudioRecordSamplesReady] samples: ${samples.stringify()}" }
        // The native audio layer on Android should use 16-bit PCM format.
        if (samples.audioFormat != AudioFormat.ENCODING_PCM_16BIT) {
            logger.e { "[onWebRtcAudioRecordSamplesReady] failed; invalid audioFormat: ${samples.audioFormat}" }
            return
        }
        synchronized(lock) {

            // Abort early if stop() has been called.
            if (!isRunning) {
                return
            }
            // Open a new file for the first callback only since it allows us to add audio parameters to
            // the file name.
            if (rawAudioFileOutputStream == null) {
                openRawAudioOutputFile(samples.sampleRate, samples.channelCount)
                fileSizeInBytes = 0
            }
        }
        // Append the recorded 16-bit audio samples to the open output file.
        executor.execute {
            if (rawAudioFileOutputStream != null) {
                try {
                    // Set a limit on max file size. 58348800 bytes corresponds to
                    // approximately 10 minutes of recording in mono at 48kHz.
                    if (fileSizeInBytes < MAX_FILE_SIZE_IN_BYTES) {
                        // Writes samples.getData().length bytes to output stream.
                        rawAudioFileOutputStream!!.write(samples.data)
                        fileSizeInBytes += samples.data.size.toLong()
                    }
                } catch (e: IOException) {
                    logger.e { "[onWebRtcAudioRecordSamplesReady] failed: $e" }
                }
            }
        }
    }

    companion object {
        private const val MAX_FILE_SIZE_IN_BYTES = 58348800L
    }
}