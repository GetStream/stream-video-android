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

package io.getstream.video.android.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Config for the [TaskSchedulerWithDebounce] to schedule a task.
 *
 * @param debounce The debounce time in milliseconds.
 */
internal data class ScheduleConfig(
    val debounce: () -> Long = { 0 },
)

/**
 * Helper class to schedule a task with debounce.
 */
internal class TaskSchedulerWithDebounce() {
    private var job: Job? = null

    /**
     * Schedule a task with debounce.
     *
     * @param scope The coroutine scope to launch the task.
     * @param config The config for the task.
     * @param block The task to be executed.
     */
    fun schedule(scope: CoroutineScope, config: ScheduleConfig, block: () -> Unit) {
        job?.cancel()
        val delay: Long = config.debounce()
        if (delay <= 0L) {
            block()
            return
        } else {
            job = scope.launch(Dispatchers.Unconfined) {
                delay(delay)
                block()
            }
        }
    }
}
