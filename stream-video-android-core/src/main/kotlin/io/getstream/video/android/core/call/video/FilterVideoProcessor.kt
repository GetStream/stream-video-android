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

package io.getstream.video.android.core.call.video

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLUtils
import io.getstream.log.taggedLogger
import org.webrtc.SurfaceTextureHelper
import org.webrtc.TextureBufferImpl
import org.webrtc.VideoFrame
import org.webrtc.VideoProcessor
import org.webrtc.VideoSink
import org.webrtc.YuvConverter
import kotlin.getValue
import androidx.core.graphics.get

internal class FilterVideoProcessor(
    val filter: () -> VideoFilter?,
    private val surfaceTextureHelper: () -> SurfaceTextureHelper,
) : VideoProcessor {

    private var sink: VideoSink? = null
    private val yuvConverter = YuvConverter()
    private var inputWidth = 0
    private var inputHeight = 0
    private var inputBuffer: VideoFrame.TextureBuffer? = null
    private var yuvBuffer: VideoFrame.I420Buffer? = null
    private val textures = IntArray(1)
    private var inputFrameBitmap: Bitmap? = null
    private val logger by taggedLogger("FilterVideoProcessor")
    private var frameCount = 0L
    private var lastLogTime = System.currentTimeMillis()
    private val logIntervalMs = 10_000L // Log every 10 seconds
    private var reusableBitmap: Bitmap? = null
//    internal val yuvFrame = YuvFrame()
    internal val yuvFrame = YuvFrameCopyBuffer()

    private fun logPerformanceMetrics() {
        frameCount++
        val now = System.currentTimeMillis()
        val elapsed = now - lastLogTime

        if (elapsed >= logIntervalMs) {
            val fps = (frameCount.toDouble() / elapsed) * 1000.0
            logger.i {
                "FilterVideoProcessor: ${String.format("%.1f", fps)} fps, " +
                        "Frames processed: $frameCount"
            }
            frameCount = 0
            lastLogTime = now
        }
    }

    init {
        GLES20.glGenTextures(1, textures, 0)
    }

    override fun onCapturerStarted(success: Boolean) {
    }

    override fun onCapturerStopped() {
    }

    override fun onFrameCaptured(frame: VideoFrame) {
        // check if there is a filter currently set, if not then just return the unmodified frame
        val currentFilter = filter.invoke()
        if (currentFilter == null) {
            sink?.onFrame(frame)
            return
        }
        val time1 = System.currentTimeMillis()
        if (currentFilter is RawVideoFilter) {
            val filteredFrame = currentFilter.applyFilter(frame, surfaceTextureHelper.invoke())
            sink?.onFrame(filteredFrame)
        } else if (currentFilter is BitmapVideoFilter) {
            // first prepare a Bitmap for the client
            logger.d { "[onFrameCaptured], prepare bitmap for client start at: $time1" }
            inputFrameBitmap = yuvFrame.bitmapFromVideoFrame(frame)

            val time2 = System.currentTimeMillis()
            val timeTakenTonFinishBitmapFromVideo = time2 - time1
            logger.d { "[onFrameCaptured], Time took to prepare bitmap for client finish: $timeTakenTonFinishBitmapFromVideo" }
            if (inputFrameBitmap != null && sink != null) {
                // Prepare helpers (runs only once or if the dimensions change)
                if (isLikelyBlack(inputFrameBitmap!!)) {
                    logger.w { "[onFrameCaptured], BLACK FRAME 0: Bitmap from YUV conversion is black" }
                }
                initializeGLResources(
                    inputFrameBitmap!!.width,
                    inputFrameBitmap!!.height,
                    surfaceTextureHelper.invoke(),
                )
                val time3 = System.currentTimeMillis()
                val timeTakenToInitialise = time3 - time2
                logger.d { "[onFrameCaptured], Time took to initialise: $timeTakenToInitialise" }

                if (isLikelyBlack(inputFrameBitmap!!)) {
                    logger.w { "[onFrameCaptured], BLACK FRAME 1: Bitmap from YUV conversion is black" }
                }
                // now ask client to make modifications
                currentFilter.applyFilter(inputFrameBitmap!!)

                val time4 = System.currentTimeMillis()
                val timeTakenToApplyFilter = time4 - time3
                logger.d { "[onFrameCaptured], Time took to apply filter: $timeTakenToApplyFilter" }

                // feed back the modified bitmap
                GLES20.glTexParameteri(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST,
                )
                GLES20.glTexParameteri(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_NEAREST,
                )
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, inputFrameBitmap!!, 0)

                val time5 = System.currentTimeMillis()
                val timeTakenTalkToGpu = time5 - time4
                logger.d { "[onFrameCaptured] Time took to talk to GPU: $timeTakenTalkToGpu" }

                // Convert the buffer back to YUV (VideoFrame needs YUV)
                yuvBuffer = yuvConverter.convert(inputBuffer)

                val time6 = System.currentTimeMillis()
                val timeTakenToConvertBufferToYuv = time6 - time5
                logger.d { "[onFrameCaptured] Time took to convert buffer to Yuv: $timeTakenToConvertBufferToYuv" }


                sink?.onFrame(VideoFrame(yuvBuffer, 0, frame.timestampNs))
            }
        } else {
            throw Error("Unsupported video filter type ${filter.invoke()}")
        }
        logPerformanceMetrics()
    }

    private fun getOrCreateReusableBitmap(width: Int, height: Int): Bitmap {
        val existing = reusableBitmap

        // Reuse if dimensions match and bitmap is still valid
        if (existing != null &&
            existing.width == width &&
            existing.height == height &&
            !existing.isRecycled) {
            return existing
        }

        // Recycle old bitmap if dimensions changed
        existing?.recycle()

        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        reusableBitmap = newBitmap

        logger.d { "Created new reusable bitmap: ${width}x${height}" }
        return newBitmap
    }

    override fun setSink(sink: VideoSink?) {
        this@FilterVideoProcessor.sink = sink
    }

    private fun initializeGLResources(width: Int, height: Int, textureHelper: SurfaceTextureHelper) {
        yuvBuffer?.release()

        if (this.inputWidth != width || this.inputHeight != height) {
            this.inputWidth = width
            this.inputHeight = height
            inputBuffer?.release()

            val type = VideoFrame.TextureBuffer.Type.RGB
            val matrix = Matrix()
            matrix.preScale(1.0f, -1.0f)

            this.inputBuffer = TextureBufferImpl(
                inputWidth, inputHeight, type, textures[0], matrix, textureHelper.handler,
                yuvConverter, null as Runnable?,
            )
        }
    }

    private fun isLikelyBlack(bitmap: Bitmap): Boolean {
        if (bitmap.width == 0 || bitmap.height == 0) return true

        // Sample center pixel
        val x = bitmap.width / 2
        val y = bitmap.height / 2
        val pixel = bitmap[x, y]

        // Ignore alpha, check RGB
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        return r < 5 && g < 5 && b < 5
    }

}