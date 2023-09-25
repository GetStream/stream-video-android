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

package io.getstream.video.android.ui.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.querysort.QuerySortByField
import io.getstream.result.Result
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.MainActivity
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

class CallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // step 1 - get the StreamVideo instance and create a call
        val streamVideo = StreamVideo.instance()
        val cid = intent.getParcelableExtra<StreamCallId>(EXTRA_CID)
            ?: throw IllegalArgumentException("call type and id is invalid!")
        val call = streamVideo.call(type = cid.type, id = cid.id)

        // optional - call settings. We disable the mic if coming from QR code demo
        if (intent.getBooleanExtra(EXTRA_DISABLE_MIC_BOOLEAN, false)) {
            call.microphone.disable(true)
        }

        // step 2 - join a call
        lifecycleScope.launch {
            val result = call.join(create = true)

            // Unable to join. Device is offline or other usually connection issue.
            if (result is Result.Failure) {
                Log.e("CallActivity", "Call.join failed ${result.value}")
                Toast.makeText(
                    this@CallActivity,
                    "Failed to join call (${result.value.message})",
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            }
        }

        // step 3 - build a call screen
        setContent {
            CallScreen(
                call = call,
                showDebugOptions = io.getstream.video.android.BuildConfig.DEBUG,
                onCallDisconnected = {
                    // call state changed to disconnected - we can leave the screen
                    goBackToMainScreen()
                },
                onUserLeaveCall = {
                    call.leave()
                    // we don't need to wait for the call state to change to disconnected, we can
                    // leave immediately
                    goBackToMainScreen()
                },
            )

            // step 4 (optional) - chat integration
            val user by ChatClient.instance().clientState.user.collectAsState(initial = null)
            LaunchedEffect(key1 = user) {
                if (user != null) {
                    val channel = ChatClient.instance().channel("videocall", cid.id)
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
    }

    private fun goBackToMainScreen() {
        if (!isFinishing) {
            val intent = Intent(this@CallActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_CID: String = "EXTRA_CID"
        const val EXTRA_DISABLE_MIC_BOOLEAN: String = "EXTRA_DISABLE_MIC"

        /**
         * @param callId the Call ID you want to join
         * @param disableMicOverride optional parameter if you want to override the users setting
         * and disable the microphone.
         */
        @JvmStatic
        fun createIntent(
            context: Context,
            callId: StreamCallId,
            disableMicOverride: Boolean = false,
        ): Intent {
            return Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_CID, callId)
                putExtra(EXTRA_DISABLE_MIC_BOOLEAN, disableMicOverride)
            }
        }
    }
}
