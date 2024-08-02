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

package io.getstream.video.android.core.telecom

import android.content.Context
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.model.StreamCallId

internal object TelecomCompat {

    fun registerCall(
        context: Context,
        trigger: String,
        callDisplayName: String = "",
        call: StreamCall? = null,
        callId: StreamCallId? = null,
    ) {
        getCallObject(call, callId)?.let {
            val applicationContext = context.applicationContext
            val isTelecomSupported = TelecomHandler.isSupported(applicationContext)
            val telecomHandler = TelecomHandler.getInstance(applicationContext)

            if (isTelecomSupported) {
                telecomHandler?.registerCall(it)
            } else {
                if (trigger == CallService.TRIGGER_INCOMING_CALL) {
                    CallService.showIncomingCall(
                        applicationContext,
                        StreamCallId.fromCallCid(it.cid),
                        callDisplayName,
                    )
                } else {
                    // TODO-Telecom: Take runForegroundService flag into account here and above?
                    ContextCompat.startForegroundService(
                        applicationContext,
                        CallService.buildStartIntent(
                            applicationContext,
                            StreamCallId.fromCallCid(it.cid),
                            trigger,
                        ),
                    )
                }
            }
        }
    }

    private fun getCallObject(call: StreamCall?, callId: StreamCallId?): StreamCall? = when {
        call != null -> call

        callId != null -> StreamVideo.instanceOrNull()?.call(callId.type, callId.id)

        else -> null
    }

    fun unregisterCall(
        context: Context,
        trigger: String,
        call: StreamCall? = null,
    ) {
        val applicationContext = context.applicationContext
        val isTelecomSupported = TelecomHandler.isSupported(applicationContext)
        val telecomHandler = TelecomHandler.getInstance(applicationContext)

        if (isTelecomSupported) {
            telecomHandler?.unregisterCall()
        } else {
            if (trigger == CallService.TRIGGER_INCOMING_CALL) {
                call?.let {
                    CallService.removeIncomingCall(context, StreamCallId.fromCallCid(it.cid))
                }
            } else {
                context.stopService(CallService.buildStopIntent(applicationContext))
            }
        }
    }
}

internal typealias StreamCall = io.getstream.video.android.core.Call

internal const val TELECOM_LOG_TAG = "StreamVideo:Telecom"
