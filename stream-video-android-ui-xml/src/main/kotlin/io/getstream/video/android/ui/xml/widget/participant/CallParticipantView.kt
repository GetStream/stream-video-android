/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.ui.xml.widget.participant

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.User
import io.getstream.video.android.model.VideoTrack
import io.getstream.video.android.model.toUser
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.databinding.ViewCallParticipantBinding
import io.getstream.video.android.ui.xml.utils.extensions.inflater
import io.getstream.video.android.ui.xml.utils.extensions.load
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Represents a single participant in a call.
 */
public class CallParticipantView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewCallParticipantBinding.inflate(inflater, this)

    /**
     * Flag that notifies if we initialised the renderer for this view or not.
     */
    private var wasRendererInitialised: Boolean = false

    /**
     * Handler to initialise the renderer.
     */
    private var rendererInitializer: RendererInitializer? = null

    /**
     * The track of the current participant.
     */
    private var track: VideoTrack? = null

    /**
     * Handler when the video renders.
     */
    public var onRender: (View) -> Unit = {}

    /**
     * Sets this view as the active speaker.
     *
     * @param isActive Whether this participant is the active speaker or not.
     */
    public fun setActive(isActive: Boolean) {
        binding.activeCallParticipantBorder.isVisible = isActive
    }

    /**
     * Sets the [RendererInitializer] handler so we can initialize the texture view with the renderer.
     *
     * @param rendererInitializer Handler to initialise the renderer.
     */
    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        wasRendererInitialised = false
        initRenderer()
    }

    /**
     * Sets the participant for which we wish to display video for.
     *
     * @param participant The call participant whose video we wish to show.
     */
    public fun setParticipant(participant: CallParticipantState) {
        setUserData(participant.toUser())
        setTrack(participant.videoTrack, participant.hasVideo)
        setHasAudio(participant.hasAudio)
    }

    /**
     * Updates the ui depending if the participant has mic turned on or not.
     *
     * @param hasAudio Whether the participants mic is on and recording audio or not.
     */
    // TODO build sound level view
    private fun setHasAudio(hasAudio: Boolean) {
        val tint = ContextCompat.getColor(context, if (hasAudio) R.color.stream_white else RCommon.color.stream_error_accent)
        val icon = if (hasAudio) RCommon.drawable.ic_mic_on else RCommon.drawable.ic_mic_off

        binding.soundIndicator.setImageResource(icon)
        binding.soundIndicator.setColorFilter(tint)
    }

    /**
     * Updates the participant avatar and name.
     *
     * @param user The [User] whose video we are viewing.
     */
    private fun setUserData(user: User) {
        binding.participantAvatar.load(user.imageUrl)
        binding.participantName.text = user.name.ifBlank { user.id }
    }

    /**
     * Updates the current track which contains the current participants video.
     *
     * @param track The [VideoTrack] of the participant.
     * @param hasVideo Whether the participants video is being sent or not.
     */
    private fun setTrack(track: VideoTrack?, hasVideo: Boolean) {
        binding.participantAvatar.isVisible = !hasVideo

        if (this.track == track) return

        this.track?.video?.removeSink(binding.participantVideoRenderer)
        this.track = track

        if (track == null) return

        this.track!!.video.addSink(binding.participantVideoRenderer)
        initRenderer()
    }

    /**
     * Initialises the renderer for this view if it was not initialised.
     */
    private fun initRenderer() {
        if (!wasRendererInitialised) {
            track?.let {
                rendererInitializer?.initRenderer(binding.participantVideoRenderer, it.streamId) { onRender(it) }
                wasRendererInitialised = true
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.participantVideoRenderer.apply {
            track?.video?.removeSink(this)
            release()
        }
        track = null
    }
}
