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
import io.getstream.video.android.core.moderations.ModerationConfig

class CallServiceConfigBuilder {
    private var serviceClass: Class<*> = CallService::class.java
    private var runCallServiceInForeground: Boolean = true
    private var audioUsage: Int = AudioAttributes.USAGE_VOICE_COMMUNICATION
    private var enableTelecom: Boolean = false
    private var moderationConfig: ModerationConfig = ModerationConfig()

    fun setServiceClass(serviceClass: Class<*>): CallServiceConfigBuilder = apply {
        this.serviceClass = serviceClass
    }

    fun setRunCallServiceInForeground(flag: Boolean): CallServiceConfigBuilder = apply {
        this.runCallServiceInForeground = flag
    }

    fun setAudioUsage(audioUsage: Int): CallServiceConfigBuilder = apply {
        this.audioUsage = audioUsage
    }

    fun enableTelecom(enableTelecom: Boolean): CallServiceConfigBuilder = apply {
        this.enableTelecom = enableTelecom
    }

    fun setModerationConfig(moderationConfig: ModerationConfig): CallServiceConfigBuilder = apply {
        this.moderationConfig = moderationConfig
    }

    fun build(): CallServiceConfig {
        return CallServiceConfig(
            serviceClass = serviceClass,
            runCallServiceInForeground = runCallServiceInForeground,
            audioUsage = audioUsage,
            enableTelecom = enableTelecom,
            moderationConfig = moderationConfig,
        )
    }
}
