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

package io.getstream.video.android.util.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import io.getstream.video.android.R
import io.getstream.video.android.core.call.video.BitmapVideoFilter

/**
 * Applies a virtual background to a video frame.
 *
 * Note that this filter is still in beta and may not work as expected. To tweak it, see constants at bottom of file.
 */
class VirtualBackgroundVideoFilter(val context: Context) : BitmapVideoFilter() {
    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
    private val segmenter = Segmentation.getClient(options)

    private lateinit var segmentationMask: SegmentationMask
    private val onlyForegroundBitmap by lazy {
        Bitmap.createBitmap(
            segmentationMask.width,
            segmentationMask.height,
            Bitmap.Config.ARGB_8888,
        )
    }

    private val virtualBackgroundBitmap by lazy(::convertVirtualBackgroundToBitmap)

    override fun filter(videoFrameBitmap: Bitmap) {
        val mlImage = InputImage.fromBitmap(videoFrameBitmap, 0)
        val task = segmenter.process(mlImage)
        segmentationMask = Tasks.await(task)

        copySegment(
            segment = Segment.FOREGROUND,
            source = videoFrameBitmap,
            destination = onlyForegroundBitmap,
            segmentationMask = segmentationMask,
        )

        val canvas = Canvas(videoFrameBitmap)

        virtualBackgroundBitmap?.let { virtualBackgroundBitmap ->
            val scaledVirtualBackgroundBitmap = scaleVirtualBackgroundBitmap(
                bitmap = virtualBackgroundBitmap,
                targetHeight = canvas.height,
            )
            val leftPosition = ((canvas.width - scaledVirtualBackgroundBitmap.width) / 2).toFloat()
            canvas.drawBitmap(scaledVirtualBackgroundBitmap, leftPosition, 0f, null)

            val matrix = newSegmentationMaskMatrix(videoFrameBitmap, segmentationMask)
            canvas.drawBitmap(onlyForegroundBitmap, matrix, null)
        }
    }

    private fun convertVirtualBackgroundToBitmap(): Bitmap? =
        BitmapFactory.decodeResource(context.resources, R.drawable.amsterdam1)

    private fun scaleVirtualBackgroundBitmap(bitmap: Bitmap, targetHeight: Int): Bitmap {
        val scale = targetHeight.toFloat() / bitmap.height
        return Bitmap.createScaledBitmap(
            /* src = */
            bitmap,
            /* dstWidth = */
            (bitmap.width * scale).toInt(),
            /* dstHeight = */
            targetHeight,
            /* filter = */
            true,
        )
    }
}
