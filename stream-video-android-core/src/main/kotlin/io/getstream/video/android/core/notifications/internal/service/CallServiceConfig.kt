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

package io.getstream.video.android.core.notifications.internal.service

import android.media.AudioAttributes
import io.getstream.video.android.core.moderations.ModerationConfig
import io.getstream.video.android.core.utils.isAndroid17OrHigher

// Constants
/** Marker for all the call types. */
internal const val ANY_MARKER = "ALL_CALL_TYPES"

// API
/**
 * Configuration class for the call foreground service.
 * @param serviceClass Name of the service to run
 *
 * @see CallServiceConfigRegistry
 * @see DefaultCallConfigurations
 *
 */
public data class CallServiceConfig(
    val runCallServiceInForeground: Boolean = true,
    val audioUsage: Int = AudioAttributes.USAGE_VOICE_COMMUNICATION,
    val serviceClass: Class<*> = CallService::class.java,
    val enableTelecom: Boolean = isAndroid17OrHigher(),
    val moderationConfig: ModerationConfig = ModerationConfig(),
)
