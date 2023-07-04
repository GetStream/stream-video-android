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

package io.getstream.video.android.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.log.Priority
import io.getstream.result.Result
import io.getstream.video.android.app.VideoApp
import io.getstream.video.android.app.network.StreamVideoNetwork
import io.getstream.video.android.app.videoApp
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _call: MutableStateFlow<Call?> = MutableStateFlow(null)
    val call: StateFlow<Call?> = _call

    private val _result: MutableStateFlow<Result<RtcSession>?> = MutableStateFlow(null)
    val result: StateFlow<Result<RtcSession>?> = _result

    fun joinCall(context: Context, user: User) {
        viewModelScope.launch {
            val response = StreamVideoNetwork.tokenService.fetchToken(
                userId = user.id,
                apiKey = VideoApp.API_KEY
            )
            val token = response.token

            val streamVideo = context.videoApp.initializeStreamVideo(
                user = user,
                apiKey = VideoApp.API_KEY,
                token = token,
                loggingLevel = LoggingLevel(priority = Priority.DEBUG)
            )

            val call = streamVideo.call(type = "default", id = "1egoN4tKm4w2")
            val result = call.join(create = true)
            _result.value = result

            result.onSuccess {
                _call.value = call
            }
        }
    }
}
