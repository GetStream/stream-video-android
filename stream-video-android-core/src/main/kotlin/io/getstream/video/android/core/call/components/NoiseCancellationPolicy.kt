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

import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.utils.isAutoOn
import io.getstream.video.android.core.utils.isEnabled

/**
 * Decides whether local noise cancellation may run for a call.
 *
 * Two server-driven things gate it: the `enable-noise-cancellation` capability granted to the
 * user, and the noise-cancellation mode configured on the call type.
 *
 * Decision only — applying it belongs to [io.getstream.video.android.core.Call].
 */
internal class NoiseCancellationPolicy {

    /**
     * True when noise cancellation may be enabled for this call.
     *
     * A null [settings] means the server has not told us yet. Callers must treat that as "not
     * decided" and wait, rather than as a refusal — deciding before settings resolve would switch
     * noise cancellation off and straight back on during join.
     */
    fun isAllowed(
        capabilities: List<OwnCapability>,
        settings: CallSettingsResponse?,
    ): Boolean {
        val noiseCancellation = settings?.audio?.noiseCancellation ?: return false
        if (!noiseCancellation.isEnabled) return false
        return capabilities.contains(OwnCapability.EnableNoiseCancellation)
    }

    /**
     * True when the call type asks for noise cancellation to start on, and it is allowed to.
     */
    fun isAutoOn(
        capabilities: List<OwnCapability>,
        settings: CallSettingsResponse?,
    ): Boolean {
        val noiseCancellation = settings?.audio?.noiseCancellation ?: return false
        return noiseCancellation.isAutoOn && isAllowed(capabilities, settings)
    }
}
