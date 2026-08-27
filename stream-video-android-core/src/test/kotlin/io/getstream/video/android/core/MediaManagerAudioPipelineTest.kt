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

package io.getstream.video.android.core

import android.content.Context
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.EglBase

/**
 * Covers [MediaManagerImpl.replaceAudioSourceAndTrack] — the swap that lets audio-source
 * constraints change mid-call. Disposal and rollback are the whole risk here: the audio track is
 * handed to a sender that must not own it, so the media manager is the only thing allowed to
 * dispose it, exactly once, and only after a swap that actually happened.
 */
class MediaManagerAudioPipelineTest {

    private val testScope = TestScope(StandardTestDispatcher())

    private val factory = mockk<StreamPeerConnectionFactory>(relaxed = true)
    private val call = mockk<Call>(relaxed = true)

    private fun mediaManager(): MediaManagerImpl {
        every { call.peerConnectionFactory } returns factory
        return MediaManagerImpl(
            context = mockk<Context>(relaxed = true),
            call = call,
            scope = testScope,
            eglBaseContext = mockk<EglBase.Context>(relaxed = true),
        )
    }

    private fun stubSourcesAndTracks(
        sources: List<AudioSource>,
        tracks: List<AudioTrack>,
    ) {
        every { factory.makeAudioSource(any()) } returnsMany sources
        every { factory.makeAudioTrack(any(), any()) } returnsMany tracks
    }

    @Test
    fun `a successful swap installs the new pair and disposes the replaced one`() {
        val firstSource = mockk<AudioSource>(relaxed = true)
        val secondSource = mockk<AudioSource>(relaxed = true)
        val firstTrack = mockk<AudioTrack>(relaxed = true)
        val secondTrack = mockk<AudioTrack>(relaxed = true)
        stubSourcesAndTracks(
            listOf(firstSource, secondSource),
            listOf(firstTrack, secondTrack),
        )
        val manager = mediaManager()

        // Force the first pair into existence, the way publishing does.
        assertSame(firstTrack, manager.audioTrack)

        var handed: AudioTrack? = null
        assertTrue(
            manager.replaceAudioSourceAndTrack {
                handed = it
                true
            },
        )

        assertSame(secondTrack, handed)
        assertSame(secondTrack, manager.audioTrack)
        assertSame(secondSource, manager.audioSource)
        verify { firstTrack.dispose() }
        verify { firstSource.dispose() }
        // The new pair is live, so disposing it here would hand the sender a dead track.
        verify(exactly = 0) { secondTrack.dispose() }
        verify(exactly = 0) { secondSource.dispose() }
    }

    @Test
    fun `a refused swap rolls the new pair back and leaves the live one alone`() {
        val firstSource = mockk<AudioSource>(relaxed = true)
        val secondSource = mockk<AudioSource>(relaxed = true)
        val firstTrack = mockk<AudioTrack>(relaxed = true)
        val secondTrack = mockk<AudioTrack>(relaxed = true)
        stubSourcesAndTracks(
            listOf(firstSource, secondSource),
            listOf(firstTrack, secondTrack),
        )
        val manager = mediaManager()
        assertSame(firstTrack, manager.audioTrack)

        assertFalse(manager.replaceAudioSourceAndTrack { false })

        // Nothing was replaced, so tearing the live pair down would silence the call.
        assertSame(firstTrack, manager.audioTrack)
        assertSame(firstSource, manager.audioSource)
        verify(exactly = 0) { firstTrack.dispose() }
        verify(exactly = 0) { firstSource.dispose() }
        verify { secondTrack.dispose() }
        verify { secondSource.dispose() }
    }

    @Test
    fun `the swap carries the microphone mute state onto the new track`() {
        val track = mockk<AudioTrack>(relaxed = true)
        stubSourcesAndTracks(
            listOf(mockk(relaxed = true)),
            listOf(track),
        )
        val manager = mediaManager()

        manager.replaceAudioSourceAndTrack { true }

        // The microphone was never enabled, so a fresh track — which starts enabled — has to be
        // muted or the swap would unmute the user.
        verify { track.setEnabled(false) }
    }

    @Test
    fun `no swap happens after the media manager has been released`() {
        stubSourcesAndTracks(listOf(mockk(relaxed = true)), listOf(mockk(relaxed = true)))
        val manager = mediaManager()
        manager.cleanup()

        var swapCalled = false
        assertFalse(
            manager.replaceAudioSourceAndTrack {
                swapCalled = true
                true
            },
        )

        // Rebuilding after teardown would resurrect native objects nothing disposes.
        assertFalse(swapCalled)
    }
}
