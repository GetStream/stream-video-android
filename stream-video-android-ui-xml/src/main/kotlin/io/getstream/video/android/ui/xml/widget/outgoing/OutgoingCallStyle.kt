package io.getstream.video.android.ui.xml.widget.outgoing

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
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [CallDetailsView].
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
 * @param mediaButtonBackgroundAlphaEnabled The alpha to be applied when a media item is enabled (camera and mic).
 * @param mediaButtonBackgroundAlphaDisabled The alpha to be applied when a media item is disabled (camera and mic).
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
    public val mediaButtonBackgroundAlphaEnabled: Float,
    public val mediaButtonBackgroundAlphaDisabled: Float,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): OutgoingCallStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.OutgoingCallView,
                0,
                0
            ).use {
                return Builder(context, it).build()
            }
        }

        internal class Builder(private val context: Context, private val attributes: TypedArray) {

            internal fun build(): OutgoingCallStyle {

                val outgoingScreenBackground = attributes.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingScreenBackground
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_call_end)!!

                val cancelCallIcon = attributes.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCancelCallIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_call_end)!!

                val streamCancelCallIconTint = attributes.getColor(
                    R.styleable.OutgoingCallView_streamOutgoingCancelCallIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val cancelCallBackground = attributes.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingCancelCallBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_option)!!

                val cancelCallBackgroundTint = attributes.getColor(
                    R.styleable.OutgoingCallView_streamOutgoingCancelCallBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_error_accent)
                )

                val videoEnabledIcon = attributes.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingVideoEnabledIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_videocam_on)!!

                val videoDisabledIcon = attributes.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingVideoDisabledIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_videocam_off)!!

                val microphoneEnabledIcon = attributes.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingMicrophoneEnabledIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_mic_on)!!

                val microphoneDisabledIcon = attributes.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingMicrophoneDisabledIcon
                ) ?: context.getDrawableCompat(RCommon.drawable.ic_mic_off)!!

                val mediaButtonIconTint = attributes.getColor(
                    R.styleable.OutgoingCallView_streamOutgoingMediaControlIconTint,
                    context.getColorCompat(R.color.stream_black)
                )

                val mediaButtonBackground = attributes.getDrawable(
                    R.styleable.OutgoingCallView_streamOutgoingMediaButtonBackground
                ) ?: context.getDrawableCompat(R.drawable.bg_call_control_option)!!

                val mediaButtonBackgroundTint = attributes.getColor(
                    R.styleable.OutgoingCallView_streamOutgoingMediaButtonBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_app_background)
                )

                val mediaButtonBackgroundAlphaEnabled = attributes.getFloat(
                    R.styleable.OutgoingCallView_streamOutgoingMediaButtonBackgroundAlphaEnabled,
                    context.getFloatResource(RCommon.dimen.buttonToggleOnAlpha)
                )

                val mediaButtonBackgroundAlphaDisabled = attributes.getFloat(
                    R.styleable.OutgoingCallView_streamOutgoingMediaButtonBackgroundAlphaDisabled,
                    context.getFloatResource(RCommon.dimen.buttonToggleOffAlpha)
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
                    mediaButtonBackgroundAlphaEnabled = mediaButtonBackgroundAlphaEnabled,
                    mediaButtonBackgroundAlphaDisabled = mediaButtonBackgroundAlphaDisabled
                ).let(TransformStyle.outgoingCallStyleTransformer::transform)
            }
        }
    }
}