/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.events

import stream.video.Call

/**
 * Represents the events coming in from the socket.
 */
public sealed class VideoEvent

/**
 * Triggered when a user gets connected to the WS
 */
public data class ConnectedEvent(
    val clientId: String,
) : VideoEvent()

public data class HealthCheckEvent(
    val clientId: String,
    val userId: String
) : VideoEvent()

public data class CallCreatedEvent(
    val call: Call
) : VideoEvent()

public object UnknownEvent : VideoEvent()

// TODO - rest of the events
