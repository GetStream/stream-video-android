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

package io.getstream.video.android.core.e2ee

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.webrtc.EncryptionManager
import stream.video.sfu.models.TrackType

/**
 * Guards the hand-written mappings between the SDK's public E2EE enums and WebRTC's.
 *
 * These exist because the public API should not force apps onto `org.webrtc` types, but that means
 * a WebRTC bump adding an enum constant leaves the mapping silently incomplete. Asserting over
 * `values()` on both sides makes that a test failure instead of a runtime surprise.
 */
class E2EENativeMappingTest {

    @Test
    fun `every native track type maps to a public one`() {
        val mapped = EncryptionManager.TrackType.values().map { it.toE2EETrackType() }

        assertThat(mapped).containsExactlyElementsIn(E2EETrackType.values())
    }

    @Test
    fun `every public track type round-trips through the native one`() {
        E2EETrackType.values().forEach { trackType ->
            assertThat(trackType.toNativeTrackType().toE2EETrackType()).isEqualTo(trackType)
        }
    }

    @Test
    fun `every native algorithm maps to a public one`() {
        val mapped = EncryptionManager.Algorithm.values().map { it.toE2EEAlgorithm() }

        assertThat(mapped).containsExactlyElementsIn(E2EEAlgorithm.values())
    }

    @Test
    fun `every public algorithm round-trips through the native one`() {
        E2EEAlgorithm.values().forEach { algorithm ->
            assertThat(algorithm.toNativeAlgorithm().toE2EEAlgorithm()).isEqualTo(algorithm)
        }
    }

    @Test
    fun `every native event type maps to a known public one`() {
        val mapped = EncryptionManager.E2eeEventType.values().map { it.toE2EEEventType() }

        // UNKNOWN is reserved for constants a newer WebRTC adds, so nothing known may land on it.
        assertThat(mapped).doesNotContain(E2EEEventType.UNKNOWN)
        assertThat(mapped).containsExactlyElementsIn(
            E2EEEventType.values().toList() - E2EEEventType.UNKNOWN,
        )
    }

    @Test
    fun `every SFU track type except unspecified maps to a public one`() {
        val mapped = TrackType.values().mapNotNull { it.toE2EETrackType() }

        assertThat(mapped).containsExactlyElementsIn(E2EETrackType.values())
        assertThat(TrackType.TRACK_TYPE_UNSPECIFIED.toE2EETrackType()).isNull()
    }
}
