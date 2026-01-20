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

package io.getstream.video.android.xml.widget.appbar

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Px
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.font.TextStyle
import io.getstream.video.android.xml.utils.extensions.getColorCompat
import io.getstream.video.android.xml.utils.extensions.getDimension
import io.getstream.video.android.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.xml.utils.extensions.use
import io.getstream.video.android.xml.widget.appbar.internal.DefaultCallAppBarCenterContent
import io.getstream.video.android.xml.widget.appbar.internal.DefaultCallAppBarLeadingContent
import io.getstream.video.android.xml.widget.appbar.internal.DefaultCallAppBarTrailingContent
import io.getstream.video.android.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [CallAppBarView].
 * Use this class together with [TransformStyle.callAppBarStyleTransformer] to change [CallAppBarStyle] styles
 * programmatically.
 *
 * @param backgroundColour The background color of the app bar.
 * @param appBarPadding The padding around the app bar content.
 * @param leadingContentIcon The icon shown in the [DefaultCallAppBarLeadingContent].
 * @param leadingContentIconTint The icon color in the [DefaultCallAppBarLeadingContent] in portrait mode.
 * @param leadingContentIconTintLandscape The icon color in the [DefaultCallAppBarLeadingContent] in landscape mode.
 * @param leadingContentMarginStart The margin between leading content and the start of the toolbar.
 * @param leadingContentMarginEnd The margin between leading and center content.
 * @param centerContentTextStyle The text style of the [DefaultCallAppBarCenterContent] in portrait mode.
 * @param centerContentTextStyleLandscape The text style of the [DefaultCallAppBarCenterContent] in landscape mode.
 * @param centerContentMarginStart The margin between center and leading content.
 * @param centerContentMarginEnd The margin between center and trailing content.
 * @param trailingContentIcon The icon shown in the [DefaultCallAppBarTrailingContent].
 * @param trailingContentIconTint The icon color in the [DefaultCallAppBarTrailingContent] in portrait mode.
 * @param trailingContentIconTintLandscape The text style of the [DefaultCallAppBarTrailingContent] in landscape mode.
 * @param trailingContentMarginStart The margin between trailing and center content.
 * @param trailingContentMarginEnd The margin between leading trailing content and the end of the toolbar.
 */
public data class CallAppBarStyle(
    @ColorInt public val backgroundColour: Int,
    @ColorInt public val backgroundColourLandscape: Int,
    @Px public val appBarPadding: Int,
    public val leadingContentIcon: Drawable,
    @ColorInt public val leadingContentIconTint: Int,
    @ColorInt public val leadingContentIconTintLandscape: Int,
    @Px public val leadingContentMarginStart: Int,
    @Px public val leadingContentMarginEnd: Int,
    public val centerContentTextStyle: TextStyle,
    public val centerContentTextStyleLandscape: TextStyle,
    @Px public val centerContentMarginStart: Int,
    @Px public val centerContentMarginEnd: Int,
    public val trailingContentIcon: Drawable,
    @ColorInt public val trailingContentIconTint: Int,
    @ColorInt public val trailingContentIconTintLandscape: Int,
    @Px public val trailingContentMarginStart: Int,
    @Px public val trailingContentMarginEnd: Int,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): CallAppBarStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallAppBar,
                R.attr.streamVideoCallAppBarStyle,
                R.style.StreamVideo_CallAppBar,
            ).use {
                val backgroundColour = it.getColor(
                    R.styleable.CallAppBar_streamVideoCallAppBarBackgroundColour,
                    context.getColorCompat(RCommon.color.stream_video_bars_background),
                )

                val backgroundColourLandscape = it.getColor(
                    R.styleable.CallAppBar_streamVideoCallAppBarBackgroundColourLandscape,
                    context.getColorCompat(RCommon.color.stream_video_overlay_regular),
                )

                val appBarPadding = it.getDimension(
                    R.styleable.CallAppBar_streamVideoCallAppBarPadding,
                    context.getDimension(RCommon.dimen.stream_video_callAppBarPadding).toFloat(),
                ).toInt()

                val leadingContentIcon = it.getDrawable(
                    R.styleable.CallAppBar_streamVideoCallAppBarLeadingContentIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_arrow_back)!!

                val leadingContentIconTint = it.getColor(
                    R.styleable.CallAppBar_streamVideoCallAppBarLeadingContentIconTint,
                    context.getColorCompat(RCommon.color.stream_video_text_high_emphasis),
                )

                val leadingContentIconTintLandscape = it.getColor(
                    R.styleable.CallAppBar_streamVideoCallAppBarLeadingContentIconTintLandscape,
                    context.getColorCompat(R.color.stream_video_white),
                )

                val leadingContentMarginStart = it.getDimension(
                    R.styleable.CallAppBar_streamVideoCallAppBarLeadingContentMarginStart,
                    context.getDimension(
                        RCommon.dimen.stream_video_callAppBarLeadingContentSpacingStart,
                    ).toFloat(),
                ).toInt()

                val leadingContentMarginEnd = it.getDimension(
                    R.styleable.CallAppBar_streamVideoCallAppBarLeadingContentMarginEnd,
                    context.getDimension(
                        RCommon.dimen.stream_video_callAppBarLeadingContentSpacingEnd,
                    ).toFloat(),
                ).toInt()

                val centerContentTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentTextSize,
                        context.getDimension(RCommon.dimen.stream_video_topAppbarTextSize),
                    )
                    .color(
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentTextColor,
                        context.getColorCompat(RCommon.color.stream_video_text_high_emphasis),
                    )
                    .font(
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentFontAsset,
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentFont,
                    )
                    .style(
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentTextStyle,
                        Typeface.NORMAL,
                    )
                    .build()

                val centerContentTextStyleLandscape = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentTextSizeLandscape,
                        context.getDimension(RCommon.dimen.stream_video_topAppbarTextSize),
                    )
                    .color(
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentTextColorLandscape,
                        context.getColorCompat(R.color.stream_video_white),
                    )
                    .font(
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentFontAssetLandscape,
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentFont,
                    )
                    .style(
                        R.styleable.CallAppBar_streamVideoCallAppBarCenterContentTextStyleLandscape,
                        Typeface.NORMAL,
                    )
                    .build()

                val centerContentMarginStart = it.getDimension(
                    R.styleable.CallAppBar_streamVideoCallAppBarCenterContentMarginStart,
                    context.getDimension(
                        RCommon.dimen.stream_video_callAppBarCenterContentSpacingStart,
                    ).toFloat(),
                ).toInt()

                val centerContentMarginEnd = it.getDimension(
                    R.styleable.CallAppBar_streamVideoCallAppBarCenterContentMarginEnd,
                    context.getDimension(
                        RCommon.dimen.stream_video_callAppBarCenterContentSpacingEnd,
                    ).toFloat(),
                ).toInt()

                val trailingContentIcon = it.getDrawable(
                    R.styleable.CallAppBar_streamVideoCallAppBarTrailingContentIcon,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_participants)!!

                val trailingContentIconTint = it.getColor(
                    R.styleable.CallAppBar_streamVideoCallAppBarTrailingContentIconTint,
                    context.getColorCompat(RCommon.color.stream_video_text_high_emphasis),
                )

                val trailingContentIconTintLandscape = it.getColor(
                    R.styleable.CallAppBar_streamVideoCallAppBarTrailingContentIconTintLandscape,
                    context.getColorCompat(R.color.stream_video_white),
                )

                val trailingContentMarginStart = it.getDimension(
                    R.styleable.CallAppBar_streamVideoCallAppBarTrailingContentMarginStart,
                    context.getDimension(
                        RCommon.dimen.stream_video_callAppBarTrailingContentSpacingStart,
                    ).toFloat(),
                ).toInt()

                val trailingContentMarginEnd = it.getDimension(
                    R.styleable.CallAppBar_streamVideoCallAppBarTrailingContentMarginEnd,
                    context.getDimension(
                        RCommon.dimen.stream_video_callAppBarTrailingContentSpacingEnd,
                    ).toFloat(),
                ).toInt()

                return CallAppBarStyle(
                    backgroundColour = backgroundColour,
                    backgroundColourLandscape = backgroundColourLandscape,
                    appBarPadding = appBarPadding,
                    leadingContentIcon = leadingContentIcon,
                    leadingContentIconTint = leadingContentIconTint,
                    leadingContentIconTintLandscape = leadingContentIconTintLandscape,
                    leadingContentMarginStart = leadingContentMarginStart,
                    leadingContentMarginEnd = leadingContentMarginEnd,
                    centerContentTextStyle = centerContentTextStyle,
                    centerContentTextStyleLandscape = centerContentTextStyleLandscape,
                    centerContentMarginStart = centerContentMarginStart,
                    centerContentMarginEnd = centerContentMarginEnd,
                    trailingContentIcon = trailingContentIcon,
                    trailingContentIconTint = trailingContentIconTint,
                    trailingContentIconTintLandscape = trailingContentIconTintLandscape,
                    trailingContentMarginStart = trailingContentMarginStart,
                    trailingContentMarginEnd = trailingContentMarginEnd,
                ).let(TransformStyle.callAppBarStyleTransformer::transform)
            }
        }
    }
}
