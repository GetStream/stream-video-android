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

package io.getstream.video.android.xml.widget.view

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job

/**
 * Used to abstract part of the logic needed to clear running jobs for views that have been removed from the screen.
 * Only thing that need to be overridden from the interface is [runningJobs]. There should also be a way to clear the
 * running jobs, eg. calling [stopAllJobs] inside [View.onDetachedFromWindow].
 */
internal interface JobHolder {

    /**
     * List of the currently running jobs collecting data for the screen.
     */
    val runningJobs: MutableList<Job>

    /**
     * Used to start a job for this screen. The started job will be added to the [runningJobs] list so we can clean them
     * up when not needed any more.
     *
     * @param lifecycleOwner The [LifecycleOwner] under which we are starting the job, usually the parent Activity
     * or fragment.
     * @param job The job we want to run, eg. collecting data from a flow.
     */
    fun startJob(lifecycleOwner: LifecycleOwner, job: suspend () -> Unit) {
        runningJobs.add(
            lifecycleOwner.lifecycleScope.launchWhenCreated {
                job()
            },
        )
    }

    /**
     * Stops and clears all of the currently running jobs.
     */
    fun stopAllJobs() {
        runningJobs.forEach { it.cancel() }
        runningJobs.clear()
    }
}
