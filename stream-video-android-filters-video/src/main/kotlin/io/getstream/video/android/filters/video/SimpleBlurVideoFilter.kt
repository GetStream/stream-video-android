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

package io.getstream.video.android.filters.video

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.Keep
import com.google.android.renderscript.Toolkit
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.call.video.BitmapVideoFilter

@Keep
public class SimpleBlurVideoFilter(
    private val blurIntensity: BlurIntensity = BlurIntensity.ULTRA,
) : BitmapVideoFilter() {

    private val logger by taggedLogger("SimpleBlurVideoFilter")

    override fun applyFilter(videoFrameBitmap: Bitmap) {
        // Blur the entire frame
        val timeBeforeBlur = System.currentTimeMillis()
        val blurredBitmap = Toolkit.blur(videoFrameBitmap, blurIntensity.radius)
        logger.d { "Time taken to blur image : ${System.currentTimeMillis() - timeBeforeBlur}" }

        // Copy the blurred result back to the original bitmap
        val canvas = Canvas(videoFrameBitmap)
        canvas.drawBitmap(blurredBitmap, 0f, 0f, null)
    }
}
