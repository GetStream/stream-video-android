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

package io.getstream.video.android.core.api

import android.content.Intent
import androidx.compose.runtime.Stable
import io.getstream.video.android.core.DeviceStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages screen sharing for a video call.
 *
 * Provides controls for enabling/disabling screen sharing and observing its state.
 * This is a cross-cutting concern that requires access to the call session for
 * track management.
 */
@Stable
public interface ScreenShareManager {

    /** The status of screen sharing (enabled, disabled, or not selected). */
    public val status: StateFlow<DeviceStatus>

    /** Whether screen sharing is currently enabled. */
    public val isEnabled: StateFlow<Boolean>

    /** Whether screen share audio capture is enabled. */
    public val audioEnabled: StateFlow<Boolean>

    /**
     * Enables screen sharing.
     *
     * @param mediaProjectionPermissionResultData The intent data from the screen capture permission result.
     * @param fromUser Whether this was triggered by a user action.
     * @param includeAudio Whether to include audio in the screen share.
     */
    public fun enable(
        mediaProjectionPermissionResultData: Intent,
        fromUser: Boolean = true,
        includeAudio: Boolean = false,
    )

    /**
     * Disables screen sharing.
     *
     * @param fromUser Whether this was triggered by a user action.
     */
    public fun disable(fromUser: Boolean = true)
}
