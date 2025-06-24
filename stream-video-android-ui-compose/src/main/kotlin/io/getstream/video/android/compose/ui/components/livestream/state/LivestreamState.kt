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

package io.getstream.video.android.compose.ui.components.livestream.state

/**
 * Defines the various states of a livestream.
 *
 * @property INITIAL    Before attempting to join.
 * @property BACKSTAGE  Joined but the livestream hasn't officially started, they are in the backstage.
 * @property LIVE       Livestream is active.
 * @property ENDED      Livestream has finished.
 * @property ERROR      Error joining or displaying the livestream.
 * @property JOINING    In the process of joining the livestream.
 */
public enum class LivestreamState {
    INITIAL,
    BACKSTAGE,
    LIVE,
    ENDED,
    ERROR,
    JOINING,
}
