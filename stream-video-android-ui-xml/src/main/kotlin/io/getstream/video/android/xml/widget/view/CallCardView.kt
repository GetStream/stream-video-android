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

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import io.getstream.video.android.xml.utils.OrientationChangeListener
import kotlinx.coroutines.Job

/**
 * Base class for content when entering or inside a call. Will handle cleaning all of the jobs when removed from the
 * screen if they are attached programmatically. If they are used with XML and you wish to stop observing to clean
 * up resources when hiding content call [stopAllJobs].
 */
public abstract class CallCardView : CardView, JobHolder, OrientationChangeListener {

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    override val runningJobs: MutableList<Job> = mutableListOf()

    override fun onDetachedFromWindow() {
        stopAllJobs()
        super.onDetachedFromWindow()
    }
}
