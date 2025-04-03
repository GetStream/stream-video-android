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

package io.getstream.video.android.pages

import androidx.test.uiautomator.By

class CallDetailsPage {

    companion object {
        val userName = By.res("Stream_UserName")
        val userAvatar = By.res("Stream_UserAvatar")
        val startNewCallButton = By.res("Stream_StartNewCallButton")
        val joinCallButton = By.res("Stream_JoinCallButton")
        val scanQrCodeButton = By.res("Stream_ScanQrCodeButton")
        val callIdInputField = By.res("Stream_CallIdInputField")
        val wheelIcon = By.res("Stream_SettingsIcon")
        val directCallButton = By.res("Stream_DirectCallButton")
        val signOutButton = By.res("Stream_SignOutButton")
    }
}
