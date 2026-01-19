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

package io.getstream.video.android.xml.widget.incoming

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
                R.attr.streamVideoIncomingViewStyle,
                R.style.StreamVideo_IncomingCall,
            ).use {
                val callBackground = it.getDrawable(
                    R.styleable.IncomingCallView_streamVideoIncomingCallScreenBackground,
                ) ?: context.getDrawableCompat(R.drawable.stream_video_bg_call)!!

                val acceptCallIcon = it.getDrawable(
                    R.styleable.IncomingCallView_streamVideoIncomingCallAcceptCallIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_call)!!

                val acceptCallIconTint = it.getColor(
                    R.styleable.IncomingCallView_streamVideoIncomingCallAcceptCallIconTint,
                    context.getColorCompat(R.color.stream_video_white),
                )

                val acceptCallBackground = it.getDrawable(
                    R.styleable.IncomingCallView_streamVideoIncomingCallAcceptCallBackground,
                ) ?: context.getDrawableCompat(R.drawable.stream_video_bg_call_option)!!

                val acceptCallBackgroundTint = it.getColor(
                    R.styleable.IncomingCallView_streamVideoIncomingCallAcceptCallBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_video_info_accent),
                )

                val declineCallIcon = it.getDrawable(
                    R.styleable.IncomingCallView_streamVideoIncomingCallDeclineCallIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_call_end)!!

                val declineCallIconTint = it.getColor(
                    R.styleable.IncomingCallView_streamVideoIncomingCallDeclineCallIconTint,
                    context.getColorCompat(R.color.stream_video_white),
                )

                val declineCallBackground = it.getDrawable(
                    R.styleable.IncomingCallView_streamVideoIncomingCallDeclineCallBackground,
                ) ?: context.getDrawableCompat(R.drawable.stream_video_bg_call_option)!!

                val declineCallBackgroundTint = it.getColor(
                    R.styleable.IncomingCallView_streamVideoIncomingCallDeclineCallBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_video_error_accent),
                )

                val videoEnabledIcon = it.getDrawable(
                    R.styleable.IncomingCallView_streamVideoIncomingCallVideoEnabledIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_videocam_on)!!

                val videoDisabledIcon = it.getDrawable(
                    R.styleable.IncomingCallView_streamVideoIncomingCallVideoDisabledIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_videocam_off)!!

                val mediaControlIconTint = it.getColor(
                    R.styleable.IncomingCallView_streamVideoIncomingCallVideoIconTint,
                    context.getColorCompat(RCommon.color.stream_video_text_high_emphasis),
                )

                val mediaControlBackground = it.getDrawable(
                    R.styleable.IncomingCallView_streamVideoIncomingCallVideoBackground,
                ) ?: context.getDrawableCompat(R.drawable.stream_video_bg_call_control_option)!!

                val mediaControlBackgroundTint = it.getColor(
                    R.styleable.IncomingCallView_streamVideoIncomingCallVideoBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_video_app_background),
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
