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

package io.getstream.video.android.benchmark

import android.os.Build
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/**
 * Convenience parameter to use proper package name with regards to build type and build flavor.
 */
val PACKAGE_NAME = buildString {
    append("io.getstream.video.android")
    append(BuildConfig.APP_FLAVOR_SUFFIX)
}

internal fun UiDevice.waitForObject(selector: BySelector, timeout: Long = 5_000): UiObject2? {
    if (wait(Until.hasObject(selector), timeout)) {
        return findObject(selector)
    }
    return null
}

internal fun getPermissionText(): String {
    return when {
        Build.VERSION.SDK_INT <= 28 -> "ALLOW"
        Build.VERSION.SDK_INT == 29 -> "Allow only while using the app"
        else -> "While using the app"
    }
}
