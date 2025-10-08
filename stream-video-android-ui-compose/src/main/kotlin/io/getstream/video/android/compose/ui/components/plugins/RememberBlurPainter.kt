/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter

/**
 * Originated from [Landscapist](https://github.com/skydoves/landscapist).
 *
 * This is an extension of the [Painter] for giving blur transformation effect to the given [imageBitmap].
 *
 * @param imageBitmap an image bitmap for loading the content.
 * @property radius The radius of the pixels used to blur, a value from 1 to 25. Default is 10.
 */
@Composable
internal fun Painter.rememberBlurPainter(
    imageBitmap: ImageBitmap,
    radius: Int,
): Painter {
    var androidBitmap = imageBitmap.asAndroidBitmap()

    if (!(
            androidBitmap.config == Bitmap.Config.ARGB_8888 ||
                androidBitmap.config == Bitmap.Config.ALPHA_8
            )
    ) {
        androidBitmap = androidBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    val blurredBitmap = remember(imageBitmap, radius) {
        BlurUtils.blur(androidBitmap, radius)
    }
    return remember(this) {
        TransformationPainter(
            imageBitmap = blurredBitmap.asImageBitmap(),
            painter = this,
        )
    }
}
