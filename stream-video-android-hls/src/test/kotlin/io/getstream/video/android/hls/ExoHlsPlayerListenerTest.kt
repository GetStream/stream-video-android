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

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class ExoHlsPlayerListenerTest {

    private val player = mockk<Player>(relaxed = true)
    private val aspectRatios = mutableListOf<Float>()
    private val listener = ExoHlsPlayerListener(player) { aspectRatios += it }

    @Test
    fun `falling behind the live window rejoins the live edge`() {
        listener.onPlayerError(playbackError(PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW))

        verifyOrder {
            player.seekToDefaultPosition()
            player.prepare()
        }
        confirmVerified(player)
    }

    @Test
    fun `other playback errors are left alone`() {
        listener.onPlayerError(
            playbackError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED),
        )
        listener.onPlayerError(
            playbackError(PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED),
        )

        confirmVerified(player)
    }

    @Test
    fun `resuming far behind the live edge jumps to it`() {
        givenLive(liveOffsetMs = 120_000)

        listener.onPlaybackStateChanged(Player.STATE_READY)

        verify { player.seekToDefaultPosition() }
    }

    @Test
    fun `resuming near the live edge stays where it is`() {
        givenLive(liveOffsetMs = 20_000)

        listener.onPlaybackStateChanged(Player.STATE_READY)

        verify(exactly = 0) { player.seekToDefaultPosition() }
    }

    @Test
    fun `only ready on a live stream is checked`() {
        givenLive(liveOffsetMs = 120_000)
        listener.onPlaybackStateChanged(Player.STATE_BUFFERING)
        every { player.isCurrentMediaItemLive } returns false
        listener.onPlaybackStateChanged(Player.STATE_READY)

        verify(exactly = 0) { player.seekToDefaultPosition() }
    }

    @Test
    fun `the jump threshold follows the target live offset`() {
        assertThat(isFarBehindLiveEdge(liveOffsetMs = 40_000, targetOffsetMs = 30_000)).isFalse()
        assertThat(isFarBehindLiveEdge(liveOffsetMs = 46_000, targetOffsetMs = 30_000)).isTrue()
    }

    @Test
    fun `an unknown target uses the default and an unknown offset never jumps`() {
        assertThat(isFarBehindLiveEdge(liveOffsetMs = 29_000, targetOffsetMs = C.TIME_UNSET))
            .isFalse()
        assertThat(isFarBehindLiveEdge(liveOffsetMs = 31_000, targetOffsetMs = C.TIME_UNSET))
            .isTrue()
        assertThat(isFarBehindLiveEdge(liveOffsetMs = C.TIME_UNSET, targetOffsetMs = 12_000))
            .isFalse()
    }

    private fun givenLive(liveOffsetMs: Long) {
        every { player.isCurrentMediaItemLive } returns true
        every { player.currentLiveOffset } returns liveOffsetMs
        every { player.currentTimeline } returns Timeline.EMPTY
    }

    @Test
    fun `a known video size publishes its aspect ratio`() {
        listener.onVideoSizeChanged(VideoSize.UNKNOWN)
        listener.onVideoSizeChanged(VideoSize(1920, 1080))

        assertThat(aspectRatios).hasSize(1)
        assertThat(aspectRatios.single()).isWithin(0.001f).of(16f / 9f)
    }

    @OptIn(UnstableApi::class)
    private fun playbackError(errorCode: Int) = PlaybackException("test", null, errorCode)
}
