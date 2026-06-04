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

import org.webrtc.RtpParameters
import stream.video.sfu.models.DegradationPreference as SfuDegradationPreference

/**
 * Converts the SFU's [SfuDegradationPreference] to the WebRTC
 * [RtpParameters.DegradationPreference] used by the publisher.
 *
 * Mirrors the JS SDK helper introduced in
 * https://github.com/GetStream/stream-video-js/pull/2241.
 *
 * Returns `null` when the SFU did not specify a preference
 * ([SfuDegradationPreference.DEGRADATION_PREFERENCE_UNSPECIFIED]) so callers can
 * keep the current value or apply their own default.
 */
internal fun SfuDegradationPreference.toRtcDegradationPreference():
    RtpParameters.DegradationPreference? = when (this) {
    SfuDegradationPreference.DEGRADATION_PREFERENCE_BALANCED ->
        RtpParameters.DegradationPreference.BALANCED
    SfuDegradationPreference.DEGRADATION_PREFERENCE_MAINTAIN_FRAMERATE ->
        RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE
    SfuDegradationPreference.DEGRADATION_PREFERENCE_MAINTAIN_RESOLUTION ->
        RtpParameters.DegradationPreference.MAINTAIN_RESOLUTION
    SfuDegradationPreference.DEGRADATION_PREFERENCE_MAINTAIN_FRAMERATE_AND_RESOLUTION ->
        RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE_AND_RESOLUTION
    SfuDegradationPreference.DEGRADATION_PREFERENCE_UNSPECIFIED -> null
}
