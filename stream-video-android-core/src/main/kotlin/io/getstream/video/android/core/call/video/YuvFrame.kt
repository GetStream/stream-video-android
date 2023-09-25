/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import io.getstream.log.taggedLogger
import io.github.crow_misia.libyuv.AbgrBuffer
import io.github.crow_misia.libyuv.I420Buffer
import org.webrtc.JniCommon
import org.webrtc.VideoFrame
import org.webrtc.YuvHelper
import java.nio.ByteBuffer

object YuvFrame {

    private val logger by taggedLogger("YuvFrame")

    /**
     * Converts VideoFrame.Buffer YUV frame to an ARGB_8888 Bitmap. Applies stored rotation.
     * @return A new Bitmap containing the converted frame.
     */
    fun bitmapFromVideoFrame(videoFrame: VideoFrame?): Bitmap? {
        if (videoFrame == null) {
            return null
        }
        return try {
            val buffer = videoFrame.buffer
            val i420buffer = copyPlanes(buffer, buffer.width, buffer.height)
            val bitmap = getBitmap(i420buffer, buffer.width, buffer.height, videoFrame.rotation)
            i420buffer.close()
            bitmap
        } catch (t: Throwable) {
            logger.e(t) { "Failed to convert a VideoFrame" }
            null
        }
    }

    private fun copyPlanes(videoFrameBuffer: VideoFrame.Buffer, width: Int, height: Int): I420Buffer {
        val toI420 = videoFrameBuffer.toI420()!!

        val planes = arrayOf<ByteBuffer>(toI420.dataY, toI420.dataU, toI420.dataV)
        val strides = intArrayOf(toI420.strideY, toI420.strideU, toI420.strideV)

        toI420.release()

        val halfWidth = (width + 1).shr(1)
        val halfHeight = (height + 1).shr(1)

        val capacity = width * height
        val halfCapacity = (halfWidth + 1).shr(1) * height

        val planeWidths = intArrayOf(width, halfWidth, halfWidth)
        val planeHeights = intArrayOf(height, halfHeight, halfHeight)

        val byteBuffer = JniCommon.nativeAllocateByteBuffer(capacity + halfCapacity + halfCapacity)

        for (i in 0..2) {
            if (strides[i] == planeWidths[i]) {
                byteBuffer.put(planes[i])
            } else {
                val sliceLengths = planeWidths[i] * planeHeights[i]

                val limit = byteBuffer.position() + sliceLengths
                byteBuffer.limit(limit)

                val copyBuffer = byteBuffer.slice()

                YuvHelper.copyPlane(
                    planes[i],
                    strides[i],
                    copyBuffer,
                    planeWidths[i],
                    planeWidths[i],
                    planeHeights[i],
                )
                byteBuffer.position(limit)
            }
        }

        return I420Buffer.wrap(byteBuffer, width, height)
    }

    private fun getBitmap(i420buffer: I420Buffer, width: Int, height: Int, rotationDegree: Int): Bitmap {
        val newBuffer = AbgrBuffer.allocate(width, height)
        i420buffer.convertTo(newBuffer)
        i420buffer.close()

        // Construct a Bitmap based on the new pixel data
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(newBuffer.asBuffer())
        newBuffer.close()

        // If necessary, generate a rotated version of the Bitmap
        return when (rotationDegree) {
            90, -270 -> {
                val m = Matrix()
                m.postRotate(90f)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            }

            180, -180 -> {
                val m = Matrix()
                m.postRotate(180f)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            }

            270, -90 -> {
                val m = Matrix()
                m.postRotate(270f)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            }

            else -> {
                // Don't rotate, just return the Bitmap
                bitmap
            }
        }
    }
}
