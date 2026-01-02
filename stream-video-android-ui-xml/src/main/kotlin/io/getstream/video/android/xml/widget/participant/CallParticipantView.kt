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
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.utils.initials
import io.getstream.video.android.model.User
import io.getstream.video.android.xml.databinding.StreamVideoViewCallParticipantBinding
import io.getstream.video.android.xml.font.setTextStyle
import io.getstream.video.android.xml.utils.extensions.clearConstraints
import io.getstream.video.android.xml.utils.extensions.constrainViewToParentBySide
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.utils.extensions.updateConstraints
import io.getstream.video.android.xml.widget.renderer.VideoRenderer
import io.getstream.video.android.xml.widget.view.CallCardView
import stream.video.sfu.models.TrackType
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Represents a single participant in a call.
 */
public class CallParticipantView : CallCardView, VideoRenderer {

    private val logger by taggedLogger()

    private val binding = StreamVideoViewCallParticipantBinding.inflate(streamThemeInflater, this)

    private lateinit var style: CallParticipantStyle

    /**
     * Flag that notifies if we initialised the renderer for this view or not.
     */
    private var isRendererInitialised: Boolean = false

    /**
     * Handler to initialise the renderer.
     */
    private var rendererInitializer: RendererInitializer? = null

    /**
     * The track of the current participant.
     */
    private var track: MediaTrack? = null

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
        0,
    )

    public constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
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
    public fun setParticipant(participant: ParticipantState) {
        binding.labelHolder.isVisible = !participant.isLocal
        val userName = participant.userNameOrId.value
        setUserData(userName = userName, userImage = participant.image.value)
        setTrack(participant.videoTrack.value)
        setAvatarVisibility(participant)
        setHasAudio(participant.audioEnabled.value)
    }

    /**
     * Updates the ui depending if the participant has mic turned on or not.
     *
     * @param hasAudio Whether the participants mic is on and recording audio or not.
     */
    // TODO build sound level view
    private fun setHasAudio(hasAudio: Boolean) {
        val tint =
            if (hasAudio) style.participantAudioLevelTint else style.participantMicOffIconTint
        val icon =
            if (hasAudio) {
                context.getDrawableCompat(
                    RCommon.drawable.stream_video_ic_mic_on,
                )
            } else {
                style.participantMicOffIcon
            }

        binding.soundIndicator.setImageDrawable(icon)
        binding.soundIndicator.setColorFilter(tint)
    }

    /**
     * Updates the participant avatar and name.
     *
     * @param user The [User] whose video we are viewing.
     */
    private fun setUserData(userName: String, userImage: String?) {
        binding.participantAvatar.setData(initials = userName.initials(), userImage = userImage)
        binding.participantLabel.text = userName
    }

    /**
     * Updates the current track which contains the current participants video.
     *
     * @param track The [VideoTrackWrapper] of the participant.
     */
    private fun setTrack(track: MediaTrack?) {
        if (this.track == track) return

        try {
            this.track?.asVideoTrack()?.video?.removeSink(binding.participantVideoRenderer)
            this.track = track
        } catch (e: java.lang.Exception) {
            logger.e { "[setTrack] Failed to remove sink." }
            e.printStackTrace()
        }

        if (track == null) return

        try {
            this.track!!.asVideoTrack()?.video?.addSink(binding.participantVideoRenderer)
            initRenderer()
        } catch (e: IllegalStateException) {
            logger.e { "[setTrack] Failed to add sink." }
            e.printStackTrace()
        }
    }

    /**
     * Initialises the renderer for this view if it was not initialised.
     */
    private fun initRenderer() {
        if (!isRendererInitialised) {
            track?.let { videoTrack ->
                rendererInitializer?.let { rendererInitializer ->
                    rendererInitializer.initRenderer(
                        binding.participantVideoRenderer,
                        videoTrack.streamId,
                        TrackType.TRACK_TYPE_VIDEO,
                    ) { onRender(it) }
                    isRendererInitialised = true
                }
            }
        }
    }

    /**
     * Shows the avatar if there is no video track for the participant, hides it otherwise.
     *
     * @param participant The [CallParticipantState] for the current participant.
     */
    private fun setAvatarVisibility(participant: ParticipantState) {
        val track = participant.videoTrack.value
        val isVideoEnabled = try {
            track?.video?.enabled() == true
        } catch (error: Throwable) {
            false
        }
        // TODO
        val shouldShowAvatar = false
        binding.participantAvatar.isVisible = shouldShowAvatar
    }

    /**
     * Updates the label alignment inside the view.
     *
     * @param labelAlignment [CallParticipantLabelAlignment] to be applied to the name label.
     */
    private fun setLabelAlignment(labelAlignment: CallParticipantLabelAlignment) {
        val holder = binding.labelHolder

        binding.contentHolder.updateConstraints {
            clearConstraints(holder)
            when (labelAlignment) {
                CallParticipantLabelAlignment.TOP_LEFT -> {
                    constrainViewToParentBySide(holder, ConstraintSet.TOP, style.labelMargin)
                    constrainViewToParentBySide(holder, ConstraintSet.START, style.labelMargin)
                }

                CallParticipantLabelAlignment.TOP_RIGHT -> {
                    constrainViewToParentBySide(holder, ConstraintSet.TOP, style.labelMargin)
                    constrainViewToParentBySide(holder, ConstraintSet.END, style.labelMargin)
                }

                CallParticipantLabelAlignment.BOTTOM_LEFT -> {
                    constrainViewToParentBySide(holder, ConstraintSet.BOTTOM, style.labelMargin)
                    constrainViewToParentBySide(holder, ConstraintSet.START, style.labelMargin)
                }

                CallParticipantLabelAlignment.BOTTOM_RIGHT -> {
                    constrainViewToParentBySide(holder, ConstraintSet.BOTTOM, style.labelMargin)
                    constrainViewToParentBySide(holder, ConstraintSet.END, style.labelMargin)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.participantVideoRenderer.apply {
            track?.asVideoTrack()?.video?.removeSink(this)
        }
        track = null
    }
}
