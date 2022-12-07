/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.ui.xml.widget.avatar

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Px
import io.getstream.video.android.ui.xml.R
import io.getstream.video.android.ui.xml.font.TextStyle
import io.getstream.video.android.ui.xml.utils.extensions.dpToPx
import io.getstream.video.android.ui.xml.utils.extensions.getColorCompat
import io.getstream.video.android.ui.xml.utils.extensions.getDimension
import io.getstream.video.android.ui.xml.utils.extensions.getEnum
import io.getstream.video.android.ui.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [AvatarView].
 */
public data class AvatarStyle(
    @Px public val avatarBorderWidth: Int,
    @ColorInt public val avatarBorderColor: Int,
    public val avatarInitialsTextStyle: TextStyle,
    public val groupAvatarInitialsTextStyle: TextStyle,
    public val avatarShape: AvatarShape,
    @Px public val borderRadius: Float,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): AvatarStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.AvatarView,
                0,
                0,
            ).use {
                val avatarBorderWidth = it.getDimensionPixelSize(
                    R.styleable.AvatarView_streamXmlAvatarBorderWidth,
                    0
                )

                val avatarBorderColor = it.getColor(
                    R.styleable.AvatarView_streamXmlAvatarBorderColor,
                    context.getColorCompat(R.color.stream_xml_black)
                )

                val avatarInitialsTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.AvatarView_streamXmlAvatarTextSize,
                        context.getDimension(RCommon.dimen.title3TextSize)
                    )
                    .color(
                        R.styleable.AvatarView_streamXmlAvatarTextColor,
                        context.getColorCompat(RCommon.color.stream_text_avatar_initials)
                    )
                    .font(
                        R.styleable.AvatarView_streamXmlAvatarTextFontAssets,
                        R.styleable.AvatarView_streamXmlAvatarTextFont
                    )
                    .style(
                        R.styleable.AvatarView_streamXmlAvatarTextStyle,
                        Typeface.BOLD
                    )
                    .build()

                val groupAvatarInitialsTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.AvatarView_streamXmlGroupAvatarTextSize,
                        context.getDimension(RCommon.dimen.title3TextSize)
                    )
                    .color(
                        R.styleable.AvatarView_streamXmlGroupAvatarTextColor,
                        context.getColorCompat(R.color.stream_xml_white)
                    )
                    .font(
                        R.styleable.AvatarView_streamXmlGroupAvatarTextFontAssets,
                        R.styleable.AvatarView_streamXmlGroupAvatarTextFont
                    )
                    .style(
                        R.styleable.AvatarView_streamXmlGroupAvatarTextStyle,
                        Typeface.BOLD
                    )
                    .build()

                val avatarShape =
                    it.getEnum(R.styleable.AvatarView_streamXmlAvatarShape, AvatarShape.CIRCLE)

                val borderRadius =
                    it.getDimensionPixelSize(
                        R.styleable.AvatarView_streamXmlAvatarBorderRadius,
                        4.dpToPx()
                    ).toFloat()

                return AvatarStyle(
                    avatarBorderWidth = avatarBorderWidth,
                    avatarBorderColor = avatarBorderColor,
                    avatarInitialsTextStyle = avatarInitialsTextStyle,
                    groupAvatarInitialsTextStyle = groupAvatarInitialsTextStyle,
                    avatarShape = avatarShape,
                    borderRadius = borderRadius,
                ).let(TransformStyle.avatarStyleTransformer::transform)
            }
        }
    }
}
