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

package io.getstream.video.android.core.call.hls

import android.content.Context
import android.view.SurfaceView
import androidx.media3.common.VideoSize
import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.Call
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class Media3HlsPlaybackTest {

    private companion object {
        const val URL = "https://example.com/live.m3u8"
    }

    private val defaultFactory = HlsPlaybackStore.factory
    private val created = mutableListOf<FakeHlsPlayback>()

    @After
    fun tearDown() {
        HlsPlaybackStore.reset()
        HlsPlaybackStore.factory = defaultFactory
    }

    @Test
    fun `one call shares a single player until the last renderer leaves`() {
        val call = mockk<Call>()
        HlsPlaybackStore.factory = { _, url -> FakeHlsPlayback(url).also { created += it } }

        val context = mockk<Context>(relaxed = true)
        val first = HlsPlaybackStore.acquire(context, call, URL)
        val second = HlsPlaybackStore.acquire(context, call, URL)

        assertThat(second).isSameInstanceAs(first)
        assertThat(created).hasSize(1)

        HlsPlaybackStore.release(call, first)
        assertThat(created.single().released).isFalse()
        HlsPlaybackStore.release(call, second)
        assertThat(created.single().released).isTrue()
    }

    @Test
    fun `exoplayer is pointed at the playlist`() {
        val playback = Media3HlsPlayback(
            context = RuntimeEnvironment.getApplication(),
            playlistUrl = "https://example.com/live.m3u8",
        )
        try {
            assertThat(playback.player.currentMediaItem?.localConfiguration?.uri.toString())
                .isEqualTo("https://example.com/live.m3u8")
            assertThat(playback.player.playWhenReady).isTrue()
        } finally {
            playback.release()
        }
    }

    @Test
    fun `aspect ratio uses the display shape and stays unknown without a frame size`() {
        assertThat(videoAspectRatioOf(VideoSize(1920, 1080))).isWithin(0.001f).of(16f / 9f)
        assertThat(videoAspectRatioOf(VideoSize(960, 1080, 2f))).isWithin(0.001f).of(16f / 9f)
        assertThat(videoAspectRatioOf(VideoSize.UNKNOWN)).isNull()
    }

    private class FakeHlsPlayback(
        override val playlistUrl: String,
    ) : HlsPlayback {
        override val videoAspectRatio = MutableStateFlow<Float?>(null)
        var released = false

        override fun attach(surfaceView: SurfaceView) = Unit

        override fun detach(surfaceView: SurfaceView) = Unit

        override fun release() {
            released = true
        }
    }
}
