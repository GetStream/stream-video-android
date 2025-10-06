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

package io.getstream.video.android.core

import android.hardware.camera2.CameraCharacteristics
import io.getstream.video.android.core.camera.CameraCharacteristicsValidator
import io.getstream.webrtc.Camera2Enumerator
import io.getstream.webrtc.CameraEnumerationAndroid.CaptureFormat
import io.getstream.webrtc.EglBase
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class CameraManagerTest {

    @Test
    fun `Sort devices by resolution even if there is an error in one of the devices`() {
        // Given
        val failingId = "2"
        val successIdMaxRes = "1"
        val successIdMinRes = "3"
        val minFormat = CaptureFormat(100, 100, CaptureFormat.FramerateRange(25, 60))
        val maxFormat = CaptureFormat(1000, 1000, CaptureFormat.FramerateRange(25, 60))
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val elgContext = mockk<EglBase.Context>(relaxed = true)
        val cameraManagerSystem = mockk<android.hardware.camera2.CameraManager>(relaxed = true)
        val characteristics = mockk<CameraCharacteristics>(relaxed = true)
        val enumerator = mockk<Camera2Enumerator>(relaxed = true)
        every { cameraManagerSystem.cameraIdList } returns arrayOf("1", "2", "3")
        every { enumerator.getSupportedFormats(failingId) } throws IllegalArgumentException()
        every { enumerator.getSupportedFormats(successIdMinRes) } returns listOf(minFormat)
        every { enumerator.getSupportedFormats(successIdMaxRes) } returns listOf(maxFormat)
        every { cameraManagerSystem.getCameraCharacteristics(any()) } returns characteristics

        val cameraCharacteristicsValidator = mockk<CameraCharacteristicsValidator>(relaxed = true)
        every {
            cameraCharacteristicsValidator.getLensFacing(characteristics)
        } returns CameraCharacteristics.LENS_FACING_FRONT
        every { cameraCharacteristicsValidator.isUsable(characteristics) } returns true
        val cameraManager = CameraManager(mediaManager, elgContext, cameraCharacteristicsValidator)

        // When
        val actual = cameraManager.sortDevices(
            cameraManagerSystem.cameraIdList,
            cameraManagerSystem,
            enumerator,
        )
        // Then
        // Verify only two devices, failing ID is not included in the list
        assertEquals(2, actual.size)
        // Check that the actual IDs are correct and sort order is correct i.e. from min res to max.
        assertEquals(listOf(successIdMinRes, successIdMaxRes), actual.map { it.id })
    }
}
