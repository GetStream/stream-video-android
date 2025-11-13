/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.moderations

import io.getstream.video.android.core.call.video.BitmapVideoFilter
import io.getstream.video.android.core.call.video.DefaultModerationVideoFilter

/**
 * Configuration for displaying moderation warnings during a call.
 *
 * @property enable Whether moderation warnings are enabled.
 * @property displayTime The duration (in milliseconds) for which the moderation warning should be visible.
 */
data class ModerationWarningConfig(val enable: Boolean, val displayTime: Long)

/**
 * Configuration for video moderation behavior during policy violations.
 *
 * @property enable Whether video moderation (e.g., blurring) is enabled.
 * @property blurDuration The duration (in milliseconds) for which the blur effect should remain active.
 * @property bitmapVideoFilter The [BitmapVideoFilter] used to apply the moderation effect.
 * By default, [DefaultModerationVideoFilter] is used.
 */
data class VideoModerationConfig(
    val enable: Boolean,
    val blurDuration: Long,
    val bitmapVideoFilter: BitmapVideoFilter = DefaultModerationVideoFilter(),
)

/**
 * Top-level configuration for all moderation features.
 *
 * This class allows customizing both warning and video moderation behavior.
 *
 * @property moderationWarningConfig Configuration for moderation warnings displayed to the user.
 * @property videoModerationConfig Configuration for video moderation (e.g., applying blur effects).
 *
 * Example usage:
 * ```
 * val moderationConfig = ModerationConfig(
 *     moderationWarningConfig = ModerationWarningConfig(enable = true, displayTime = 5000L),
 *     videoModerationConfig = VideoModerationConfig(enable = true, blurDuration = 20000L)
 * )
 * ```
 */
data class ModerationConfig(
    val moderationWarningConfig: ModerationWarningConfig = ModerationWarningConfig(
        true,
        5_000L,
    ),
    val videoModerationConfig: VideoModerationConfig = VideoModerationConfig(
        true,
        20_000L,
    ),
)
