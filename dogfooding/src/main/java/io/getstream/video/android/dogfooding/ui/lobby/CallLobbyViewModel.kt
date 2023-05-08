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

package io.getstream.video.android.dogfooding.ui.lobby

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.DeviceStatus
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.mapper.toTypeAndId
import io.getstream.video.android.core.user.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CallLobbyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    userPreferences: UserPreferences
) : ViewModel() {

    val cid: String = checkNotNull(savedStateHandle["cid"])

    val call: Call by lazy {
        val streamVideo = StreamVideo.instance()
        val (type, id) = cid.toTypeAndId()
        streamVideo.call(type = type, id = id)
    }

    val user: User? = userPreferences.getUserCredentials()

    val deviceState: StateFlow<CallDeviceState> =
        combine(call.camera.status, call.microphone.status) { cameraEnabled, microphoneEnabled ->
            CallDeviceState(
                isCameraEnabled = cameraEnabled is DeviceStatus.Enabled,
                isMicrophoneEnabled = microphoneEnabled is DeviceStatus.Enabled
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, CallDeviceState())

    fun enableCamera(enabled: Boolean) {
        call.camera.setEnabled(enabled)
    }

    fun enableMicrophone(enabled: Boolean) {
        call.microphone.setEnabled(enabled)
    }

    fun signOut() {
        StreamVideo.instance().logOut()
    }
}
