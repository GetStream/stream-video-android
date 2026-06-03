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

package io.getstream.video.android.core.call.connection.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.webrtc.RtpParameters
import stream.video.sfu.models.DegradationPreference

class DegradationPreferenceMapperTest {

    @Test
    fun `BALANCED maps to RtpParameters BALANCED`() {
        assertEquals(
            RtpParameters.DegradationPreference.BALANCED,
            DegradationPreference.DEGRADATION_PREFERENCE_BALANCED.toRtcDegradationPreference(),
        )
    }

    @Test
    fun `MAINTAIN_FRAMERATE maps to RtpParameters MAINTAIN_FRAMERATE`() {
        assertEquals(
            RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE,
            DegradationPreference.DEGRADATION_PREFERENCE_MAINTAIN_FRAMERATE
                .toRtcDegradationPreference(),
        )
    }

    @Test
    fun `MAINTAIN_RESOLUTION maps to RtpParameters MAINTAIN_RESOLUTION`() {
        assertEquals(
            RtpParameters.DegradationPreference.MAINTAIN_RESOLUTION,
            DegradationPreference.DEGRADATION_PREFERENCE_MAINTAIN_RESOLUTION
                .toRtcDegradationPreference(),
        )
    }

    @Test
    fun `MAINTAIN_FRAMERATE_AND_RESOLUTION maps to RtpParameters MAINTAIN_FRAMERATE_AND_RESOLUTION`() {
        assertEquals(
            RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE_AND_RESOLUTION,
            DegradationPreference
                .DEGRADATION_PREFERENCE_MAINTAIN_FRAMERATE_AND_RESOLUTION
                .toRtcDegradationPreference(),
        )
    }

    @Test
    fun `UNSPECIFIED maps to null so callers can keep the current value`() {
        assertNull(
            DegradationPreference.DEGRADATION_PREFERENCE_UNSPECIFIED.toRtcDegradationPreference(),
        )
    }
}
