package io.getstream.video.android.ui.xml.widget.call

import android.content.Context
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
        setDefaultBackground()
        scaleType = ScaleType.CENTER_CROP
    }

    /**
     * Set the call participants to show the background image.
     *
     * @param participants The list of participants in the call.
     */
    public fun setParticipants(participants: List<CallUser>) {
        if (participants.size == 1) {
            loadImage(participants)
        } else {
            setDefaultBackground()
        }
    }

    /**
     * Sets the default background.
     */
    private fun setDefaultBackground() {
        background = AppCompatResources.getDrawable(context, R.drawable.bg_call)
    }

    /**
     * Sets the participants avatar as the background.
     */
    private fun loadImage(participants: List<CallUser>) {
        val firstParticipant = participants.first()
        load(firstParticipant.imageUrl, R.drawable.bg_call)
    }

}