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

package io.getstream.video.android.ui.xml.widget.outgoing

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CancelCall
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.common.util.getFloatResource
import io.getstream.video.android.model.CallStatus
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.ui.xml.databinding.ViewOutgoingCallBinding
import io.getstream.video.android.ui.xml.utils.extensions.dpToPx
import io.getstream.video.android.ui.xml.utils.extensions.inflater
import kotlin.math.roundToInt
import io.getstream.video.android.ui.common.R as RCommon

public class OutgoingCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewOutgoingCallBinding.inflate(inflater, this)

    private var actionListener: CallActionListener? = null

    private var isMicrophoneEnabled: Boolean = false
    private var isCameraEnabled: Boolean = false

    init {
        with(binding) {
            cancelCall.setOnClickListener { actionListener?.onCallAction(CancelCall) }
            micToggle.setOnClickListener { actionListener?.onCallAction(ToggleMicrophone(!isMicrophoneEnabled)) }
            cameraToggle.setOnClickListener { actionListener?.onCallAction(ToggleCamera(!isCameraEnabled)) }
        }
    }

    public fun setParticipants(participants: List<CallUser>) {
        binding.participantsInfo.setParticipants(participants)
        if (participants.size >= 2) setGroupCallControlsLayout()
        binding.callBackground.setParticipants(participants)
    }

    public fun setCallStatus(callStatus: CallStatus) {
        binding.participantsInfo.setCallStatus(callStatus)
    }

    public fun setMicrophoneEnabled(isEnabled: Boolean) {
        isMicrophoneEnabled = isEnabled
        val icon = if (isEnabled) RCommon.drawable.ic_mic_on else RCommon.drawable.ic_mic_off
        binding.micToggle.setImageResource(icon)
        binding.micToggle.isEnabled = isEnabled
    }

    public fun setCameraEnabled(isEnabled: Boolean) {
        isCameraEnabled = isEnabled
        val icon = if (isEnabled) RCommon.drawable.ic_videocam_on else RCommon.drawable.ic_videocam_off
        binding.cameraToggle.setImageResource(icon)
        binding.cameraToggle.isEnabled = isEnabled
    }

    private fun setGroupCallControlsLayout() {
        with(binding) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(controlsHolder)
            constraintSet.clear(cancelCall.id, ConstraintSet.BOTTOM)
            constraintSet.clear(micToggle.id, ConstraintSet.BOTTOM)
            constraintSet.clear(cameraToggle.id, ConstraintSet.BOTTOM)

            constraintSet.connect(micToggle.id, ConstraintSet.BOTTOM, controlsHolder.id, ConstraintSet.BOTTOM)
            constraintSet.connect(cameraToggle.id, ConstraintSet.BOTTOM, controlsHolder.id, ConstraintSet.BOTTOM)

            constraintSet.connect(cancelCall.id, ConstraintSet.BOTTOM, micToggle.id, ConstraintSet.TOP)
            constraintSet.setMargin(cancelCall.id, ConstraintSet.BOTTOM, 32.dpToPx())

            controlsHolder.setConstraintSet(constraintSet)
        }
    }

    public fun setCallActionListener(callActionListener: CallActionListener) {
        actionListener = callActionListener
    }

    public fun interface CallActionListener {
        public fun onCallAction(callAction: CallAction)
    }
}
