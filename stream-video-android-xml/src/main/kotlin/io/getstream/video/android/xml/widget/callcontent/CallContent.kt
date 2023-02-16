/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.xml.widget.callcontent

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job

/**
 * Base class for content when entering or inside a call. Will handle cleaning all of the jobs when removed from the
 * screen if they are attached programmatically. If they are used with XML and you wish to stop observing to clean
 * up resources when hiding content call [stopAllJobs].
 */
public abstract class CallContent : ConstraintLayout {

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * List of the currently running jobs collecting data for the screen.
     */
    private val runningJobs: MutableList<Job> = mutableListOf()

    /**
     * Used to start a job for this screen. The started job will be added to the [runningJobs] list so we can clean them
     * up when not needed any more.
     *
     * @param lifecycleOwner The [LifecycleOwner] under which we are starting the job, usually the parent Activity
     * or fragment.
     * @param job The job we want to run, eg. collecting data from a flow.
     */
    public fun startJob(lifecycleOwner: LifecycleOwner, job: suspend () -> Unit) {
        runningJobs.add(
            lifecycleOwner.lifecycleScope.launchWhenResumed {
                job()
            }
        )
    }

    /**
     * Stops and clears all of the currently running jobs.
     */
    public fun stopAllJobs() {
        runningJobs.forEach { it.cancel() }
        runningJobs.clear()
    }

    override fun onDetachedFromWindow() {
        stopAllJobs()
        super.onDetachedFromWindow()
    }
}
