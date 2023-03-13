/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import io.getstream.video.android.ui.common.R as RCommon

public data class CallContainerStyle(
    @Px public val appBarHeight: Int,
    @Px public val landscapeAppBarHeight: Int,
    @Px public val callControlsHeight: Int,
    @Px public val callControlsLandscapeWidth: Int,
) {
    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): CallContainerStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallContainerView,
                R.attr.streamCallContainerStyle,
                R.style.Stream_CallContainer
            ).use {

                val appBarHeight = it.getDimension(
                    R.styleable.CallContainerView_streamCallContainerAppBarHeight,
                    context.getDimension(RCommon.dimen.topAppbarHeight).toFloat()
                ).toInt()

                val landscapeAppBarHeight = it.getDimension(
                    R.styleable.CallContainerView_streamCAllContainerAppBarLandscapeHeight,
                    context.getDimension(RCommon.dimen.landscapeTopAppBarHeight).toFloat()
                ).toInt()

                val callControlsHeight = it.getDimension(
                    R.styleable.CallContainerView_streamCallContainerCallControlsHeight,
                    context.getDimension(RCommon.dimen.callControlsSheetHeight).toFloat()
                ).toInt()

                val callControlsLandscapeWidth = it.getDimension(
                    R.styleable.CallContainerView_streamCallContainerCallControlsLandscapeWidth,
                    context.getDimension(RCommon.dimen.landscapeCallControlsSheetWidth).toFloat()
                ).toInt()

                return CallContainerStyle(
                    appBarHeight = appBarHeight,
                    landscapeAppBarHeight = landscapeAppBarHeight,
                    callControlsHeight = callControlsHeight,
                    callControlsLandscapeWidth = callControlsLandscapeWidth
                )
            }
        }
    }
}
