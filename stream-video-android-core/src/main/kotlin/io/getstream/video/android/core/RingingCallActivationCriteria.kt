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

package io.getstream.video.android.core

/**
 * Defines when a ringing call transitions to the active state.
 *
 * Controls how early or late the call UI becomes interactive based on
 * connection or media readiness.
 *
 * Note: Faster options may activate before media is fully available.
 */
public enum class RingingCallActivationCriteria {

    /** Uses existing SDK behavior (backward compatible). */
    LEGACY_BEHAVIOR,

    /** Activate when any peer connection is established. */
    ANY_PEER_CONNECTED,

    /** Activate when both publisher and subscriber connections are established. */
    BOTH_PEER_CONNECTED,

    /** Activate when publishing (outgoing media) is ready. */
    PUBLISHER_CONNECTED,

    /** Activate when subscribing (incoming media) is ready. */
    SUBSCRIBER_CONNECTED,

    /** Activate after the first media packet is received. */
    FIRST_PACKET_RECEIVED,
}
