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

package io.getstream.video.android.compose.theme

import io.getstream.video.android.core.mapper.ReactionMapper

/**
 * Central behavioral configuration for the Video SDK, accessible through [VideoTheme.config].
 *
 * Groups the behavior settings that are unrelated to styling, so [VideoTheme] keeps a small
 * parameter list of visual properties while feature behavior is configured in a single place.
 *
 * @param allowUIAutomationTest Whether the Compose test tags used by the SDK are exposed as
 * resource ids for UI automation tools.
 * @param reactionMapper Maps the emoji code from reaction events to the emoji shown in the UI.
 */
public data class VideoUiConfig(
    val allowUIAutomationTest: Boolean = true,
    val reactionMapper: ReactionMapper = ReactionMapper.defaultReactionMapper(),
)
