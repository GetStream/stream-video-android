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
import android.os.Build
import androidx.annotation.RequiresApi

internal class DefaultCameraCharacteristicsValidator : CameraCharacteristicsValidator {
    override fun isUsable(
        characteristics: CameraCharacteristics?,
        allowMono: Boolean,
        allowNir: Boolean,
    ): Boolean {
        val caps = characteristics?.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: return false

        // Must always support backward compatibility
        if (!caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // If camera is mono and not allowed → reject
            if (isMono(characteristics) && !allowMono) return false

            // If camera is IR and not allowed → reject
            if (isNir(characteristics) && !allowNir) return false
        }

        return true
    }

    override fun getLensFacing(characteristics: CameraCharacteristics?): Int? {
        return characteristics?.get(CameraCharacteristics.LENS_FACING)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isMono(characteristics: CameraCharacteristics): Boolean {
        val caps = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: return false
        if (!caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME)) return false

        val arrangement = characteristics.get(
            CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT,
        )
        return arrangement == CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isNir(characteristics: CameraCharacteristics): Boolean {
        val caps = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: return false
        if (!caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME)) return false

        val arrangement = characteristics.get(
            CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT,
        )
        return arrangement == CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_NIR
    }
}
