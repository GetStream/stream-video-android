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
    private val onDeviceSelected: ((StreamAudioDevice) -> Unit)? = null,
) : AudioHandler {

    private val logger by taggedLogger(TAG)
    private var streamAudioSwitch: StreamAudioSwitch? = null
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

                    val switch = StreamAudioSwitch(
                        context = context,
                        preferredDeviceList = preferredDeviceList,
                        onDeviceSelected = onDeviceSelected,
                    )
                    streamAudioSwitch = switch
                    switch.start(audioDeviceChangeListener)
                }
            }
        }
    }

    override fun stop() {
        logger.d { "[stop] no args" }
        mainThreadHandler.removeCallbacksAndMessages(null)
        mainThreadHandler.post {
            streamAudioSwitch?.stop()
            streamAudioSwitch = null
        }
        synchronized(this) {
            isAudioSwitchInitScheduled = false
        }
    }

    override fun selectDevice(audioDevice: StreamAudioDevice?) {
        streamAudioSwitch?.selectDevice(audioDevice)
    }

    companion object {
        private const val TAG = "StreamAudioSwitchHandler"
    }
}
