package io.getstream.video.android.ui.xml.widget.call

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.ui.xml.utils.extensions.load

/**
 * Sets the incoming/outgoing call background. If the call is 1:1 will show the avatar of the other user, if there are
 * more participants it will show a default background.
 */
public class CallBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        scaleType = ScaleType.CENTER_CROP
    }

    /**
     * Set the call participants to show the background image.
     *
     * @param participants The list of participants in the call.
     * @param groupCallBackground The background used if the call is group call.
     */
    public fun setParticipants(participants: List<CallUser>, groupCallBackground: Drawable?) {
        if (participants.size == 1) {
            loadImage(participants)
        } else {
            setGroupCallBackground(groupCallBackground)
        }
    }

    /**
     * Sets the default background.
     *
     * @param groupCallBackground The background drawable to be shown.
     */
    private fun setGroupCallBackground(groupCallBackground: Drawable?) {
        background = groupCallBackground
    }

    /**
     * Sets the participants avatar as the background.
     */
    private fun loadImage(participants: List<CallUser>) {
        val firstParticipant = participants.first()
        load(firstParticipant.imageUrl, R.drawable.bg_call)
    }
}