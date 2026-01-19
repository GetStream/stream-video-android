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
import io.getstream.video.android.core.call.video.DefaultModerationVideoFilter
import io.getstream.video.android.core.moderations.ModerationConfig
import io.getstream.video.android.core.moderations.VideoModerationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallServiceConfigBuilderTest {

    @Test
    fun `build returns default CallServiceConfig when nothing is set`() {
        // When
        val config = CallServiceConfigBuilder().build()

        // Then
        assertEquals(CallService::class.java, config.serviceClass)
        assertTrue(config.runCallServiceInForeground)
        assertEquals(AudioAttributes.USAGE_VOICE_COMMUNICATION, config.audioUsage)
        assertFalse(config.enableTelecom)
        assertNotNull(config.moderationConfig)
    }

    @Test
    fun `build returns CallServiceConfig with custom values`() {
        // Given
        val customServiceClass = CustomCallService::class.java
        val customModerationConfig = ModerationConfig(
            videoModerationConfig = VideoModerationConfig(
                enable = true,
                blurDuration = 5000L,
                bitmapVideoFilter = DefaultModerationVideoFilter(),
            ),
        )

        // When
        val config = CallServiceConfigBuilder()
            .setServiceClass(customServiceClass)
            .setRunCallServiceInForeground(false)
            .setAudioUsage(AudioAttributes.USAGE_MEDIA)
            .enableTelecom(true)
            .setModerationConfig(customModerationConfig)
            .build()

        // Then
        assertEquals(customServiceClass, config.serviceClass)
        assertFalse(config.runCallServiceInForeground)
        assertEquals(AudioAttributes.USAGE_MEDIA, config.audioUsage)
        assertTrue(config.enableTelecom)
        assertEquals(customModerationConfig, config.moderationConfig)
    }

    @Test
    fun `builder should be reusable without affecting previous built instances`() {
        // Given
        val builder = CallServiceConfigBuilder()
            .setRunCallServiceInForeground(false)
        val firstConfig = builder.build()

        // When
        builder.enableTelecom(true)
        val secondConfig = builder.build()

        // Then
        assertFalse(firstConfig.enableTelecom) // unchanged
        assertTrue(secondConfig.enableTelecom)
    }

    private class CustomCallService
}
