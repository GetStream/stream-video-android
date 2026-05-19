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

package io.getstream.video.android.core.sorting

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.call.CallType
import org.junit.Test

/**
 * Verifies the call-type → default sort preset mapping, mirroring React's
 * `CallType` → `sortParticipantsBy` association. Picked up by `CallState` on
 * construction; callers can still override at runtime.
 */
class CallTypeSortPresetTest {

    @Test
    fun `Default call type uses SortPreset Default`() {
        assertThat(CallType.Default.sortPreset).isEqualTo(SortPreset.Default)
    }

    @Test
    fun `Livestream call type uses SortPreset LivestreamOrAudioRoom`() {
        assertThat(CallType.Livestream.sortPreset).isEqualTo(SortPreset.LivestreamOrAudioRoom)
    }

    @Test
    fun `AudioCall call type falls back to SortPreset Default (1to1 audio is not livestream-like)`() {
        // CallType.AudioCall represents a 1:1 audio call, not the React/iOS audio_room
        // concept. Auto-applying LivestreamOrAudioRoom there would mis-sort the two
        // participants. Stays on Default until a dedicated AudioRoom call type lands.
        assertThat(CallType.AudioCall.sortPreset).isEqualTo(SortPreset.Default)
    }

    @Test
    fun `Custom call types default to SortPreset Default`() {
        val custom = CallType.CustomCallType("my_custom_type")
        assertThat(custom.sortPreset).isEqualTo(SortPreset.Default)
    }

    @Test
    fun `Unknown call type name resolves to null and CallState falls back to Default`() {
        // This is the resolution path CallState uses:
        //   CallType.fromName(call.type)?.sortPreset ?: SortPreset.Default
        val resolved = CallType.fromName("does_not_exist")?.sortPreset ?: SortPreset.Default
        assertThat(resolved).isEqualTo(SortPreset.Default)
    }

    @Test
    fun `fromName livestream resolves to Livestream call type with LivestreamOrAudioRoom preset`() {
        val resolved = CallType.fromName("livestream")?.sortPreset ?: SortPreset.Default
        assertThat(resolved).isEqualTo(SortPreset.LivestreamOrAudioRoom)
    }
}
