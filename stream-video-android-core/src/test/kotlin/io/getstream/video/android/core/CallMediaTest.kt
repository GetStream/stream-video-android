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

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CallMediaTest : IntegrationTestBase() {

    /**
     * Muting is tricky
     * - you can mute yourself
     * - a moderator can mute you (receive an event)
     *
     * Updating state and actual audio should be in 1 place
     * So they can't end up in different states
     *
     *
     */

    @Test
    fun `Camera API`() = runTest {
        call.camera.devices
        val cameraId = call.camera.devices.value.first()
        call.camera.flip()
        call.camera.disable()
        call.camera.enable()
        call.camera.status
        call.camera.select(cameraId)
        // TODO: how to connect the buildCameraCapturer?
        // TODO: how to send a new track when the camera changes?
    }

    @Test
    fun `Microphone API`() = runTest {
        // TODO: Maybe audio in/out is better?
        call.microphone.devices
        call.microphone.disable()
        call.microphone.enable()
        call.microphone.status
    }

    @Test
    fun `Speaker API`() = runTest {
        call.speaker.devices
        call.speaker.disable()
        call.speaker.enable()
        call.speaker.setVolume(100L)
        call.speaker.setSpeakerPhone(true)
        call.speaker.status
    }
}
