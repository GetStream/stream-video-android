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

package io.getstream.video.android.ui.common.renderer

import android.content.Context
import android.graphics.SurfaceTexture
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.VideoTextureViewRenderer

public class StreamVideoTextureViewRenderer(
    context: Context,
) : VideoTextureViewRenderer(context) {

    private val logger by taggedLogger()

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        logger.d { "onSurfaceTextureDestroyed: $surfaceTexture" }
        return super.onSurfaceTextureDestroyed(surfaceTexture)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        logger.d { "onDetachedFromWindow" }
    }
}
