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

package io.getstream.video.android.xml.widget.participant

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.xml.databinding.StreamVideoViewFloatingParticipantBinding
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.view.JobHolder
import kotlinx.coroutines.Job

/**
 * Renders the call participant overlaid above the rest od the participants.
 */
public class FloatingParticipantView : CardView, JobHolder {

    override val runningJobs: MutableList<Job> = mutableListOf()

    private val binding = StreamVideoViewFloatingParticipantBinding.inflate(
        streamThemeInflater,
        this,
    )

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
    )

    /**
     * Sets the [RendererInitializer] handler so we can initialize the texture view with the renderer.
     *
     * @param rendererInitializer Handler to initialise the renderer.
     */
    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        binding.localParticipant.setRendererInitializer(rendererInitializer)
    }

    /**
     * Sets the participant for which we wish to display video for.
     *
     * @param participant The call participant whose video we wish to show.
     */
    public fun setParticipant(participant: ParticipantState) {
        binding.localParticipant.setParticipant(participant)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAllJobs()
    }
}
