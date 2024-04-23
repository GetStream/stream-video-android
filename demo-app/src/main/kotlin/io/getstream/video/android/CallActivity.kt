/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.querysort.QuerySortByField
import io.getstream.result.Result
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId
import io.getstream.video.android.ui.call.CallScreen
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.StreamCallActivityConfiguration
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi
import io.getstream.video.android.util.FullScreenCircleProgressBar
import kotlinx.coroutines.launch
import java.lang.Exception

@OptIn(StreamCallActivityDelicateApi::class)
class CallActivity : ComposeStreamCallActivity() {

    override val uiDelegate: StreamActivityUiDelegate<StreamCallActivity> = StreamDemoUiDelegate()
    override val configuration: StreamCallActivityConfiguration =
        StreamCallActivityConfiguration(closeScreenOnCallEnded = false)

    private class StreamDemoUiDelegate : StreamCallActivityComposeDelegate() {

        @Composable
        override fun StreamCallActivity.LoadingContent(call: Call) {
            // Use as loading screen.. so the layout is shown.
            if (call.type == "default") {
                VideoCallContent(call = call)
            } else {
                FullScreenCircleProgressBar(text = "Connecting...")
            }
        }

        @Composable
        override fun StreamCallActivity.CallDisconnectedContent(call: Call) {
            goBackToMainScreen()
        }

        @Composable
        override fun StreamCallActivity.VideoCallContent(call: Call) {
            CallScreen(
                call = call,
                showDebugOptions = BuildConfig.DEBUG,
                onCallDisconnected = {
                    leave(call)
                    goBackToMainScreen()
                },
                onUserLeaveCall = {
                    leave(call)
                    goBackToMainScreen()
                },
            )

            // step 4 (optional) - chat integration
            val user by ChatClient.instance().clientState.user.collectAsState(initial = null)
            LaunchedEffect(key1 = user) {
                if (user != null) {
                    val channel = ChatClient.instance().channel("videocall", call.id)
                    channel.queryMembers(
                        offset = 0,
                        limit = 10,
                        filter = Filters.neutral(),
                        sort = QuerySortByField(),
                    ).await().onSuccessSuspend { members ->
                        if (members.isNotEmpty()) {
                            channel.addMembers(listOf(user!!.id)).await()
                        } else {
                            channel.create(listOf(user!!.id), emptyMap()).await()
                        }
                    }
                }
            }
        }

        private fun StreamCallActivity.goBackToMainScreen() {
            if (!isFinishing) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}
