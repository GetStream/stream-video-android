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
 * Represents the different states a livestream can be in, based on the current call state.
 *
 * This enum helps drive the UI and behavior of the livestream experience.
 */
public enum class LivestreamState {

    /**
     * The initial state before any attempt to join the livestream has been made.
     */
    INITIAL,

    /**
     * The state when the user has joined but the livestream hasn't officially started,
     * and they are in the backstage.
     */
    BACKSTAGE,

    /**
     * The livestream is actively live and visible to the audience.
     */
    LIVE,

    /**
     * The livestream has ended.
     */
    ENDED,

    /**
     * An error occurred while attempting to join or display the livestream.
     */
    ERROR,

    /**
     * Indicates that the user is in the process of joining the livestream.
     */
    JOINING,
}
