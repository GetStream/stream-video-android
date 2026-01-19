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

class RingPage {

    companion object {
        val declineCallButton = By.res("Stream_DeclineCallButton")
        val acceptCallButton = By.res("Stream_AcceptCallButton")
        val cameraEnabledToggle = CallPage.cameraEnabledToggle
        val microphoneEnabledToggle = CallPage.microphoneEnabledToggle
        val cameraDisabledToggle = CallPage.cameraDisabledToggle
        val microphoneDisabledToggle = CallPage.microphoneDisabledToggle
        val callParticipantAvatar = By.res("Stream_ParticipantAvatar")
        val incomingCallLabel = By.res("Stream_IncomingCallLabel")
        val outgoingCallLabel = By.res("Stream_OutgoingCallLabel")
    }
}
