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
import android.util.AttributeSet
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.getResourceId
import io.getstream.video.android.xml.widget.transformer.TransformStyle

/**
 * Style for [PictureInPictureView].
 * Use this class together with [TransformStyle.pictureInPictureStyleTransformer] to change [PictureInPictureView]
 * styles programmatically.
 *
 * @param callParticipantStyle The id of the custom style for [CallParticipantView] to be applied for the primary
 * speaker.
 */
public data class PictureInPictureStyle(
    public val callParticipantStyle: Int,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): PictureInPictureStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.PictureInPictureView,
                R.attr.streamVideoPictureInPictureCallParticipantViewStyle,
                R.style.StreamVideo_PictureInPicture,
            ).use {
                val callParticipantStyle = it.getResourceId(
                    R.styleable.PictureInPictureView_streamVideoPictureInPictureCallParticipantViewStyle,
                    context.getResourceId(
                        R.style.StreamVideoTheme,
                        R.attr.streamVideoCallParticipantViewStyle,
                    ),
                )

                return PictureInPictureStyle(
                    callParticipantStyle = callParticipantStyle,
                ).let(TransformStyle.pictureInPictureStyleTransformer::transform)
            }
        }
    }
}
