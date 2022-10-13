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
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.app.ui.call.CallActivity
import io.getstream.video.android.app.videoApp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.incomingcall.IncomingCall
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

class IncomingCallActivity : AppCompatActivity() {

    private val logger by lazy { StreamLog.getLogger("Call:Incoming-View") }
    private val viewModel by viewModels<IncomingViewModel> { IncomingViewModelFactory(videoApp.streamCalls) }

    override fun onCreate(savedInstanceState: Bundle?) {
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)

        observeIncomingCall()
        observeAcceptedCall()
        observeError()
        observeDrop()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun observeIncomingCall() {
        lifecycleScope.launchWhenCreated {
            viewModel.incomingData.filterNotNull().collectLatest { data ->
                logger.d { "[observeIncomingCall] data: $data" }
                setContent {
                    VideoTheme {
                        IncomingCall(
                            callInfo = data.callInfo,
                            participants = data.participants,
                            callType = data.callType,
                            onDeclineCall = { viewModel.hangUp() },
                            onAcceptCall = { viewModel.pickUp() },
                            onVideoToggleChanged = { }
                        )
                    }
                }
            }
        }
    }

    private fun observeAcceptedCall() {
        lifecycleScope.launchWhenCreated {
            viewModel.acceptedEvent.collectLatest { data ->
                logger.d { "[observeAcceptedCall] data: $data" }
                startActivity(
                    CallActivity.getIntent(
                        this@IncomingCallActivity,
                        callCid = data.callCid,
                        signalUrl = data.signalUrl,
                        userToken = data.userToken,
                        iceServers = data.iceServers
                    )
                )
            }
        }
    }

    private fun observeError() {
        lifecycleScope.launchWhenCreated {
            viewModel.errorEvent.collectLatest {
                logger.e { "[observeError] no args" }
                Toast.makeText(
                    this@IncomingCallActivity,
                    "Unable to accept call!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeDrop() {
        lifecycleScope.launchWhenCreated {
            viewModel.dropEvent.collectLatest {
                logger.i { "[observeDrop] no args" }
                finish()
            }
        }
    }

    companion object {
        fun getLaunchIntent(
            context: Context
        ): Intent {
            return Intent(context, IncomingCallActivity::class.java)
        }
    }
}
