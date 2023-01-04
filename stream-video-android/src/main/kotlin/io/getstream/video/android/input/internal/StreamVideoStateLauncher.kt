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

package io.getstream.video.android.input.internal

import android.content.Context
import io.getstream.log.taggedLogger
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.input.CallAndroidInput
import io.getstream.video.android.input.CallAndroidInputLauncher
import io.getstream.video.android.model.state.StreamCallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class StreamVideoStateLauncher(
    private val context: Context,
    private val streamVideo: StreamVideo,
    private val androidInputs: Set<CallAndroidInput>,
    private val inputLauncher: CallAndroidInputLauncher
) {

    private val logger by taggedLogger("Call:State-Launcher")

    private var lastState: StreamCallState = StreamCallState.Idle

    fun run(scope: CoroutineScope) {
        scope.launch(DispatcherProvider.Main) {
            streamVideo.callState.collect { state ->
                logger.v { "[run] $state <= $lastState" }
                when {
                    lastState is StreamCallState.Idle &&
                        state is StreamCallState.Joining -> {
                        androidInputs.forEach {
                            inputLauncher.launch(context, it)
                        }
                    }
                    lastState is StreamCallState.Idle &&
                        state is StreamCallState.Outgoing &&
                        !state.acceptedByCallee -> {
                        androidInputs.forEach {
                            inputLauncher.launch(context, it)
                        }
                    }
                    lastState is StreamCallState.Idle &&
                        state is StreamCallState.Incoming &&
                        !state.acceptedByMe -> {
                        androidInputs.forEach {
                            inputLauncher.launch(context, it)
                        }
                    }
                    else -> {}
                }
                lastState = state
            }
        }
    }
}
