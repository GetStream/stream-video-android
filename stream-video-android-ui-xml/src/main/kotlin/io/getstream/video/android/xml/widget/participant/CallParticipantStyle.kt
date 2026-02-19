/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.xml.widget.participant

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Px
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.font.TextStyle
import io.getstream.video.android.xml.utils.extensions.dpToPxPrecise
import io.getstream.video.android.xml.utils.extensions.getColorCompat
import io.getstream.video.android.xml.utils.extensions.getDimension
import io.getstream.video.android.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.xml.utils.extensions.getEnum
import io.getstream.video.android.xml.utils.extensions.use
import io.getstream.video.android.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [CallParticipantView].
 * Use this class together with [TransformStyle.callParticipantStyleTransformer] to change [CallParticipantView]
 * styles programmatically.
 *
 * @param labelAlignment The alignment of the microphone off/on icon and name label.
 * @param labelMargin The margin between the name label and the [CallParticipantView] borders.
 * @param activeSpeakerBorderColor The colour of the active speaking participant border.
 * @param labelTextStyle The text style for the participants name label.
 * @param labelBackgroundColor The colour of the participant name label background.
 * @param participantMicOffIcon The icon indicating when the participants microphone is off.
 * @param participantMicOffIconTint The colour of the microphone off indicator drawable.
 * @param participantAudioLevelTint The color of the audio level when the microphone is on.
 * @param elevation The elevation of the [CallParticipantView].
 * @param cornerRadius The radius of the [CallParticipantView] corners.
 */
public data class CallParticipantStyle(
    public val labelAlignment: CallParticipantLabelAlignment,
    @Px public val labelMargin: Int,
    @Px public val activeSpeakerBorderWidth: Int,
    @ColorInt public val activeSpeakerBorderColor: Int,
    public val labelTextStyle: TextStyle,
    @ColorInt public val labelBackgroundColor: Int,
    public val participantMicOffIcon: Drawable,
    @ColorInt public val participantMicOffIconTint: Int,
    @ColorInt public val participantAudioLevelTint: Int,
    @Px public val elevation: Float,
    @Px public val cornerRadius: Float,
) {

    internal companion object {
        private val DEFAULT_LABEL_MARGIN = 8.dpToPxPrecise()

        operator fun invoke(
            context: Context,
            attrs: AttributeSet?,
            styleAttrs: Int,
            styleRes: Int,
        ): CallParticipantStyle {
            val viewStyleAttr =
                if (styleAttrs == 0) R.attr.streamVideoCallParticipantViewStyle else styleAttrs
            val viewStyleRes = if (styleRes == 0) R.style.StreamVideo_CallParticipant else styleRes

            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallParticipantView,
                viewStyleAttr,
                viewStyleRes,
            ).use {
                val labelAlignment = it.getEnum(
                    R.styleable.CallParticipantView_streamVideoCallParticipantLabelAlignment,
                    CallParticipantLabelAlignment.BOTTOM_LEFT,
                )

                val labelMargin = it.getDimension(
                    R.styleable.CallParticipantView_streamVideoCallParticipantLabelMargin,
                    DEFAULT_LABEL_MARGIN,
                )

                val activeSpeakerBorderWidth = it.getDimension(
                    R.styleable.CallParticipantView_streamVideoCallParticipantActiveSpeakerBorderWidth,
                    context.getDimension(
                        RCommon.dimen.stream_video_activeSpeakerBoarderWidth,
                    ).toFloat(),
                ).toInt()

                val activeSpeakerBorderColor = it.getColor(
                    R.styleable.CallParticipantView_streamVideoCallParticipantActiveSpeakerBorderColor,
                    context.getColorCompat(RCommon.color.stream_video_info_accent),
                )

                val labelTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallParticipantView_streamVideoCallParticipantLabelTextSize,
                        context.getDimension(RCommon.dimen.stream_video_bodyTextSize),
                    )
                    .color(
                        R.styleable.CallParticipantView_streamVideoCallParticipantLabelTextColor,
                        context.getColorCompat(R.color.stream_video_white),
                    )
                    .font(
                        R.styleable.CallParticipantView_streamVideoCallParticipantLabelTextFontAssets,
                        R.styleable.CallParticipantView_streamVideoCallParticipantLabelTextFont,
                    )
                    .style(
                        R.styleable.CallParticipantView_streamVideoCallParticipantLabelTextStyle,
                        Typeface.NORMAL,
                    )
                    .build()

                val labelBackgroundColor = it.getColor(
                    R.styleable.CallParticipantView_streamVideoCallParticipantLabelBackgroundColor,
                    context.getColorCompat(R.color.stream_video_dark_gray),
                )

                val participantMicOffIcon = it.getDrawable(
                    R.styleable.CallParticipantView_streamVideoCallParticipantMicrophoneOffIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_mic_off)!!

                val participantMicOffIconTint = it.getColor(
                    R.styleable.CallParticipantView_streamVideoCallParticipantMicrophoneOffTint,
                    context.getColorCompat(RCommon.color.stream_video_error_accent),
                )

                val participantAudioLevelTint = it.getColor(
                    R.styleable.CallParticipantView_streamVideoCallParticipantAudioLevelTint,
                    context.getColorCompat(R.color.stream_video_white),
                )

                val elevation = it.getDimension(
                    R.styleable.CallParticipantView_streamVideoCallParticipantElevation,
                    0.dpToPxPrecise(),
                )

                val cornersRadius = it.getDimension(
                    R.styleable.CallParticipantView_streamVideoCallParticipantCornerRadius,
                    0.dpToPxPrecise(),
                )

                return CallParticipantStyle(
                    labelAlignment = labelAlignment,
                    labelMargin = labelMargin.toInt(),
                    activeSpeakerBorderWidth = activeSpeakerBorderWidth,
                    activeSpeakerBorderColor = activeSpeakerBorderColor,
                    labelTextStyle = labelTextStyle,
                    labelBackgroundColor = labelBackgroundColor,
                    participantMicOffIcon = participantMicOffIcon,
                    participantMicOffIconTint = participantMicOffIconTint,
                    participantAudioLevelTint = participantAudioLevelTint,
                    elevation = elevation,
                    cornerRadius = cornersRadius,
                ).let(TransformStyle.callParticipantStyleTransformer::transform)
            }
        }
    }
}
