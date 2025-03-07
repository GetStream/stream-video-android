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

package io.getstream.video.android.uiautomator

import android.content.Intent
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import java.io.ByteArrayOutputStream

public fun UiDevice.startApp() {
    val intent = testContext.packageManager.getLaunchIntentForPackage(packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    testContext.startActivity(intent)
}

public fun UiDevice.stopApp() {
    executeShellCommand("pm clear $packageName")
}

public fun UiDevice.typeText(text: String) {
    executeShellCommand("input text '$text'")
}

public fun UiObject2.typeText(text: String): UiObject2 {
    this.text = text
    return this
}

public fun UiObject2.longPress(steps: Int = 100) {
    val centerX = this.visibleBounds.centerX()
    val centerY = this.visibleBounds.centerY()
    io.getstream.video.android.uiautomator.device.swipe(centerX, centerY, centerX, centerY, steps)
}

public fun UiDevice.swipeDown(times: Int = 1, steps: Int = 10) {
    repeat(times) {
        val middleOfTheScreenHorizontally = displayWidth / 2
        val middleOfTheScreenVertically = displayHeight / 2
        swipe(
            middleOfTheScreenHorizontally,
            middleOfTheScreenVertically,
            middleOfTheScreenHorizontally,
            displayHeight,
            steps,
        )
    }
}

public fun UiDevice.swipeUp(times: Int = 1, steps: Int = 10) {
    repeat(times) {
        val middleOfTheScreenHorizontally = displayWidth / 2
        val middleOfTheScreenVertically = displayHeight / 2
        swipe(
            middleOfTheScreenHorizontally,
            middleOfTheScreenVertically,
            middleOfTheScreenHorizontally,
            0,
            steps,
        )
    }
}

public fun UiDevice.tapOnScreenCenter() {
    io.getstream.video.android.uiautomator.device.click(
        io.getstream.video.android.uiautomator.device.displayWidth / 2,
        io.getstream.video.android.uiautomator.device.displayHeight / 2,
    )
}

public fun UiDevice.goToBackground() {
    io.getstream.video.android.uiautomator.device.pressHome()
    sleep(1000)
}

public fun UiDevice.goToForeground() {
    io.getstream.video.android.uiautomator.device.pressRecentApps()
    sleep(500)
    io.getstream.video.android.uiautomator.device.tapOnScreenCenter()
}

public fun UiDevice.enableInternetConnection() {
    executeShellCommand("svc data enable")
    executeShellCommand("svc wifi enable")
}

public fun UiDevice.disableInternetConnection() {
    executeShellCommand("svc data disable")
    executeShellCommand("svc wifi disable")
}

public fun UiDevice.dumpWindowHierarchy() {
    val outputStream = ByteArrayOutputStream()
    io.getstream.video.android.uiautomator.device.dumpWindowHierarchy(outputStream)
    println(outputStream.toString("UTF-8"))
}

public fun <T> UiDevice.retryOnStaleObjectException(retries: Int = 3, action: () -> T): T {
    repeat(retries - 1) {
        try {
            return action()
        } catch (e: StaleObjectException) {
            println(e)
            Thread.sleep(500)
        }
    }
    return action()
}
