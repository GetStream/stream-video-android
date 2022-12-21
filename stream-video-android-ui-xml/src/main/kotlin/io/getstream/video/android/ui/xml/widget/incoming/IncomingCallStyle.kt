package io.getstream.video.android.ui.xml.widget.incoming

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import io.getstream.video.android.common.util.getFloatResource
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.getColorCompat
import io.getstream.video.android.ui.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.ui.xml.utils.extensions.use
import io.getstream.video.android.ui.xml.widget.call.CallDetailsView
import io.getstream.video.android.ui.xml.widget.transformer.TransformStyle

/**
 * Style for [CallDetailsView].
 * Use this class together with [TransformStyle.incomingCallStyleTransformer] to change [IncomingCallView] styles
 * programmatically.
 *
 * @param incomingScreenBackground Background to be applied when in group calls or if the other user in direct call does not have
 * an avatar.
 * @param acceptCallIcon Icon for the accept call button.
 * @param acceptCallIconTint Color of the accept call icon.
 * @param acceptCallBackground The background drawable of accept button.
 * @param acceptCallBackgroundTint The background color of the accept button.
 * @param declineCallIcon Icon for the cancel call button.
 * @param declineCallIconTint Color of the cancel call icon.
 * @param declineCallBackground The background drawable of cancel button.
 * @param declineCallBackgroundTint The background color of the cancel button.
 * @param videoButtonIconTint The color of the video button icon.
 * @param videoButtonBackground The background for the video button.
 * @param videoButtonBackgroundTint The background color for the video button.
 * @param videoButtonBackgroundAlphaEnabled The alpha to be applied when the video button is enabled.
 * @param videoButtonBackgroundAlphaDisabled The alpha to be applied when the video button is disabled.
 */
public data class IncomingCallStyle(
    public val incomingScreenBackground: Drawable,
    public val acceptCallIcon: Drawable,
    @ColorInt public val acceptCallIconTint: Int,
    public val acceptCallBackground: Drawable,
    @ColorInt public val acceptCallBackgroundTint: Int,
    public val declineCallIcon: Drawable,
    @ColorInt public val declineCallIconTint: Int,
    public val declineCallBackground: Drawable,
    @ColorInt public val declineCallBackgroundTint: Int,
    public val videoButtonIconEnabled: Drawable,
    public val videoButtonIconDisabled: Drawable,
    @ColorInt val videoButtonIconTint: Int,
    public val videoButtonBackground: Drawable,
    @ColorInt public val videoButtonBackgroundTint: Int,
    public val videoButtonBackgroundAlphaEnabled: Float,
    public val videoButtonBackgroundAlphaDisabled: Float,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): IncomingCallStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.IncomingCallView,
                0,
                0
            ).use {
                return Builder(context, it).build()
            }
        }

        internal class Builder(private val context: Context, private val attributes: TypedArray) {

            internal fun build(): IncomingCallStyle {

                val callBackground = attributes.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingScreenBackground
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.bg_call)!!

                val acceptCallIcon = attributes.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingAcceptCallIcon
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.ic_call)!!

                val acceptCallIconTint = attributes.getColor(
                    R.styleable.IncomingCallView_streamIncomingAcceptCallIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val acceptCallBackground = attributes.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingAcceptCallBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_option)!!

                val acceptCallBackgroundTint = attributes.getColor(
                    R.styleable.IncomingCallView_streamIncomingAcceptCallBackgroundTint,
                    context.getColorCompat(io.getstream.video.android.ui.common.R.color.stream_info_accent)
                )

                val declineCallIcon = attributes.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingDeclineCallIcon
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.ic_call_end)!!

                val declineCallIconTint = attributes.getColor(
                    R.styleable.IncomingCallView_streamIncomingDeclineCallIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val declineCallBackground = attributes.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingDeclineCallBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_option)!!

                val declineCallBackgroundTint = attributes.getColor(
                    R.styleable.IncomingCallView_streamIncomingDeclineCallBackgroundTint,
                    context.getColorCompat(io.getstream.video.android.ui.common.R.color.stream_error_accent)
                )

                val videoEnabledIcon = attributes.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingVideoEnabledIcon
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.ic_videocam_on)!!

                val videoDisabledIcon = attributes.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingVideoDisabledIcon
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.ic_videocam_off)!!

                val mediaControlIconTint = attributes.getColor(
                    R.styleable.IncomingCallView_streamIncomingVideoIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val mediaControlBackground = attributes.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingVideoBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_control_option)!!

                val mediaControlBackgroundTint = attributes.getColor(
                    R.styleable.IncomingCallView_streamIncomingVideoBackgroundTint,
                    context.getColorCompat(io.getstream.video.android.ui.common.R.color.stream_app_background)
                )

                val mediaControlBackgroundAlphaEnabled = attributes.getFloat(
                    R.styleable.IncomingCallView_streamIncomingVideoBackgroundAlphaEnabled,
                    context.getFloatResource(io.getstream.video.android.ui.common.R.dimen.buttonToggleOnAlpha)
                )

                val mediaControlBackgroundAlphaDisabled = attributes.getFloat(
                    R.styleable.IncomingCallView_streamIncomingVideoBackgroundAlphaDisabled,
                    context.getFloatResource(io.getstream.video.android.ui.common.R.dimen.buttonToggleOffAlpha)
                )

                return IncomingCallStyle(
                    incomingScreenBackground = callBackground,
                    acceptCallIcon = acceptCallIcon,
                    acceptCallIconTint = acceptCallIconTint,
                    acceptCallBackground = acceptCallBackground,
                    acceptCallBackgroundTint = acceptCallBackgroundTint,
                    declineCallIcon = declineCallIcon,
                    declineCallIconTint = declineCallIconTint,
                    declineCallBackground = declineCallBackground,
                    declineCallBackgroundTint = declineCallBackgroundTint,
                    videoButtonIconEnabled = videoEnabledIcon,
                    videoButtonIconDisabled = videoDisabledIcon,
                    videoButtonIconTint = mediaControlIconTint,
                    videoButtonBackground = mediaControlBackground,
                    videoButtonBackgroundTint = mediaControlBackgroundTint,
                    videoButtonBackgroundAlphaEnabled = mediaControlBackgroundAlphaEnabled,
                    videoButtonBackgroundAlphaDisabled = mediaControlBackgroundAlphaDisabled
                ).let(TransformStyle.incomingCallStyleTransformer::transform)
            }
        }
    }

}