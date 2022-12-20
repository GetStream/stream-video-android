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
public class CallDetailsStyle(
    @Px public val callAvatarSize: Int,
    @Px public val singleAvatarSize: Int,
    @Px public val avatarSpacing: Int,
    public val participantsTextStyle: TextStyle,
    public val singleParticipantTextStyle: TextStyle,
    public val callStateTextStyle: TextStyle,
    public val callStateTextAlpha: Float
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): CallDetailsStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallDetailsView,
                0,
                0
            ).use {
                return Builder(context, it).build()
            }
        }
    }

    internal class Builder(private val context: Context, private val attributes: TypedArray) {
        private val DEFAULT_AVATAR_SPACING = 20.dpToPx()

        private val DIRECT_CALL_INFO_TEXT_SIZE = RCommon.dimen.directCallUserNameTextSize
        private val GROUP_CALL_INFO_TEXT_SIZE = RCommon.dimen.groupCallUserNameTextSize

        private val DEFAULT_TEXT_COLOR = RCommon.color.stream_text_high_emphasis

        internal fun build(): CallDetailsStyle {
            val callAvatarSize = attributes.getDimensionPixelSize(
                R.styleable.CallDetailsView_streamCallAvatarSize,
                context.getDimension(RCommon.dimen.callAvatarSize)
            )

            val singleAvatarSize = attributes.getDimensionPixelSize(
                R.styleable.CallDetailsView_streamDirectCallAvatarSize,
                context.getDimension(RCommon.dimen.singleAvatarSize)
            )

            val avatarSpacing = attributes.getDimensionPixelSize(
                R.styleable.CallDetailsView_streamAvatarSpacing,
                DEFAULT_AVATAR_SPACING
            )

            val participantsTextStyle = TextStyle.Builder(attributes)
                .size(
                    R.styleable.CallDetailsView_streamParticipantsInfoTextSize,
                    context.getDimension(GROUP_CALL_INFO_TEXT_SIZE)
                )
                .color(
                    R.styleable.CallDetailsView_streamParticipantsInfoTextColor,
                    context.getColorCompat(DEFAULT_TEXT_COLOR)
                )
                .font(
                    R.styleable.CallDetailsView_streamParticipantsInfoFontAsset,
                    R.styleable.CallDetailsView_streamParticipantsInfoFont,
                )
                .style(
                    R.styleable.CallDetailsView_streamParticipantsInfoTextStyle,
                    Typeface.NORMAL
                )
                .build()

            val singleParticipantTextStyle = TextStyle.Builder(attributes)
                .size(
                    R.styleable.CallDetailsView_streamDirectCallParticipantInfoTextSize,
                    context.getDimension(DIRECT_CALL_INFO_TEXT_SIZE)
                )
                .color(
                    R.styleable.CallDetailsView_streamParticipantsInfoTextColor,
                    context.getColorCompat(DEFAULT_TEXT_COLOR)
                )
                .font(
                    R.styleable.CallDetailsView_streamParticipantsInfoFontAsset,
                    R.styleable.CallDetailsView_streamParticipantsInfoFont,
                )
                .style(
                    R.styleable.CallDetailsView_streamParticipantsInfoTextStyle,
                    Typeface.NORMAL
                )
                .build()

            val callStateTextStyle = TextStyle.Builder(attributes)
                .size(
                    R.styleable.CallDetailsView_streamCallStateTextSize,
                    context.getDimension(GROUP_CALL_INFO_TEXT_SIZE)
                )
                .color(
                    R.styleable.CallDetailsView_streamCallStateTextColor,
                    context.getColorCompat(DEFAULT_TEXT_COLOR)
                )
                .font(
                    R.styleable.CallDetailsView_streamCallStateFontAsset,
                    R.styleable.CallDetailsView_streamCallStateFont,
                )
                .style(
                    R.styleable.CallDetailsView_streamCallStateTextStyle,
                    Typeface.BOLD
                )
                .build()

            val callStateTextAlpha = attributes.getFloat(
                R.styleable.CallDetailsView_streamCallStateTextAlpha,
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