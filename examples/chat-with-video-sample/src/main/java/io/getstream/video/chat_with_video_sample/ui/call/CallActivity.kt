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

package io.getstream.video.chat_with_video_sample.ui.call

import android.content.Context
import androidx.compose.runtime.Composable
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.AbstractComposeCallActivity
import io.getstream.video.android.compose.ui.components.call.CallContent
import io.getstream.video.android.permission.PermissionManagerImpl
import io.getstream.video.android.viewmodel.CallViewModelFactory
import io.getstream.video.chat_with_video_sample.application.chatWithVideoApp

class CallActivity : AbstractComposeCallActivity() {

    /**
     * Provides the StreamVideo instance through the videoApp.
     */
    override fun getStreamVideo(context: Context): StreamVideo =
        context.chatWithVideoApp.streamVideo

    /**
     * Provides a custom factory for the ViewModel, that provides fake users for invites.
     */
    override fun getCallViewModelFactory(): CallViewModelFactory {
        return CallViewModelFactory(
            streamVideo = getStreamVideo(this),
            permissionManager = PermissionManagerImpl(applicationContext),
            usersProvider = chatWithVideoApp.usersLoginProvider
        )
    }

    override fun buildContent(): @Composable () -> Unit {
        return {
            VideoTheme {
                CallContent(
                    viewModel = callViewModel,
                    onBackPressed = ::handleBackPressed,
                    onRejectCall = callViewModel::rejectCall,
                    onAcceptCall = callViewModel::acceptCall,
                    onCancelCall = callViewModel::cancelCall,
                    onMicToggleChanged = { isEnabled ->
                        callViewModel.onCallAction(
                            ToggleMicrophone(isEnabled)
                        )
                    },
                ) { isEnabled ->
                    callViewModel.onCallAction(
                        ToggleCamera(isEnabled)
                    )
                }
            }
        }
    }
}
