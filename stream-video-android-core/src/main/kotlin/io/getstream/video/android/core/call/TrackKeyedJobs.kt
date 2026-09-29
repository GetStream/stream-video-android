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

package io.getstream.video.android.core.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import stream.video.sfu.models.TrackType
import java.util.concurrent.ConcurrentHashMap

/**
 * One job per [TrackType]. Replacing a job cancels only that track's previous work, so an
 * in-flight audio mute RPC is not dropped when video or screen-share sync starts.
 */
internal class TrackKeyedJobs {
    private val jobs = ConcurrentHashMap<TrackType, Job>()

    fun launch(
        scope: CoroutineScope,
        trackType: TrackType,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        val job = scope.launch(start = CoroutineStart.LAZY, block = block)
        jobs.put(trackType, job)?.cancel()
        job.start()
    }

    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
}
