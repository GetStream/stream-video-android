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

package io.getstream.video.android.dogfooding

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.common.AbstractCallActivity
import io.getstream.video.android.common.viewmodel.CallViewModel
import io.getstream.video.android.common.viewmodel.CallViewModelFactory
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.launch

class IncomingCallActivity : AbstractCallActivity() {

    private val factory by lazy { CallViewModelFactory() }
    private val vm by viewModels<CallViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // release the lock and turn on screen.
        showWhenLockedAndTurnScreenOn()

        lifecycleScope.launch {
            if (NotificationHandler.ACTION_ACCEPT_CALL == intent.action) {
                call.accept()
                call.join()
            }
        }

        setContent {
            VideoTheme {
                val onCallAction: (CallAction) -> Unit = { callAction ->
                    when (callAction) {
                        is ToggleCamera -> call.camera.setEnabled(callAction.isEnabled)
                        is ToggleMicrophone -> call.microphone.setEnabled(callAction.isEnabled)
                        is ToggleSpeakerphone -> call.speaker.setEnabled(callAction.isEnabled)
                        is LeaveCall -> finish()
                        else -> Unit
                    }
                }
                RingingCallContent(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                    call = call,
                    onBackPressed = { handleBackPressed() },
                    onAcceptedContent = {
                        CallContent(
                            modifier = Modifier.fillMaxSize(),
                            call = call,
                            onCallAction = onCallAction
                        )
                    },
                    onCallAction = onCallAction
                )
            }
        }
    }

    override fun pipChanged(isInPip: Boolean) {
        super.pipChanged(isInPip)
        vm.onPictureInPictureModeChanged(isInPip)
    }

    override fun provideCall(): Call {
        val callId = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)!!
        return StreamVideo.instance().call(callId.type, callId.id)
    }
}
