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

package io.getstream.video.android.xml.widget.screenshare

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
 * Style for [ScreenShareView].
 * Use this class together with [TransformStyle.screenShareStyleTransformer] to change [ScreenShareView]
 * styles programmatically.
 *
 * @param fullscreenIcon Control icon to indicate fullscreen request.
 * @param fullscreenExitIcon Control icon to indicate fullscreen exit request.
 * @param portraitIcon Control icon to indicate portrait mode request.
 * @param landscapeIcon Control icon to indicate landscape mode request.
 * @param controlButtonBackgroundTint Background color of control buttons.
 * @param controlButtonIconTint Controls icon color.
 */
public data class ScreenShareStyle(
    public val fullscreenIcon: Drawable,
    public val fullscreenExitIcon: Drawable,
    public val portraitIcon: Drawable,
    public val landscapeIcon: Drawable,
    @ColorInt public val controlButtonBackgroundTint: Int,
    @ColorInt public val controlButtonIconTint: Int,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): ScreenShareStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.ScreenShareView,
                R.attr.streamVideoScreenShareStyle,
                R.style.StreamVideo_ScreenShare,
            ).use {
                val fullscreenIcon =
                    it.getDrawable(R.styleable.ScreenShareView_streamVideoScreenShareFullscreenIcon)
                        ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_fullscreen)!!

                val fullscreenExitIcon =
                    it.getDrawable(R.styleable.ScreenShareView_streamVideoScreenShareFullscreenExitIcon)
                        ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_fullscreen_exit)!!

                val portraitIcon =
                    it.getDrawable(R.styleable.ScreenShareView_streamVideoScreenSharePortraitIcon)
                        ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_portrait_mode)!!

                val landscapeIcon =
                    it.getDrawable(R.styleable.ScreenShareView_streamVideoScreenShareLandscapeIcon)
                        ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_landscape_mode)!!

                val controlButtonBackgroundTint = it.getColor(
                    R.styleable.ScreenShareView_streamVideoScreenShareControlsBackgroundTint,
                    context.getColorCompat(RCommon.color.stream_video_bars_background),
                )

                val controlButtonIconTint = it.getColor(
                    R.styleable.ScreenShareView_streamVideoScreenShareControlsIconTint,
                    context.getColorCompat(R.color.stream_video_black),
                )

                return ScreenShareStyle(
                    fullscreenIcon = fullscreenIcon,
                    fullscreenExitIcon = fullscreenExitIcon,
                    portraitIcon = portraitIcon,
                    landscapeIcon = landscapeIcon,
                    controlButtonBackgroundTint = controlButtonBackgroundTint,
                    controlButtonIconTint = controlButtonIconTint,
                ).let(TransformStyle.screenShareStyleTransformer::transform)
            }
        }
    }
}
