package io.getstream.video.android.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions

class BlurredBackgroundVideoFilter {
    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
    private val segmenter = Segmentation.getClient(options)

    fun applyFilter(bitmap: Bitmap) {
        // Send the bitmap into ML Kit for processing
        val mlImage = InputImage.fromBitmap(bitmap, 0)
        val task = segmenter.process(mlImage)
        // Wait for result synchronously on same thread
        val mask = Tasks.await(task)

        val isRawSizeMaskEnabled =  mask.width != bitmap.width || mask.height != bitmap.height
        val scaleX = bitmap.width * 1f / mask.width
        val scaleY = bitmap.height * 1f / mask.height

        // Create a bitmap mask to cover the background
        val maskBitmap = Bitmap.createBitmap(
            maskColorsFromByteBuffer(mask), mask.width, mask.height, Bitmap.Config.ARGB_8888
        )
        // Create a canvas from the frame bitmap
        val canvas = Canvas(bitmap)
        val matrix = Matrix()
        if (isRawSizeMaskEnabled) {
            matrix.preScale(scaleX, scaleY)
        }
        // And now draw the bitmap mask onto the original bitmap
        canvas.drawBitmap(maskBitmap, matrix, null)

        maskBitmap.recycle()
    }

    private fun maskColorsFromByteBuffer(mask: SegmentationMask): IntArray {
        val colors = IntArray(mask.width * mask.height)
        for (i in 0 until mask.width * mask.height) {
            val backgroundLikelihood = 1 - mask.buffer.float
            if (backgroundLikelihood > 0.9) {
                colors[i] = Color.argb(128, 0, 0, 255)
            } else if (backgroundLikelihood > 0.2) {
                val alpha = (182.9 * backgroundLikelihood - 36.6 + 0.5).toInt()
                colors[i] = Color.argb(alpha, 0, 0, 255)
            }
        }
        return colors
    }
}