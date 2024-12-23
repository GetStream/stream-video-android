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
 * @param configs The configuration for each call type.
 *
 * @see callServiceConfig
 * @see livestreamCallServiceConfig
 * @see livestreamAudioCallServiceConfig
 * @see livestreamGuestCallServiceConfig
 * @see audioCallServiceConfig
 */
public data class CallServiceConfig(
    // Kept for tests
    internal val runCallServiceInForeground: Boolean = true,
    internal val audioUsage: Int = AudioAttributes.USAGE_VOICE_COMMUNICATION,
    internal val callServicePerType: Map<String, Class<*>> = mapOf(
        Pair(ANY_MARKER, CallService::class.java),
    ),
    val configs: MutableMap<String, CallTypeServiceConfig> = mutableMapOf(
        Pair(ANY_MARKER, CallTypeServiceConfig()),
    ),
)

public data class CallTypeServiceConfig(
    val serviceClass: Class<*> = CallService::class.java,
    val runCallServiceInForeground: Boolean = true,
    val audioUsage: Int = AudioAttributes.USAGE_VOICE_COMMUNICATION,
)

/**
 * Returns the default call foreground service configuration.
 * Uses: `FOREGROUND_SERVICE_TYPE_PHONE_CALL`.
 */
public fun callServiceConfig(): CallServiceConfig {
    return CallServiceConfig(
        configs = mutableMapOf(
            Pair(ANY_MARKER, CallTypeServiceConfig()),
        ),
    )
}

/**
 * Returns a foreground service configuration appropriate for livestream hosts.
 * Uses: `FOREGROUND_SERVICE_TYPE_CAMERA` and `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
 */
public fun livestreamCallServiceConfig(): CallServiceConfig {
    return CallServiceConfig(
        configs = mutableMapOf(
            Pair(ANY_MARKER, CallTypeServiceConfig()),
            Pair("default", CallTypeServiceConfig()),
            Pair("livestream", CallTypeServiceConfig(LivestreamCallService::class.java)),
        ),
    )
}

/**
 * Returns a foreground service configuration appropriate for audio-only livestream hosts.
 * Uses: `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
 */
public fun livestreamAudioCallServiceConfig(): CallServiceConfig {
    return CallServiceConfig(
        configs = mutableMapOf(
            Pair(ANY_MARKER, CallTypeServiceConfig()),
            Pair("default", CallTypeServiceConfig()),
            Pair("livestream", CallTypeServiceConfig(LivestreamAudioCallService::class.java)),
        ),
    )
}

/**
 * Returns a foreground service configuration appropriate for livestream viewers.
 * Uses: `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
 */
public fun livestreamGuestCallServiceConfig(): CallServiceConfig {
    return CallServiceConfig(
        configs = mutableMapOf(
            Pair(ANY_MARKER, CallTypeServiceConfig()),
            Pair("default", CallTypeServiceConfig()),
            Pair("audio_call", CallTypeServiceConfig(AudioCallService::class.java)),
            Pair(
                "livestream",
                CallTypeServiceConfig(
                    serviceClass = LivestreamViewerService::class.java,
                    runCallServiceInForeground = false,
                    audioUsage = AudioAttributes.USAGE_MEDIA,
                ),
            ),
        ),
    )
}

/**
 * Returns a foreground service configuration appropriate for audio-only calls.
 * Uses: `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
 */
public fun audioCallServiceConfig(): CallServiceConfig {
    return CallServiceConfig(
        configs = mutableMapOf(
            Pair(ANY_MARKER, CallTypeServiceConfig()),
            Pair("audio_call", CallTypeServiceConfig(AudioCallService::class.java)),
        ),
    )
}

/**
 * Updates the configuration for the given call type.
 * @param callType The call type to update.
 * @param runCallServiceInForeground Whether to start the foreground service.
 * @param audioUsage The audio usage for the call service, e.g. [AudioAttributes.USAGE_VOICE_COMMUNICATION] or [AudioAttributes.USAGE_MEDIA].
 */
fun CallServiceConfig.update(
    callType: String,
    runCallServiceInForeground: Boolean? = null,
    audioUsage: Int? = null,
): CallServiceConfig {
    val config = configs[callType]

    config?.let {
        configs[callType] = config.copy(
            runCallServiceInForeground = runCallServiceInForeground ?: config.runCallServiceInForeground,
            audioUsage = audioUsage ?: config.audioUsage,
        )
    }

    return this
}

// Internal
internal fun resolveServiceClass(callId: StreamCallId, config: CallServiceConfig): Class<*> {
    // Kept for tests
    val callType = callId.type
    val resolvedServiceClass = config.callServicePerType[callType]
    return resolvedServiceClass ?: config.callServicePerType[ANY_MARKER] ?: CallService::class.java
}

internal fun CallServiceConfig.resolveServiceClass(callType: String): Class<*> {
    return resolveCallTypeServiceConfig(callType, this).serviceClass
}

internal fun CallServiceConfig.resolveRunCallServiceInForeground(callType: String): Boolean {
    return resolveCallTypeServiceConfig(callType, this).runCallServiceInForeground
}

internal fun CallServiceConfig.resolveAudioUsage(callType: String): Int {
    return resolveCallTypeServiceConfig(callType, this).audioUsage
}

internal fun resolveCallTypeServiceConfig(callType: String, config: CallServiceConfig): CallTypeServiceConfig {
    val resolvedConfig = config.configs[callType]
    return resolvedConfig ?: config.configs[ANY_MARKER] ?: CallTypeServiceConfig()
}
