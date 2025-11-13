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
import io.getstream.log.taggedLogger
import io.getstream.webrtc.VideoFrame
import io.github.crow_misia.libyuv.AbgrBuffer
import io.github.crow_misia.libyuv.I420Buffer
import io.github.crow_misia.libyuv.PlanePrimitive
import io.github.crow_misia.libyuv.RotateMode
import io.github.crow_misia.libyuv.RowStride

object YuvFrame {
    private val logger by taggedLogger("YuvFrame")

    private lateinit var webRtcI420Buffer: VideoFrame.I420Buffer
    private lateinit var libYuvI420Buffer: I420Buffer
    private var libYuvRotatedI420Buffer: I420Buffer? = null
    private var libYuvAbgrBuffer: AbgrBuffer? = null

    /**
     * Converts VideoFrame.Buffer YUV frame to an ARGB_8888 Bitmap. Applies stored rotation.
     * @return A new Bitmap containing the converted frame.
     */
    fun bitmapFromVideoFrame(videoFrame: VideoFrame?): Bitmap? {
        if (videoFrame == null) {
            return null
        }

        return try {
            webRtcI420Buffer = videoFrame.buffer.toI420()!!
            createLibYuvI420Buffer()
            rotateLibYuvI420Buffer(videoFrame.rotation)
            createLibYuvAbgrBuffer()
            cleanUp()
            libYuvAbgrBuffer!!.asBitmap()
        } catch (t: Throwable) {
            logger.e(t) { "Failed to convert a VideoFrame" }
            null
        }
    }

    private fun createLibYuvI420Buffer() {
        val width = webRtcI420Buffer.width
        val height = webRtcI420Buffer.height

        libYuvI420Buffer = I420Buffer.wrap(
            planeY = PlanePrimitive(RowStride(webRtcI420Buffer.strideY), webRtcI420Buffer.dataY),
            planeU = PlanePrimitive(RowStride(webRtcI420Buffer.strideU), webRtcI420Buffer.dataU),
            planeV = PlanePrimitive(RowStride(webRtcI420Buffer.strideV), webRtcI420Buffer.dataV),
            width = width,
            height = height,
        )
    }

    private fun rotateLibYuvI420Buffer(rotationDegrees: Int) {
        val width = webRtcI420Buffer.width
        val height = webRtcI420Buffer.height

        when (rotationDegrees) {
            90, -270 -> changeOrientation(width, height, RotateMode.ROTATE_90) // upside down, 90
            180, -180 -> keepOrientation(width, height, RotateMode.ROTATE_180) // right, 180
            270, -90 -> changeOrientation(width, height, RotateMode.ROTATE_270) // upright, 270
            else -> keepOrientation(width, height, RotateMode.ROTATE_0) // left, 0, default
        }
    }

    private fun changeOrientation(width: Int, height: Int, rotateMode: RotateMode) {
        libYuvRotatedI420Buffer?.close()
        libYuvRotatedI420Buffer = I420Buffer.allocate(height, width) // swapped width and height
        libYuvI420Buffer.rotate(libYuvRotatedI420Buffer!!, rotateMode)
    }

    private fun keepOrientation(width: Int, height: Int, rotateMode: RotateMode) {
        if (width != libYuvRotatedI420Buffer?.width || height != libYuvRotatedI420Buffer?.height) {
            libYuvRotatedI420Buffer?.close()
            libYuvRotatedI420Buffer = I420Buffer.allocate(width, height)
        }
        libYuvI420Buffer.rotate(libYuvRotatedI420Buffer!!, rotateMode)
    }

    private fun createLibYuvAbgrBuffer() {
        val width = libYuvRotatedI420Buffer!!.width
        val height = libYuvRotatedI420Buffer!!.height

        if (width != libYuvAbgrBuffer?.width || height != libYuvAbgrBuffer?.height) {
            libYuvAbgrBuffer?.close()
            libYuvAbgrBuffer = AbgrBuffer.allocate(width, height)
        }
        libYuvRotatedI420Buffer!!.convertTo(libYuvAbgrBuffer!!)
    }

    private fun cleanUp() {
        libYuvI420Buffer.close()
        webRtcI420Buffer.release()
        // Rest of buffers are closed in the methods above
    }
}
