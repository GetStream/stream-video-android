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

package io.getstream.video.android.pages

import androidx.test.uiautomator.By

class CallPage {
    companion object {
        val cameraEnabledToggle = LobbyPage.cameraEnabledToggle
        val cameraDisabledToggle = LobbyPage.cameraDisabledToggle
        val microphoneEnabledToggle = LobbyPage.microphoneEnabledToggle
        val microphoneDisabledToggle = LobbyPage.microphoneDisabledToggle
        val callSettingsOpenToggle = By.res("Stream_CallSettingsToggle_Open_true")
        val callSettingsClosedToggle = By.res("Stream_CallSettingsToggle_Open_false")
        val cameraPositionToggleFront = By.res("Stream_FlipCameraIcon_Front")
        val cameraPositionToggleBack = By.res("Stream_FlipCameraIcon_Back")
        val hangUpButton = By.res("Stream_HangUpButton")
        val participantsCountBadge = By.res("Stream_ParticipantsCountBadge")
        val participantMenu = By.res("Stream_ParticipantsMenuIcon")
        val cornerDraggableView = By.res("Stream_FloatingVideoView")
        val callViewButton = By.res("Stream_CallViewButton")
        val chatButton = By.res("Stream_ChatButton")
        val connectionQualityIndicator = By.res("Stream_ParticipantNetworkQualityIndicator")
        val callInfoView = By.res("Stream_CallInfoView") // duration, recording, reconnecting
        val recordingIcon = By.res("Stream_RecordingIcon")
        val videoView = By.res("Stream_VideoView")
        val videoViewWithMediaTrack = By.res("Stream_VideoViewWithMediaTrack")
        val raisedHand = By.res("TODO") // https://linear.app/stream/issue/AND-562

//        val participantEvent = By.res("") // not implemented in the demo app yet
//        val minimizedCallView = By.res("") // not implemented in the demo app yet
    }

    class AudioCall {
        companion object {
            val durationLabel = By.res("Stream_OngoingCallDurationLabel")
            val participantName = By.res("Stream_ParticipantInformation")
            val participantAvatar = By.res("Stream_UserAvatar")
        }
    }

    class ViewMenu {
        companion object {
            val dynamic = By.text("Dynamic")
            val spotlight = By.text("Spotlight")
            val grid = By.text("Grid")
        }
    }

    class RecordingButtons {
        companion object {
            val accept = By.text("Continue")
            val leave = By.text("Leave")
            val end = By.text("End")
            val cancel = By.text("Cancel")
        }
    }

    class SettingsMenu {
        companion object {
            val raiseHandButton = By.text("✋ Raise hand")
            val lowerHandButton = By.text("TODO") // https://linear.app/stream/issue/AND-562
            val defaultBackgroundEnabledToggle = By.res(
                "Stream_Background_Filled.AccountCircle_On",
            )
            val defaultBackgroundDisabledToggle = By.res(
                "Stream_Background_Filled.AccountCircle_Off",
            )
            val blurBackgroundEnabledToggle = By.res("Stream_Background_Filled.BlurOn_On")
            val blurBackgroundDisabledToggle = By.res("Stream_Background_Filled.BlurOn_Off")
            val imageBackgroundEnabledToggle = By.res("Stream_Background_Image_On")
            val imageBackgroundDisabledToggle = By.res("Stream_Background_Image_Off")
            val noiseCancellationButton = By.text("Noise cancellation")
            val startTranscriptionButton = By.text("Transcribe the call")
            val stopTranscriptionButton = By.text("Stop transcription")
            val startClosedCaptionButton = By.text("Start Closed Caption")
            val stopClosedCaptionButton = By.text("Stop Closed Caption")
        }
    }

    class ParticipantMenu {
        companion object {
            val avatar = By.res("Stream_ParticipantsListUserAvatar")
            val name = By.res("Stream_ParticipantsListUserName")
            val cameraEnabled = By.res("Stream_ParticipantsListUserCamera_Enabled_true")
            val cameraDisabled = By.res("Stream_ParticipantsListUserCamera_Enabled_false")
            val microphoneEnabled = By.res("Stream_ParticipantsListUserMicrophone_Enabled_true")
            val microphoneDisabled = By.res("Stream_ParticipantsListUserMicrophone_Enabled_false")
            val closeButton = By.res("Stream_ParticipantsListCloseButton")
        }
    }

    class ParticipantView {
        companion object {
            val actionsIcon = By.res("Stream_ParticipantActionsIcon")
            val pinActionIcon = By.text("Pin")
            val unpinActionIcon = By.res("Unpin")
            val name = By.res("Stream_ParticipantName")
            val networkQualityIndicator = By.res("Stream_ParticipantNetworkQualityIndicator")
            val microphoneEnabledIcon = By.res("Stream_ParticipantMicrophone_Enabled_true")
            val microphoneDisabledIcon = By.res("Stream_ParticipantMicrophone_Enabled_false")
            val videoView = CallPage.videoView
            val videoViewWithMediaTrack = CallPage.videoViewWithMediaTrack
            val spotlightView = By.res("Stream_SpotlightView")
            val gridView = By.res("Stream_GridView")
            val screenSharingLabel = By.res("Stream_ParticipantScreenSharingLabel")
            val screenSharingView = By.res("Stream_ParticipantsScreenSharingView")
        }
    }
}
