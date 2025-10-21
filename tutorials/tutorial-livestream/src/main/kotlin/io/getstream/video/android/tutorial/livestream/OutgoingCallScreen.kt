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

package io.getstream.video.android.tutorial.livestream

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.LeaveCall
import kotlinx.coroutines.launch

const val TAG = "OutgoingCallScreen"

@Composable
fun OutgoingCallScreen(
    navController: NavController,
    callId: String,
    client: StreamVideo,
) {
    val call = remember(callId) { client.call("default", callId) }

    Log.d(TAG, "[OutgoingCallScreen] call_id: $callId")
    LaunchedEffect(Unit) {
        val instance = StreamVideo.instance()
        val result = call.create(
            // List of all users, containing the caller also
            memberIds = listOf(instance.userId),
            // If other users will get push notification.
            ring = false,
        )
        result.onSuccess {
            Log.d(TAG, "[OutgoingCallScreen] onSuccess")
            launch {
                val joinResult = call.join()
                joinResult.onSuccess {
                    Log.d(TAG, "[OutgoingCallScreen] Join Success")
                }.onError {
                    Log.d(TAG, "[OutgoingCallScreen] Join Error")
                }
            }
        }.onError {
            Log.e(TAG, "[OutgoingCallScreen] onError: ${it.message}")
        }
    }
    CallContent(call, enableInPictureInPicture = true, onCallAction = {
        when (it) {
            LeaveCall -> {
                call.leave()
                navController.popBackStack()
            }

            else -> {}
        }
    })
}
