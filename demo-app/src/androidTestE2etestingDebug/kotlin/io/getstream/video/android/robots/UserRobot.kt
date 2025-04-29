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

package io.getstream.video.android.robots

import androidx.test.uiautomator.Direction
import io.getstream.video.android.pages.CallDetailsPage
import io.getstream.video.android.pages.CallPage
import io.getstream.video.android.pages.DirectCallPage
import io.getstream.video.android.pages.DirectCallPage.Companion.audioCallButton
import io.getstream.video.android.pages.DirectCallPage.Companion.videoCallButton
import io.getstream.video.android.pages.LobbyPage
import io.getstream.video.android.uiautomator.defaultTimeout
import io.getstream.video.android.uiautomator.device
import io.getstream.video.android.uiautomator.findObject
import io.getstream.video.android.uiautomator.findObjects
import io.getstream.video.android.uiautomator.seconds
import io.getstream.video.android.uiautomator.typeText
import io.getstream.video.android.uiautomator.waitForText
import io.getstream.video.android.uiautomator.waitToAppear

class UserRobot {

    // Common actions

    fun sleep(timeOutMillis: Long = defaultTimeout): UserRobot {
        io.getstream.video.android.uiautomator.sleep(timeOutMillis)
        return this
    }

    fun pressBack(): UserRobot {
        device.pressBack()
        return this
    }

    // CallDetailsPage actions

    fun directCall(video: Boolean): UserRobot {
        CallDetailsPage.wheelIcon.waitToAppear().click()
        CallDetailsPage.directCallButton.findObject().click()
        DirectCallPage.sampleUser.waitToAppear().click()
        val callButton = if (video) videoCallButton else audioCallButton
        callButton.findObject().click()
        return this
    }

    fun joinCall(
        callId: String? = null,
        camera: UserControls? = null,
        microphone: UserControls? = null,
    ): UserRobot {
        enterLobby(callId)
        if (camera != null) {
            camera(camera, hard = true)
        }
        if (microphone != null) {
            microphone(microphone, hard = true)
        }
        joinCallFromLobby()
        waitForCallToStart()
        return this
    }

    fun enterLobby(callId: String? = null): UserRobot {
        if (callId != null) {
            CallDetailsPage.callIdInputField.waitToAppear().typeText(callId)
        }
        CallDetailsPage.joinCallButton.waitToAppear().click()
        waitForLobbyToOpen()
        return this
    }

    // LobbyPage actions

    fun joinCallFromLobby(): UserRobot {
        LobbyPage.joinCallButton.findObject().click()
        waitForCallToStart()
        return this
    }

    private fun waitForLobbyToOpen(): UserRobot {
        LobbyPage.closeButton.waitToAppear()
        return this
    }

    // CallPage actions

    fun camera(position: CameraPosition): UserRobot {
        CallPage.callInfoView.waitToAppear()
        val isFrontCamera = CallPage.cameraPositionToggleFront.findObjects().isNotEmpty()

        if (position == CameraPosition.BACK && isFrontCamera) {
            CallPage.cameraPositionToggleFront.findObject().click()
        } else if (position == CameraPosition.FRONT && !isFrontCamera) {
            CallPage.cameraPositionToggleBack.findObject().click()
        }

        return this
    }

    fun camera(
        action: UserControls,
        hard: Boolean = false, // When true, hard-toggles to ensure server state syncs properly
    ): UserRobot {
        val isEnabled = CallPage.cameraEnabledToggle.findObjects().isNotEmpty()

        when {
            hard -> when (action) {
                UserControls.ENABLE -> {
                    if (isEnabled) {
                        CallPage.cameraEnabledToggle.findObject().click()
                        CallPage.cameraDisabledToggle.waitToAppear().click()
                    } else {
                        CallPage.cameraDisabledToggle.findObject().click()
                    }
                }
                UserControls.DISABLE -> {
                    if (isEnabled) {
                        CallPage.cameraEnabledToggle.findObject().click()
                    } else {
                        CallPage.cameraDisabledToggle.findObject().click()
                        CallPage.cameraEnabledToggle.waitToAppear().click()
                    }
                }
            }
            action == UserControls.ENABLE && !isEnabled -> {
                CallPage.cameraDisabledToggle.findObject().click()
            }
            action == UserControls.DISABLE && isEnabled -> {
                CallPage.cameraEnabledToggle.findObject().click()
            }
        }

        return this
    }

    fun microphone(
        action: UserControls,
        hard: Boolean = false, // When true, hard-toggles to ensure server state syncs properly
    ): UserRobot {
        val isEnabled = CallPage.microphoneEnabledToggle.findObjects().isNotEmpty()

        when {
            hard -> when (action) {
                UserControls.ENABLE -> {
                    if (isEnabled) {
                        CallPage.microphoneEnabledToggle.findObject().click()
                        CallPage.microphoneDisabledToggle.waitToAppear().click()
                    } else {
                        CallPage.microphoneDisabledToggle.findObject().click()
                    }
                }
                UserControls.DISABLE -> {
                    if (isEnabled) {
                        CallPage.microphoneEnabledToggle.findObject().click()
                    } else {
                        CallPage.microphoneDisabledToggle.findObject().click()
                        CallPage.microphoneEnabledToggle.waitToAppear().click()
                    }
                }
            }
            action == UserControls.ENABLE && !isEnabled -> {
                CallPage.microphoneDisabledToggle.findObject().click()
            }
            action == UserControls.DISABLE && isEnabled -> {
                CallPage.microphoneEnabledToggle.findObject().click()
            }
        }

        return this
    }

    fun settings(action: UserControls): UserRobot {
        val isEnabled = CallPage.callSettingsOpenToggle.findObjects().isNotEmpty()

        if (action == UserControls.ENABLE && !isEnabled) {
            CallPage.callSettingsClosedToggle.findObject().click()
        } else if (action == UserControls.DISABLE && isEnabled) {
            CallPage.callSettingsOpenToggle.findObject().click()
        }

        return this
    }

    fun openChat(): UserRobot {
        CallPage.chatButton.waitToAppear().click()
        return this
    }

    fun closeChat(): UserRobot {
        CallPage.Chat.closeButton.waitToAppear().click()
        return this
    }

    fun endCall(): UserRobot {
        CallPage.hangUpButton.waitToAppear().click()
        return this
    }

    fun setView(mode: VideoView): UserRobot {
        CallPage.callViewButton.waitToAppear().click()
        when (mode) {
            VideoView.DYNAMIC -> CallPage.ViewMenu.dynamic.waitToAppear().click()
            VideoView.SPOTLIGHT -> CallPage.ViewMenu.spotlight.waitToAppear().click()
            VideoView.GRID -> CallPage.ViewMenu.grid.waitToAppear().click()
        }
        return this
    }

    fun moveCornerDraggableView(direction: Direction): UserRobot {
        CallPage.cornerDraggableView.waitToAppear().swipe(direction, 1.0f)
        return this
    }

    fun acceptCallRecording(): UserRobot {
        CallPage.RecordingButtons.accept.waitToAppear(timeOutMillis = 10.seconds).click()
        return this
    }

    fun declineCallRecording(): UserRobot {
        CallPage.RecordingButtons.leave.waitToAppear(timeOutMillis = 10.seconds).click()
        return this
    }

    fun waitForParticipantsToJoin(count: Int = 1, timeOutMillis: Long = 30.seconds): UserRobot {
        val user = 1
        val participants = user + count
        CallPage.participantsCountBadge
            .waitToAppear()
            .waitForText(expectedText = participants.toString(), timeOutMillis = timeOutMillis)
        return this
    }

    private fun waitForCallToStart(): UserRobot {
        CallPage.callInfoView.waitToAppear()
        return this
    }
}

enum class UserControls {
    ENABLE, DISABLE
}

enum class CameraPosition {
    BACK, FRONT
}

enum class VideoView {
    DYNAMIC, SPOTLIGHT, GRID
}
