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

package io.getstream.video.android.demo.ui.call

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.demo.BuildConfig
import io.getstream.video.android.demo.demoVideoApp
import io.getstream.video.android.compose.ui.AbstractComposeCallActivity
import io.getstream.video.android.compose.ui.components.call.CallContainer
import io.getstream.video.android.compose.ui.components.call.activecall.DefaultPictureInPictureContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.permission.StreamPermissionManagerImpl
import io.getstream.video.android.core.user.EmptyUsersProvider
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.core.viewmodel.CallViewModelFactory

class CallActivity : AppCompatActivity() {

    /**
     * Provides the StreamVideo instance through the videoApp.
     */
    fun getStreamVideo(context: Context): StreamVideo = context.demoVideoApp.streamVideo
    private val streamVideo: StreamVideo by lazy { getStreamVideo(this) }

    protected val callViewModel: CallViewModel by viewModels(factoryProducer = { defaultViewModelFactory() })

    //private val permissionsManager = setupPermissionManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callViewModel.joinCall()

        setContent {
            VideoTheme {
                CallContainer(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                    viewModel = callViewModel,
                    //onCallAction = ::handleCallAction,
                    //onBackPressed = ::handleBackPressed,
                    pictureInPictureContent = { PictureInPictureContent(call = it) }
                )
            }
        }
    }

    /**
     * Provides the default ViewModel factory.
     */
    public fun defaultViewModelFactory(): CallViewModelFactory {
        return CallViewModelFactory(
            streamVideo = streamVideo,
            permissionManager = null,
            // TODO: ->
            call = streamVideo.call("default", "123")
        )
    }


//    fun setupPermissionManager(): StreamPermissionManagerImpl {
//        return StreamPermissionManagerImpl(
//            fragmentActivity = this,
//            onPermissionResult = { permission, isGranted ->
//                when (permission) {
////                    Manifest.permission.CAMERA -> callViewModel.onCallAction(ToggleCamera(isGranted))
////                    Manifest.permission.RECORD_AUDIO -> callViewModel.onCallAction(
////                        ToggleMicrophone(
////                            isGranted
////                        )
////                    )
//                }
//            },
//            onShowSettings = {
//                //
//            })
//    }
    /**
     * Provides a custom factory for the ViewModel, that provides fake users for invites.
     */

    @Composable
    protected open fun PictureInPictureContent(call: Call) {
        DefaultPictureInPictureContent(call = call)
    }
}
