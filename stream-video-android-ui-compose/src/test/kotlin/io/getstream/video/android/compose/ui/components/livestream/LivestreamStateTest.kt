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

package io.getstream.video.android.compose.ui.components.livestream

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.compose.ui.components.livestream.state.LivestreamState
import io.getstream.video.android.core.RealtimeConnection
import org.junit.Test

internal class LivestreamStateTest {

    @Test
    fun `a call that ends and disconnects together shows the ended state`() {
        val state = nextLivestreamState(
            current = LivestreamState.LIVE,
            connection = RealtimeConnection.Disconnected,
            ended = true,
            backstage = false,
            hasJoined = true,
        )

        assertThat(state).isEqualTo(LivestreamState.ENDED)
    }

    @Test
    fun `a disconnect without an end keeps the current state`() {
        val state = nextLivestreamState(
            current = LivestreamState.LIVE,
            connection = RealtimeConnection.Disconnected,
            ended = false,
            backstage = false,
            hasJoined = true,
        )

        assertThat(state).isEqualTo(LivestreamState.LIVE)
    }

    @Test
    fun `an ended call the viewer never joined keeps the current state`() {
        val state = nextLivestreamState(
            current = LivestreamState.INITIAL,
            connection = RealtimeConnection.Disconnected,
            ended = true,
            backstage = false,
            hasJoined = false,
        )

        assertThat(state).isEqualTo(LivestreamState.INITIAL)
    }

    @Test
    fun `a connected call maps to ended, backstage or live`() {
        fun connected(ended: Boolean, backstage: Boolean) = nextLivestreamState(
            current = LivestreamState.JOINING,
            connection = RealtimeConnection.Connected,
            ended = ended,
            backstage = backstage,
            hasJoined = true,
        )

        assertThat(connected(ended = true, backstage = true)).isEqualTo(LivestreamState.ENDED)
        assertThat(connected(ended = false, backstage = true)).isEqualTo(LivestreamState.BACKSTAGE)
        assertThat(connected(ended = false, backstage = false)).isEqualTo(LivestreamState.LIVE)
    }

    @Test
    fun `joining and failing map to their states`() {
        fun on(connection: RealtimeConnection) = nextLivestreamState(
            current = LivestreamState.LIVE,
            connection = connection,
            ended = false,
            backstage = false,
            hasJoined = true,
        )

        assertThat(on(RealtimeConnection.InProgress)).isEqualTo(LivestreamState.JOINING)
        assertThat(on(RealtimeConnection.Failed("boom"))).isEqualTo(LivestreamState.ERROR)
    }
}
