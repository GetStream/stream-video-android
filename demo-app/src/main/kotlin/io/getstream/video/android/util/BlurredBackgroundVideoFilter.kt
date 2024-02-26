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

package io.getstream.video.android.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import com.google.android.gms.tasks.Tasks
import com.google.android.renderscript.Toolkit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions

/**
 * Applies a blur effect to the background of a video frame.
 *
 * Note that this filter is still in beta and may not work as expected. To tweak it, see constants at bottom of file.
 *
 * To do:
 * - For better performance research the [Android built-in accelerated image operations](https://developer.android.com/guide/topics/renderscript/migrate#image_blur_on_android_12_rendered_into_a_bitmap).
 * - Determine what is available for which Android version (Toolkit library vs built-in operations).
 */
class BlurredBackgroundVideoFilter {
    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
    private val segmenter = Segmentation.getClient(options)

    private lateinit var segmentationMask: SegmentationMask
    private val onlyBackgroundBitmap by lazy {
        Bitmap.createBitmap(
            segmentationMask.width,
            segmentationMask.height,
            Bitmap.Config.ARGB_8888,
        )
    }

    fun applyFilter(bitmap: Bitmap) {
        val mlImage = InputImage.fromBitmap(bitmap, 0)
        val task = segmenter.process(mlImage)
        segmentationMask = Tasks.await(task)

        copySegment(
            segment = Segment.BACKGROUND,
            source = bitmap,
            destination = onlyBackgroundBitmap,
            segmentationMask = segmentationMask,
        )

        val blurredBackgroundBitmap = Toolkit.blur(onlyBackgroundBitmap, BLUR_RADIUS.toInt())
        val canvas = Canvas(bitmap)
        val matrix = newMatrix(bitmap, segmentationMask)

        canvas.drawBitmap(blurredBackgroundBitmap, matrix, null)
    }

    private fun copySegment(
        segment: Segment,
        source: Bitmap,
        destination: Bitmap,
        segmentationMask: SegmentationMask,
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

                if (((segment == Segment.BACKGROUND) && confidence.isBackground()) ||
                    ((segment == Segment.FOREGROUND) && !confidence.isBackground())
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

    private enum class Segment {
        FOREGROUND, BACKGROUND
    }

    private fun getScalingFactors(widths: Pair<Int, Int>, heights: Pair<Int, Int>) =
        Pair(widths.first.toFloat() / widths.second, heights.first.toFloat() / heights.second)

    private fun newMatrix(bitmap: Bitmap, mask: SegmentationMask): Matrix {
        val isRawSizeMaskEnabled = mask.width != bitmap.width || mask.height != bitmap.height
        return if (!isRawSizeMaskEnabled) {
            Matrix()
        } else {
            val scale =
                getScalingFactors(Pair(bitmap.width, mask.width), Pair(bitmap.height, mask.height))
            Matrix().apply { preScale(scale.first, scale.second) }
        }
    }
}

private fun Float.isBackground() = this <= BACKGROUND_UPPER_CONFIDENCE

private const val BACKGROUND_UPPER_CONFIDENCE = 0.999 // 1 is max confidence that pixel is in the foreground
private const val BLUR_RADIUS = 10f // Set the radius of the Blur. Supported range 0 < radius <= 25
