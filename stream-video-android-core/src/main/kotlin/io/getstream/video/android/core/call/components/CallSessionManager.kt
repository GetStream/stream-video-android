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

import io.getstream.video.android.core.call.RtcSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Owns the live RTC session state for a call and the bookkeeping shared across the
 * join / reconnect flows: the current [session], the participant [sessionId], the cached
 * SFU [location], reconnect attempt counters and connect/reconnect timestamps.
 *
 * Keeping this state in a single component gives the join, reconnect, connectivity and
 * lifecycle collaborators a single source of truth to depend on.
 */
internal class CallSessionManager() {
    private val _session: MutableStateFlow<RtcSession?> = MutableStateFlow(null)

    /**
     * The live RTC session that handles all real time communication for video and audio.
     * Read-only to collaborators — mutate exclusively through [setActiveSession] so there is a
     * single, unidirectional write path for the session.
     */
    val session: StateFlow<RtcSession?> = _session.asStateFlow()

    /** Sole write path for [session]: swaps in a new RTC session or clears it with `null`. */
    fun setActiveSession(rtcSession: RtcSession?) {
        _session.value = rtcSession
    }

    var sessionId = UUID.randomUUID().toString()

    val unifiedSessionId = UUID.randomUUID().toString()

    /** Cached SFU location used when (re)joining a call. */
    var location: String? = null

    /** Increment this only for REJOIN and MIGRATION strategies. */
    var nonFastReconnectAttempts = 0

    var connectStartTime = 0L
    var reconnectStartTime = 0L

    /** Seconds elapsed since [connectStartTime], for the connection-time telemetry. */
    fun connectionTimeSeconds(): Float = (System.currentTimeMillis() - connectStartTime) / 1000f

    /** Seconds elapsed since [reconnectStartTime], for the reconnection-time telemetry. */
    fun reconnectionTimeSeconds(): Float =
        (System.currentTimeMillis() - reconnectStartTime) / 1000f

    /**
     * Fast-reconnect deadline (in millis), updated at runtime from the SFU's
     * `fastReconnectDeadlineSeconds`. Written by the session observer and read by the
     * reconnect loop and the connectivity monitor, so it lives with the other shared
     * reconnect bookkeeping.
     */
    var reconnectDeadlineMillis: Int = 10_000
}
