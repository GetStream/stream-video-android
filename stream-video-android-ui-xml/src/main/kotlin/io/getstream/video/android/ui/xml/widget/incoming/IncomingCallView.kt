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

public class IncomingCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewIncomingCallBinding.inflate(inflater, this)

    private var isCameraEnabled = false

    public var callActionListener: (CallAction) -> Unit = {}

    public var backListener: () -> Unit = {}

    init {
        with(binding) {
            incomingCallToolbar.backListener = { backListener() }

            acceptCall.setOnClickListener { callActionListener(AcceptCall) }
            declineCall.setOnClickListener { callActionListener(DeclineCall) }
            cameraToggle.setOnClickListener { callActionListener(ToggleCamera(!isCameraEnabled)) }
        }
    }

    public fun setCameraEnabled(isEnabled: Boolean) {
        isCameraEnabled = isEnabled
        val icon = if (isEnabled) R.drawable.ic_videocam_on else R.drawable.ic_videocam_off
        binding.cameraToggle.setImageResource(icon)
        binding.cameraToggle.isEnabled = isEnabled
    }

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

    public fun setCallStatus(callStatus: CallStatus) {
        binding.participantsInfo.setCallStatus(callStatus)
    }
}
