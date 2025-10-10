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

package io.getstream.video.android.ui.common.renderer

import android.content.Context
import android.graphics.SurfaceTexture
import io.getstream.log.taggedLogger
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer

public class StreamVideoTextureViewRenderer(
    context: Context,
) : VideoTextureViewRenderer(context) {

    private val logger by taggedLogger("StreamVideoTextureViewRenderer")

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        logger.d {
            "[onLayout] #track; changed: $changed, left: $left, top: $top, right: $right, " +
                "bottom: $bottom"
        }
    }

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        super.onSurfaceTextureAvailable(surfaceTexture, width, height)
        logger.d {
            "[onSurfaceTextureAvailable] #track; width: $width, height: $height, " +
                "surfaceTexture: $surfaceTexture"
        }
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        super.onSurfaceTextureSizeChanged(surfaceTexture, width, height)
        logger.d {
            "[onSurfaceTextureSizeChanged] #track; width: $width, height: $height, " +
                "surfaceTexture: $surfaceTexture"
        }
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        logger.d { "[onSurfaceTextureDestroyed] #track; surfaceTexture: $surfaceTexture" }
        return super.onSurfaceTextureDestroyed(surfaceTexture)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        logger.d { "[onDetachedFromWindow] no args" }
    }
}
