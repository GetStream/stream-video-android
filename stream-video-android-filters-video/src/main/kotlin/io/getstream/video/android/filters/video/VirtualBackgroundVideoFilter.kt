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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import io.getstream.video.android.core.call.video.BitmapVideoFilter

/**
 * Applies a virtual background (custom image) to a video call.
 *
 * @param context Context used to access resources.
 * @param backgroundImage The drawable resource ID of the custom background image.
 * @param foregroundThreshold The confidence threshold for the foreground. Pixels with a confidence value greater than or equal to this threshold are considered to be in the foreground. Value is coerced between 0 and 1, inclusive.
 */
@Keep
public class VirtualBackgroundVideoFilter(
    private val context: Context,
    @DrawableRes backgroundImage: Int,
    foregroundThreshold: Double = DEFAULT_FOREGROUND_THRESHOLD,
) : BitmapVideoFilter() {
    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
    private val segmenter = Segmentation.getClient(options)
    private lateinit var segmentationMask: SegmentationMask
    private lateinit var segmentationMatrix: Matrix

    private var foregroundThreshold: Double = foregroundThreshold.coerceIn(0.0, 1.0)
    private val foregroundBitmap by lazy {
        Bitmap.createBitmap(
            segmentationMask.width,
            segmentationMask.height,
            Bitmap.Config.ARGB_8888,
        )
    }
    private val foregroundPaint by lazy {
        Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
    }

    private val virtualBackgroundBitmap by lazy {
        convertVirtualBackgroundToBitmap(backgroundImage)
    }
    private var scaledVirtualBackgroundBitmap: Bitmap? = null
    private var scaledVirtualBackgroundBitmapCopy: Bitmap? = null

    private var latestFrameWidth: Int? = null
    private var latestFrameHeight: Int? = null

    override fun applyFilter(videoFrameBitmap: Bitmap) {
        // Apply segmentation
        val mlImage = InputImage.fromBitmap(videoFrameBitmap, 0)
        val task = segmenter.process(mlImage)
        segmentationMask = Tasks.await(task)

        // Copy the foreground segment (the person) to a new bitmap - foregroundBitmap
        copySegment(
            segment = Segment.FOREGROUND,
            source = videoFrameBitmap,
            destination = foregroundBitmap,
            segmentationMask = segmentationMask,
            confidenceThreshold = foregroundThreshold,
        )

        virtualBackgroundBitmap?.let { virtualBackgroundBitmap ->
            val videoFrameCanvas = Canvas(videoFrameBitmap)

            // Scale the virtual background bitmap to the height of the video frame, if needed
            if (scaledVirtualBackgroundBitmap == null ||
                videoFrameCanvas.width != latestFrameWidth ||
                videoFrameCanvas.height != latestFrameHeight
            ) {
                scaledVirtualBackgroundBitmap = scaleVirtualBackgroundBitmap(
                    bitmap = virtualBackgroundBitmap,
                    targetHeight = videoFrameCanvas.height,
                )
                // Make a copy of the scaled virtual background bitmap. Used when processing each frame.
                scaledVirtualBackgroundBitmapCopy = scaledVirtualBackgroundBitmap!!.copy(
                    /* config = */
                    scaledVirtualBackgroundBitmap!!.config!!,
                    /* isMutable = */
                    true,
                )

                latestFrameWidth = videoFrameBitmap.width
                latestFrameHeight = videoFrameBitmap.height

                segmentationMatrix = newSegmentationMaskMatrix(videoFrameBitmap, segmentationMask)
            }

            // Restore the virtual background after cutting-out the person in the previous frame
            val backgroundCanvas = Canvas(scaledVirtualBackgroundBitmapCopy!!)
            backgroundCanvas.drawBitmap(scaledVirtualBackgroundBitmap!!, 0f, 0f, null)

            // Cut out the person from the virtual background

            backgroundCanvas.drawBitmap(foregroundBitmap, segmentationMatrix, foregroundPaint)

            // Draw the virtual background (with the cutout) on the video frame bitmap
            videoFrameCanvas.drawBitmap(scaledVirtualBackgroundBitmapCopy!!, 0f, 0f, null)
        }
    }

    private fun convertVirtualBackgroundToBitmap(@DrawableRes backgroundImage: Int): Bitmap? =
        BitmapFactory.decodeResource(context.resources, backgroundImage, BitmapFactory.Options())

    private fun scaleVirtualBackgroundBitmap(bitmap: Bitmap, targetHeight: Int): Bitmap {
        val scale = targetHeight.toFloat() / bitmap.height
        return ensureAlpha(
            Bitmap.createScaledBitmap(
                /* src = */
                bitmap,
                /* dstWidth = */
                (bitmap.width * scale).toInt(),
                /* dstHeight = */
                targetHeight,
                /* filter = */
                true,
            ),
        )
    }

    private fun ensureAlpha(original: Bitmap): Bitmap {
        return if (original.hasAlpha()) {
            original
        } else {
            val bitmapWithAlpha = Bitmap.createBitmap(
                original.width,
                original.height,
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(bitmapWithAlpha)
            canvas.drawBitmap(original, 0f, 0f, null)
            bitmapWithAlpha
        }
    }
}

private const val DEFAULT_FOREGROUND_THRESHOLD: Double = 0.7 // 1 is max confidence that pixel is in the foreground
