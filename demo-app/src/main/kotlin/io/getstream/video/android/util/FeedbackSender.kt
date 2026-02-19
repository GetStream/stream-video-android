/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URL

/**
 * A simple http post sender for the feedback request.
 */
class FeedbackSender {
    private val client = OkHttpClient()

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})".toRegex()
        return email.matches(emailRegex)
    }

    fun sendFeedback(email: String, message: String, callId: String, coroutineScope: CoroutineScope = MainScope(), onFinished: (isError: Boolean) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val error = try {
                val response = sendFeedbackInternal(email, message, callId)
                when (response.code) {
                    204 -> false
                    else -> true
                }
            } catch (e: Exception) {
                true
            }
            onFinished(error)
        }
    }

    private fun sendFeedbackInternal(email: String, message: String, callId: String): Response {
        val url = URL("https://getstream.io/api/crm/video_feedback/")
        val formData = MultipartBody.Builder().apply {
            addFormDataPart("email", email)
            addFormDataPart("message", message)
            addFormDataPart("page_url", "https://www.getstream.io?meeting=true&id=$callId")
            setType(MultipartBody.FORM)
        }
        val request = Request.Builder()
            .url(url)
            .post(formData.build())
            .header("User-Agent", "StreamDemoApp-Android/1.0.0")
            .header("Connection", "keep-alive")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        return client.newCall(request).execute()
    }
}
