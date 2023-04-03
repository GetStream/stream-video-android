package io.getstream.video.android.core

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaManagerTest : TestBase() {

    @Test
    fun `list devices`() = runTest {
        val mediaManager = MediaManagerImpl(context)
        val devices = mediaManager.camera.devices()
        println(devices)
    }

    @Test
    fun `start capture`() = runTest {
        val mediaManager = MediaManagerImpl(context)
        val devices = mediaManager.camera.devices()
        val result = mediaManager.camera.select(devices.first())
    }

    @Test
    fun `disable camera`() = runTest {
        val mediaManager = MediaManagerImpl(context)
        mediaManager.camera.enable()
        mediaManager.camera.disable()
    }

}
