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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import stream.video.sfu.models.AudioBitrateProfile

/**
 * Covers [AudioProfileResult.complete], which decides whether `audioBitrateProfile` moves.
 *
 * Every stage has to hold. A single stage left behind means something is still processing the
 * audio the old way, and a toggle sitting on MUSIC while a suppressor eats the music is
 * indistinguishable from the bug this feature exists to fix — so the flag is only true when all
 * four agree, and each one has to be able to veto on its own.
 */
class AudioProfileResultTest {

    private fun result(
        noiseCancellation: Boolean = true,
        platformSuppressor: Boolean = true,
        softwareProcessing: Boolean = true,
        maxBitrate: Boolean = true,
    ) = AudioProfileResult(
        profile = AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY,
        audioMaxBitrateBps = 128_000,
        noiseCancellationApplied = noiseCancellation,
        platformNoiseSuppressorApplied = platformSuppressor,
        softwareAudioProcessingApplied = softwareProcessing,
        audioMaxBitrateApplied = maxBitrate,
    )

    @Test
    fun `all four stages applied is complete`() {
        assertTrue(result().complete)
    }

    @Test
    fun `the noise-cancellation processor alone can veto`() {
        assertFalse(result(noiseCancellation = false).complete)
    }

    @Test
    fun `the platform noise suppressor alone can veto`() {
        assertFalse(result(platformSuppressor = false).complete)
    }

    @Test
    fun `the software audio processing stage alone can veto`() {
        assertFalse(result(softwareProcessing = false).complete)
    }

    @Test
    fun `the bitrate stage alone can veto`() {
        // The quietest failure of the four: nothing sounds different, the ceiling is just wrong.
        assertFalse(result(maxBitrate = false).complete)
    }
}
