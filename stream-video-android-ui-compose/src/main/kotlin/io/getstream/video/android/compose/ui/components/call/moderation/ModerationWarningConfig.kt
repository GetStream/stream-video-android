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

package io.getstream.video.android.compose.ui.components.call.moderation

/**
 * Configuration for the animation and visibility behavior of the Moderation Warning UI.
 *
 * @param displayTime The duration (in milliseconds) for which the moderation warning UI remains visible.
 * @param slideInDuration The duration (in milliseconds) of the slide-in animation when the warning UI appears.
 * @param slideOutDuration The duration (in milliseconds) of the slide-out animation when the warning UI disappears.
 *
 * See [io.getstream.video.android.compose.ui.components.call.moderation.ModerationUi] for implementation details.
 */
internal data class ModerationWarningAnimationConfig(
    val displayTime: Long = 5_000L,
    val slideInDuration: Int = 500,
    val slideOutDuration: Int = slideInDuration,
)

/**
 * Defines the textual content displayed in the Moderation Warning UI.
 *
 * @param title The title text shown at the top of the moderation warning.
 * @param message The message text displayed below the title, providing additional context.
 */
internal data class ModerationText(val title: String, val message: String)
