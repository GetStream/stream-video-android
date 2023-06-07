/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.common.permission.PermissionManager
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ShowCallParticipantInfo
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * The CallViewModel is a light wrapper over
 *
 * - call
 * - call.state
 * - call.camera/microphone/speaker
 *
 * It's main purpose is to
 *
 * - Add awareness to the UI lifecycle. It makes sure we cleanup video state etc when you leave a call
 * - Helpers for picture in picture
 * - Helpers for fullscreen mode
 *
 * It also handles some UI state
 *
 * - Opening/closing the participant menu
 *
 */
public open class CallViewModel(public val call: Call) : ViewModel() {

    private val logger by taggedLogger("Call:ViewModel")

    public val client: StreamVideo by lazy { StreamVideo.instance() }

    /** if we are in picture in picture mode */
    private val _isInPictureInPicture: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture

    private val _isShowingCallInfoMenu = MutableStateFlow(false)
    public val isShowingCallInfoMenu: StateFlow<Boolean> = _isShowingCallInfoMenu

    override fun onCleared() {
        super.onCleared()
        dismissCallInfoMenu()
        // properly clean up
        call.leave()
    }

    public fun openCallInfoMenu() {
        _isShowingCallInfoMenu.value = true
    }
    public fun dismissCallInfoMenu() {
        this._isShowingCallInfoMenu.value = false
    }

    public fun onPictureInPictureModeChanged(inPictureInPictureMode: Boolean) {
        this._isInPictureInPicture.value = inPictureInPictureMode
    }
}

private const val CONNECT_TIMEOUT = 30_000L
