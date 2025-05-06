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

package io.getstream.video.android.robots

import io.getstream.video.android.uiautomator.device
import io.getstream.video.android.uiautomator.enableInternetConnection
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import org.junit.Assert.fail

class ParticipantRobot(
    val testName: String,
    val headless: Boolean = true,
    val record: Boolean = false,
    val logs: Boolean = true,
) {
    private val videoBuddyUrlString = "http://10.0.2.2:5678/stream-video-buddy"
    private var screenSharingDuration: Int? = null
    private var callRecordingDuration: Int? = null
    private var messageCount: Int? = null
    private var userCount: Int = 1
    private var callDuration: Double = 30.0

    enum class Options(val value: String) {
        WITH_CAMERA("camera"),
        WITH_MICROPHONE("mic"),
        BE_SILENT("silent"),
    }

    enum class Actions(val value: String) {
        SHARE_SCREEN("screen-share"),
        RECORD_CALL("record"),
        SEND_MESSAGE("message"),
    }

    enum class DebugActions(val value: String) {
        SHOW_WINDOW("show-window"),
        RECORD_SESSION("record-session"),
        PRINT_CONSOLE_LOGS("verbose"),
    }

    private enum class Config(val value: String) {
        TEST_NAME("test-name"),
        CALL_ID("call-id"),
        USER_COUNT("user-count"),
        MESSAGE_COUNT("message-count"),
        CALL_DURATION("duration"),
        SCREEN_SHARING_DURATION("screen-sharing-duration"),
        CALL_RECORDING_DURATION("recording-duration"),
    }

    fun setScreenSharingDuration(duration: Int): ParticipantRobot {
        screenSharingDuration = duration
        return this
    }

    fun setCallRecordingDuration(duration: Int): ParticipantRobot {
        callRecordingDuration = duration
        return this
    }

    fun setCallDuration(duration: Double): ParticipantRobot {
        callDuration = duration
        return this
    }

    fun setUserCount(count: Int): ParticipantRobot {
        userCount = count
        return this
    }

    fun setMessageCount(count: Int): ParticipantRobot {
        messageCount = count
        return this
    }

    fun joinCall(
        callId: String,
        options: Array<Options> = arrayOf(),
        actions: Array<Actions> = arrayOf(),
        async: Boolean = true,
    ) {
        val params = mutableMapOf<String, Any>()
        params[Config.TEST_NAME.value] = testName
        params[Config.CALL_ID.value] = callId
        params[Config.USER_COUNT.value] = userCount
        messageCount?.let { params[Config.MESSAGE_COUNT.value] = it }
        params[Config.CALL_DURATION.value] = callDuration
        params[DebugActions.SHOW_WINDOW.value] = !headless
        params[DebugActions.PRINT_CONSOLE_LOGS.value] = logs
        params[DebugActions.RECORD_SESSION.value] = record

        for (option in options) {
            params[option.value] = true
        }

        for (action in actions) {
            params[action.value] = true
        }

        callRecordingDuration?.let {
            params[Config.CALL_RECORDING_DURATION.value] = it
        }

        screenSharingDuration?.let {
            params[Config.SCREEN_SHARING_DURATION.value] = it
        }

        messageCount?.let {
            params[Config.MESSAGE_COUNT.value] = it
        }

        postRequest("$videoBuddyUrlString/?async=$async", params)
    }

    private fun postRequest(url: String, params: Map<String, Any>): ResponseBody? {
        val body = JSONObject(params).toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        val response = execute(request)
        return response.body
    }

    private fun execute(request: Request): Response {
        try {
            return OkHttpClient().newCall(request).execute()
        } catch (e: Exception) {
            device.enableInternetConnection()
            fail(e.message)
        }
        return Response.Builder().build()
    }
}
