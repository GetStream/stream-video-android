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

private const val DEFAULT_CAPTIONS_AUTO_DISMISS_TIME_MS = 2700L
private const val DEFAULT_DUPLICATE_CAPTIONS_AUTO_CLEANUP_TIME_MS = 30_000L // Every 30 seconds

/**
 * Configuration for managing closed captions in the [ClosedCaptionManager].
 *
 * @param visibilityDurationMs The duration (in milliseconds) after which captions will be automatically removed.
 * Set to  [DEFAULT_CAPTIONS_AUTO_DISMISS_TIME_MS] by default.
 *
 * @param autoDismissCaptions Determines whether closed captions should be automatically dismissed after a delay.
 * If set to `false`, captions will remain visible indefinitely.
 *
 * @param maxVisibleCaptions The maximum number of closed captions to retain in the [ClosedCaptionManager.closedCaptions] flow.
 *
 */

data class ClosedCaptionsSettings(
    val visibilityDurationMs: Long = DEFAULT_CAPTIONS_AUTO_DISMISS_TIME_MS,
    val autoDismissCaptions: Boolean = true,
    val maxVisibleCaptions: Int = 2, // Default to keep the latest 2 captions
)

/**
 * Configuration for managing deduplication of captions in the [ClosedCaptionManager].
 *
 * @param duplicateCleanupFrequencyMs The duration (in milliseconds) after which [ClosedCaptionManager.seenKeys] will be automatically removed.
 * Set to  [DEFAULT_DUPLICATE_CAPTIONS_AUTO_CLEANUP_TIME_MS] by default.
 *
 * @param autoRemoveDuplicateCaptions Determines whether client sdk should be deduplicate closed captions or not
 *
 * @param captionSplitFactor Factor to determine how many items to clean (e.g., 2 means clean half)
 *
 */

data class ClosedCaptionDeduplicationConfig(
    val duplicateCleanupFrequencyMs: Long = DEFAULT_DUPLICATE_CAPTIONS_AUTO_CLEANUP_TIME_MS,
    val autoRemoveDuplicateCaptions: Boolean = true,
    val captionSplitFactor: Int = 2,
)
