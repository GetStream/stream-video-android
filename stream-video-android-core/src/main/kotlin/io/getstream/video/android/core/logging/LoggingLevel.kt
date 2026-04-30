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

package io.getstream.video.android.core.logging

import io.getstream.log.Priority
import org.webrtc.Logging

/**
 * Represents and wraps SDK logging levels for Stream logger, HTTP interceptor and native WebRTC logging.
 *
 * @property priority The priority level of information logged by the Stream Android logger.
 * @property httpLoggingLevel The level of information logged by our HTTP interceptor.
 * @property webRtcLoggingLevel The minimum severity for native WebRTC log messages delivered to
 *   the injectable logger. Defaults to [Logging.Severity.LS_WARNING] to avoid the heavy string
 *   allocation caused by codec-negotiation VERBOSE messages, which compounds under reconnect
 *   storms. Set to [Logging.Severity.LS_VERBOSE] when debugging WebRTC internals.
 */
public data class LoggingLevel @JvmOverloads constructor(
    public val priority: Priority = Priority.ERROR,
    public val httpLoggingLevel: HttpLoggingLevel = HttpLoggingLevel.BASIC,
    public val webRtcLoggingLevel: Logging.Severity = Logging.Severity.LS_NONE,
) {
    // Restores the binary-compatible copy(Priority, HttpLoggingLevel) signature that existed
    // before webRtcLoggingLevel was added.
    public fun copy(
        priority: Priority,
        httpLoggingLevel: HttpLoggingLevel,
    ): LoggingLevel = LoggingLevel(priority, httpLoggingLevel, this.webRtcLoggingLevel)
}
