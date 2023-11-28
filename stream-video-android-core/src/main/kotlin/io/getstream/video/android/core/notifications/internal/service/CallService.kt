/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallDisplayName
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A foreground service that is running when there is an active call.
 */
internal class CallService : Service() {
    private val logger by taggedLogger("CallService")
    private var callId: StreamCallId? = null
    private var callDisplayName: String? = null
    private var startJob: Job? = null
    private var observeJob: Job? = null

    companion object {
        const val TRIGGER_KEY = "io.getstream.video.android.core.notifications.internal.service.CallService.call_trigger"
        const val TRIGGER_INCOMING_CALL = "incomming_call"
        const val TRIGGER_ONGOING_CALL = "ongoing_call"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        callDisplayName = intent?.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME)
        val trigger = intent?.getStringExtra(TRIGGER_KEY)
        val streamVideo = StreamVideo.instanceOrNull()
        val started = if (callId != null && streamVideo != null && trigger != null) {
            val notification: Notification? =
                when (trigger) {
                    TRIGGER_ONGOING_CALL -> streamVideo.getOngoingCallNotification(callId!!)
                    TRIGGER_INCOMING_CALL -> streamVideo.getRingingCallNotification(
                        callId!!,
                        callDisplayName!!,
                    )
                    else -> null
                }
            if (notification != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceCompat.startForeground(
                        this@CallService,
                        callId.hashCode(),
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                    )
                } else {
                    startForeground(callId.hashCode(), notification)
                }
                true
            } else {
                // Service not started no notification
                logger.e { "Could not get notification for ongoing call" }
                false
            }
        } else {
            // Service not started, no call Id or stream video
            logger.e { "Call id or streamVideo or trigger are not available." }
            false
        }

        if (!started) {
            logger.w { "Foreground service did not start!" }
            stopSelf()
        } else {
            if (trigger == TRIGGER_INCOMING_CALL){
                launchStartJob(streamVideo!!, callId!!)
            }
            observeCallState(callId!!, streamVideo!!)
        }
        return START_NOT_STICKY
    }

    private fun observeCallState(callId: StreamCallId, streamVideo: StreamVideo) {
        observeJob = GlobalScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            call.state.ringingState.collectLatest {
                logger.i { "Ringing state: $it" }
                when(it) {
                    is RingingState.RejectedByAll -> stopSelf()
                    else -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun launchStartJob(
        streamVideo: StreamVideo,
        callId: StreamCallId
    ) {
        startJob = GlobalScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            val update = call.get()
            if (update.isFailure) {
                update.errorOrNull()?.let {
                    logger.e { it.message }
                } ?: let {
                    logger.e { "Failed to update call." }
                }
                stopSelf() // Failed to update call
                return@launch
            }
            val coordinator = streamVideo.connectAsync().await()
            if (coordinator.isFailure) {
                coordinator.errorOrNull()?.let {
                    logger.e { it.message }
                } ?: let {
                    logger.e { "Failed to start coordinator socket." }
                }
                stopSelf() // Failed to connect coordinator socket
                return@launch
            }
        }
    }

    override fun onDestroy() {
        callId?.let {
            val notificationId = callId.hashCode()
            NotificationManagerCompat.from(this).cancel(notificationId)
        }
        // Stop any jobs
        startJob?.cancel()
        observeJob?.cancel()
        super.onDestroy()
    }

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null
}
