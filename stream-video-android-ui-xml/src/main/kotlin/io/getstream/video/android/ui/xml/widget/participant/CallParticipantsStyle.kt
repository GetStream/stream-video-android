package io.getstream.video.android.ui.xml.widget.participant

import android.content.Context
import android.util.AttributeSet
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.use

public data class CallParticipantsStyle(
    public val callParticipantStyle: Int,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): CallParticipantsStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallParticipantsView,
                R.attr.streamCallParticipantsViewStyle,
                R.style.Stream_CallParticipants
            ).use {
                val callParticipantStyle = it.getResourceId(
                    R.styleable.CallParticipantsView_streamCallParticipantsCallParticipantStyle,
                    context.theme.obtainStyledAttributes(
                        R.style.StreamVideoTheme,
                        intArrayOf(R.attr.streamCallParticipantViewStyle)
                    ).getResourceId(0, 0)
                )

                return CallParticipantsStyle(
                    callParticipantStyle = callParticipantStyle
                )
            }
        }
    }
}