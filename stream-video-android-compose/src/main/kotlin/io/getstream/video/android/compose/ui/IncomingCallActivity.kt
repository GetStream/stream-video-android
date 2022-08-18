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

package io.getstream.video.android.compose.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.compose.ui.components.incomingcall.IncomingCall
import io.getstream.video.android.model.CallType
import kotlinx.coroutines.delay

public class IncomingCallActivity : AppCompatActivity() {

    // TODO - Build a ViewModel for this

    override fun onCreate(savedInstanceState: Bundle?) {
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            delay(10000)
            finish()
        }
        setContent {
            // TODO - load the data from a getCall(GetCallRequest)
            IncomingCall(
                callId = "",
                callType = CallType.VIDEO,
                participants = emptyList(),
                onDeclineCall = { finish() },
                onAcceptCall = { callId, isVideoEnabled ->
                    joinCall(callId, isVideoEnabled)
                }
            )
        }
    }

    private fun joinCall(callId: String, videoEnabled: Boolean) {
        // TODO - do we start an activity here or do we trigger some handler in the VideoClient that lets the user decide what to do?
        finish()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    public companion object {
        public fun getLaunchIntent(context: Context): Intent {
            return Intent(context, IncomingCallActivity::class.java).apply {
                // TODO - maybe set data
            }
        }
    }
}
