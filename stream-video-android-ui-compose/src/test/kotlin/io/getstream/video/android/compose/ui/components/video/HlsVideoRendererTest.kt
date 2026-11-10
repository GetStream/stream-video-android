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

import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.call.hls.HlsPlayback
import io.getstream.video.android.core.call.hls.HlsPlaybackStore
import io.getstream.video.android.core.model.PlaybackType
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class HlsVideoRendererTest {

    private companion object {
        const val URL = "https://example.com/live.m3u8"
        const val OTHER_URL = "https://example.com/other.m3u8"
        const val FRAMES = 5
        const val FRAME_MS = 20L
    }

    private val created = mutableListOf<FakeHlsPlayback>()
    private var aspectRatio: Float? = null
    private val store = HlsPlaybackStore { _, url ->
        FakeHlsPlayback(url).also {
            it.videoAspectRatio.value = aspectRatio
            created += it
        }
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
    fun `hls playback renders the playlist`() {
        val playback = MutableStateFlow<PlaybackType>(PlaybackType.Hls(URL))

        val activity = show(playback)

        assertThat(containsSurfaceView(activity.window.decorView)).isTrue()
        assertThat(created.map { it.playlistUrl }).containsExactly(URL)
        assertThat(created.single().attachCount).isEqualTo(1)

        playback.value = PlaybackType.WebRtc
        settle()

        assertThat(containsSurfaceView(activity.window.decorView)).isFalse()
        assertThat(created.single().released).isTrue()
    }

    @Test
    fun `a new playlist attaches the new player and releases the old one`() {
        val playback = MutableStateFlow<PlaybackType>(PlaybackType.Hls(URL))
        show(playback)

        playback.value = PlaybackType.Hls(OTHER_URL)
        settle()

        assertThat(created.map { it.playlistUrl }).containsExactly(URL, OTHER_URL).inOrder()
        assertThat(created[0].released).isTrue()
        assertThat(created[1].released).isFalse()
        assertThat(created[1].attachCount).isEqualTo(1)
    }

    @Test
    fun `hls surface keeps the stream aspect ratio instead of stretching to the tile`() {
        aspectRatio = 16f / 9f
        val activity = show(MutableStateFlow(PlaybackType.Hls(URL)))
        val container = activity.findViewById<View>(android.R.id.content)
        assertThat(container.height).isGreaterThan(container.width)

        val surface = activity.window.decorView.findSurfaceView()!!
        assertThat(surface.width).isEqualTo(container.width)
        assertThat(surface.width.toFloat() / surface.height).isWithin(0.02f).of(16f / 9f)
    }

    @Test
    fun `webrtc playback does not create an hls player`() {
        val activity = show(MutableStateFlow(PlaybackType.WebRtc))

        assertThat(containsSurfaceView(activity.window.decorView)).isFalse()
        assertThat(created).isEmpty()
    }

    @Test
    fun `an abandoned composition does not start a player`() {
        val call = call(MutableStateFlow(PlaybackType.Hls(URL)))
        // A private recomposer keeps the failing composition away from the main-thread one
        // that the activity-based tests share.
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(NoNodeApplier(), recomposer)

        val result = runCatching {
            composition.setContent {
                CompositionLocalProvider(
                    LocalContext provides RuntimeEnvironment.getApplication(),
                    LocalLifecycleOwner provides ResumedLifecycleOwner(),
                ) {
                    HlsVideoRendererImpl(call = call, store = store)
                }
                error("composition abandoned before it was applied")
            }
        }
        runCatching { composition.dispose() }
        recomposer.cancel()

        assertThat(result.isFailure).isTrue()
        assertThat(created).isEmpty()
    }

    @Test
    fun `two renderers of one call share a player and the second release stops it`() {
        val playback = MutableStateFlow<PlaybackType>(PlaybackType.Hls(URL))
        val call = call(playback)

        val first = show(call)
        val second = show(call)

        assertThat(created).hasSize(1)
        activities.remove(first.second)
        first.second.pause().stop().destroy()
        assertThat(created.single().released).isFalse()

        activities.remove(second.second)
        second.second.pause().stop().destroy()
        assertThat(created.single().released).isTrue()
    }

    private fun show(playback: MutableStateFlow<PlaybackType>): ComponentActivity =
        show(call(playback)).first

    private fun show(call: Call): Pair<ComponentActivity, ActivityController<ComponentActivity>> {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activities += controller
        val activity = controller.get()
        val view = ComposeView(activity).apply {
            setParentCompositionContext(recomposer)
            setContent {
                HlsVideoRendererImpl(
                    call = call,
                    store = store,
                    modifier = Modifier.fillMaxSize(),
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

    private class NoNodeApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }

    private class ResumedLifecycleOwner : LifecycleOwner {
        override val lifecycle: Lifecycle = LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.RESUMED
        }
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
