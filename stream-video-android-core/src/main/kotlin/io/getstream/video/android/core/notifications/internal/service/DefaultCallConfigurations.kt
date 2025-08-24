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
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.call.CallType

/**
 * Provides default configurations for different types of call services.
 *
 * The [DefaultCallConfigurations] object centralizes predefined configurations
 * for various call scenarios, such as livestreams, audio calls, and guest calls.
 * These configurations can be used as defaults or templates for call-related operations.
 */
object DefaultCallConfigurations {

    /**
     * The default configuration for call services.
     * This serves as a fallback configuration and uses the `CallService` class.
     */
    val default =
        CallServiceConfig().copy(serviceClass = if (isVoipEnabled()) TelecomVoipService::class.java else CallService::class.java)

    /**
     * The configuration for livestream calls.
     * Uses the [LivestreamCallService] class and runs the service in the foreground.
     */
    val livestream = CallServiceConfig(
        serviceClass = LivestreamCallService::class.java,
        runCallServiceInForeground = true,
    )

    /**
     * The configuration for livestream audio-only calls.
     * Uses the [LivestreamAudioCallService] class.
     */
    val livestreamAudioCall =
        CallServiceConfig(serviceClass = LivestreamAudioCallService::class.java)

    /**
     * The configuration for guest calls in livestreams.
     * Uses the [LivestreamViewerService] class with media audio usage.
     */
    val livestreamGuestCall = CallServiceConfig(
        audioUsage = AudioAttributes.USAGE_MEDIA,
        serviceClass = LivestreamViewerService::class.java,
    )

    /**
     * The configuration for audio-only calls.
     * Uses the [AudioCallService] class with voice communication audio usage.
     */
    val audioCall = CallServiceConfig(
        serviceClass = AudioCallService::class.java,
        audioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
    )

    /**
     * Retrieves a map of livestream call configurations.
     *
     * This includes configurations for any marker, default calls, and livestream calls.
     *
     * @return A map where the keys are [CallType] values and the values are [CallServiceConfig] instances.
     */
    fun getLivestreamCallServiceConfig(): Map<CallType, CallServiceConfig> {
        return mapOf(
            Pair(CallType.AnyMarker, default),
            Pair(CallType.Default, default),
            Pair(CallType.Livestream, livestream),
        )
    }

    /**
     * Retrieves a map of livestream guest call configurations.
     *
     * This includes configurations for any marker, audio calls, and livestream guest calls.
     * Modifies the livestream configuration to run in the foreground and use media audio.
     *
     * @return A map where the keys are [CallType` values and the values are [CallServiceConfig] instances.
     */
    fun getLivestreamGuestCallServiceConfig(): Map<CallType, CallServiceConfig> {
        return mapOf(
            Pair(CallType.AnyMarker, default),
            Pair(CallType.AudioCall, audioCall),
            Pair(CallType.Livestream, livestreamGuestCall),
        )
    }

    fun isVoipEnabled() = (StreamVideo.instanceOrNull() as? StreamVideoClient)?.telecomConfig != null
}
