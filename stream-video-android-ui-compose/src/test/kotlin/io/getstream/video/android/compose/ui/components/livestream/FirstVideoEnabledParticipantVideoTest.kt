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

import app.cash.turbine.test
import io.getstream.video.android.core.ParticipantState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

public class FirstVideoEnabledParticipantVideoTest {

    private class FakeParticipant(sessionId: String, videoEnabled: Boolean) {
        val enabled = MutableStateFlow(videoEnabled)
        val video = MutableStateFlow<ParticipantState.Video?>(video(sessionId, videoEnabled))
        val state = mockk<ParticipantState> {
            every { this@mockk.videoEnabled } returns enabled
            every { this@mockk.video } returns this@FakeParticipant.video
        }

        fun setVideoEnabled(value: Boolean) {
            enabled.value = value
            video.value = video.value?.copy(enabled = value)
        }
    }

    @Test
    public fun `emits null when there are no participants`(): Unit = runBlocking {
        val participants = MutableStateFlow(emptyList<ParticipantState>())

        firstVideoEnabledParticipantVideo(participants).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    public fun `emits null when no participant has video enabled`(): Unit = runBlocking {
        val a = FakeParticipant("a", videoEnabled = false)
        val participants = MutableStateFlow(listOf(a.state))

        firstVideoEnabledParticipantVideo(participants).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    public fun `emits the video of the first participant with video enabled`(): Unit = runBlocking {
        val a = FakeParticipant("a", videoEnabled = false)
        val b = FakeParticipant("b", videoEnabled = true)
        val c = FakeParticipant("c", videoEnabled = true)
        val participants = MutableStateFlow(listOf(a.state, b.state, c.state))

        firstVideoEnabledParticipantVideo(participants).test {
            assertEquals("b", awaitItem()?.sessionId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    public fun `follows videoEnabled changes of the participants`(): Unit = runBlocking {
        val a = FakeParticipant("a", videoEnabled = false)
        val b = FakeParticipant("b", videoEnabled = true)
        val participants = MutableStateFlow(listOf(a.state, b.state))

        firstVideoEnabledParticipantVideo(participants).test {
            assertEquals("b", awaitItem()?.sessionId)

            a.setVideoEnabled(true)
            assertEquals("a", awaitItem()?.sessionId)

            a.setVideoEnabled(false)
            b.setVideoEnabled(false)
            // intermediate emissions are allowed while both changes propagate; it must settle on null
            @Suppress("ControlFlowWithEmptyBody")
            while (awaitItem() != null) { }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    public fun `follows participant list changes`(): Unit = runBlocking {
        val a = FakeParticipant("a", videoEnabled = true)
        val b = FakeParticipant("b", videoEnabled = true)
        val participants = MutableStateFlow(listOf(a.state))

        firstVideoEnabledParticipantVideo(participants).test {
            assertEquals("a", awaitItem()?.sessionId)

            participants.value = listOf(b.state)
            assertEquals("b", awaitItem()?.sessionId)

            participants.value = emptyList()
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        fun video(sessionId: String, enabled: Boolean) = ParticipantState.Video(
            sessionId = sessionId,
            track = null,
            enabled = enabled,
            paused = false,
        )
    }
}
