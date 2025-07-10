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

package io.getstream.video.android.core.call.connection.job

import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class RestartIceJobDelegate(private val scope: CoroutineScope, private val scheduledJobTimeoutMs: Long = 3000) {

    private var scheduledJob: Job? = null

    fun scheduleRestartIce(timeoutMs: Long = scheduledJobTimeoutMs, restartIce: suspend () -> Unit) {
        scheduledJob?.cancel()
        scheduledJob = scope.launch {
            safeCall {
                delay(timeoutMs)
                restartIce()
            }
        }
    }

    fun cancelScheduledRestartIce() {
        scheduledJob?.cancel()
    }
}
