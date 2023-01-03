package io.getstream.video.android.ui.xml.widget.call

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.annotation.Px
import io.getstream.video.android.common.util.getFloatResource
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.font.TextStyle
import io.getstream.video.android.ui.xml.utils.extensions.dpToPx
import io.getstream.video.android.ui.xml.utils.extensions.getColorCompat
import io.getstream.video.android.ui.xml.utils.extensions.getDimension
import io.getstream.video.android.ui.xml.utils.extensions.use
import io.getstream.video.android.ui.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [CallDetailsView].
 * Use this class together with [TransformStyle.callDetailsStyleTransformer] to change [CallDetailsView] styles
 * programmatically.
 *
 * @param callAvatarSize The size of avatars for group calls.
 * @param singleAvatarSize The size of avatar for direct call.
 * @param avatarSpacing The spacing between avatars when in group call.
 * @param participantsTextStyle Text style of the participants info text for a group call.
 * @param singleParticipantTextStyle Text style of participants info for a direct call.
 * @param callStateTextStyle Text style for the call state.
 * @param callStateTextAlpha Alpha value for the call state.
 */
public data class CallDetailsStyle(
    @Px public val callAvatarSize: Int,
    @Px public val singleAvatarSize: Int,
    @Px public val avatarSpacing: Int,
    public val participantsTextStyle: TextStyle,
    public val singleParticipantTextStyle: TextStyle,
    public val callStateTextStyle: TextStyle,
    public val callStateTextAlpha: Float
) {

    internal companion object {
        private val DEFAULT_AVATAR_SPACING = 20.dpToPx()

        private val DIRECT_CALL_INFO_TEXT_SIZE = RCommon.dimen.directCallUserNameTextSize
        private val GROUP_CALL_INFO_TEXT_SIZE = RCommon.dimen.groupCallUserNameTextSize

        private val DEFAULT_TEXT_COLOR = RCommon.color.stream_text_high_emphasis

        operator fun invoke(context: Context, attrs: AttributeSet?): CallDetailsStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallDetailsView,
                R.attr.streamCallDetailsViewStyle,
                R.style.Stream_CallDetails
            ).use {
                val callAvatarSize = it.getDimensionPixelSize(
                    R.styleable.CallDetailsView_streamCallDetailsAvatarSize,
                    context.getDimension(RCommon.dimen.callAvatarSize)
                )

                val singleAvatarSize = it.getDimensionPixelSize(
                    R.styleable.CallDetailsView_streamCallDetailsDirectCallAvatarSize,
                    context.getDimension(RCommon.dimen.singleAvatarSize)
                )

                val avatarSpacing = it.getDimensionPixelSize(
                    R.styleable.CallDetailsView_streamCallDetailsAvatarSpacing,
                    DEFAULT_AVATAR_SPACING
                )

                val participantsTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallDetailsView_streamCallDetailsParticipantsInfoTextSize,
                        context.getDimension(GROUP_CALL_INFO_TEXT_SIZE)
                    )
                    .color(
                        R.styleable.CallDetailsView_streamCallDetailsParticipantsInfoTextColor,
                        context.getColorCompat(DEFAULT_TEXT_COLOR)
                    )
                    .font(
                        R.styleable.CallDetailsView_streamCallDetailsParticipantsInfoFontAsset,
                        R.styleable.CallDetailsView_streamCallDetailsParticipantsInfoFont,
                    )
                    .style(
                        R.styleable.CallDetailsView_streamCallDetailsParticipantsInfoTextStyle,
                        Typeface.NORMAL
                    )
                    .build()

                val singleParticipantTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallDetailsView_streamCallDetailsDirectCallParticipantInfoTextSize,
                        context.getDimension(DIRECT_CALL_INFO_TEXT_SIZE)
                    )
                    .color(
                        R.styleable.CallDetailsView_streamCallDetailsParticipantsInfoTextColor,
                        context.getColorCompat(DEFAULT_TEXT_COLOR)
                    )
                    .font(
                        R.styleable.CallDetailsView_streamCallDetailsParticipantsInfoFontAsset,
                        R.styleable.CallDetailsView_streamCallDetailsParticipantsInfoFont,
                    )
                    .style(
                        R.styleable.CallDetailsView_streamCallDetailsParticipantsInfoTextStyle,
                        Typeface.NORMAL
                    )
                    .build()

                val callStateTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallDetailsView_streamCallDetailsCallStateTextSize,
                        context.getDimension(GROUP_CALL_INFO_TEXT_SIZE)
                    )
                    .color(
                        R.styleable.CallDetailsView_streamCallDetailsCallStateTextColor,
                        context.getColorCompat(DEFAULT_TEXT_COLOR)
                    )
                    .font(
                        R.styleable.CallDetailsView_streamCallDetailsCallStateFontAsset,
                        R.styleable.CallDetailsView_streamCallDetailsCallStateFont,
                    )
                    .style(
                        R.styleable.CallDetailsView_streamCallDetailsCallStateTextStyle,
                        Typeface.BOLD
                    )
                    .build()

                val callStateTextAlpha = it.getFloat(
                    R.styleable.CallDetailsView_streamCallDetailsStateTextAlpha,
                    context.getFloatResource(io.getstream.video.android.ui.common.R.dimen.onCallStatusTextAlpha)
                )

                return CallDetailsStyle(
                    callAvatarSize = callAvatarSize,
                    singleAvatarSize = singleAvatarSize,
                    avatarSpacing = avatarSpacing,
                    participantsTextStyle = participantsTextStyle,
                    singleParticipantTextStyle =  singleParticipantTextStyle,
                    callStateTextStyle = callStateTextStyle,
                    callStateTextAlpha = callStateTextAlpha
                ).let(TransformStyle.callDetailsStyleTransformer::transform)
            }
        }
    }
}