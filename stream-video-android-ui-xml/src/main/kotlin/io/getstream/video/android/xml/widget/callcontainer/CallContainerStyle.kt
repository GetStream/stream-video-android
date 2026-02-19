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

package io.getstream.video.android.xml.widget.callcontainer

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.Px
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.getDimension
import io.getstream.video.android.xml.utils.extensions.use
import io.getstream.video.android.xml.widget.appbar.CallAppBarView
import io.getstream.video.android.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [CallContainerView].
 * Use this class together with [TransformStyle.callContainerStyleTransformer] to change [CallContainerView]
 * styles programmatically.
 *
 * @param appBarHeight The height of the [CallAppBarView] in portrait mode.
 * @param landscapeAppBarHeight The height of the [CallAppBarView] in landscape mode.
 */
public data class CallContainerStyle(
    @Px public val appBarHeight: Int,
    @Px public val landscapeAppBarHeight: Int,
) {
    internal companion object {

        operator fun invoke(context: Context, attrs: AttributeSet?): CallContainerStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallContainerView,
                R.attr.streamVideoCallContainerStyle,
                R.style.StreamVideo_CallContainer,
            ).use {
                val appBarHeight = it.getDimension(
                    R.styleable.CallContainerView_streamVideoCallContainerAppBarHeight,
                    context.getDimension(RCommon.dimen.stream_video_topAppbarHeight).toFloat(),
                ).toInt()

                val landscapeAppBarHeight = it.getDimension(
                    R.styleable.CallContainerView_streamVideoCallContainerAppBarLandscapeHeight,
                    context.getDimension(
                        RCommon.dimen.stream_video_landscapeTopAppBarHeight,
                    ).toFloat(),
                ).toInt()

                return CallContainerStyle(
                    appBarHeight = appBarHeight,
                    landscapeAppBarHeight = landscapeAppBarHeight,
                ).let(TransformStyle.callContainerStyleTransformer::transform)
            }
        }
    }
}
