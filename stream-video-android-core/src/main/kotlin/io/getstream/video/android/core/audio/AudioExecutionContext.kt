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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.Executors

internal class AudioExecutionContext {

    private val dispatcher: ExecutorCoroutineDispatcher by lazy {
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "stream-audio-thread").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    }

    private val scope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + dispatcher)
    }

    fun createChildScope(): CoroutineScope {
        return CoroutineScope(scope.coroutineContext + Job())
    }

    fun release() {
        scope.cancel()
        dispatcher.close()
    }
}
