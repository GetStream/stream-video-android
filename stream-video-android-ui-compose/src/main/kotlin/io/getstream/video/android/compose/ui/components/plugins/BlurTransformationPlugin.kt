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

package io.getstream.video.android.compose.ui.components.plugins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import com.skydoves.landscapist.plugins.ImagePlugin

/**
 * Originated from [Landscapist](https://github.com/skydoves/landscapist).
 *
 * BlurTransformationPlugin adds blur transformation effect while rendering an image.
 * An image plugin that extends [ImagePlugin.PainterPlugin] to be executed while rendering painters.
 *
 * @property radius The radius of the pixels used to blur, a value from 0 to infinite. Default is 10.
 */
@Immutable
internal data class BlurTransformationPlugin(
    val radius: Int = 10,
) : ImagePlugin.PainterPlugin {

    /**
     * Compose circular reveal painter with an [imageBitmap] to the given [painter].
     *
     * @param imageBitmap A target [ImageBitmap] to be drawn on the painter.
     * @param painter A given painter to be executed circular reveal animation.
     */
    @Composable
    override fun compose(imageBitmap: ImageBitmap, painter: Painter): Painter {
        return painter.rememberBlurPainter(
            imageBitmap = imageBitmap,
            radius = radius,
        )
    }
}
