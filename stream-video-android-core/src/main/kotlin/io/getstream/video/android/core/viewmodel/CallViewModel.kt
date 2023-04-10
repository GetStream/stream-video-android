/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.getstream.video.android.core.viewmodel

import android.hardware.camera2.CameraMetadata
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ConnectionState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.CustomAction
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.InviteUsersToCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.SelectAudioDevice
import io.getstream.video.android.core.call.state.ShowCallInfo
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleScreenConfiguration
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.permission.PermissionManager
import io.getstream.video.android.core.user.UsersProvider
import io.getstream.video.android.core.utils.mapState
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.webrtc.RendererCommon
import stream.video.sfu.models.TrackType
import java.util.UUID

private const val CONNECT_TIMEOUT = 30_000L

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
 * - Helpers for picture in picture and fullscreen
 * - Informs the call about what resolution video is displayed at
 */
public class CallViewModel(
    public val client: StreamVideo,
    public val call: Call,
    private val permissions: PermissionManager,
    private val usersProvider: UsersProvider,
) : ViewModel() {

    private val logger by taggedLogger("Call:ViewModel")

    // shortcut to the call settings
    private val settings = call.state.settings

    private val clientImpl = client as StreamVideoImpl

    /** if we are in picture in picture mode */
    private val _isInPictureInPicture: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture

    /** if the call is fullscreen */
    private val _isFullscreen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isFullscreen: StateFlow<Boolean> = _isFullscreen

     // what does this do?
    private val _isShowingCallInfo = MutableStateFlow(false)
    public val isShowingCallInfo: StateFlow<Boolean> = _isShowingCallInfo

    public fun joinCall() {
        viewModelScope.launch {
            withTimeout(CONNECT_TIMEOUT) {
                call.join()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // TODO: properly clean up
    }

    public fun onPictureInPictureModeChanged(inPictureInPictureMode: Boolean) {
        this._isInPictureInPicture.value = inPictureInPictureMode
    }
}
