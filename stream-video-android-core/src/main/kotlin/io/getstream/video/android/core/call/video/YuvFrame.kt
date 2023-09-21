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
import io.github.crow_misia.libyuv.AbgrBuffer
import io.github.crow_misia.libyuv.Nv21Buffer
import org.webrtc.VideoFrame
import java.nio.ByteBuffer

// Based on http://stackoverflow.com/a/12702836
internal class YuvFrame(videoFrame: VideoFrame?) {
    var width = 0
    var height = 0

    private var nv21Buffer: ByteArray? = null
    private var rotationDegree: Int = 0

    init {
        fromVideoFrame(videoFrame)
    }

    private fun fromVideoFrame(videoFrame: VideoFrame?) {
        if (videoFrame == null) {
            return
        }
        try {
            // Copy rotation information
            // Save rotation info for now, doing actual rotation can wait until per-pixel processing.
            rotationDegree = videoFrame.rotation
            copyPlanes(videoFrame.buffer)
        } catch (t: Throwable) {
            dispose()
        }
    }

    fun dispose() {
        nv21Buffer = null
    }

    /**
     * Copy the Y, V, and U planes from the source I420Buffer.
     * Sets width and height.
     * @param videoFrameBuffer Source frame buffer.
     */
    private fun copyPlanes(videoFrameBuffer: VideoFrame.Buffer?) {
        var i420Buffer: VideoFrame.I420Buffer? = null
        if (videoFrameBuffer != null) {
            i420Buffer = videoFrameBuffer.toI420()
        }
        if (i420Buffer == null) {
            return
        }

        // Set the width and height of the frame.
        width = i420Buffer.width
        height = i420Buffer.height

        // Calculate sizes needed to convert to NV21 buffer format
        val size = width * height
        val chromaStride = width
        val chromaWidth = (width + 1) / 2
        val chromaHeight = (height + 1) / 2
        val nv21Size = size + chromaStride * chromaHeight
        if (nv21Buffer == null || nv21Buffer!!.size != nv21Size) {
            nv21Buffer = ByteArray(nv21Size)
        }
        val yPlane = i420Buffer.dataY
        val yPlaneArr = getByteArrayFromByteBuffer(yPlane)
        val uPlane = i420Buffer.dataU
        val uPlaneArr = getByteArrayFromByteBuffer(uPlane)
        val vPlane = i420Buffer.dataV
        val vPlaneArr = getByteArrayFromByteBuffer(vPlane)
        val yStride = i420Buffer.strideY
        val uStride = i420Buffer.strideU
        val vStride = i420Buffer.strideV

        val yPlaneArrOffset = 0
        val nv21BufferOffset = 0
        val rowLength = width

        for (y in 0 until height) {
            val srcOffset = yPlaneArrOffset + y * yStride
            val destOffset = nv21BufferOffset + y * width
            System.arraycopy(yPlaneArr, srcOffset, nv21Buffer, destOffset, rowLength)
        }

        for (y2 in 0 until chromaHeight) {
            for (x2 in 0 until chromaWidth) {
                nv21Buffer!![size + y2 * chromaStride + 2 * x2 + 1] =
                    uPlaneArr[y2 * uStride + x2]
                nv21Buffer!![size + y2 * chromaStride + 2 * x2] =
                    vPlaneArr[y2 * vStride + x2]
            }
        }

        i420Buffer.release()
    }

    /**
     * Converts this YUV frame to an ARGB_8888 Bitmap. Applies stored rotation.
     * @return A new Bitmap containing the converted frame.
     */
    fun getBitmap(): Bitmap? {
        val localNv24Buffer = nv21Buffer ?: return null

        val newBuffer = AbgrBuffer.allocate(width, height)
        val directNv21Buffer = ByteBuffer.allocateDirect(width * height * 3 / 2)
        directNv21Buffer.put(localNv24Buffer)

        val nv21BufferClass = Nv21Buffer.wrap(directNv21Buffer, width, height)
        nv21BufferClass.convertTo(newBuffer)

        // Construct a Bitmap based on the new pixel data
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(newBuffer.asBuffer())

        nv21BufferClass.close()
        directNv21Buffer.clear()
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

    private fun getByteArrayFromByteBuffer(byteBuffer: ByteBuffer): ByteArray {
        val bytesArray = ByteArray(byteBuffer.remaining())
        byteBuffer[bytesArray, 0, bytesArray.size]
        return bytesArray
    }
}
