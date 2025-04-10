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

import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

public fun sleep(timeOutMillis: Long = io.getstream.video.android.uiautomator.defaultTimeout) {
    Thread.sleep(timeOutMillis)
}

public fun BySelector.waitToAppear(timeOutMillis: Long = io.getstream.video.android.uiautomator.defaultTimeout): UiObject2 {
    wait(timeOutMillis)
    return findObject()
}

public fun BySelector.waitToAppear(withIndex: Int, timeOutMillis: Long = io.getstream.video.android.uiautomator.defaultTimeout): UiObject2 {
    wait(timeOutMillis)
    return findObjects()[withIndex]
}

public fun BySelector.wait(timeOutMillis: Long = io.getstream.video.android.uiautomator.defaultTimeout): BySelector {
    io.getstream.video.android.uiautomator.device.wait(Until.hasObject(this), timeOutMillis)
    return this
}

public fun BySelector.waitToDisappear(timeOutMillis: Long = io.getstream.video.android.uiautomator.defaultTimeout): BySelector {
    io.getstream.video.android.uiautomator.device.wait(Until.gone(this), timeOutMillis)
    return this
}

public fun UiObject2.waitForText(
    expectedText: String,
    mustBeEqual: Boolean = true,
    timeOutMillis: Long = io.getstream.video.android.uiautomator.defaultTimeout,
): UiObject2 {
    val endTime = System.currentTimeMillis() + timeOutMillis
    var textPresent = false
    while (!textPresent && System.currentTimeMillis() < endTime) {
        textPresent = if (mustBeEqual) text == expectedText else text.contains(expectedText)
    }
    return this
}

public fun BySelector.waitForCount(count: Int, timeOutMillis: Long = io.getstream.video.android.uiautomator.defaultTimeout): List<UiObject2> {
    val endTime = System.currentTimeMillis() + timeOutMillis
    var elements: List<UiObject2> = emptyList()
    var success = false
    while (!success && System.currentTimeMillis() < endTime) {
        elements = findObjects()
        success = elements.size == count
    }
    return elements
}

public fun UiObject2.waitForTextToChange(text: String, timeOutMillis: Long = io.getstream.video.android.uiautomator.defaultTimeout): UiObject2 {
    val endTime = System.currentTimeMillis() + timeOutMillis
    var textChanged = false
    while (!textChanged && System.currentTimeMillis() < endTime) {
        textChanged = this.text != text
    }
    return this
}
