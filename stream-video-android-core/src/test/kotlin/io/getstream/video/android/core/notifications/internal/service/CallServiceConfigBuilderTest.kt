package io.getstream.video.android.core.notifications.internal.service

import android.media.AudioAttributes
import io.getstream.video.android.core.call.video.DefaultModerationVideoFilter
import io.getstream.video.android.core.moderations.ModerationConfig
import io.getstream.video.android.core.moderations.VideoModerationConfig
import org.junit.Assert.*
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
                bitmapVideoFilter = DefaultModerationVideoFilter()
            )
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
