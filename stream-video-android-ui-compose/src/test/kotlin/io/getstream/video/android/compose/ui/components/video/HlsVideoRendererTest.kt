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

package io.getstream.video.android.compose.ui.components.video

import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.call.hls.HlsPlayback
import io.getstream.video.android.core.call.hls.HlsPlaybackStore
import io.getstream.video.android.core.model.PlaybackType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class HlsVideoRendererTest {

    private companion object {
        const val URL = "https://example.com/live.m3u8"
    }

    private val defaultFactory = HlsPlaybackStore.factory
    private val created = mutableListOf<FakeHlsPlayback>()
    private val activities = mutableListOf<ActivityController<ComponentActivity>>()

    @After
    fun tearDown() {
        activities.forEach { it.pause().stop().destroy() }
        HlsPlaybackStore.reset()
        HlsPlaybackStore.factory = defaultFactory
    }

    @Test
    fun `hls playback renders the playlist`() {
        val playlistUrl = URL
        HlsPlaybackStore.factory = { _, url ->
            FakeHlsPlayback(url).also { created += it }
        }
        val playback = MutableStateFlow<PlaybackType>(PlaybackType.Hls(playlistUrl))

        val activity = show(playback)

        assertThat(containsSurfaceView(activity.window.decorView)).isTrue()
        assertThat(created.map { it.playlistUrl }).containsExactly(playlistUrl)
        assertThat(created.single().attachCount).isEqualTo(1)

        playback.value = PlaybackType.WebRtc
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(containsSurfaceView(activity.window.decorView)).isFalse()
        assertThat(created.single().released).isTrue()
    }

    @Test
    fun `hls surface keeps the stream aspect ratio instead of stretching to the tile`() {
        HlsPlaybackStore.factory = { _, url ->
            FakeHlsPlayback(url).also {
                it.videoAspectRatio.value = 16f / 9f
                created += it
            }
        }
        val activity = show(MutableStateFlow(PlaybackType.Hls(URL)))
        val container = activity.findViewById<View>(android.R.id.content)
        assertThat(container.height).isGreaterThan(container.width)

        val surface = activity.window.decorView.findSurfaceView()!!
        assertThat(surface.width).isEqualTo(container.width)
        assertThat(surface.width.toFloat() / surface.height).isWithin(0.02f).of(16f / 9f)
    }

    @Test
    fun `webrtc playback does not create an hls player`() {
        HlsPlaybackStore.factory = { _, url ->
            FakeHlsPlayback(url).also { created += it }
        }

        val activity = show(MutableStateFlow(PlaybackType.WebRtc))

        assertThat(containsSurfaceView(activity.window.decorView)).isFalse()
        assertThat(created).isEmpty()
    }

    private fun show(playback: MutableStateFlow<PlaybackType>): ComponentActivity {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activities += controller
        val activity = controller.get()
        activity.setContent {
            HlsVideoRenderer(
                call = call(playback),
                modifier = Modifier.fillMaxSize(),
            )
        }
        shadowOf(Looper.getMainLooper()).idle()
        return activity
    }

    private fun call(playback: MutableStateFlow<PlaybackType>): Call {
        val state = mockk<CallState>()
        every { state.playbackType } returns playback
        return mockk {
            every { this@mockk.state } returns state
        }
    }

    private fun containsSurfaceView(view: View): Boolean = view.findSurfaceView() != null

    private fun View.findSurfaceView(): SurfaceView? {
        if (this is SurfaceView) return this
        if (this is ViewGroup) {
            for (index in 0 until childCount) {
                getChildAt(index).findSurfaceView()?.let { return it }
            }
        }
        return null
    }

    private class FakeHlsPlayback(
        override val playlistUrl: String,
    ) : HlsPlayback {
        override val videoAspectRatio = MutableStateFlow<Float?>(null)
        var attachCount = 0
        var released = false

        override fun attach(surfaceView: SurfaceView) {
            attachCount += 1
        }

        override fun detach(surfaceView: SurfaceView) = Unit

        override fun release() {
            released = true
        }
    }
}
