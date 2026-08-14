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

package io.getstream.video.android.core.call.components

import kotlinx.coroutines.CoroutineScope
import stream.video.sfu.models.WebsocketReconnectStrategy

/**
 * Lets an RTC session ask the call to reconnect it.
 *
 * The session detects the conditions that require a reconnect — a fatal SFU API error, a strategy
 * pushed by the SFU, a health-check timeout, or a publisher validation failure — but it cannot
 * carry one out: a reconnect tears the session down and replaces it. This interface hands the
 * decision back to [CallReconnector] without the session having to know about it, which would
 * otherwise be a cycle since the reconnector is what creates sessions in the first place.
 */
internal interface ReconnectRequests {

    /**
     * Scope that outlives the requesting session. Reconnect work must be launched here rather than
     * on the session's own scope, which is cancelled as part of the reconnect it just requested.
     */
    val scope: CoroutineScope

    suspend fun reconnect(strategy: WebsocketReconnectStrategy, reason: String)

    suspend fun rejoin(reason: String)
}
