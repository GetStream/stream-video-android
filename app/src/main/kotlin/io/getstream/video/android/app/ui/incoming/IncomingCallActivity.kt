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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.app.ui.call.CallActivity
import io.getstream.video.android.app.videoApp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.incomingcall.IncomingCall
import io.getstream.video.android.events.CallCreatedEvent
import io.getstream.video.android.model.CallInfo
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Success
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import stream.video.coordinator.client_v1_rpc.UserEventType

class IncomingCallActivity : AppCompatActivity() {

    private val streamCalls by lazy { videoApp.streamCalls }
    private val logger by lazy { StreamLog.getLogger("IncomingCallActivity") }

    override fun onCreate(savedInstanceState: Bundle?) {
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)
        val data = intent.getSerializableExtra(KEY_EVENT_DATA) as? CallCreatedEvent

        lifecycleScope.launchWhenCreated {
            delay(10000)
            finish()
        }

        if (data == null) {
            finish()
        } else {
            setContent {
                VideoTheme {
                    IncomingCall(
                        callInfo = data.info!!,
                        participants = data.users.values.toList(),
                        onDeclineCall = { finish() },
                        onAcceptCall = { event ->
                            acceptCall(event)
                        },
                        onVideoToggleChanged = { }
                    )
                }
            }
        }
    }

    private fun declineCall(
        callInfo: CallInfo
    ) {
        lifecycleScope.launch {
            val result = streamCalls.sendEvent(
                callInfo.callId,
                callInfo.type,
                UserEventType.USER_EVENT_TYPE_REJECTED_CALL
            )

            logger.d { "[declineCall] $result" }
            finish()
        }
    }

    private fun acceptCall(callInfo: CallInfo) {
        lifecycleScope.launch {
            val callId =
                callInfo.callId.replace("${callInfo.type}:", "") // TODO - we have cid here, not call ID

            val eventResult = streamCalls.sendEvent(
                callId,
                callInfo.type,
                UserEventType.USER_EVENT_TYPE_ACCEPTED_CALL
            )
            logger.d { "[acceptCall] $eventResult" }

            when (val joinResult = streamCalls.joinCall(callInfo.type, callId)) {
                is Success -> {
                    val data = joinResult.data

                    startActivity(
                        CallActivity.getIntent(
                            this@IncomingCallActivity,
                            callCid = callInfo.callId,
                            signalUrl = data.callUrl,
                            userToken = data.userToken,
                            iceServers = data.iceServers
                        )
                    )
                    finish()
                }
                is Failure -> {
                    Toast.makeText(
                        this@IncomingCallActivity,
                        "Unable to accept call!",
                        Toast.LENGTH_SHORT
                    ).show()
                    declineCall(callInfo)
                }
            }
        }
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

    companion object {
        private const val KEY_EVENT_DATA = "event_data"

        fun getLaunchIntent(
            context: Context,
            callCreatedEvent: CallCreatedEvent
        ): Intent {
            return Intent(context, IncomingCallActivity::class.java).apply {
                putExtra(KEY_EVENT_DATA, callCreatedEvent)
            }
        }
    }
}
