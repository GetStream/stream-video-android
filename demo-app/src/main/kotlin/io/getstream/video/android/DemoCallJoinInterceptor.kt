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

package io.getstream.video.android

import io.getstream.log.taggedLogger
import io.getstream.video.android.CallActivity.Companion.USE_CALL_JOIN_INTERCEPTOR
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallJoinInterceptor
import io.getstream.video.android.core.RingingState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * Do the following changes before testing this flow
 * Set [io.getstream.video.android.core.INTERCEPTOR_TIMEOUT_MS] to 10_000L
 * Set [io.getstream.video.android.core.PEER_CONNECTION_OBSERVER_TIMEOUT] to 10_000L
 */
class DemoCallJoinInterceptor(
    private val callReadyToJoinFlow: StateFlow<Boolean>,
    private val previousRingingStates: Set<RingingState>,
) : CallJoinInterceptor {
    private val logger by taggedLogger("DemoCallJoinInterceptor")

    companion object {
        const val CALLER_READY_TO_JOIN_EVENT_TYPE = "caller_ready_join"
        const val CALLEE_READY_TO_JOIN_EVENT_TYPE = "callee_ready_join"
    }

    override suspend fun callReadyToJoin(call: Call) {
        val previousRingingStatesText = previousRingingStates.joinToString { it.toString() }
        logger.d {
            "[callReadyToJoin] USE_CALL_JOIN_INTERCEPTOR: $USE_CALL_JOIN_INTERCEPTOR, previous ringingState: $previousRingingStatesText"
        }
        if (USE_CALL_JOIN_INTERCEPTOR) {
            val isIncomingOrOutgoing = previousRingingStates.first {
                it is RingingState.Incoming || it is RingingState.Outgoing
            }

            val isOutgoing = isIncomingOrOutgoing is RingingState.Outgoing
            val isIncoming = isIncomingOrOutgoing is RingingState.Incoming

            val currentUserId = call.user.id
            if (isIncoming) {
                val result = call.sendCustomEvent(
                    mapOf(
                        "type" to CALLEE_READY_TO_JOIN_EVENT_TYPE,
                        "user_id" to currentUserId,
                    ),
                )
                if (result.isSuccess) {
                    logger.d { "[callReadyToJoin] Successfully sent custom $CALLEE_READY_TO_JOIN_EVENT_TYPE event" }
                    App.demoApp.callerReadyToJoinFlow.filter { it != null && it.callCid == call.cid }.first()
                } else {
                    logger.d { "[callReadyToJoin] Failed to send custom $CALLEE_READY_TO_JOIN_EVENT_TYPE event" }
                }
                logger.d { "[callReadyToJoin] callerReadyToJoinFlow finish" }
            } else if (isOutgoing) {
                val result = call.sendCustomEvent(
                    mapOf(
                        "type" to CALLER_READY_TO_JOIN_EVENT_TYPE,
                        "user_id" to currentUserId,
                    ),
                )

                if (result.isSuccess) {
                    logger.d { "[callReadyToJoin] Successfully sent custom $CALLER_READY_TO_JOIN_EVENT_TYPE event" }
                    App.demoApp.calleeReadyToJoinFlow.filter { it != null && it.callCid == call.cid }.first()
                } else {
                    logger.d { "[callReadyToJoin] Failed to send custom $CALLER_READY_TO_JOIN_EVENT_TYPE event" }
                }
                logger.d { "[callReadyToJoin] calleeReadyToJoinFlow finish" }
            }
        }
    }
}
