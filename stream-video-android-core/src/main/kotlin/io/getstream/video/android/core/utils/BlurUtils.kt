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

package io.getstream.video.android.core.utils

import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.log.StreamLog

/**
 * Utility class for applying blur effects to bitmaps.
 * Uses RenderEffect for API 31+ and fallback implementation for older versions.
 *
 * This is an internal API for use within Stream Video SDK modules only.
 */
@InternalStreamVideoApi
public object BlurUtils {

    /**
     * Applies blur effect to a bitmap using the most appropriate method for the current API level.
     *
     * @param bitmap The input bitmap to blur
     * @param radius The blur radius (1-25)
     * @return The blurred bitmap
     */
    public fun blur(bitmap: Bitmap, radius: Int): Bitmap {
        val startTime = System.currentTimeMillis()
        val method = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "HardwareRenderer" else "CPU Fallback"
        
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurWithRenderEffect(bitmap, radius)
        } else {
            blurWithFallback(bitmap, radius)
        }
        
        val duration = System.currentTimeMillis() - startTime
        StreamLog.d("BlurUtils") { 
            "Blur completed using $method: ${bitmap.width}x${bitmap.height} radius=$radius in ${duration}ms" 
        }
        
        return result
    }

    /**
     * Applies blur using RenderEffect with HardwareRenderer (API 31+).
     * This is the preferred method for modern Android versions.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun blurWithRenderEffect(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val setupStart = System.currentTimeMillis()

        // Create RenderNode and apply blur effect
        val renderNode = RenderNode("blur").apply {
            setPosition(0, 0, width, height)
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    radius.toFloat(),
                    radius.toFloat(),
                    android.graphics.Shader.TileMode.CLAMP,
                ),
            )
        }

        // Draw the bitmap into the RenderNode
        val recordingCanvas = renderNode.beginRecording()
        recordingCanvas.drawBitmap(bitmap, 0f, 0f, null)
        renderNode.endRecording()

        // Create ImageReader to capture the rendered output
        val imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            1,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
        )

        // Create HardwareRenderer to render the RenderNode
        val hardwareRenderer = HardwareRenderer()
        hardwareRenderer.setSurface(imageReader.surface)
        hardwareRenderer.setContentRoot(renderNode)

        val setupTime = System.currentTimeMillis() - setupStart
        val renderStart = System.currentTimeMillis()

        // Render and wait for completion
        hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        val renderTime = System.currentTimeMillis() - renderStart
        val extractStart = System.currentTimeMillis()

        // Extract the blurred bitmap from the ImageReader
        val image = imageReader.acquireLatestImage()
        val blurredBitmap = if (image != null) {
            val hardwareBuffer = image.hardwareBuffer
            if (hardwareBuffer != null) {
                Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                    ?: bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }

        val extractTime = System.currentTimeMillis() - extractStart
        val cleanupStart = System.currentTimeMillis()

        // Cleanup
        image?.close()
        imageReader.close()
        hardwareRenderer.destroy()
        renderNode.discardDisplayList()

        val cleanupTime = System.currentTimeMillis() - cleanupStart

        StreamLog.d("BlurUtils") { 
            "HardwareRenderer breakdown: setup=${setupTime}ms, render=${renderTime}ms, extract=${extractTime}ms, cleanup=${cleanupTime}ms" 
        }

        val result = blurredBitmap.copy(Bitmap.Config.ARGB_8888, false)
        if (blurredBitmap != result && blurredBitmap.config != Bitmap.Config.HARDWARE) {
            blurredBitmap.recycle()
        }
        return result
    }

    /**
     * Fallback blur implementation for API < 31.
     * Uses a simple box blur algorithm.
     */
    private fun blurWithFallback(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixelExtractStart = System.currentTimeMillis()
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val pixelExtractTime = System.currentTimeMillis() - pixelExtractStart
        val blurStart = System.currentTimeMillis()

        val blurredPixels = boxBlur(pixels, width, height, radius)
        
        val blurTime = System.currentTimeMillis() - blurStart
        val bitmapCreateStart = System.currentTimeMillis()

        val blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        blurredBitmap.setPixels(blurredPixels, 0, width, 0, 0, width, height)
        
        val bitmapCreateTime = System.currentTimeMillis() - bitmapCreateStart

        StreamLog.d("BlurUtils") { 
            "CPU Fallback breakdown: pixelExtract=${pixelExtractTime}ms, blur=${blurTime}ms, bitmapCreate=${bitmapCreateTime}ms" 
        }

        return blurredBitmap
    }

    /**
     * Simple box blur implementation.
     */
    private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val radiusClamped = radius.coerceIn(1, 25)
        val horizontalResult = IntArray(pixels.size)

        // Horizontal pass
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                var count = 0

                for (dx in -radiusClamped..radiusClamped) {
                    val nx = (x + dx).coerceIn(0, width - 1)
                    val pixel = pixels[y * width + nx]

                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                    a += (pixel shr 24) and 0xFF
                    count++
                }

                val finalR = (r / count).coerceIn(0, 255)
                val finalG = (g / count).coerceIn(0, 255)
                val finalB = (b / count).coerceIn(0, 255)
                val finalA = (a / count).coerceIn(0, 255)

                horizontalResult[y * width + x] = (finalA shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }

        // Vertical pass
        val result = IntArray(pixels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                var count = 0

                for (dy in -radiusClamped..radiusClamped) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    val pixel = horizontalResult[ny * width + x]

                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                    a += (pixel shr 24) and 0xFF
                    count++
                }

                val finalR = (r / count).coerceIn(0, 255)
                val finalG = (g / count).coerceIn(0, 255)
                val finalB = (b / count).coerceIn(0, 255)
                val finalA = (a / count).coerceIn(0, 255)

                result[y * width + x] = (finalA shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }

        return result
    }
}
