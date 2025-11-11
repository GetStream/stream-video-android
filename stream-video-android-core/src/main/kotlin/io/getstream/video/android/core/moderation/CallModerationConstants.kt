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

package io.getstream.video.android.core.moderation

object CallModerationConstants {
    const val POLICY_VIOLATION = "PolicyViolationModeration"
    const val DEFAULT_MODERATION_DISPLAY_TIME_MS = 5_000L
    const val DEFAULT_MODERATION_AUTO_DISMISS_TIME_MS = DEFAULT_MODERATION_DISPLAY_TIME_MS + 3_000L
    const val DEFAULT_BLUR_AUTO_DISMISS_TIME_MS = 10_000L
}
