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

package io.getstream.video.android.robots

import io.getstream.video.android.pages.LobbyPage
import io.getstream.video.android.uiautomator.isDisplayed
import io.getstream.video.android.uiautomator.waitToAppear
import org.junit.Assert.assertTrue

fun UserRobot.assertLobby(microphone: Boolean, camera: Boolean): UserRobot {
    assertTrue(LobbyPage.closeButton.waitToAppear().isDisplayed())
    assertTrue(LobbyPage.joinCallButton.isDisplayed())

    if (microphone) {
        assertTrue(LobbyPage.microphoneEnabledToggle.waitToAppear().isDisplayed())
        assertTrue(LobbyPage.microphoneEnabledIcon.isDisplayed())
    } else {
        assertTrue(LobbyPage.microphoneDisabledToggle.waitToAppear().isDisplayed())
        assertTrue(LobbyPage.microphoneDisabledIcon.isDisplayed())
    }

    if (camera) {
        assertTrue(LobbyPage.cameraEnabledToggle.waitToAppear().isDisplayed())
        assertTrue(LobbyPage.cameraEnabledView.isDisplayed())
    } else {
        assertTrue(LobbyPage.cameraDisabledToggle.waitToAppear().isDisplayed())
        assertTrue(LobbyPage.cameraDisabledView.isDisplayed())
    }

    return this
}

fun UserRobot.assertParticipantsCountInLobby(count: Int): UserRobot {
    assertTrue(LobbyPage.callParticipantsCount(count).waitToAppear().isDisplayed())
    return this
}
