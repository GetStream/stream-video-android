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
import io.github.crow_misia.libyuv.AbgrBuffer
import io.github.crow_misia.libyuv.I420Buffer
import io.github.crow_misia.libyuv.PlanePrimitive
import io.github.crow_misia.libyuv.RotateMode
import io.github.crow_misia.libyuv.RowStride
import org.webrtc.VideoFrame

// TODO: should be an internal class instead of object
object YuvFrame {

    private val logger by taggedLogger("YuvFrame")

    /**
     * Converts VideoFrame.Buffer YUV frame to an ARGB_8888 Bitmap.
     * Applies rotation and returns a newly created Bitmap.
     */
    fun bitmapFromVideoFrame(videoFrame: VideoFrame?): Bitmap? {
        if (videoFrame == null) return null

        val i420Buffer = videoFrame.buffer.toI420() ?: return null

        try {
            // Wrap WebRTC I420 planes (borrowed, read-only)
            val libYuvI420 = I420Buffer.wrap(
                planeY = PlanePrimitive(RowStride(i420Buffer.strideY), i420Buffer.dataY),
                planeU = PlanePrimitive(RowStride(i420Buffer.strideU), i420Buffer.dataU),
                planeV = PlanePrimitive(RowStride(i420Buffer.strideV), i420Buffer.dataV),
                width = i420Buffer.width,
                height = i420Buffer.height,
            )

            // Rotate into a new owned buffer
            val rotatedI420 = rotateI420(
                source = libYuvI420,
                rotationDegrees = videoFrame.rotation,
            )

            // Convert to ABGR (owned buffer)
            val abgrBuffer = AbgrBuffer.allocate(
                rotatedI420.width,
                rotatedI420.height,
            )

            rotatedI420.convertTo(abgrBuffer)

            // Materialize Bitmap
            val bmp = abgrBuffer.asBitmap()
            rotatedI420.close()
            abgrBuffer.close()
            i420Buffer.release()
            return bmp
        } catch (t: Throwable) {
            logger.e(t) { "Failed to convert VideoFrame to Bitmap" }
            return null
        }
    }

    private fun rotateI420(
        source: I420Buffer,
        rotationDegrees: Int,
    ): I420Buffer {
        val rotateMode = when (rotationDegrees) {
            90, -270 -> RotateMode.ROTATE_90
            180, -180 -> RotateMode.ROTATE_180
            270, -90 -> RotateMode.ROTATE_270
            else -> RotateMode.ROTATE_0
        }

        val targetWidth =
            if (rotateMode == RotateMode.ROTATE_90 || rotateMode == RotateMode.ROTATE_270) {
                source.height
            } else {
                source.width
            }

        val targetHeight =
            if (rotateMode == RotateMode.ROTATE_90 || rotateMode == RotateMode.ROTATE_270) {
                source.width
            } else {
                source.height
            }

        val rotated = I420Buffer.allocate(targetWidth, targetHeight)
        source.rotate(rotated, rotateMode)
        return rotated
    }
}
