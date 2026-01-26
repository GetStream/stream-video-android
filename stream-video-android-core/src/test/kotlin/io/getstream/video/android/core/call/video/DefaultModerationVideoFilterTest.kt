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

package io.getstream.video.android.core.call.video

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultModerationVideoFilterTest {

    @Test
    fun `blur filter significantly changes pixel data`() {
        val bitmap = createPatternBitmap(width = 64, height = 64)
        val before = bitmap.copy(bitmap.config!!, true)

        DefaultModerationVideoFilter(blurRadius = 48).applyFilter(bitmap)

        val changedPixels = countPixelDifferences(before, bitmap)
        assertThat(changedPixels).isGreaterThan(64 * 64 / 2)
    }

    @Test
    fun `blur filter keeps single pixel intact`() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val color = 0xff336699.toInt()
        bitmap.setPixel(0, 0, color)

        DefaultModerationVideoFilter(blurRadius = 48).applyFilter(bitmap)

        assertThat(bitmap.getPixel(0, 0)).isEqualTo(color)
    }

    private fun createPatternBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 4) and 0xff
                val g = (y * 4) and 0xff
                val b = ((x + y) * 2) and 0xff
                val color = (0xff shl 24) or (r shl 16) or (g shl 8) or b
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    private fun countPixelDifferences(before: Bitmap, after: Bitmap): Int {
        require(before.width == after.width)
        require(before.height == after.height)
        var changed = 0
        for (y in 0 until before.height) {
            for (x in 0 until before.width) {
                if (before.getPixel(x, y) != after.getPixel(x, y)) {
                    changed++
                }
            }
        }
        return changed
    }
}
