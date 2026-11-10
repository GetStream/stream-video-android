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
        const val OTHER_URL = "https://example.com/other.m3u8"
    }

    private val created = mutableListOf<FakeHlsPlayback>()
    private val store = HlsPlaybackStore { _, url -> FakeHlsPlayback(url).also { created += it } }
    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `one call shares a single player until the last renderer leaves`() {
        val call = mockk<Call>()

        val first = store.acquire(context, call, URL)
        val second = store.acquire(context, call, URL)

        assertThat(second).isSameInstanceAs(first)
        assertThat(created).hasSize(1)

        store.release(call, first)
        assertThat(created.single().released).isFalse()
        store.release(call, second)
        assertThat(created.single().released).isTrue()
    }

    @Test
    fun `a new playlist releases the previous player before the next one starts`() {
        val call = mockk<Call>()

        val old = store.acquire(context, call, URL)
        val new = store.acquire(context, call, OTHER_URL)

        assertThat(new).isNotSameInstanceAs(old)
        assertThat(created.map { it.playlistUrl }).containsExactly(URL, OTHER_URL).inOrder()
        assertThat(created[0].released).isTrue()
        assertThat(created[1].released).isFalse()
    }

    @Test
    fun `releasing a replaced player leaves the current one playing`() {
        val call = mockk<Call>()
        val old = store.acquire(context, call, URL)
        store.acquire(context, call, OTHER_URL)

        store.release(call, old)

        assertThat(created[1].released).isFalse()
    }

    @Test
    fun `different calls get different players`() {
        val first = store.acquire(context, mockk<Call>(), URL)
        val second = store.acquire(context, mockk<Call>(), URL)

        assertThat(second).isNotSameInstanceAs(first)
        assertThat(created).hasSize(2)
    }

    @Test
    fun `a failing factory leaves nothing behind for the next acquire`() {
        val call = mockk<Call>()
        var fail = true
        val failing = HlsPlaybackStore { _, url ->
            if (fail) error("decoder unavailable")
            FakeHlsPlayback(url).also { created += it }
        }

        assertThat(runCatching { failing.acquire(context, call, URL) }.isFailure).isTrue()
        fail = false
        failing.acquire(context, call, URL)

        assertThat(created).hasSize(1)
        assertThat(created.single().released).isFalse()
    }

    @Test
    fun `exoplayer is pointed at the playlist`() {
        val playback = Media3HlsPlayback(
            context = RuntimeEnvironment.getApplication(),
            playlistUrl = URL,
        )
        try {
            assertThat(playback.player.currentMediaItem?.localConfiguration?.uri.toString())
                .isEqualTo(URL)
            assertThat(playback.player.playWhenReady).isTrue()
        } finally {
            playback.release()
        }
    }

    @Test
    fun `swapping surfaces and calling after release does not throw`() {
        val application = RuntimeEnvironment.getApplication()
        val playback = Media3HlsPlayback(context = application, playlistUrl = URL)
        val first = SurfaceView(application)
        val second = SurfaceView(application)

        playback.attach(first)
        playback.attach(first)
        playback.attach(second)
        playback.detach(first)
        playback.detach(second)
        playback.release()
        playback.release()
        playback.attach(first)
        playback.detach(first)
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
