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

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Buffering
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Ended
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Loading
import io.getstream.video.android.core.call.hls.HlsPlaybackState.Playing
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class HlsPlaybackObserverTest {

    private val state = MutableStateFlow<HlsPlaybackState?>(null)
    private val error = MutableStateFlow<String?>(null)

    private fun observer(reporter: HlsStatsReporter? = null) =
        HlsPlaybackObserver(playbackState = state, playbackError = error, statsReporter = reporter)

    @Test
    fun `publishes player states without a stats reporter`() {
        val observer = observer()

        observer.onPlaybackStateChanged(Loading)
        assertThat(state.value).isEqualTo(Loading)
        observer.onPlaybackStateChanged(Playing)
        assertThat(state.value).isEqualTo(Playing)
        observer.onPlaybackStateChanged(Ended)
        assertThat(state.value).isEqualTo(Ended)
    }

    @Test
    fun `an error stays until playback plays again`() {
        val observer = observer()
        observer.onPlaybackStateChanged(Playing)

        observer.onPlaybackError(NETWORK_ERROR)
        observer.onPlaybackStateChanged(Buffering)
        assertThat(error.value).isEqualTo(NETWORK_ERROR)

        observer.onPlaybackStateChanged(Playing)
        assertThat(error.value).isNull()
    }

    @Test
    fun `close resets the published state`() {
        val observer = observer()
        observer.onPlaybackStateChanged(Ended)
        observer.onPlaybackError(NETWORK_ERROR)

        observer.close()

        assertThat(state.value).isNull()
        assertThat(error.value).isNull()
    }

    @Test
    fun `callbacks after close change nothing and are not forwarded`() {
        val reporter = mockk<HlsStatsReporter>(relaxed = true)
        val observer = observer(reporter)

        observer.close()
        observer.onPlaybackStateChanged(Playing)
        observer.onPlaybackError(NETWORK_ERROR)

        assertThat(state.value).isNull()
        assertThat(error.value).isNull()
        verify(exactly = 0) { reporter.onPlaybackStateChanged(any()) }
        verify(exactly = 0) { reporter.onPlaybackError(any()) }
    }

    @Test
    fun `forwards states and errors to the stats reporter`() {
        val reporter = mockk<HlsStatsReporter>(relaxed = true)
        val observer = observer(reporter)

        observer.onPlaybackStateChanged(Playing)
        observer.onPlaybackError(NETWORK_ERROR)

        verifyOrder {
            reporter.onPlaybackStateChanged(Playing)
            reporter.onPlaybackError(NETWORK_ERROR)
        }
    }

    private companion object {
        const val NETWORK_ERROR = "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED"
    }
}
