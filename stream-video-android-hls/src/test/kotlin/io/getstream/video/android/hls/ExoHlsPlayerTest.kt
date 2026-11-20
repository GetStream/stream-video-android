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

import android.os.Looper
import android.view.SurfaceView
import androidx.media3.common.VideoSize
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class ExoHlsPlayerTest {

    private companion object {
        const val URL = "https://example.com/live.m3u8"
    }

    private val application = RuntimeEnvironment.getApplication()
    private val player = ExoHlsPlayerFactory().create(application, URL) as ExoHlsPlayer

    @After
    fun tearDown() {
        player.release()
    }

    @Test
    fun `creating the player does not build exoplayer until a surface is attached`() {
        assertThat(player.exoPlayer).isNull()

        player.attach(SurfaceView(application))

        val exoPlayer = player.exoPlayer!!
        assertThat(exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()).isEqualTo(URL)
        assertThat(exoPlayer.playWhenReady).isTrue()
    }

    @Test
    fun `detaching keeps playing and a later attach shows the same exoplayer`() {
        val first = SurfaceView(application)
        player.attach(first)
        val exoPlayer = player.exoPlayer

        player.detach(first)
        assertThat(player.attachedSurface).isNull()
        assertThat(exoPlayer!!.playWhenReady).isTrue()

        val second = SurfaceView(application)
        player.attach(second)
        assertThat(player.exoPlayer).isSameInstanceAs(exoPlayer)
        assertThat(player.attachedSurface).isSameInstanceAs(second)
    }

    @Test
    fun `detaching a surface that is no longer shown keeps the current surface`() {
        val first = SurfaceView(application)
        val second = SurfaceView(application)
        player.attach(first)
        player.attach(second)

        player.detach(first)

        assertThat(player.attachedSurface).isSameInstanceAs(second)
        assertThat(player.exoPlayer!!.playWhenReady).isTrue()
    }

    @Test
    fun `audio muted before the first attach applies to the new exoplayer`() {
        player.setAudioEnabled(false)
        player.attach(SurfaceView(application))
        assertThat(player.exoPlayer!!.volume).isEqualTo(0f)
        assertThat(player.exoPlayer!!.playWhenReady).isTrue()

        player.setAudioEnabled(true)
        assertThat(player.exoPlayer!!.volume).isEqualTo(1f)
    }

    @Test
    fun `release from another thread runs on the main thread and ends playback`() {
        val surface = SurfaceView(application)
        player.attach(surface)

        thread { player.release() }.join()
        assertThat(player.exoPlayer).isNotNull()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(player.exoPlayer).isNull()
        player.attach(surface)
        player.detach(surface)
        player.setAudioEnabled(false)
        assertThat(player.exoPlayer).isNull()
    }

    @Test
    fun `aspect ratio uses the display shape and stays unknown without a frame size`() {
        assertThat(videoAspectRatioOf(VideoSize(1920, 1080))).isWithin(0.001f).of(16f / 9f)
        assertThat(videoAspectRatioOf(VideoSize(960, 1080, 2f))).isWithin(0.001f).of(16f / 9f)
        assertThat(videoAspectRatioOf(VideoSize.UNKNOWN)).isNull()
    }
}
