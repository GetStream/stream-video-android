/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

internal fun MacrobenchmarkScope.dogfoodingScenarios() {
    // -------------
    // Authenticate
    // -------------
    device.authenticateAndNavigateToHome()

    // -------------
    // JoinCall
    // -------------
    device.navigateFromJoinCallToLobby()

    // -------------
    // Lobby
    // -------------
    device.navigateFromLobbyToCall()

    // -------------
    // Call
    // -------------
    device.testCall()
}

internal fun UiDevice.authenticateAndNavigateToHome() {
    wait(Until.hasObject(By.res("authenticate")), 5_000)

    // Click the Authenticate button and login.
    waitForObject(By.res("authenticate"))?.click()

    waitForIdle()
}

internal fun UiDevice.navigateFromJoinCallToLobby() {
    wait(Until.hasObject(By.res("start_new_call")), 5_000)

    // wait for the Join Call button and navigate to the lobby screen by clicking.
    waitForObject(By.res("start_new_call"))?.click()

    waitForIdle()
}

internal fun UiDevice.navigateFromLobbyToCall() {
    waitForObject(By.text(getPermissionText()), 5_000)?.click()
    waitForObject(By.text(getPermissionText()), 5_000)?.click()

    wait(Until.hasObject(By.res("call_lobby")), 15_000)
    wait(Until.hasObject(By.res("participant_video_renderer")), 15_000)

    // wait for the Start Call button and navigate to the call screen by clicking.
    waitForObject(By.res("start_call"), 15_000)?.click()

    waitForIdle()
}

internal fun UiDevice.testCall() {
    wait(Until.hasObject(By.res("call_content")), 5_000)
    wait(Until.hasObject(By.res("video_renderer")), 5_000)
    waitForIdle()
}
