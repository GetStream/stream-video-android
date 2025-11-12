package io.getstream.video.android.core.call.video

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Uses a stack blur to aggressively obscure each frame without requiring RenderEffect/RenderScript.
 */
internal class DefaultModerationVideoFilter(
    private val blurRadius: Int = DEFAULT_RADIUS,
) : BitmapVideoFilter() {

    private val stackBlurProcessor = StackBlurProcessor()

    override fun applyFilter(videoFrameBitmap: Bitmap) {
        val radius = blurRadius.coerceIn(MIN_RADIUS, MAX_RADIUS)
        if (radius <= 0 || videoFrameBitmap.width == 0 || videoFrameBitmap.height == 0) return

        stackBlurProcessor.blur(videoFrameBitmap, radius)
    }

    private class StackBlurProcessor {
        private var pixelBuffer = IntArray(0)
        private var red = IntArray(0)
        private var green = IntArray(0)
        private var blue = IntArray(0)
        private var vMin = IntArray(0)

        fun blur(bitmap: Bitmap, radius: Int) {
            val width = bitmap.width
            val height = bitmap.height
            val wm = width - 1
            val hm = height - 1
            val size = width * height

            ensureCapacity(size, max(width, height))

            bitmap.getPixels(pixelBuffer, 0, width, 0, 0, width, height)

            val div = radius + radius + 1
            val radiusPlus1 = radius + 1
            val divsum = (div + 1) shr 1
            val divsumSquared = divsum * divsum
            val dv = IntArray(256 * divsumSquared) { it / divsumSquared }

            val stack = IntArray(div * 3)
            var stackPointer: Int
            var stackStart: Int
            var sirOffset: Int
            var rbs: Int

            var yi = 0
            var yw = 0

            for (y in 0 until height) {
                var rsum = 0
                var gsum = 0
                var bsum = 0
                var rinsum = 0
                var ginsum = 0
                var binsum = 0
                var routsum = 0
                var goutsum = 0
                var boutsum = 0

                for (i in -radius..radius) {
                    val p = pixelBuffer[yi + min(wm, max(i, 0))]
                    sirOffset = (i + radius) * 3
                    stack[sirOffset] = p shr 16 and 0xff
                    stack[sirOffset + 1] = p shr 8 and 0xff
                    stack[sirOffset + 2] = p and 0xff
                    rbs = radiusPlus1 - abs(i)
                    rsum += stack[sirOffset] * rbs
                    gsum += stack[sirOffset + 1] * rbs
                    bsum += stack[sirOffset + 2] * rbs
                    if (i > 0) {
                        rinsum += stack[sirOffset]
                        ginsum += stack[sirOffset + 1]
                        binsum += stack[sirOffset + 2]
                    } else {
                        routsum += stack[sirOffset]
                        goutsum += stack[sirOffset + 1]
                        boutsum += stack[sirOffset + 2]
                    }
                }
                stackPointer = radius

                for (x in 0 until width) {
                    red[yi] = dv[rsum]
                    green[yi] = dv[gsum]
                    blue[yi] = dv[bsum]

                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum

                    stackStart = stackPointer - radius + div
                    sirOffset = (stackStart % div) * 3

                    routsum -= stack[sirOffset]
                    goutsum -= stack[sirOffset + 1]
                    boutsum -= stack[sirOffset + 2]

                    if (y == 0) {
                        vMin[x] = min(x + radiusPlus1, wm)
                    }
                    val p = pixelBuffer[yw + vMin[x]]

                    stack[sirOffset] = p shr 16 and 0xff
                    stack[sirOffset + 1] = p shr 8 and 0xff
                    stack[sirOffset + 2] = p and 0xff

                    rinsum += stack[sirOffset]
                    ginsum += stack[sirOffset + 1]
                    binsum += stack[sirOffset + 2]

                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum

                    stackPointer = (stackPointer + 1) % div
                    sirOffset = (stackPointer) * 3

                    routsum += stack[sirOffset]
                    goutsum += stack[sirOffset + 1]
                    boutsum += stack[sirOffset + 2]

                    rinsum -= stack[sirOffset]
                    ginsum -= stack[sirOffset + 1]
                    binsum -= stack[sirOffset + 2]

                    yi++
                }
                yw += width
            }

            for (x in 0 until width) {
                var rsum = 0
                var gsum = 0
                var bsum = 0
                var rinsum = 0
                var ginsum = 0
                var binsum = 0
                var routsum = 0
                var goutsum = 0
                var boutsum = 0

                var yp = -radius * width
                for (i in -radius..radius) {
                    val yiIndex = max(0, yp) + x
                    sirOffset = (i + radius) * 3
                    stack[sirOffset] = red[yiIndex]
                    stack[sirOffset + 1] = green[yiIndex]
                    stack[sirOffset + 2] = blue[yiIndex]
                    rbs = radiusPlus1 - abs(i)
                    rsum += red[yiIndex] * rbs
                    gsum += green[yiIndex] * rbs
                    bsum += blue[yiIndex] * rbs
                    if (i > 0) {
                        rinsum += stack[sirOffset]
                        ginsum += stack[sirOffset + 1]
                        binsum += stack[sirOffset + 2]
                    } else {
                        routsum += stack[sirOffset]
                        goutsum += stack[sirOffset + 1]
                        boutsum += stack[sirOffset + 2]
                    }
                    if (i < hm) {
                        yp += width
                    }
                }
                var yiIndex = x
                stackPointer = radius

                for (y in 0 until height) {
                    val baseColor = pixelBuffer[yiIndex] and -0x1000000
                    pixelBuffer[yiIndex] = baseColor or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum

                    stackStart = stackPointer - radius + div
                    sirOffset = (stackStart % div) * 3

                    routsum -= stack[sirOffset]
                    goutsum -= stack[sirOffset + 1]
                    boutsum -= stack[sirOffset + 2]

                    if (x == 0) {
                        vMin[y] = min(y + radiusPlus1, hm) * width
                    }
                    val p = x + vMin[y]

                    stack[sirOffset] = red[p]
                    stack[sirOffset + 1] = green[p]
                    stack[sirOffset + 2] = blue[p]

                    rinsum += stack[sirOffset]
                    ginsum += stack[sirOffset + 1]
                    binsum += stack[sirOffset + 2]

                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum

                    stackPointer = (stackPointer + 1) % div
                    sirOffset = stackPointer * 3

                    routsum += stack[sirOffset]
                    goutsum += stack[sirOffset + 1]
                    boutsum += stack[sirOffset + 2]

                    rinsum -= stack[sirOffset]
                    ginsum -= stack[sirOffset + 1]
                    binsum -= stack[sirOffset + 2]

                    yiIndex += width
                }
            }

            bitmap.setPixels(pixelBuffer, 0, width, 0, 0, width, height)
        }

        private fun ensureCapacity(pixelCount: Int, minArraySize: Int) {
            if (pixelBuffer.size < pixelCount) {
                pixelBuffer = IntArray(pixelCount)
                red = IntArray(pixelCount)
                green = IntArray(pixelCount)
                blue = IntArray(pixelCount)
            }
            if (vMin.size < minArraySize) {
                vMin = IntArray(minArraySize)
            }
        }
    }

    private companion object {
        const val MIN_RADIUS: Int = 4
        const val DEFAULT_RADIUS: Int = 80
        const val MAX_RADIUS: Int = 96
    }
}
