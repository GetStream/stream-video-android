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

import android.media.MediaRecorder
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.models.AudioBitrateProfile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the bridges [MicrophoneManager.setAudioBitrateProfile] reaches the call through. The
 * microphone manager cannot see the media component or the session, so every stage of a mid-call
 * profile switch travels one of these methods.
 *
 * What is worth guarding is the answer each gives when its target is absent, because that answer
 * is what [AudioProfileResult] reports as a stage. A missing session means nothing is publishing,
 * which is a stage that could not be reached — except for the capture-pipeline rebuild, where it
 * means the constraints are read fresh at the next build and the change already holds.
 */
@RunWith(RobolectricTestRunner::class)
class CallAudioProfileBridgeTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private fun call(): Call = client.call("default", randomUUID())

    /**
     * Injected outside any `every { }` block on purpose: [injectSession] stubs the session's
     * socket state itself, and MockK cannot record one stubbing block inside another.
     */
    private fun Call.withSession(): RtcSession =
        mockk<RtcSession>(relaxed = true).also { injectSession(it) }

    private fun Call.withFactory(): StreamPeerConnectionFactory =
        mockk<StreamPeerConnectionFactory>(relaxed = true).also { injectPeerConnectionFactory(it) }

    //region session-backed bridges

    @Test
    fun `setAudioMaxBitrate reaches the session`() = runTest {
        val call = call()
        val session = call.withSession()
        every { session.setAudioMaxBitrate(128_000) } returns true

        assertTrue(call.setAudioMaxBitrate(128_000))
    }

    @Test
    fun `setAudioMaxBitrate reports false before a session exists`() = runTest {
        // Nothing is publishing, so the live setter has nowhere to put the ceiling.
        assertFalse(call().setAudioMaxBitrate(128_000))
    }

    @Test
    fun `hasLiveAudioSender is false before a session exists`() = runTest {
        assertFalse(call().hasLiveAudioSender())
    }

    @Test
    fun `hasLiveAudioSender reaches the session`() = runTest {
        val call = call()
        val session = call.withSession()
        every { session.hasLiveAudioSender() } returns true

        assertTrue(call.hasLiveAudioSender())
    }

    @Test
    fun `audioMaxBitrate reads through the session`() = runTest {
        val call = call()
        val session = call.withSession()
        every { session.audioMaxBitrate() } returns 128_000

        assertEquals(128_000, call.audioMaxBitrate())
    }

    @Test
    fun `negotiatedAudioBitrate reads through the session`() = runTest {
        val call = call()
        val session = call.withSession()
        every { session.negotiatedAudioBitrate() } returns 64_000

        assertEquals(64_000, call.negotiatedAudioBitrate())
    }

    @Test
    fun `audioBitrateFor reads through the session`() = runTest {
        val call = call()
        val session = call.withSession()
        every {
            session.audioBitrateFor(AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY)
        } returns 128_000

        assertEquals(
            128_000,
            call.audioBitrateFor(AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY),
        )
    }

    @Test
    fun `the bitrate readers are null before a session exists`() = runTest {
        val call = call()

        assertNull(call.audioMaxBitrate())
        assertNull(call.negotiatedAudioBitrate())
        assertNull(
            call.audioBitrateFor(AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY),
        )
    }

    @Test
    fun `rebuildAudioCapturePipeline reaches the session`() = runTest {
        val call = call()
        val session = call.withSession()
        every { session.rebuildAudioCapturePipeline() } returns true

        assertTrue(call.rebuildAudioCapturePipeline())
    }

    @Test
    fun `rebuildAudioCapturePipeline succeeds before a session exists`() = runTest {
        // The source has not been built yet and is built from the current constraints when it is,
        // so there is nothing to rebuild and the change already holds. Reporting a failed stage
        // here would fail a switch that in fact took.
        assertTrue(call().rebuildAudioCapturePipeline())
    }

    //endregion

    //region media-backed bridges

    @Test
    fun `setCaptureAudioSource reaches the media component`() = runTest {
        val call = call()
        val factory = call.withFactory()
        every { factory.setCaptureAudioSource(MediaRecorder.AudioSource.MIC) } returns true

        assertTrue(call.setCaptureAudioSource(MediaRecorder.AudioSource.MIC))
        verify { factory.setCaptureAudioSource(MediaRecorder.AudioSource.MIC) }
    }

    @Test
    fun `setCaptureAudioSource reports false before a factory exists`() = runTest {
        assertFalse(call().setCaptureAudioSource(MediaRecorder.AudioSource.MIC))
    }

    @Test
    fun `setHardwareNoiseSuppressorEnabled reaches the media component`() = runTest {
        val call = call()
        val factory = call.withFactory()
        every { factory.setHardwareNoiseSuppressorEnabled(false) } returns true

        assertTrue(call.setHardwareNoiseSuppressorEnabled(false))
        verify { factory.setHardwareNoiseSuppressorEnabled(false) }
    }

    @Test
    fun `setHardwareAcousticEchoCancelerEnabled reaches the media component`() = runTest {
        val call = call()
        val factory = call.withFactory()
        every { factory.setHardwareAcousticEchoCancelerEnabled(false) } returns true

        assertTrue(call.setHardwareAcousticEchoCancelerEnabled(false))
        verify { factory.setHardwareAcousticEchoCancelerEnabled(false) }
    }

    @Test
    fun `isAudioProcessingReachable follows whether a processor is attached`() = runTest {
        val call = call()
        val factory = call.withFactory()
        every { factory.hasAudioProcessingAttached() } returns true

        assertTrue(call.isAudioProcessingReachable())
    }

    @Test
    fun `isAudioProcessingReachable is false with no factory built`() = runTest {
        // An absent processor is not a stage that failed — nothing is processing, so a profile
        // asking for no processing is already satisfied.
        assertFalse(call().isAudioProcessingReachable())
    }

    @Test
    fun `isHardwareNoiseSuppressorSupported follows the device capability`() = runTest {
        val call = call()
        val factory = call.withFactory()
        every { factory.isHardwareNoiseSuppressorSupported() } returns true

        assertTrue(call.isHardwareNoiseSuppressorSupported())
    }

    @Test
    fun `isHardwareAcousticEchoCancelerSupported follows the device capability`() = runTest {
        val call = call()
        val factory = call.withFactory()
        every { factory.isHardwareAcousticEchoCancelerSupported() } returns true

        assertTrue(call.isHardwareAcousticEchoCancelerSupported())
    }

    //endregion
}
