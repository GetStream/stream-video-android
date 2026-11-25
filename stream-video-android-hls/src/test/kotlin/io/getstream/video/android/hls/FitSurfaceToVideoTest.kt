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

package io.getstream.video.android.hls

import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

internal class FitSurfaceToVideoTest {

    private val holder = mockk<SurfaceHolder>(relaxed = true)

    @Test
    fun `sizes the surface buffer to the video`() {
        fitSurfaceToVideo(holder, VideoSize(1280, 720))

        verify { holder.setFixedSize(1280, 720) }
    }

    @OptIn(UnstableApi::class)
    @Test
    fun `widens the buffer for non-square pixels`() {
        fitSurfaceToVideo(holder, VideoSize(960, 720, 4f / 3f))

        verify { holder.setFixedSize(1280, 720) }
    }

    @Test
    fun `leaves the surface alone while the video size is unknown`() {
        fitSurfaceToVideo(holder, VideoSize.UNKNOWN)

        confirmVerified(holder)
    }
}
