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

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.CallInput
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import kotlinx.coroutines.launch

class DeeplinkingActivity : AppCompatActivity() {

    private val logger = StreamLog.getLogger("Call:DeeplinkView")

    private val controller by lazy {
        dogfoodingApp.streamCalls
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
            val createCallResult = controller.joinCall("default", callId)

            createCallResult.onSuccessSuspend { response ->
                navigateToCall(
                    CallInput(
                        response.call.cid,
                        response.call.type,
                        response.call.id,
                        response.callUrl,
                        response.userToken,
                        response.iceServers
                    )
                )
            }
            createCallResult.onError {
                Log.d("Couldn't select server", it.message ?: "")
                Toast.makeText(this@DeeplinkingActivity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToCall(callInput: CallInput) {
        startActivity(CallActivity.getIntent(this, callInput))
        finish()
    }

    private fun logIn() {
        val selectedUser = dogfoodingApp.userPreferences.getCachedCredentials()
        logger.d { "[logIn] selectedUser: $selectedUser" }
        dogfoodingApp.initializeStreamCalls(
            credentialsProvider = AuthCredentialsProvider(
                user = selectedUser.toUser(),
                userToken = selectedUser.token,
                apiKey = "key10"
            ),
            loggingLevel = LoggingLevel.BODY
        )
    }
}
