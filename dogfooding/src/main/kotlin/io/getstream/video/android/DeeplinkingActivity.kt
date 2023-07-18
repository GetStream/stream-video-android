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

package io.getstream.video.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.getstream.log.Priority
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import io.getstream.video.android.token.StreamVideoNetwork
import io.getstream.video.android.ui.call.CallActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeeplinkingActivity : ComponentActivity() {

    private val logger by taggedLogger("Call:DeeplinkView")

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.d { "[onCreate] savedInstanceState: $savedInstanceState" }
        super.onCreate(savedInstanceState)

        setContent {
            VideoTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VideoTheme.colors.appBackground)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VideoTheme.colors.primaryAccent
                    )
                }
            }
        }

        val data: Uri = intent?.data ?: return
        val callId = data.toString().split("/").lastOrNull() ?: return

        logger.d { "Action: ${intent?.action}" }
        logger.d { "Data: ${intent?.data}" }

        joinCall(callId)
    }

    private fun joinCall(cid: String) {
        val dataStore = StreamUserDataStore.install(this)

        lifecycleScope.launch {
            val data = dataStore.data
            data.collectLatest { preferences ->
                if (preferences != null) {
                    app.initializeStreamChat(
                        user = preferences.user!!,
                        token = preferences.userToken
                    )
                    app.initializeStreamVideo(
                        user = preferences.user!!,
                        token = preferences.userToken,
                        apiKey = preferences.apiKey,
                        loggingLevel = LoggingLevel(priority = Priority.VERBOSE)
                    )
                } else {
                    val guest = User(id = "guest", name = "Guest", role = "guest")
                    val result = StreamVideoNetwork.tokenService.fetchToken(
                        userId = guest.id,
                        apiKey = BuildConfig.DOGFOODING_API_KEY
                    )
                    app.initializeStreamChat(user = guest, token = result.token)
                    app.initializeStreamVideo(
                        user = guest,
                        token = result.token,
                        apiKey = BuildConfig.DOGFOODING_API_KEY,
                        loggingLevel = LoggingLevel(priority = Priority.VERBOSE)
                    )
                }

                if (StreamVideo.isInstalled) {
                    val callId = StreamCallId(type = "default", id = cid)
                    val intent = CallActivity.createIntent(
                        context = this@DeeplinkingActivity, callId = callId
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
