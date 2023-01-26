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

package io.getstream.video.android.xml.widget.participant

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.User
import io.getstream.video.android.model.VideoTrack
import io.getstream.video.android.model.toUser
import io.getstream.video.android.xml.databinding.ViewCallParticipantBinding
import io.getstream.video.android.xml.font.setTextStyle
import io.getstream.video.android.xml.utils.extensions.clearConstraints
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.dpToPx
import io.getstream.video.android.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.xml.utils.extensions.load
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.utils.extensions.updateConstraints
import stream.video.sfu.models.TrackType
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Represents a single participant in a call.
 */
public class CallParticipantView : ConstraintLayout {

    private val binding = ViewCallParticipantBinding.inflate(streamThemeInflater, this)

    private lateinit var style: CallParticipantStyle

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

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun init(context: Context, attrs: AttributeSet?, styleAttr: Int, styleRes: Int) {
        style = CallParticipantStyle(context, attrs, styleAttr, styleRes)

        initNameHolder()
        initSpeakerBorder()
    }

    private fun initNameHolder() {
        binding.participantName.setTextStyle(style.tagTextStyle)
        (binding.nameHolder.layoutParams as LayoutParams).setMargins(style.tagPadding)
        binding.nameHolder.background.setTint(style.tagBackgroundColor)
        setTagAlignment(style.tagAlignment)
    }

    private fun initSpeakerBorder() {
        val borderDrawable = GradientDrawable()
        borderDrawable.setStroke(3.dpToPx(), style.activeSpeakerBorderColor)
        binding.activeCallParticipantBorder.background = borderDrawable
    }

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
        initRenderer()
    }

    /**
     * Sets the participant for which we wish to display video for.
     *
     * @param participant The call participant whose video we wish to show.
     */
    public fun setParticipant(participant: CallParticipantState) {
        binding.nameHolder.isVisible = !participant.isLocal
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
        val tint = if (hasAudio) style.participantAudioLevelTint else style.participantMicOffIconTint
        val icon = if (hasAudio) context.getDrawableCompat(RCommon.drawable.ic_mic_on) else style.participantMicOffIcon

        binding.soundIndicator.setImageDrawable(icon)
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
                rendererInitializer?.initRenderer(
                    binding.participantVideoRenderer,
                    it.streamId,
                    TrackType.TRACK_TYPE_VIDEO
                ) { onRender(it) }
                wasRendererInitialised = true
            }
        }
    }

    /**
     * Updates the tag alignment inside the view.
     *
     * @param tagAlignment [CallParticipantTagAlignment] to be applied to the name tag.
     */
    private fun setTagAlignment(tagAlignment: CallParticipantTagAlignment) {
        val holderId = binding.nameHolder.id
        val parentId = LayoutParams.PARENT_ID

        updateConstraints {
            clearConstraints(holderId)
            when (tagAlignment) {
                CallParticipantTagAlignment.TOP_LEFT -> {
                    connect(holderId, ConstraintSet.TOP, parentId, ConstraintSet.TOP, style.tagPadding)
                    connect(holderId, ConstraintSet.LEFT, parentId, ConstraintSet.LEFT, style.tagPadding)
                }
                CallParticipantTagAlignment.TOP_RIGHT -> {
                    connect(holderId, ConstraintSet.TOP, parentId, ConstraintSet.TOP, style.tagPadding)
                    connect(holderId, ConstraintSet.RIGHT, parentId, ConstraintSet.RIGHT, style.tagPadding)
                }
                CallParticipantTagAlignment.BOTTOM_LEFT -> {
                    connect(holderId, ConstraintSet.BOTTOM, parentId, ConstraintSet.BOTTOM, style.tagPadding)
                    connect(holderId, ConstraintSet.LEFT, parentId, ConstraintSet.LEFT, style.tagPadding)
                }
                CallParticipantTagAlignment.BOTTOM_RIGHT -> {
                    connect(holderId, ConstraintSet.BOTTOM, parentId, ConstraintSet.BOTTOM, style.tagPadding)
                    connect(holderId, ConstraintSet.RIGHT, parentId, ConstraintSet.RIGHT, style.tagPadding)
                }
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
