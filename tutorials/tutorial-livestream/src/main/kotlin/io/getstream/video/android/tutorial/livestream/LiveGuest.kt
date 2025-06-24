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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.compose.ui.components.livestream.LivestreamState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import kotlinx.coroutines.launch

@Composable
fun LiveAudience(
    navController: NavController,
    callId: String,
    client: StreamVideo,
) {
    val context = LocalContext.current

    // Step 1 - Update call settings via callConfigRegistry
    client.state.callConfigRegistry.register(
        DefaultCallConfigurations.getLivestreamGuestCallServiceConfig(),
    )

    // Step 2 - join a call, which type is `default` and id is `123`.
    val call = remember(callId) { client.call("livestream", callId) }
    var livestreamState by remember { mutableStateOf(LivestreamState.INITIAL) }
    var hasJoinedSuccessFully by remember { mutableStateOf(false) }

    val endedAt by call.state.endedAt.collectAsStateWithLifecycle()
    val backstage by call.state.backstage.collectAsStateWithLifecycle()

    // Shared logic to derive state
    fun computeLivestreamState(): LivestreamState {
        return when {
            endedAt != null -> LivestreamState.ENDED
            backstage -> LivestreamState.BACKSTAGE
            else -> LivestreamState.LIVE
        }
    }

    if (hasJoinedSuccessFully) {
        livestreamState = computeLivestreamState()
    }

    suspend fun performJoin() {
        livestreamState = LivestreamState.JOINING
        call.join().onSuccess {
            hasJoinedSuccessFully = true

            livestreamState = computeLivestreamState()
            Log.d("LiveAudience", "onSuccess, LivestreamState = $livestreamState")
        }.onError {
            livestreamState = LivestreamState.ERROR
        }
    }

    LaunchedEffect(call) {
        call.microphone.setEnabled(false, fromUser = true)
        call.camera.setEnabled(false, fromUser = true)
        performJoin()
    }

    val coroutineScope = rememberCoroutineScope()

    val onRetryJoin: () -> Unit = {
        coroutineScope.launch {
            performJoin()
        }
    }
    Log.d("LiveAudience", "livestreamState=$livestreamState")
    Box {
        LivestreamPlayer(
            call = call,
            livestreamState = livestreamState,
            onRetryJoin = onRetryJoin,
        )
        CallAppBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(end = 16.dp, top = 16.dp),
            call = call,
            centerContent = { },
            onCallAction = {
                call.leave()
                navController.popBackStack()
            },
        )
    }
}
