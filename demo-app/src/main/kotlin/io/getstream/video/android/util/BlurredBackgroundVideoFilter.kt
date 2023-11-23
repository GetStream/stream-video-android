package io.getstream.video.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.google.android.gms.tasks.Tasks
import com.google.android.renderscript.Toolkit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions


class BlurredBackgroundVideoFilter(private val context: Context) {
    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
    private val segmenter = Segmentation.getClient(options)

    fun applyFilter(bitmap: Bitmap) {
        val mlImage = InputImage.fromBitmap(bitmap, 0)
        val task = segmenter.process(mlImage)
        val mask: SegmentationMask = Tasks.await(task)

        val onlyBackgroundBitmap = Bitmap.createBitmap(mask.width, mask.height, Bitmap.Config.ARGB_8888)
        val scaleBetweenBitmapAndMask = getScalingFactors(
            widths = Pair(bitmap.width, mask.width),
            heights = Pair(bitmap.height, mask.height)
        )
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                if (mask.buffer.float.isBackground()) {
                    val scaledX = (x * scaleBetweenBitmapAndMask.first).toInt()
                    val scaledY = (y * scaleBetweenBitmapAndMask.second).toInt()
                    onlyBackgroundBitmap.setPixel(x, y, bitmap.getPixel(scaledX, scaledY))
                }
            }
        }

        val blurredBackgroundBitmap = Toolkit.blur(onlyBackgroundBitmap, BLUR_RADIUS.toInt())

        // Create a canvas from the frame bitmap
        val canvas = Canvas(bitmap)
        val matrix = newMatrix(bitmap, mask)
        // Draw the mask onto the original bitmap
        canvas.drawBitmap(blurredBackgroundBitmap, matrix, null)

        // Clear references to pixel data and allow it to be garbage collected
//        onlyBackgroundBitmap.recycle()
//        blurredBackgroundBitmap.recycle()
//        matrix.reset()
    }

    private fun getScalingFactors(widths: Pair<Int, Int>, heights: Pair<Int, Int>) =
        Pair(widths.first.toFloat() / widths.second, heights.first.toFloat() / heights.second)

    private fun newMatrix(bitmap: Bitmap, mask: SegmentationMask): Matrix {
        val isRawSizeMaskEnabled = mask.width != bitmap.width || mask.height != bitmap.height
        return if (!isRawSizeMaskEnabled) {
            Matrix()
        } else {
            val scale = getScalingFactors(Pair(bitmap.width, mask.width), Pair(bitmap.height, mask.height))
            Matrix().apply { preScale(scale.first, scale.second) }
        }
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



    fun _applyFilter(bitmap: Bitmap) {
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

    private fun blur(bitmap: Bitmap): Bitmap {
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        val outputBitmap = Bitmap.createBitmap(bitmap)

        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blur.setInput(input)
        blur.setRadius(BLUR_RADIUS)
        blur.forEach(output)
        output.copyTo(outputBitmap)

        return outputBitmap
    }
}

private fun Float.isBackground() = this < BACKGROUND_UPPER_CONFIDENCE

private const val BACKGROUND_UPPER_CONFIDENCE = 0.4f // 1 is max confidence that pixel is in the foreground
private const val BLUR_RADIUS = 25f // Set the radius of the Blur. Supported range 0 < radius <= 25