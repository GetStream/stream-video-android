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

package io.getstream.video.android.core.notifications.internal.service

import android.media.AudioAttributes
import io.getstream.video.android.model.StreamCallId

// Constants
/** Marker for all the call types. */
internal const val ANY_MARKER = "ALL_CALL_TYPES"

// API
/**
 * Configuration class for the call foreground service.
 * @param runCallServiceInForeground If the call service should run in the foreground.
 * @param callServicePerType A map of call service per type.
 *
 * @see callServiceConfig
 * @see livestreamCallServiceConfig
 * @see livestreamAudioCallServiceConfig
 * @see livestreamGuestCallServiceConfig
 * @see audioCallServiceConfig
 */
public data class CallServiceConfig(
    val runCallServiceInForeground: Boolean = true,
    val audioUsage: Int = AudioAttributes.USAGE_VOICE_COMMUNICATION,
    val callServicePerType: Map<String, Class<*>> = mapOf(
        Pair(ANY_MARKER, CallService::class.java),
    ),
    val configPerType: Map<String, CallTypeConfig> = mapOf(
        ANY_MARKER to CallTypeConfig(
            runCallServiceInForeground = false,
            audioUsage = AudioAttributes.USAGE_NOTIFICATION,
        ),
    ),
)

public data class CallTypeConfig(
    val callServiceClass: Class<*> = CallService::class.java,
    val runCallServiceInForeground: Boolean = true,
    val audioUsage: Int = AudioAttributes.USAGE_VOICE_COMMUNICATION,
)

/**
 * Returns the default call foreground service configuration.
 * Uses: `FOREGROUND_SERVICE_TYPE_PHONE_CALL`.
 */
public fun callServiceConfig(): CallServiceConfig {
    return CallServiceConfig()
}

/**
 * Returns a foreground service configuration appropriate for livestream hosts.
 * Uses: `FOREGROUND_SERVICE_TYPE_CAMERA` and `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
 */
public fun livestreamCallServiceConfig(): CallServiceConfig {
    return CallServiceConfig(
        runCallServiceInForeground = true,
        callServicePerType = mapOf(
            Pair(ANY_MARKER, CallService::class.java),
            Pair("livestream", LivestreamCallService::class.java),
        ),
        configPerType = mapOf(
            ANY_MARKER to CallTypeConfig(),
            "livestream" to CallTypeConfig(
                callServiceClass = LivestreamCallService::class.java,
                runCallServiceInForeground = true,
                audioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
            ),
        ),
    )
}

/**
 * Returns a foreground service configuration appropriate for audio-only livestream hosts.
 * Uses: `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
 */
public fun livestreamAudioCallServiceConfig(): CallServiceConfig {
    return CallServiceConfig(
        runCallServiceInForeground = true,
        callServicePerType = mapOf(
            Pair(ANY_MARKER, CallService::class.java),
            Pair("livestream", LivestreamAudioCallService::class.java),
        ),
    )
}

/**
 * Returns a foreground service configuration appropriate for livestream viewers.
 * Uses: `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
 */
public fun livestreamGuestCallServiceConfig(): CallServiceConfig {
    return CallServiceConfig(
        runCallServiceInForeground = true,
        audioUsage = AudioAttributes.USAGE_MEDIA,
        callServicePerType = mapOf(
            Pair(ANY_MARKER, LivestreamViewerService::class.java),
            Pair("livestream", LivestreamViewerService::class.java),
        ),
    )
}

/**
 * Returns a foreground service configuration appropriate for audio-only calls.
 * Uses: `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
 */
public fun audioCallServiceConfig(): CallServiceConfig {
    return CallServiceConfig(
        runCallServiceInForeground = true,
        callServicePerType = mapOf(
            Pair(ANY_MARKER, CallService::class.java),
            Pair("audio_call", AudioCallService::class.java),
        ),
    )
}

// Internal
internal fun resolveServiceClass(callId: StreamCallId, config: CallServiceConfig): Class<*> {
    val callType = callId.type
    val resolvedServiceClass = config.callServicePerType[callType]
    return resolvedServiceClass ?: config.callServicePerType[ANY_MARKER] ?: CallService::class.java
}

internal fun resolveCallTypeConfig(callType: String?, config: CallServiceConfig): CallTypeConfig {
    val resolvedConfig = config.configPerType[callType]
    return resolvedConfig ?: config.configPerType[ANY_MARKER] ?: CallTypeConfig()
}
