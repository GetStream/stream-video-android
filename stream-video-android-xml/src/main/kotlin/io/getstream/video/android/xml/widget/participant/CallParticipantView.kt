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
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import io.getstream.log.StreamLog
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.model.toUser
import io.getstream.video.android.xml.databinding.ViewCallParticipantBinding
import io.getstream.video.android.xml.font.setTextStyle
import io.getstream.video.android.xml.utils.extensions.clearConstraints
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.xml.utils.extensions.load
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.utils.extensions.updateConstraints
import io.getstream.video.android.xml.widget.renderer.VideoRenderer
import stream.video.sfu.models.TrackType
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Represents a single participant in a call.
 */
public class CallParticipantView : CardView, VideoRenderer {

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
        defStyleAttr
    ) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun init(context: Context, attrs: AttributeSet?, styleAttr: Int, styleRes: Int) {
        style = CallParticipantStyle(context, attrs, styleAttr, styleRes)

        elevation = style.elevation
        radius = style.cornerRadius

        initNameHolder()
        initSpeakerBorder()
    }

    private fun initNameHolder() {
        binding.participantLabel.setTextStyle(style.labelTextStyle)
        (binding.labelHolder.layoutParams as ConstraintLayout.LayoutParams).setMargins(style.labelMargin)
        binding.labelHolder.background.setTint(style.labelBackgroundColor)
        setLabelAlignment(style.labelAlignment)
    }

    private fun initSpeakerBorder() {
        val borderDrawable = GradientDrawable()
        borderDrawable.setStroke(style.activeSpeakerBorderWidth, style.activeSpeakerBorderColor)
        borderDrawable.cornerRadius = style.cornerRadius
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
    override fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        this.rendererInitializer = rendererInitializer
        initRenderer()
    }

    /**
     * Sets the participant for which we wish to display video for.
     *
     * @param participant The call participant whose video we wish to show.
     */
    public fun setParticipant(participant: CallParticipantState) {
        binding.labelHolder.isVisible = !participant.isLocal
        setUserData(participant.toUser())
        setTrack(participant.videoTrack)
        setAvatarVisibility(participant)
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
        binding.participantLabel.text = user.name.ifBlank { user.id }
    }

    /**
     * Updates the current track which contains the current participants video.
     *
     * @param track The [VideoTrack] of the participant.
     */
    private fun setTrack(track: VideoTrack?) {
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

    private fun setAvatarVisibility(participant: CallParticipantState) {
        val track = participant.videoTrack
        val isVideoEnabled = try {
            track?.video?.enabled() == true
        } catch (error: Throwable) {
            false
        }
        val shouldShowAvatar =
            track == null || !isVideoEnabled || TrackType.TRACK_TYPE_VIDEO !in participant.publishedTracks

        StreamLog.d("shouldshowavatar") { "shouldShowAvatar: $shouldShowAvatar, isTrackNull: ${track == null}, isVideoEnabled: $isVideoEnabled, isVideoInPublishedTracks: ${TrackType.TRACK_TYPE_VIDEO in participant.publishedTracks}" }

        binding.participantAvatar.isVisible = shouldShowAvatar
    }

    /**
     * Use to offset label when they are covered by call controls.
     */
    public fun setLabelBottomOffset(offset: Int) {
        val layoutParams = (binding.labelHolder.layoutParams as ConstraintLayout.LayoutParams)
        layoutParams.bottomMargin = offset + style.labelMargin
        binding.labelHolder.layoutParams = layoutParams
    }

    /**
     * Updates the label alignment inside the view.
     *
     * @param labelAlignment [CallParticipantLabelAlignment] to be applied to the name label.
     */
    private fun setLabelAlignment(labelAlignment: CallParticipantLabelAlignment) {
        val holderId = binding.labelHolder.id
        val parentId = ConstraintLayout.LayoutParams.PARENT_ID

        binding.contentHolder.updateConstraints {
            clearConstraints(holderId)
            when (labelAlignment) {
                CallParticipantLabelAlignment.TOP_LEFT -> {
                    connect(holderId, ConstraintSet.TOP, parentId, ConstraintSet.TOP, style.labelMargin)
                    connect(holderId, ConstraintSet.LEFT, parentId, ConstraintSet.LEFT, style.labelMargin)
                }
                CallParticipantLabelAlignment.TOP_RIGHT -> {
                    connect(holderId, ConstraintSet.TOP, parentId, ConstraintSet.TOP, style.labelMargin)
                    connect(holderId, ConstraintSet.RIGHT, parentId, ConstraintSet.RIGHT, style.labelMargin)
                }
                CallParticipantLabelAlignment.BOTTOM_LEFT -> {
                    connect(holderId, ConstraintSet.BOTTOM, parentId, ConstraintSet.BOTTOM, style.labelMargin)
                    connect(holderId, ConstraintSet.LEFT, parentId, ConstraintSet.LEFT, style.labelMargin)
                }
                CallParticipantLabelAlignment.BOTTOM_RIGHT -> {
                    connect(holderId, ConstraintSet.BOTTOM, parentId, ConstraintSet.BOTTOM, style.labelMargin)
                    connect(holderId, ConstraintSet.RIGHT, parentId, ConstraintSet.RIGHT, style.labelMargin)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.participantVideoRenderer.apply {
            track?.video?.removeSink(this)
        }
        track = null
    }
}
