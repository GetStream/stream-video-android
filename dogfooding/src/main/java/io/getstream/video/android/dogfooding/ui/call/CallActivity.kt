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

package io.getstream.video.android.dogfooding.ui.call

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.common.AbstractCallActivity
import io.getstream.video.android.common.viewmodel.CallViewModel
import io.getstream.video.android.common.viewmodel.CallViewModelFactory
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallContainer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

class CallActivity : AbstractCallActivity() {

    // step 1 (optional) - create a call view model
    private val factory by lazy { CallViewModelFactory() }
    private val vm by viewModels<CallViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // step 2 - join a call
        lifecycleScope.launch { call.join(create = true) }

        // step 3 - build a call screen
        setContent {

            val participants: List<ParticipantState> by call.state.participants.collectAsState()
            participants.forEach { participantState ->
            }

            VideoTheme {
                CallContainer(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                    call = call,
                    callViewModel = vm, // optional
                    onBackPressed = { handleBackPressed() },
                    onCallAction = { callAction ->
                        when (callAction) {
                            is ToggleCamera -> call.camera.setEnabled(callAction.isEnabled)
                            is ToggleMicrophone -> call.microphone.setEnabled(callAction.isEnabled)
                            is ToggleSpeakerphone -> call.speaker.setEnabled(callAction.isEnabled)
                            is LeaveCall -> finish()
                            else -> Unit
                        }
                    }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        call.camera.pause()
    }

    override fun onResume() {
        super.onResume()
        call.camera.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        call.leave()
    }

    override fun pipChanged(isInPip: Boolean) {
        super.pipChanged(isInPip)
        vm.onPictureInPictureModeChanged(isInPip)
    }

    override fun createCall(): Call {
        val streamVideo = StreamVideo.instance()
        val cid = intent.getParcelableExtra<StreamCallId>(EXTRA_CID)
            ?: throw IllegalArgumentException("call type and id is invalid!")
        return streamVideo.call(cid.type, cid.id)
    }
}
