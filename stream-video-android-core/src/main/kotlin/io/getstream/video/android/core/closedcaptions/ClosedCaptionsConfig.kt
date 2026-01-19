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

package io.getstream.video.android.core.closedcaptions

private const val DEFAULT_CAPTIONS_AUTO_DISMISS_TIME_MS = 6000L

/**
 * Configuration for managing closed captions in the [ClosedCaptionManager].
 *
 * @param captionsAutoDismissTime The duration (in milliseconds) after which captions will be automatically removed.
 * Set to  [DEFAULT_CAPTIONS_AUTO_DISMISS_TIME_MS] by default.
 *
 * @param autoDismissCaptions Determines whether closed captions should be automatically dismissed after a delay.
 * If set to `false`, captions will remain visible indefinitely.
 *
 * @param maxCaptions The maximum number of closed captions to retain in the [ClosedCaptionManager.closedCaptions] flow.
 * Must be greater than or equal to [io.getstream.video.android.compose.ui.components.closedcaptions.ClosedCaptionsThemeConfig.maxVisibleCaptions]
 * to ensure the UI has sufficient data to render.
 *
 */

data class ClosedCaptionsConfig(
    val captionsAutoDismissTime: Long = DEFAULT_CAPTIONS_AUTO_DISMISS_TIME_MS,
    val autoDismissCaptions: Boolean = true,
    val maxCaptions: Int = 3, // Default to keep the latest 3 captions
)
