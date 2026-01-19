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

package io.getstream.video.android.core.call.video

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLUtils
import org.webrtc.SurfaceTextureHelper
import org.webrtc.TextureBufferImpl
import org.webrtc.VideoFrame
import org.webrtc.VideoProcessor
import org.webrtc.VideoSink
import org.webrtc.YuvConverter

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

        if (currentFilter is RawVideoFilter) {
            val filteredFrame = currentFilter.applyFilter(frame, surfaceTextureHelper.invoke())
            sink?.onFrame(filteredFrame)
        } else if (currentFilter is BitmapVideoFilter) {
            // first prepare a Bitmap for the client
            inputFrameBitmap = YuvFrame.bitmapFromVideoFrame(frame)

            if (inputFrameBitmap != null && sink != null) {
                // Prepare helpers (runs only once or if the dimensions change)
                initialize(
                    inputFrameBitmap!!.width,
                    inputFrameBitmap!!.height,
                    surfaceTextureHelper.invoke(),
                )

                // now ask client to make modifications
                currentFilter.applyFilter(inputFrameBitmap!!)

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

                // Convert the buffer back to YUV (VideoFrame needs YUV)
                yuvBuffer = yuvConverter.convert(inputBuffer)

                sink?.onFrame(VideoFrame(yuvBuffer, 0, frame.timestampNs))
            }
        } else {
            throw Error("Unsupported video filter type ${filter.invoke()}")
        }
    }

    override fun setSink(sink: VideoSink?) {
        this@FilterVideoProcessor.sink = sink
    }

    private fun initialize(width: Int, height: Int, textureHelper: SurfaceTextureHelper) {
        yuvBuffer?.release()

        if (this.inputWidth != width || this.inputHeight != height) {
            this.inputWidth = width
            this.inputHeight = height
            inputFrameBitmap?.recycle()
            inputBuffer?.release()

            val type = VideoFrame.TextureBuffer.Type.RGB

            val matrix = Matrix()
            // This is vertical flip - we need to investigate why the image is flipped vertically and
            // why we need to correct it here.
            matrix.preScale(1.0f, -1.0f)
            val surfaceTextureHelper: SurfaceTextureHelper = textureHelper
            this.inputBuffer = TextureBufferImpl(
                inputWidth, inputHeight, type, textures[0], matrix, surfaceTextureHelper.handler,
                yuvConverter, null as Runnable?,
            )
            this.inputFrameBitmap =
                Bitmap.createBitmap(this.inputWidth, this.inputHeight, Bitmap.Config.ARGB_8888)
        }
    }
}
