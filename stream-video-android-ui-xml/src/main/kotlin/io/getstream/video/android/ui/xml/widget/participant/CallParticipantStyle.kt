package io.getstream.video.android.ui.xml.widget.participant

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Px
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.font.TextStyle
import io.getstream.video.android.ui.xml.utils.extensions.dpToPxPrecise
import io.getstream.video.android.ui.xml.utils.extensions.getColorCompat
import io.getstream.video.android.ui.xml.utils.extensions.getDimension
import io.getstream.video.android.ui.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.ui.xml.utils.extensions.getEnum
import io.getstream.video.android.ui.xml.utils.extensions.use
import io.getstream.video.android.ui.common.R as RCommon

public data class CallParticipantStyle(
    public val tagAlignment: CallParticipantTagAlignment,
    @Px public val tagPadding: Int,
    @ColorInt public val activeSpeakerBorderColor: Int,
    public val tagTextStyle: TextStyle,
    @ColorInt public val tagBackgroundColor: Int,
    public val participantMicOffIcon: Drawable,
    @ColorInt public val participantMicOffIconTint: Int,
    @ColorInt public val participantAudioLevelTint: Int
) {

    internal companion object {
        private val DEFAULT_TAG_MARGIN = 8.dpToPxPrecise()

        operator fun invoke(context: Context, attrs: AttributeSet?): CallParticipantStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallParticipantView,
                R.attr.streamCallParticipantViewStyle,
                R.style.Stream_CallParticipant
            ).use {

                val tagAlignment = it.getEnum(
                    R.styleable.CallParticipantView_streamCallParticipantTagAlignment,
                    CallParticipantTagAlignment.TOP_LEFT
                )

                val tagPadding = it.getDimension(
                    R.styleable.CallParticipantView_streamCallParticipantTagMargin,
                    DEFAULT_TAG_MARGIN
                )

                val activeSpeakerBorderColor = it.getColor(
                    R.styleable.CallParticipantView_streamCallParticipantActiveSpeakerBorderColor,
                    context.getColorCompat(RCommon.color.stream_info_accent)
                )

                val tagTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallParticipantView_streamCallParticipantTagTextSize,
                        context.getDimension(RCommon.dimen.bodyTextSize)
                    )
                    .color(
                        R.styleable.CallParticipantView_streamCallParticipantTagTextColor,
                        context.getColorCompat(R.color.stream_white)
                    )
                    .font(
                        R.styleable.CallParticipantView_streamCallParticipantTagTextFontAssets,
                        R.styleable.CallParticipantView_streamCallParticipantTagTextFont
                    )
                    .style(
                        R.styleable.CallParticipantView_streamCallParticipantTagTextStyle,
                        Typeface.BOLD
                    )
                    .build()

                val tagBackgroundColor = it.getColor(
                    R.styleable.CallParticipantView_streamCallParticipantActiveSpeakerBorderColor,
                    context.getColorCompat(RCommon.color.stream_info_accent)
                )

                val participantMicOffIcon = it.getDrawable(
                    R.styleable.CallParticipantView_streamCallParticipantMicrophoneOffIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_mic_off)!!

                val participantMicOffIconTint = it.getColor(
                    R.styleable.CallParticipantView_streamCallParticipantMicrophoneOffTint,
                    context.getColorCompat(RCommon.color.stream_error_accent)
                )

                val participantAudioLevelTint = it.getColor(
                    R.styleable.CallParticipantView_streamCallParticipantAudioLevelTint,
                    context.getColorCompat(R.color.stream_white)
                )

                return CallParticipantStyle(
                    tagAlignment = tagAlignment,
                    tagPadding = tagPadding.toInt(),
                    activeSpeakerBorderColor = activeSpeakerBorderColor,
                    tagTextStyle = tagTextStyle,
                    tagBackgroundColor = tagBackgroundColor,
                    participantMicOffIcon = participantMicOffIcon,
                    participantMicOffIconTint = participantMicOffIconTint,
                    participantAudioLevelTint = participantAudioLevelTint
                )
            }
        }
    }
}

public enum class CallParticipantTagAlignment {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}