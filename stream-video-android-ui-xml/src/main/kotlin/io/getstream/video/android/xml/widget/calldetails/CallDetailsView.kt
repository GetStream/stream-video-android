/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.xml.widget.calldetails

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.ui.common.util.buildLargeCallText
import io.getstream.video.android.ui.common.util.buildSmallCallText
import io.getstream.video.android.xml.databinding.StreamVideoViewCallDetailsBinding
import io.getstream.video.android.xml.font.setTextStyle
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.avatar.AvatarView
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Shows call participants avatars, info and call information.
 */
public class CallDetailsView : ConstraintLayout {

    private lateinit var style: CallDetailsStyle

    private val binding = StreamVideoViewCallDetailsBinding.inflate(streamThemeInflater, this)

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
        style = CallDetailsStyle(context, attrs)

        binding.callStatus.setTextStyle(style.callStateTextStyle)
    }

    /**
     * Sets the call status info.
     *
     * @param callStatus The current [CallStatus].
     */
    public fun setCallStatus(callStatus: CallStatus) {
        binding.callStatus.text = when (callStatus) {
            CallStatus.Incoming -> context.getString(RCommon.string.stream_video_call_status_incoming)
            CallStatus.Outgoing -> context.getString(RCommon.string.stream_video_call_status_outgoing)
            is CallStatus.Ongoing -> callStatus.duration
        }
    }

    /**
     * Sets the participants info avatars and text.
     *
     * @param participants The list of the current call participants.
     */
    public fun setParticipants(participants: List<CallUser>) {
        setParticipantsAvatars(participants)
        setParticipantsText(participants)
    }

    /**
     * Populates the ui with the participant avatars. Up to 3 avatars will be visible. If there are 3 participants the
     * last avatar will be the last participant, otherwise the avatar will display the remaining participants count.
     *
     * @param participants The list of the current call participants.
     */
    private fun setParticipantsAvatars(participants: List<CallUser>) {
        binding.avatarsHolder.removeAllViews()
        val isSingleParticipant = participants.size == 1

        participants.take(2).forEachIndexed { index, participant ->
            if (index != 0) {
                addAvatarSpacer()
            }

            addAvatar(participant, isSingleParticipant)
        }

        if (participants.size == 3) {
            val participant = participants[2]
            addAvatarSpacer()
            addAvatar(participant, isSingleParticipant)
        }

        if (participants.size > 3) {
            addAvatarSpacer()
            addAvatar(null, isSingleParticipant, "+${participants.size - 2}")
        }
    }

    /**
     * Sets the participants info text. By default concatenates the participants names, up to 3 participants, after
     * which the remaining count is shown.
     *
     * @param participants The list of the current call participants.
     */
    private fun setParticipantsText(participants: List<CallUser>) {
        val textStyle =
            if (participants.size == 1) style.singleParticipantTextStyle else style.participantsTextStyle
        binding.participantsInfo.setTextStyle(textStyle)
        binding.participantsInfo.text = if (participants.size < 3) {
            buildSmallCallText(context, participants)
        } else {
            buildLargeCallText(context, participants)
        }
    }

    /**
     * Ease of use method to create and add a new avatar.
     *
     * @param user [CallUser] for which we want to show the avatar.
     * @param isSingleAvatar Whether there is one participant (direct call) or multiple participants (group call).
     * @param text Text which we want to show inside the avatar if the [user] is null.
     */
    private fun addAvatar(user: CallUser?, isSingleAvatar: Boolean, text: String = "") {
        val avatarSize = if (isSingleAvatar) style.singleAvatarSize else style.callAvatarSize
        val avatar = AvatarView(context).apply {
            layoutParams = LayoutParams(avatarSize, avatarSize)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        if (user != null) {
            avatar.setData(user)
        } else {
            avatar.setData(text)
        }
        binding.avatarsHolder.addView(avatar)
    }

    /**
     * Ease of use method that creates a space view between avatars.
     */
    private fun addAvatarSpacer() {
        binding.avatarsHolder.addView(
            View(context).apply {
                layoutParams = LayoutParams(style.avatarSpacing, LayoutParams.MATCH_PARENT)
            },
        )
    }
}
