package io.getstream.video.android.ui.xml.widget.incoming

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.getColorCompat
import io.getstream.video.android.ui.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.ui.xml.utils.extensions.use
import io.getstream.video.android.ui.xml.widget.transformer.TransformStyle

/**
 * Style for [IncomingCallView].
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
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): IncomingCallStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.IncomingCallView,
                R.attr.streamIncomingViewStyle,
                R.style.Stream_IncomingCall
            ).use {
                val callBackground = it.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingCallScreenBackground
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.bg_call)!!

                val acceptCallIcon = it.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingCallAcceptCallIcon
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.ic_call)!!

                val acceptCallIconTint = it.getColor(
                    R.styleable.IncomingCallView_streamIncomingCallAcceptCallIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val acceptCallBackground = it.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingCallAcceptCallBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_option)!!

                val acceptCallBackgroundTint = it.getColor(
                    R.styleable.IncomingCallView_streamIncomingCallAcceptCallBackgroundTint,
                    context.getColorCompat(io.getstream.video.android.ui.common.R.color.stream_info_accent)
                )

                val declineCallIcon = it.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingCallDeclineCallIcon
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.ic_call_end)!!

                val declineCallIconTint = it.getColor(
                    R.styleable.IncomingCallView_streamIncomingCallDeclineCallIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val declineCallBackground = it.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingCallDeclineCallBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_option)!!

                val declineCallBackgroundTint = it.getColor(
                    R.styleable.IncomingCallView_streamIncomingCallDeclineCallBackgroundTint,
                    context.getColorCompat(io.getstream.video.android.ui.common.R.color.stream_error_accent)
                )

                val videoEnabledIcon = it.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingCallVideoEnabledIcon
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.ic_videocam_on)!!

                val videoDisabledIcon = it.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingCallVideoDisabledIcon
                ) ?: context.getDrawableCompat(io.getstream.video.android.ui.common.R.drawable.ic_videocam_off)!!

                val mediaControlIconTint = it.getColor(
                    R.styleable.IncomingCallView_streamIncomingCallVideoIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val mediaControlBackground = it.getDrawable(
                    R.styleable.IncomingCallView_streamIncomingCallVideoBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_control_option)!!

                val mediaControlBackgroundTint = it.getColor(
                    R.styleable.IncomingCallView_streamIncomingCallVideoBackgroundTint,
                    context.getColorCompat(io.getstream.video.android.ui.common.R.color.stream_app_background)
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
                ).let(TransformStyle.incomingCallStyleTransformer::transform)
            }
        }
    }
}