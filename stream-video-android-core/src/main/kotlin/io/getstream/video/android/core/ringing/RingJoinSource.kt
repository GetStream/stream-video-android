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

package io.getstream.video.android.core.ringing

/**
 * Which route pulled the caller into a ringing call.
 *
 * Two paths now reach the same join, and a join driven by a poll is itself evidence that a ring
 * event went missing. Without the distinction there is no way to measure how often the fallback
 * rescues a ring the websocket lost.
 */
internal enum class RingJoinSource(val value: String) {
    /** The ring outcome arrived as a websocket event. */
    WebSocket("ring-ws"),

    /** The ring outcome was read back after the websocket went quiet. */
    PollApi("ring-poll-api"),
}
