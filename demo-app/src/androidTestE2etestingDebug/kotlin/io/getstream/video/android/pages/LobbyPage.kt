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
import androidx.test.uiautomator.BySelector

class LobbyPage {

    companion object {
        val closeButton = By.res("Stream_LobbyCloseButton")
        val cameraEnabledToggle = By.res("Stream_CameraToggle_Enabled_true")
        val cameraDisabledToggle = By.res("Stream_CameraToggle_Enabled_false")
        val microphoneEnabledToggle = By.res("Stream_MicrophoneToggle_Enabled_true")
        val microphoneDisabledToggle = By.res("Stream_MicrophoneToggle_Enabled_false")
        val microphoneEnabledIcon = By.res("Stream_UserMicrophone_Enabled_true")
        val microphoneDisabledIcon = By.res("Stream_UserMicrophone_Enabled_false")
        val cameraEnabledView = By.res("on_rendered_content")
        val cameraDisabledView = By.res("on_disabled_content")
        val joinCallButton = By.res("Stream_JoinCallButton")
        fun callParticipantsCount(count: Int): BySelector {
            return By.res("Stream_ParticipantsCount_$count")
        }
    }
}
