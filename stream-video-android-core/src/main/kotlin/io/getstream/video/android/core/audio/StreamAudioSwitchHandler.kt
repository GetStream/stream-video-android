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
import android.os.Handler
import android.os.Looper
import io.getstream.log.taggedLogger

/**
 * Handler for audio device switching using our custom StreamAudioSwitch implementation.
 * This is the new implementation that uses native Android AudioManager APIs.
 * Uses NativeStreamAudioDevice for all device operations.
 */
internal class StreamAudioSwitchHandler(
    private val context: Context,
    private val preferredDeviceList: List<Class<out StreamAudioDevice>>,
    private var audioDeviceChangeListener: StreamAudioDeviceChangeListener,
) : AudioHandler {

    private val logger by taggedLogger(TAG)
    private var streamAudioSwitch: StreamAudioSwitch? = null
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var isAudioSwitchInitScheduled = false

    /**
     * Initializes and starts the underlying StreamAudioSwitch on the main thread if not already scheduled.
     *
     * Schedules creation of a StreamAudioSwitch using the handler's context and preferred device list, stores the
     * created instance, and starts it with the configured audio device change listener. If initialization has already
     * been scheduled, this method is a no-op.
     */
    override fun start() {
        synchronized(this) {
            if (!isAudioSwitchInitScheduled) {
                isAudioSwitchInitScheduled = true
                mainThreadHandler.removeCallbacksAndMessages(null)

                logger.d { "[start] Posting on main" }

                mainThreadHandler.post {
                    logger.d { "[start] Running on main" }

                    val switch = StreamAudioSwitch(
                        context = context,
                        preferredDeviceList = preferredDeviceList,
                    )
                    streamAudioSwitch = switch
                    switch.start(audioDeviceChangeListener)
                }
            }
        }
    }

    /**
     * Stops and releases the active StreamAudioSwitch.
     *
     * Removes any pending main-thread callbacks, stops the active StreamAudioSwitch if present,
     * and clears the stored reference. All teardown is performed on the main thread.
     */
    override fun stop() {
        logger.d { "[stop] no args" }
        mainThreadHandler.removeCallbacksAndMessages(null)
        mainThreadHandler.post {
            streamAudioSwitch?.stop()
            streamAudioSwitch = null
        }
    }

    /**
     * Selects an audio device for use.
     *
     * If the internal audio switch is initialized, forwards the selection to it; otherwise no-op.
     *
     * @param audioDevice The device to select, or `null` to clear the current selection.
     */
    override fun selectDevice(audioDevice: StreamAudioDevice?) {
        streamAudioSwitch?.selectDevice(audioDevice)
    }

    public companion object {
        private const val TAG = "StreamAudioSwitchHandler"
    }
}