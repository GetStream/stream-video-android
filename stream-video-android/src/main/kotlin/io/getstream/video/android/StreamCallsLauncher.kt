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

import android.content.Context
import io.getstream.video.android.input.CallServiceInput
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.service.StreamCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class StreamCallsLauncher(
    private val context: Context,
    private val streamCalls: StreamCalls,
    private val config: StreamCallsConfig,
    private val serviceInput: CallServiceInput?

) {

    fun run(scope: CoroutineScope) {
        scope.launch {
            streamCalls.callState.collect { state ->
                when {
                    state is StreamCallState.Starting -> {
                        context.startCallService()
                    }
                    state is StreamCallState.Incoming && !state.acceptedByMe -> {
                        context.startCallService()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun Context.startCallService() {
        val input = serviceInput ?: return
        if (!config.launchCallServiceInternally) return
        StreamCallService.start(applicationContext, input)
    }
}
