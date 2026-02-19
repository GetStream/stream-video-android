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

package io.getstream.video.android.xml.widget.outgoing

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintSet
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.xml.databinding.StreamVideoViewOutgoingCallBinding
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.dpToPx
import io.getstream.video.android.xml.utils.extensions.getDimension
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.control.ControlButtonView
import io.getstream.video.android.xml.widget.view.CallConstraintLayout

/**
 *  Represents the Outgoing Call state and UI, when the user is calling other people.
 */
public class OutgoingCallView : CallConstraintLayout {

    private val binding = StreamVideoViewOutgoingCallBinding.inflate(streamThemeInflater, this)

    private lateinit var style: OutgoingCallStyle

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        style = OutgoingCallStyle(context, attrs)

        binding.callBackground.background = style.outgoingScreenBackground

        initCancelCallButton()
        initMediaControlButton(binding.micToggle)
        initMediaControlButton(binding.cameraToggle)
    }

    /**
     * Initialises the media control buttons with the style.
     */
    private fun initMediaControlButton(button: ControlButtonView) {
        with(button) {
            background = style.mediaButtonBackground
            background.setTint(style.mediaButtonBackgroundTint)
            setColorFilter(style.mediaButtonIconTint)
        }
    }

    /**
     * Initialises the cancel call button with the style.
     */
    private fun initCancelCallButton() {
        with(binding.cancelCall) {
            background = style.cancelCallBackground
            background.setTint(style.cancelCallBackgroundTint)
            setImageDrawable(style.cancelCallIcon)
            setColorFilter(style.cancelCallIconTint)
        }
    }

    /**
     * Whether the microphone is enabled or not.
     */
    private var isMicrophoneEnabled: Boolean = false

    /**
     * Whether the microphone is camera or not.
     */
    private var isCameraEnabled: Boolean = false

    /**
     * Handler that notifies when a call action has been performed.
     */
    public var callActionListener: (CallAction) -> Unit = { }

    init {
        with(binding) {
            cancelCall.setOnClickListener { callActionListener(CancelCall) }
            micToggle.setOnClickListener {
                callActionListener(
                    ToggleMicrophone(isMicrophoneEnabled),
                )
            }
            cameraToggle.setOnClickListener { callActionListener(ToggleCamera(isCameraEnabled)) }
        }
    }

    /**
     * Sets the participants avatars and updates the call background.
     *
     * @param participants The list of the participants inside the call.
     */
    public fun setParticipants(participants: List<CallUser>) {
        binding.callDetails.setParticipants(participants)
        if (participants.size > 1) setGroupCallControlsLayout()
        binding.callBackground.setParticipants(participants, style.outgoingScreenBackground)
    }

    /**
     * Sets the call status in the outgoing screen.
     *
     * @param callStatus The current [CallStatus].
     */
    public fun setCallStatus(callStatus: CallStatus) {
        binding.callDetails.setCallStatus(callStatus)
    }

    /**
     * Updates the microphone state shown in the ui.
     *
     * @param isEnabled Whether the microphone is enabled or not.
     */
    public fun setMicrophoneEnabled(isEnabled: Boolean) {
        isMicrophoneEnabled = isEnabled
        val icon = if (isEnabled) style.microphoneIconEnabled else style.microphoneIconDisabled
        binding.micToggle.setImageDrawable(icon)
        binding.micToggle.isEnabled = isEnabled
    }

    /**
     * Updates the camera state shown in the ui.
     *
     * @param isEnabled Whether the camera is enabled or not.
     */
    public fun setCameraEnabled(isEnabled: Boolean) {
        isCameraEnabled = isEnabled
        val icon = if (isEnabled) style.videoIconEnabled else style.videoIconDisabled
        binding.cameraToggle.setImageDrawable(icon)
        binding.cameraToggle.isEnabled = isEnabled
    }

    /**
     * Updates the call actions whether the call is 1:1 or more users are joining the call.
     */
    private fun setGroupCallControlsLayout() {
        with(binding) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(controlsHolder)
            constraintSet.clear(cancelCall.id, ConstraintSet.BOTTOM)
            constraintSet.clear(micToggle.id, ConstraintSet.BOTTOM)
            constraintSet.clear(cameraToggle.id, ConstraintSet.BOTTOM)

            constraintSet.connect(
                micToggle.id,
                ConstraintSet.BOTTOM,
                controlsHolder.id,
                ConstraintSet.BOTTOM,
            )
            constraintSet.connect(
                cameraToggle.id,
                ConstraintSet.BOTTOM,
                controlsHolder.id,
                ConstraintSet.BOTTOM,
            )

            constraintSet.connect(
                cancelCall.id,
                ConstraintSet.BOTTOM,
                micToggle.id,
                ConstraintSet.TOP,
            )
            constraintSet.setMargin(cancelCall.id, ConstraintSet.BOTTOM, 32.dpToPx())

            controlsHolder.setConstraintSet(constraintSet)

            (callDetails.layoutParams as LayoutParams).apply {
                this.topMargin =
                    context.getDimension(io.getstream.video.android.ui.common.R.dimen.stream_video_avatarAppbarPadding)
                requestLayout()
            }
        }
    }
}
