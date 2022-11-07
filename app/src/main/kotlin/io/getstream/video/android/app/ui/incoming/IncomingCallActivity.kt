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

package io.getstream.video.android.app.ui.incoming

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.getstream.video.android.app.router.StreamRouterImpl
import io.getstream.video.android.app.videoApp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.incomingcall.IncomingCallScreen
import io.getstream.video.android.viewmodel.IncomingCallViewModel
import io.getstream.video.android.viewmodel.IncomingCallViewModelFactory

class IncomingCallActivity : AppCompatActivity() {

    private val viewModel by viewModels<IncomingCallViewModel> {
        IncomingCallViewModelFactory(
            videoApp.streamVideo,
            // TODO passing [this] may lead to memory leak, cause VM may live longer than Activity
            StreamRouterImpl(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)

        setContent {
            VideoTheme {
                IncomingCallScreen(
                    viewModel = viewModel,
                    onDeclineCall = { viewModel.declineCall() },
                    onAcceptCall = { viewModel.acceptCall() },
                    onVideoToggleChanged = { }
                )
            }
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    companion object {
        fun getLaunchIntent(
            context: Context,
        ): Intent {
            return Intent(context, IncomingCallActivity::class.java)
        }
    }
}
