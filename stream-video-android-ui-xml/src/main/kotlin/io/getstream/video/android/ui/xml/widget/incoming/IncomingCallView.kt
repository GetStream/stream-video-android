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

package io.getstream.video.android.ui.xml.widget.incoming

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.getstream.video.android.call.state.AcceptCall
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.DeclineCall
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.model.CallStatus
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.ui.xml.databinding.ViewIncomingCallBinding
import io.getstream.video.android.ui.xml.utils.extensions.getDimension
import io.getstream.video.android.ui.xml.utils.extensions.inflater
import io.getstream.video.android.viewmodel.CallViewModel

/**
 * Represents the Incoming Call state and UI, when the user receives a call from other people.
 */
public class IncomingCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewIncomingCallBinding.inflate(inflater, this)

    /**
     * Whether the camera is enabled or not.
     */
    private var isCameraEnabled = false

    /**
     * Handled that notifies about performed Call Actions.
     */
    public var callActionListener: (CallAction) -> Unit = {}

    init {
        with(binding) {
            acceptCall.setOnClickListener { callActionListener(AcceptCall) }
            declineCall.setOnClickListener { callActionListener(DeclineCall) }
            cameraToggle.setOnClickListener { callActionListener(ToggleCamera(!isCameraEnabled)) }
        }
    }

    /**
     * Sets the camera option icon as enabled or disabled.
     *
     * @param isEnabled Whether the camera is enabled or disabled.
     */
    public fun setCameraEnabled(isEnabled: Boolean) {
        isCameraEnabled = isEnabled
        val icon = if (isEnabled) R.drawable.ic_videocam_on else R.drawable.ic_videocam_off
        binding.cameraToggle.setImageResource(icon)
        binding.cameraToggle.isEnabled = isEnabled
    }

    /**
     * Depending on the number of participants updated the avatar sizes on incoming/outgoing screens and updates
     * the background.
     *
     * @param participants The list of participants inside the call.
     */
    public fun setParticipants(participants: List<CallUser>) {
        binding.participantsInfo.setParticipants(participants)
        binding.callBackground.setParticipants(participants)

        if (participants.size > 1) {
            (binding.participantsInfo.layoutParams as LayoutParams).apply {
                this.topMargin = context.getDimension(R.dimen.avatarAppbarPadding)
                requestLayout()
            }
        }
    }

    /**
     * Sets the call status text.
     *
     * @param callStatus The current [CallStatus].
     */
    public fun setCallStatus(callStatus: CallStatus) {
        binding.participantsInfo.setCallStatus(callStatus)
    }
}
