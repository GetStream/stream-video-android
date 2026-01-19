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

package io.getstream.video.android.filters.video

import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.mlkit.vision.segmentation.SegmentationMask

internal fun copySegment(
    segment: Segment,
    source: Bitmap,
    destination: Bitmap,
    segmentationMask: SegmentationMask,
    confidenceThreshold: Double,
) {
    val scaleBetweenSourceAndMask = getScalingFactors(
        widths = Pair(source.width, segmentationMask.width),
        heights = Pair(source.height, segmentationMask.height),
    )

    segmentationMask.buffer.rewind()

    val sourcePixels = IntArray(source.width * source.height)
    source.getPixels(sourcePixels, 0, source.width, 0, 0, source.width, source.height)
    val destinationPixels = IntArray(destination.width * destination.height)

    for (y in 0 until segmentationMask.height) {
        for (x in 0 until segmentationMask.width) {
            val confidence = segmentationMask.buffer.float

            if (((segment == Segment.BACKGROUND) && confidence < confidenceThreshold) ||
                ((segment == Segment.FOREGROUND) && confidence >= confidenceThreshold)
            ) {
                val scaledX = (x * scaleBetweenSourceAndMask.first).toInt()
                val scaledY = (y * scaleBetweenSourceAndMask.second).toInt()
                destinationPixels[y * destination.width + x] = sourcePixels[scaledY * source.width + scaledX]
            }
        }
    }

    destination.setPixels(
        destinationPixels,
        0,
        destination.width,
        0,
        0,
        destination.width,
        destination.height,
    )
}

internal enum class Segment {
    FOREGROUND, BACKGROUND
}

private fun getScalingFactors(widths: Pair<Int, Int>, heights: Pair<Int, Int>) =
    Pair(widths.first.toFloat() / widths.second, heights.first.toFloat() / heights.second)

internal fun newSegmentationMaskMatrix(bitmap: Bitmap, mask: SegmentationMask): Matrix {
    val isRawSizeMaskEnabled = mask.width != bitmap.width || mask.height != bitmap.height
    return if (!isRawSizeMaskEnabled) {
        Matrix()
    } else {
        val scale =
            getScalingFactors(Pair(bitmap.width, mask.width), Pair(bitmap.height, mask.height))
        Matrix().apply { preScale(scale.first, scale.second) }
    }
}
