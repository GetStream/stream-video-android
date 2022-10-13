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

package io.getstream.video.android.app.ui.outgoing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.app.ui.call.CallActivity
import io.getstream.video.android.app.videoApp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.outcomingcall.OutgoingCall
import io.getstream.video.android.events.CallAcceptedEvent
import io.getstream.video.android.events.CallRejectedEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import stream.video.coordinator.client_v1_rpc.UserEventType

class OutgoingCallActivity : AppCompatActivity(), SocketListener {

    private val streamCalls by lazy { videoApp.streamCalls }
    private val logger by lazy { StreamLog.getLogger("OutgoingCallActivity") }

    private var isMicrophoneEnabled = false
    private var isVideoEnabled = false

    private lateinit var callMetadata: CallMetadata
    private var callRejectionCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.getSerializableExtra(KEY_DATA) as? CallMetadata

        lifecycleScope.launchWhenCreated {
            delay(10000)
            finish()
        }

        if (data == null) {
            finish()
        } else {
            callMetadata = data
            streamCalls.addSocketListener(this)

            val callType = CallType.fromType(callMetadata.type)

            setContent {
                VideoTheme {
                    OutgoingCall(
                        callId = data.id,
                        callType = callType,
                        participants = data.users.values.toList()
                            .filter { it.id != streamCalls.getUser().id },
                        onCancelCall = { hangUpCall() },
                        onMicToggleChanged = { isMicrophoneEnabled ->
                            this.isMicrophoneEnabled = isMicrophoneEnabled
                        },
                        onVideoToggleChanged = { isVideoEnabled ->
                            this.isVideoEnabled = isVideoEnabled
                        }
                    )
                }
            }
        }
    }

    private fun hangUpCall() {
        val data = intent.getSerializableExtra(KEY_DATA) as? CallMetadata

        if (data != null) {
            lifecycleScope.launch {
                streamCalls.sendEvent(
                    callId = data.id,
                    callType = data.type,
                    UserEventType.USER_EVENT_TYPE_CANCELLED_CALL
                )

                streamCalls.leaveCall()
            }
        }
        finish()
    }

    override fun onEvent(event: VideoEvent) {
        super.onEvent(event)
        when (event) {
            is CallAcceptedEvent -> joinCall()
            is CallRejectedEvent -> onCallRejected()
            else -> Unit
        }
    }

    private fun onCallRejected() {
        callRejectionCount += 1
        logger.d { "[onCallRejected] rejected call count $callRejectionCount" }
        if (callRejectionCount == (callMetadata.users.count() - 1)) {
            logger.d { "[onCallRejected] Hanging up call" }
            hangUpCall()
        }
    }

    private fun joinCall() {
        lifecycleScope.launch {
            val joinResult = streamCalls.joinCall(callMetadata)

            joinResult.onSuccessSuspend { response ->
                navigateToCall(
                    response.call.cid,
                    response.callUrl,
                    response.userToken,
                    response.iceServers
                )
            }
            joinResult.onError {
                Log.d("Couldn't select server", it.message ?: "")
                Toast.makeText(this@OutgoingCallActivity, it.message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun navigateToCall(
        callId: String,
        signalUrl: String,
        userToken: String,
        iceServers: List<IceServer>
    ) {
        val intent = CallActivity.getIntent(this, callId, signalUrl, userToken, iceServers)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val KEY_DATA = "call_id"

        internal fun getIntent(
            context: Context,
            callMetadata: CallMetadata
        ): Intent {
            return Intent(context, OutgoingCallActivity::class.java).apply {
                putExtra(KEY_DATA, callMetadata)
            }
        }
    }
}
