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
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.user.UserPreferencesManager
import kotlinx.coroutines.launch

class DeeplinkingActivity : AppCompatActivity() {

    private val logger by taggedLogger("Call:DeeplinkView")

    private val controller by lazy {
        dogfoodingApp.streamVideo
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

            createCallResult.onSuccess {
                /**
                 * Since we're using launchers, we don't need to worry about starting the
                 * CallActivity ourselves.
                 */
                Log.d("Joined", it.toString())
            }
            createCallResult.onError {
                Log.d("Couldn't select server", it.message ?: "")
                Toast.makeText(this@DeeplinkingActivity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logIn() {
        val userPreferences = UserPreferencesManager.initialize(this)
        val user = userPreferences.getUserCredentials()
        val apiKey = userPreferences.getApiKey()

        if (user != null) {
            logger.d { "[logIn] selectedUser: $user" }
            dogfoodingApp.initializeStreamVideo(
                user = user,
                apiKey = apiKey,
                loggingLevel = LoggingLevel.BODY
            )
        }
    }
}
