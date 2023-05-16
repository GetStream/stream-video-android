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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.dogfooding.ui.call.CallActivity
import io.getstream.video.android.model.StreamCallGuid
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

        logIn()
        joinCall(callId)
    }

    private fun joinCall(callId: String) {
        lifecycleScope.launch {
            val streamVideo = StreamVideo.instance()
            val guid = StreamCallGuid.fromCallCid(callId)
            val call = streamVideo.call(type = guid.type, id = guid.id)
            val result = call.join()
            result.onSuccess {
                val intent = CallActivity.getIntent(this@DeeplinkingActivity, guid = guid)
                startActivity(intent)
            }.onError {
                Toast.makeText(this@DeeplinkingActivity, it.message, Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun logIn() {
        val dataStore = StreamUserDataStore.install(this)
        val user = dataStore.user.value
        val apiKey = dataStore.apiKey.value
        val token = dataStore.userToken.value

        if (user != null) {
            logger.d { "[logIn] selectedUser: $user" }
            dogfoodingApp.initializeStreamVideo(
                user = user,
                token = token,
                apiKey = apiKey,
                loggingLevel = LoggingLevel.BODY
            )
        }
    }
}
