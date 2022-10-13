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

package io.getstream.video.android.app.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.app.FakeCredentialsProvider
import io.getstream.video.android.app.ui.call.CallActivity
import io.getstream.video.android.app.videoApp
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import kotlinx.coroutines.launch

class DeeplinkingActivity : AppCompatActivity() {

    private val logger = StreamLog.getLogger("Call:DeeplinkView")

    private val controller by lazy {
        videoApp.streamCalls
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.d { "[onCreate] savedInstanceState: $savedInstanceState" }
        super.onCreate(savedInstanceState)

        val data: Uri = intent?.data ?: return

        val callId = data.toString().split("/").lastOrNull() ?: return

        Log.d("ReceivedDeeplink", "Action: ${intent?.action}")
        Log.d("ReceivedDeeplink", "Data: ${intent?.data}")

        logIn()
        joinCall(callId)
    }

    private fun joinCall(callId: String) {
        lifecycleScope.launch {

            val createCallResult = controller.createAndJoinCall(
                "default", // TODO - hardcoded for now
                id = callId,
                participantIds = emptyList(),
                false
            )

            createCallResult.onSuccessSuspend { response ->
                navigateToCall(
                    response.call.cid,
                    response.callUrl,
                    response.userToken,
                    response.iceServers
                )
            }
            createCallResult.onError {
                Log.d("Couldn't select server", it.message ?: "")
                Toast.makeText(this@DeeplinkingActivity, it.message, Toast.LENGTH_SHORT).show()
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
    }

    private fun logIn() {
        val selectedUser = videoApp.userPreferences.getCachedCredentials()
        logger.d { "[logIn] selectedUser: $selectedUser" }
        videoApp.initializeStreamCalls(
            credentialsProvider = FakeCredentialsProvider(
                userCredentials = selectedUser,
                apiKey = "key1"
            ),
            loggingLevel = LoggingLevel.BODY
        )
    }
}
