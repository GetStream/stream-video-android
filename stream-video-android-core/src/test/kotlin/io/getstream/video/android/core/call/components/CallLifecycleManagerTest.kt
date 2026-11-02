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

package io.getstream.video.android.core.call.components

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.model.PlaybackType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CallLifecycleManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var state: CallState
    private lateinit var sessionManager: CallSessionManager
    private lateinit var playbackTypeFlow: MutableStateFlow<PlaybackType>

    @Before
    fun setup() {
        state = mockk(relaxed = true)
        sessionManager = CallSessionManager()
        playbackTypeFlow = MutableStateFlow(PlaybackType.Hls("https://hls.example.com/playlist.m3u8"))
        every { state._connection } returns MutableStateFlow(RealtimeConnection.Connected)
        every { state._playbackType } returns playbackTypeFlow
    }

    private fun manager(): CallLifecycleManager {
        val clientImpl = mockk<StreamVideoClient>(relaxed = true)
        every { clientImpl.scope } returns testScope
        return CallLifecycleManager(
            clientImpl = clientImpl,
            sessionManager = sessionManager,
            scopeProvider = mockk(relaxed = true),
            callRegistry = mockk(relaxed = true),
            shutDownJobs = {},
            state = state,
            callAnalytics = mockk(relaxed = true),
            statsReporter = mockk(relaxed = true),
            media = mockk(relaxed = true),
            iceMonitor = mockk(relaxed = true),
            sessionMonitor = { mockk(relaxed = true) },
            connectivityMonitor = { mockk(relaxed = true) },
            type = "livestream",
            id = "test-call",
        )
    }

    @Test
    fun `leave after HLS playback cleans up the playback session and resets the playback type`() =
        runTest(testDispatcher) {
            val playbackSession = mockk<RtcSession>(relaxed = true)
            sessionManager.playbackSession = playbackSession

            manager().leave("user")
            advanceUntilIdle()

            assertThat(sessionManager.playbackSession).isNull()
            assertThat(playbackTypeFlow.value).isEqualTo(PlaybackType.WebRtc)
            verify { playbackSession.cleanup() }
            coVerify(exactly = 0) { playbackSession.sendLeaveEvent(any()) }
        }
}
