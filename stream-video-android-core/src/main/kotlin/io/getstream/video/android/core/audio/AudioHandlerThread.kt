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

import android.os.Handler
import android.os.HandlerThread
import io.getstream.video.android.core.StreamVideoClient

/**
 * Wraps a [HandlerThread] and its [Handler] for all AudioSwitch operations.
 *
 * Created lazily by [StreamVideoClient] and shared across all concurrent calls so that
 * IPC-heavy audio-routing calls (setCommunicationDevice, requestAudioFocus, etc.) never
 * block the main thread. The instance is released when the SDK client is cleaned up.
 */
internal class AudioHandlerThread {
    private val thread = HandlerThread("audio-switch-thread").also { it.start() }
    val handler: Handler = Handler(thread.looper)

    fun release() {
        handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
    }
}
