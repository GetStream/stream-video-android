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

package io.getstream.video.android.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.webrtc.MediaConstraints
import stream.video.sfu.models.AudioBitrateProfile

/**
 * Covers the two pure helpers a mid-call profile switch leans on: [targetAudioMaxBitrateBps],
 * which decides the number the encoder is asked for, and [buildAudioConstraints], which decides
 * what the rebuilt audio source is allowed to process.
 *
 * Three sources feed the bitrate, in order: what the SFU offered for the profile, the constants,
 * and what the SFU negotiated at join. That precedence is the whole point, and getting it wrong is
 * silent — the switch reports success and the audio sits at the wrong ceiling, which is the
 * customer's original complaint wearing a different hat.
 */
class AudioBitrateTargetTest {

    private val music = AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY
    private val voice = AudioBitrateProfile.AUDIO_BITRATE_PROFILE_VOICE_STANDARD_UNSPECIFIED

    @Test
    fun `the server's offer for the profile wins outright`() {
        // Whatever the server named for this profile is what a freshly created transceiver would
        // have been given, so a mid-call switch has no business inventing a different number.
        assertEquals(
            96_000,
            targetAudioMaxBitrateBps(
                music,
                serverBitrateBps = 96_000,
                negotiatedBitrateBps = 64_000,
            ),
        )
        assertEquals(
            48_000,
            targetAudioMaxBitrateBps(
                voice,
                serverBitrateBps = 48_000,
                negotiatedBitrateBps = 64_000,
            ),
        )
    }

    @Test
    fun `a zero from the server is the proto default, not an offer`() {
        // Wire proto3 gives an absent int32 a zero; asking the encoder for zero would mute it.
        assertEquals(
            128_000,
            targetAudioMaxBitrateBps(music, serverBitrateBps = 0, negotiatedBitrateBps = null),
        )
        assertEquals(
            64_000,
            targetAudioMaxBitrateBps(voice, serverBitrateBps = 0, negotiatedBitrateBps = null),
        )
    }

    @Test
    fun `music never drops below the music constant`() {
        assertEquals(
            128_000,
            targetAudioMaxBitrateBps(music, serverBitrateBps = null, negotiatedBitrateBps = 64_000),
        )
        assertEquals(
            128_000,
            targetAudioMaxBitrateBps(music, serverBitrateBps = null, negotiatedBitrateBps = null),
        )
    }

    @Test
    fun `music keeps a negotiated bitrate that is already higher`() {
        // A server that joined the call above the music constant is not talked back down.
        assertEquals(
            256_000,
            targetAudioMaxBitrateBps(
                music,
                serverBitrateBps = null,
                negotiatedBitrateBps = 256_000,
            ),
        )
    }

    @Test
    fun `voice restores what was negotiated at join`() {
        // Leaving music puts back what the SFU asked for, rather than guessing the constant.
        assertEquals(
            96_000,
            targetAudioMaxBitrateBps(voice, serverBitrateBps = null, negotiatedBitrateBps = 96_000),
        )
    }

    @Test
    fun `voice falls back to the constant with nothing negotiated`() {
        assertEquals(
            64_000,
            targetAudioMaxBitrateBps(voice, serverBitrateBps = null, negotiatedBitrateBps = null),
        )
        assertEquals(
            64_000,
            targetAudioMaxBitrateBps(voice, serverBitrateBps = null, negotiatedBitrateBps = 0),
        )
    }

    //region audio-source constraints

    private fun softwareProcessingIn(constraints: MediaConstraints): Boolean =
        constraints.optional.single { it.key == "googNoiseSuppression" }.value.toBoolean()

    @Test
    fun `the music profile builds constraints with the software processing off`() {
        assertFalse(softwareProcessingIn(buildAudioConstraints { music }))
    }

    @Test
    fun `a voice profile builds constraints with the software processing on`() {
        assertTrue(softwareProcessingIn(buildAudioConstraints { voice }))
    }

    @Test
    fun `no profile provider is treated as a voice profile`() {
        // The source can be built before anything has chosen a profile; the default has to be the
        // processed one, or every call would start with the music pipeline.
        assertTrue(softwareProcessingIn(buildAudioConstraints()))
    }

    @Test
    fun `all five goog constraints move together`() {
        // buildAudioConstraints takes them as one flag on purpose — splitting it (keeping AEC
        // under music, say) is a live open question, and this pins today's behaviour.
        val off = buildAudioConstraints(softwareAudioProcessingEnabled = false)
        val googs = off.optional.filter { it.key.startsWith("goog") }

        assertEquals(5, googs.size)
        assertTrue(googs.all { it.value == "false" })
    }

    //endregion
}
