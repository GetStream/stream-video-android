/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core

import io.getstream.log.taggedLogger
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaManagerTest : TestBase() {

    private val logger by taggedLogger("Test:MediaManagerTest")

    @Test
    fun `list devices`() = runTest {
        val mediaManager = MediaManagerImpl(context)
        val devices = mediaManager.camera.devices
        logger.d { devices.toString() }
    }

    @Test
    fun `start capture`() = runTest {
        val mediaManager = MediaManagerImpl(context)
        val devices = mediaManager.camera.devices
        val result = mediaManager.camera.select(devices.value.first())
    }

    @Test
    fun `disable camera`() = runTest {
        val mediaManager = MediaManagerImpl(context)
        mediaManager.camera.enable()
        mediaManager.camera.disable()
    }
}
