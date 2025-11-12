package io.getstream.video.android.core.moderations



import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.call.video.BitmapVideoFilter
import io.getstream.video.android.core.call.video.DefaultModerationVideoFilter
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test


class ModerationManagerTest {

    private lateinit var call: Call
    private lateinit var moderationManager: ModerationManager

    @Before
    fun setup() {
        mockkObject(StreamVideo)

        call = mockk(relaxed = true)
        every { call.type } returns "default"

        moderationManager = ModerationManager(call)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `enableVideoModeration sets custom filter when provided`() {
        // Given
        val customFilter = mockk<BitmapVideoFilter>()

        // When
        moderationManager.enableVideoModeration(customFilter)

        // Then
        verify { call.videoFilter = customFilter }
    }

    @Test
    fun `enableVideoModeration sets default filter when no custom filter provided`() {
        // Given
        val defaultFilter = DefaultModerationVideoFilter()
        val moderationConfig = ModerationConfig(
            videoModerationConfig = VideoModerationConfig(
                enable = true,
                blurDuration = 20_000L,
                bitmapVideoFilter = defaultFilter
            )
        )

        val callServiceConfig = CallServiceConfig(moderationConfig = moderationConfig)

        val mockStreamVideo = mockk<StreamVideoClient>(relaxed = true)
        val mockStateStreamVideo = mockk<ClientState>(relaxed = true)
        val mockCallConfigRegistry = mockk<CallServiceConfigRegistry>(relaxed = true)

        every { StreamVideo.instance() } returns mockStreamVideo
        every { mockStreamVideo.state } returns mockStateStreamVideo
        every { mockStateStreamVideo.callConfigRegistry } returns mockCallConfigRegistry
        every { mockCallConfigRegistry.get(any()) } returns callServiceConfig

        // When
        moderationManager.enableVideoModeration()

        // Then
        verify { call.videoFilter = ofType(DefaultModerationVideoFilter::class) }

    }

    @Test
    fun `enableVideoModeration falls back to empty CallServiceConfig when instance is null`() {
        // Given
        every { StreamVideo.instanceOrNull() } returns null

        // When
        moderationManager.enableVideoModeration()

        // Then
        verify { call.videoFilter = any() }
    }

    @Test
    fun `disableVideoModeration sets videoFilter to null`() {
        // When
        moderationManager.disableVideoModeration()

        // Then
        verify { call.videoFilter = null }
    }
}
