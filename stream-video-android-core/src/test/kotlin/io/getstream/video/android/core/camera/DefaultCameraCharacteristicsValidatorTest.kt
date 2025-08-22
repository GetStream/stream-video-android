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

package io.getstream.video.android.core.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultCameraCharacteristicsValidatorTest {
    private lateinit var validator: DefaultCameraCharacteristicsValidator

    @Before
    fun setUp() {
        validator = DefaultCameraCharacteristicsValidator()
    }

    @Test
    fun `isUsable returns true when characteristics has BACKWARD_COMPATIBLE capability`() {
        // Given
        val characteristics = mockk<CameraCharacteristics>()
        every {
            characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        } returns intArrayOf(
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW,
        )

        // When
        val result = validator.isUsable(characteristics)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isUsable returns false when characteristics lacks BACKWARD_COMPATIBLE capability`() {
        // Given
        val characteristics = mockk<CameraCharacteristics>()
        every {
            characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        } returns intArrayOf(
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW,
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT,
        )

        // When
        val result = validator.isUsable(characteristics)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isUsable returns false when characteristics capabilities is null`() {
        // Given
        val characteristics = mockk<CameraCharacteristics>()
        every {
            characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        } returns null

        // When
        val result = validator.isUsable(characteristics)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isUsable returns false when characteristics capabilities is empty array`() {
        // Given
        val characteristics = mockk<CameraCharacteristics>()
        every {
            characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        } returns intArrayOf()

        // When
        val result = validator.isUsable(characteristics)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isUsable returns false when characteristics is null`() {
        // When
        val result = validator.isUsable(null)

        // Then
        assertFalse(result)
    }

    @Test
    fun `getLensFacing returns FRONT when characteristics has front facing lens`() {
        // Given
        val characteristics = mockk<CameraCharacteristics>()
        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_FRONT

        // When
        val result = validator.getLensFacing(characteristics)

        // Then
        assertEquals(CameraCharacteristics.LENS_FACING_FRONT, result)
    }

    @Test
    fun `getLensFacing returns BACK when characteristics has back facing lens`() {
        // Given
        val characteristics = mockk<CameraCharacteristics>()
        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_BACK

        // When
        val result = validator.getLensFacing(characteristics)

        // Then
        assertEquals(CameraCharacteristics.LENS_FACING_BACK, result)
    }

    @Test
    fun `getLensFacing returns EXTERNAL when characteristics has external lens`() {
        // Given
        val characteristics = mockk<CameraCharacteristics>()
        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_EXTERNAL

        // When
        val result = validator.getLensFacing(characteristics)

        // Then
        assertEquals(CameraCharacteristics.LENS_FACING_EXTERNAL, result)
    }

    @Test
    fun `getLensFacing returns null when characteristics lens facing is null`() {
        // Given
        val characteristics = mockk<CameraCharacteristics>()
        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns null

        // When
        val result = validator.getLensFacing(characteristics)

        // Then
        assertNull(result)
    }

    @Test
    fun `getLensFacing returns null when characteristics is null`() {
        // When
        val result = validator.getLensFacing(null)

        // Then
        assertNull(result)
    }

    @Test
    fun `isUsable returns true when BACKWARD_COMPATIBLE is only capability`() {
        // Given
        val characteristics = mockk<CameraCharacteristics>()
        every {
            characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        } returns intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)

        // When
        val result = validator.isUsable(characteristics)

        // Then
        assertTrue(result)
    }
}
