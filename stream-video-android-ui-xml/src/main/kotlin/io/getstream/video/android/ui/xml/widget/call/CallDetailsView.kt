package io.getstream.video.android.ui.xml.widget.call

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.getstream.video.android.common.util.buildLargeCallText
import io.getstream.video.android.common.util.buildSmallCallText
import io.getstream.video.android.model.CallStatus
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.ui.xml.databinding.ViewCallDetailsBinding
import io.getstream.video.android.ui.xml.font.setTextStyle
import io.getstream.video.android.ui.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.ui.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.ui.xml.widget.avatar.AvatarView
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Shows call participants avatars, info and call information.
 */
public class CallDetailsView : ConstraintLayout {

    private lateinit var style: CallDetailsStyle

    private val binding = ViewCallDetailsBinding.inflate(streamThemeInflater, this)

    public constructor(context: Context) : this(context, null, 0)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
        defStyleRes
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
            CallStatus.Incoming -> context.getString(RCommon.string.call_status_incoming)
            CallStatus.Outgoing -> context.getString(RCommon.string.call_status_outgoing)
            is CallStatus.Calling -> callStatus.duration
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
     * Populates the ui with the participants. Up to 3 avatars will be visible. If there are 3 participants the last
     * it will be the last avatar, otherwise the avatar will display the remaining participants count.
     *
     * @param participants The list of the current call participants.
     */
    private fun setParticipantsAvatars(participants: List<CallUser>) {
        binding.avatarsHolder.removeAllViews()
        val isSingleParticipant = participants.size == 1

        participants.take(2).forEachIndexed { index, participant ->
            if (index > 0) {
                addAvatarSpacer()
            }

            addAvatar(participant.imageUrl, participant.name, isSingleParticipant)
        }

        if (participants.size == 3) {
            val participant = participants[2]
            addAvatarSpacer()
            addAvatar(participant.imageUrl, participant.name, isSingleParticipant)
        }

        if (participants.size > 3) {
            addAvatarSpacer()
            addAvatar("", "+${participants.size - 2}", isSingleParticipant)
        }
    }

    /**
     * Sets the participants info text. By default concatenates the participants names, up to 3 participants, after
     * which the remaining count is shown.
     *
     * @param participants The list of the current call participants.
     */
    private fun setParticipantsText(participants: List<CallUser>) {
        val textStyle = if (participants.size == 1) style.singleParticipantTextStyle else style.participantsTextStyle
        binding.participantsInfo.setTextStyle(textStyle)
        binding.participantsInfo.text = if (participants.size < 3) {
            buildSmallCallText(participants)
        } else {
            buildLargeCallText(participants)
        }
    }

    /**
     * Ease of use method to create and add a new avatar.
     *
     * @param imageUrl The avatar url of the participant.
     * @param isSingleAvatar Whether there is one participant (direct call) or multiple participants (group call).
     */
    private fun addAvatar(imageUrl: String?, name: String, isSingleAvatar: Boolean) {
        val avatarSize = if (isSingleAvatar) style.singleAvatarSize else style.callAvatarSize
        val avatar = AvatarView(context).apply {
            layoutParams = LayoutParams(avatarSize, avatarSize)
        }
        avatar.setData(imageUrl ?: "", name)
        binding.avatarsHolder.addView(avatar)
    }

    /**
     * Ease of use method that creates a space view between avatars.
     */
    private fun addAvatarSpacer() {
        binding.avatarsHolder.addView(View(context).apply {
            layoutParams = LayoutParams(style.avatarSpacing, LayoutParams.MATCH_PARENT)
        })
    }
}