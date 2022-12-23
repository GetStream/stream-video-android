package io.getstream.video.android.ui.xml.widget.outgoing

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.utils.extensions.getColorCompat
import io.getstream.video.android.ui.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.ui.xml.utils.extensions.use
import io.getstream.video.android.ui.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [OutgoingCallView].
 * Use this class together with [TransformStyle.outgoingCallStyleTransformer] to change [OutgoingCallView] styles
 * programmatically.
 *
 * @param outgoingScreenBackground Background to be applied when in group calls or if the other user in direct call
 * does not have an avatar.
 * @param cancelCallIcon Icon for the cancel call button.
 * @param cancelCallIconTint Color of the cancel call icon.
 * @param cancelCallBackground The background drawable of cancel button.
 * @param cancelCallBackgroundTint The background color of the cancel button.
 * @param videoIconEnabled The icon shown when camera is enabled.
 * @param videoIconDisabled The icon shown when camera is disabled.
 * @param microphoneIconEnabled The icon shown when microphone is enabled.
 * @param microphoneIconDisabled The icon shown when microphone is disabled.
 * @param mediaButtonIconTint The color of the media control icons (camera and microphone).
 * @param mediaButtonBackground The background for media controls (camera and microphone)
 * @param mediaButtonBackgroundTint The background color for media controls (camera and microphone).
 */
public data class OutgoingCallStyle(
    public val outgoingScreenBackground: Drawable,
    public val cancelCallIcon: Drawable,
    @ColorInt public val cancelCallIconTint: Int,
    public val cancelCallBackground: Drawable,
    @ColorInt public val cancelCallBackgroundTint: Int,
    public val videoIconEnabled: Drawable,
    public val videoIconDisabled: Drawable,
    public val microphoneIconEnabled: Drawable,
    public val microphoneIconDisabled: Drawable,
    @ColorInt val mediaButtonIconTint: Int,
    public val mediaButtonBackground: Drawable,
    @ColorInt public val mediaButtonBackgroundTint: Int,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): OutgoingCallStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.OutgoingCallView,
                R.attr.streamOutgoingViewStyle,
                R.style.Stream_OutgoingCall
            ).use {
                val outgoingScreenBackground = it.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCallScreenBackground
                ) ?: context.getDrawableCompat(RCommon.drawable.bg_call)!!

                val cancelCallIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCallCancelCallIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_call_end)!!

                val streamCancelCallIconTint = it.getColor(
                    R.styleable.OutgoingCallView_streamOutgoingCallCancelCallIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val cancelCallBackground = it.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCallCancelCallBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_option)!!

                val cancelCallBackgroundTint = it.getColor(
                    R.styleable.OutgoingCallView_streamOutgoingCallCancelCallBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_error_accent)
                )

                val videoEnabledIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCallVideoEnabledIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_videocam_on)!!

                val videoDisabledIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCallVideoDisabledIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_videocam_off)!!

                val microphoneEnabledIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCallMicrophoneEnabledIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_mic_on)!!

                val microphoneDisabledIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCallMicrophoneDisabledIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_mic_off)!!

                val mediaButtonIconTint = it.getColor(
                    R.styleable.OutgoingCallView_streamOutgoingCallMediaControlIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val mediaButtonBackground = it.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCallMediaButtonBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_control_option)!!

                val mediaButtonBackgroundTint = it.getColor(
                    R.styleable.OutgoingCallView_streamOutgoingCallMediaButtonBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_app_background)
                )

                return OutgoingCallStyle(
                    outgoingScreenBackground = outgoingScreenBackground,
                    cancelCallIcon = cancelCallIcon,
                    cancelCallIconTint = streamCancelCallIconTint,
                    cancelCallBackground = cancelCallBackground,
                    cancelCallBackgroundTint = cancelCallBackgroundTint,
                    videoIconEnabled = videoEnabledIcon,
                    videoIconDisabled = videoDisabledIcon,
                    microphoneIconEnabled = microphoneEnabledIcon,
                    microphoneIconDisabled = microphoneDisabledIcon,
                    mediaButtonIconTint = mediaButtonIconTint,
                    mediaButtonBackground = mediaButtonBackground,
                    mediaButtonBackgroundTint = mediaButtonBackgroundTint,
                ).let(TransformStyle.outgoingCallStyleTransformer::transform)
            }
        }
    }
}