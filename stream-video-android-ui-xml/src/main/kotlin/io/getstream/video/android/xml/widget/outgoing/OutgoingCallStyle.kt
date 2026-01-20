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

package io.getstream.video.android.xml.widget.outgoing

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.getColorCompat
import io.getstream.video.android.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.xml.utils.extensions.use
import io.getstream.video.android.xml.widget.transformer.TransformStyle
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
                R.attr.streamVideoOutgoingViewStyle,
                R.style.StreamVideo_OutgoingCall,
            ).use {
                val outgoingScreenBackground = it.getDrawable(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallScreenBackground,
                ) ?: context.getDrawableCompat(R.drawable.stream_video_bg_call)!!

                val cancelCallIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallCancelCallIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_call_end)!!

                val streamCancelCallIconTint = it.getColor(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallCancelCallIconTint,
                    context.getColorCompat(R.color.stream_video_white),
                )

                val cancelCallBackground = it.getDrawable(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallCancelCallBackground,
                ) ?: context.getDrawableCompat(R.drawable.stream_video_bg_call_option)!!

                val cancelCallBackgroundTint = it.getColor(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallCancelCallBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_video_error_accent),
                )

                val videoEnabledIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallVideoEnabledIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_videocam_on)!!

                val videoDisabledIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallVideoDisabledIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_videocam_off)!!

                val microphoneEnabledIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallMicrophoneEnabledIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_mic_on)!!

                val microphoneDisabledIcon = it.getDrawable(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallMicrophoneDisabledIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_mic_off)!!

                val mediaButtonIconTint = it.getColor(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallMediaControlIconTint,
                    context.getColorCompat(RCommon.color.stream_video_text_high_emphasis),
                )

                val mediaButtonBackground = it.getDrawable(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallMediaButtonBackground,
                ) ?: context.getDrawableCompat(R.drawable.stream_video_bg_call_control_option)!!

                val mediaButtonBackgroundTint = it.getColor(
                    R.styleable.OutgoingCallView_streamVideoOutgoingCallMediaButtonBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_video_app_background),
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
