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

import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class AudioSwitchDecorator(private val controller: AudioSwitchController) :
    AudioHandler {

    private var scope: CoroutineScope? = null

    /**
     * Synchronized to make scope initialization and replacement atomic.
     * Prevents races where multiple scopes are created or a cancelled scope
     * is reused, leading to silent no-op executions.
     * Not a hot path, so the overhead is negligible.
     */
    @Synchronized
    private fun ensureScope(): CoroutineScope? {
        val ctx = (StreamVideo.instanceOrNull() as? StreamVideoClient)
            ?.getAudioContext() ?: return null

        val existing = scope

        if (existing != null && existing.coroutineContext[Job]?.isActive == true) {
            return existing
        }

        val newScope = ctx.createChildScope() ?: return null
        scope = newScope
        return newScope
    }

    override fun start() {
        val scope = ensureScope() ?: return
        scope.launch { controller.start() }
    }

    override fun stop() {
        val scope = ensureScope() ?: return
        scope.launch { controller.stop() }
    }

    override fun selectDevice(audioDevice: StreamAudioDevice?) {
        val scope = ensureScope() ?: return
        scope.launch { controller.selectDevice(audioDevice) }
    }
}
