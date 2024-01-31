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

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.util.Pools

/**
 * Originated from [Landscapist](https://github.com/skydoves/landscapist).
 *
 * TransformationPainter is a [Painter] which draws the [imageBitmap] to the given [painter].
 *
 * @param imageBitmap an image bitmap for loading for the content.
 * @param painter an image painter to draw an [ImageBitmap] into the provided canvas.
 */
internal class TransformationPainter(
    private val imageBitmap: ImageBitmap,
    private val painter: Painter,
) : Painter() {

    /** return the dimension size of the [painter]'s intrinsic width and height. */
    override val intrinsicSize: Size get() = painter.intrinsicSize

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            var dx = 0f
            var dy = 0f
            val scale: Float
            val shaderMatrix = Matrix()
            val shader = ImageShader(imageBitmap, TileMode.Clamp)
            val brush = ShaderBrush(shader)
            val paint = paintPool.acquire() ?: Paint()
            paint.asFrameworkPaint().apply {
                isAntiAlias = true
                isDither = true
                isFilterBitmap = true
            }

            // cache the paint in the internal stack.
            canvas.saveLayer(size.toRect(), paint)

            val mDrawableRect = RectF(0f, 0f, size.width, size.height)
            val bitmapWidth: Int = imageBitmap.asAndroidBitmap().width
            val bitmapHeight: Int = imageBitmap.asAndroidBitmap().height

            if (bitmapWidth * mDrawableRect.height() > mDrawableRect.width() * bitmapHeight) {
                scale = mDrawableRect.height() / bitmapHeight.toFloat()
                dx = (mDrawableRect.width() - bitmapWidth * scale) * 0.5f
            } else {
                scale = mDrawableRect.width() / bitmapWidth.toFloat()
                dy = (mDrawableRect.height() - bitmapHeight * scale) * 0.5f
            }

            // resize the matrix to scale by sx and sy.
            shaderMatrix.setScale(scale, scale)

            // post translate the matrix with the specified translation.
            shaderMatrix.postTranslate(
                (dx + 0.5f) + mDrawableRect.left,
                (dy + 0.5f) + mDrawableRect.top,
            )
            // apply the scaled matrix to the shader.
            shader.setLocalMatrix(shaderMatrix)
            // draw an image bitmap as a rect.
            drawRect(brush = brush)
            // restore canvas.
            canvas.restore()
            // resets the paint and release to the pool.
            paint.asFrameworkPaint().reset()
            paintPool.release(paint)
        }
    }
}

/** paint pool which caching and reusing [Paint] instances. */
private val paintPool = Pools.SimplePool<Paint>(2)
