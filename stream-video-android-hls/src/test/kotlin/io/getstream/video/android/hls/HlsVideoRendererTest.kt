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

import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.hls.HlsPlayer
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalStreamVideoApi::class)
internal class HlsVideoRendererTest {

    private companion object {
        const val URL = "https://example.com/live.m3u8"
        const val OTHER_URL = "https://example.com/other.m3u8"
        const val FRAMES = 5
        const val FRAME_MS = 20L
    }

    private val activities = mutableListOf<ActivityController<ComponentActivity>>()

    // Compose caches the window recomposer's dispatcher per thread, bound to the first test's
    // Choreographer, so later tests would never get a frame. Each test drives its own.
    private val frameClock = BroadcastFrameClock()
    private val recomposerScope = CoroutineScope(
        Handler(Looper.getMainLooper()).asCoroutineDispatcher() + frameClock + Job(),
    )
    private val recomposer = Recomposer(recomposerScope.coroutineContext).also { recomposer ->
        recomposerScope.launch { recomposer.runRecomposeAndApplyChanges() }
    }

    @After
    fun tearDown() {
        activities.forEach { runCatching { it.pause().stop().destroy() } }
        recomposer.cancel()
        recomposerScope.cancel()
    }

    @Test
    fun `the call's player is shown and hidden when playback ends`() {
        val player = FakeHlsPlayer(URL)
        val hlsPlayer = MutableStateFlow<HlsPlayer?>(player)

        val activity = show(call(hlsPlayer)).first

        assertThat(activity.window.decorView.findSurfaceView()).isNotNull()
        assertThat(player.shownOn).isNotNull()

        hlsPlayer.value = null
        settle()

        assertThat(activity.window.decorView.findSurfaceView()).isNull()
        assertThat(player.shownOn).isNull()
        assertThat(player.released).isFalse()
    }

    @Test
    fun `a recreated renderer resumes the same player without releasing it`() {
        val player = FakeHlsPlayer(URL)
        val call = call(MutableStateFlow(player))

        val (_, first) = show(call)
        activities.remove(first)
        first.pause().stop().destroy()
        assertThat(player.shownOn).isNull()

        show(call)

        assertThat(player.shownOn).isNotNull()
        assertThat(player.attachCount).isEqualTo(2)
        assertThat(player.released).isFalse()
    }

    @Test
    fun `a replacement player is shown in place of the previous one`() {
        val old = FakeHlsPlayer(URL)
        val new = FakeHlsPlayer(OTHER_URL)
        val hlsPlayer = MutableStateFlow<HlsPlayer?>(old)
        show(call(hlsPlayer))

        hlsPlayer.value = new
        settle()

        assertThat(old.shownOn).isNull()
        assertThat(new.shownOn).isNotNull()
    }

    @Test
    fun `hls surface keeps the stream aspect ratio instead of stretching to the tile`() {
        val player = FakeHlsPlayer(URL).apply { videoAspectRatio.value = 16f / 9f }
        val activity = show(call(MutableStateFlow(player))).first
        val container = activity.findViewById<View>(android.R.id.content)
        assertThat(container.height).isGreaterThan(container.width)

        val surface = activity.window.decorView.findSurfaceView()!!
        assertThat(surface.width).isEqualTo(container.width)
        assertThat(surface.width.toFloat() / surface.height).isWithin(0.02f).of(16f / 9f)
    }

    @Test
    fun `a call without an hls player renders nothing`() {
        val activity = show(call(MutableStateFlow(null))).first

        assertThat(activity.window.decorView.findSurfaceView()).isNull()
    }

    @Test
    fun `the audio toggle mutes the player and a replacement player starts muted`() {
        val player = FakeHlsPlayer(URL)
        val hlsPlayer = MutableStateFlow<HlsPlayer?>(player)
        val audioEnabled = mutableStateOf(true)
        show(call(hlsPlayer), audioEnabled)
        assertThat(player.audioEnabled).isTrue()

        audioEnabled.value = false
        settle()
        assertThat(player.audioEnabled).isFalse()

        val replacement = FakeHlsPlayer(OTHER_URL)
        hlsPlayer.value = replacement
        settle()
        assertThat(replacement.audioEnabled).isFalse()
    }

    private fun show(
        call: Call,
        audioEnabled: State<Boolean> = mutableStateOf(true),
    ): Pair<ComponentActivity, ActivityController<ComponentActivity>> {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activities += controller
        val activity = controller.get()
        val view = ComposeView(activity).apply {
            setParentCompositionContext(recomposer)
            setContent {
                HlsVideoRenderer(
                    call = call,
                    modifier = Modifier.fillMaxSize(),
                    audioEnabled = audioEnabled.value,
                )
            }
        }
        activity.setContentView(view)
        settle()
        return activity to controller
    }

    /** Runs the frames that follow, so state written by an effect is composed and laid out. */
    private fun settle() {
        val looper = shadowOf(Looper.getMainLooper())
        repeat(FRAMES) {
            Snapshot.sendApplyNotifications()
            looper.idle()
            frameClock.sendFrame(System.nanoTime())
            looper.idleFor(Duration.ofMillis(FRAME_MS))
        }
    }

    private fun call(hlsPlayer: MutableStateFlow<HlsPlayer?>): Call = mockk {
        every { this@mockk.hlsPlayer } returns hlsPlayer
    }

    private fun View.findSurfaceView(): SurfaceView? {
        if (this is SurfaceView) return this
        if (this is ViewGroup) {
            for (index in 0 until childCount) {
                getChildAt(index).findSurfaceView()?.let { return it }
            }
        }
        return null
    }

    private class FakeHlsPlayer(
        override val playlistUrl: String,
    ) : HlsPlayer {
        override val videoAspectRatio = MutableStateFlow<Float?>(null)
        var shownOn: SurfaceView? = null
        var attachCount = 0
        var released = false
        var audioEnabled: Boolean? = null

        override fun attach(surfaceView: SurfaceView) {
            attachCount += 1
            shownOn = surfaceView
        }

        override fun detach(surfaceView: SurfaceView) {
            if (shownOn === surfaceView) shownOn = null
        }

        override fun setAudioEnabled(enabled: Boolean) {
            audioEnabled = enabled
        }

        override fun release() {
            released = true
        }
    }
}
