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
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallDisplayName
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent
import java.lang.IllegalArgumentException

/**
 * A foreground service that is running when there is an active call.
 */
internal class CallService : Service() {
    private val logger by taggedLogger("CallService")

    // Data
    private var callId: StreamCallId? = null
    private var callDisplayName: String? = null

    // Running jobs
    private var observeRingingState: Job? = null
    private var updateRingingStateJob: Job? = null
    private var observeCallState: Job? = null
    private var updateCallJob: Job? = null
    private var startCoordinatorSocketJob: Job? = null
    private val supervisorJob = SupervisorJob()

    internal companion object {
        const val TRIGGER_KEY =
            "io.getstream.video.android.core.notifications.internal.service.CallService.call_trigger"
        const val TRIGGER_INCOMING_CALL = "incomming_call"
        const val TRIGGER_ONGOING_CALL = "ongoing_call"

        /**
         * Build start intent.
         *
         * @param context the context.
         * @param callId the call id.
         * @param trigger one of [TRIGGER_INCOMING_CALL] or [TRIGGER_ONGOING_CALL]
         * @param callDisplayName the display name.
         */
        fun buildStartIntent(
            context: Context,
            callId: StreamCallId,
            trigger: String,
            callDisplayName: String? = null,
        ): Intent {
            val serviceIntent = Intent(context, CallService::class.java)
            serviceIntent.putExtra(INTENT_EXTRA_CALL_CID, callId)
            when (trigger) {
                TRIGGER_INCOMING_CALL -> {
                    serviceIntent.putExtra(TRIGGER_KEY, TRIGGER_INCOMING_CALL)
                    serviceIntent.putExtra(INTENT_EXTRA_CALL_DISPLAY_NAME, callDisplayName)
                }

                TRIGGER_ONGOING_CALL -> {
                    serviceIntent.putExtra(TRIGGER_KEY, TRIGGER_ONGOING_CALL)
                }

                else -> {
                    throw IllegalArgumentException(
                        "Unknown $trigger, must be one of $TRIGGER_INCOMING_CALL or $TRIGGER_ONGOING_CALL",
                    )
                }
            }
            return serviceIntent
        }

        /**
         * Build stop intent.
         *
         * @param context the context.
         */
        fun buildStopIntent(context: Context) = Intent(context, CallService::class.java)
    }

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        logger.w { "Timeout received from the system, service will stop." }
        stopService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.i { "Starting CallService. $intent" }
        callId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        callDisplayName = intent?.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME)
        val trigger = intent?.getStringExtra(TRIGGER_KEY)
        val streamVideo = StreamVideo.instanceOrNull()
        val started = if (callId != null && streamVideo != null && trigger != null) {
            val notificationData: Pair<Notification?, Int> =
                when (trigger) {
                    TRIGGER_ONGOING_CALL -> Pair(
                        streamVideo.getOngoingCallNotification(
                            callId!!,
                        ),
                        callId.hashCode(),
                    )

                    TRIGGER_INCOMING_CALL -> Pair(
                        streamVideo.getRingingCallNotification(
                            callId!!,
                            callDisplayName!!,
                        ),
                        NotificationHandler.INCOMING_CALL_NOTIFICATION_ID,
                    )

                    else -> Pair(null, callId.hashCode())
                }
            val notification = notificationData.first
            if (notification != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val foregroundServiceType =
                        when (trigger) {
                            TRIGGER_ONGOING_CALL -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                            TRIGGER_INCOMING_CALL -> ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                            else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                        }
                    ServiceCompat.startForeground(
                        this@CallService,
                        callId.hashCode(),
                        notification,
                        foregroundServiceType,
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
            stopService()
        } else {
            if (trigger == TRIGGER_INCOMING_CALL) {
                updateRingingCall(streamVideo!!, callId!!)
                initializeCallAndSocket(streamVideo, callId!!)
            }
            observeCallState(callId!!, streamVideo!!)
        }
        return START_NOT_STICKY
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateRingingCall(streamVideo: StreamVideo, callId: StreamCallId) {
        updateRingingStateJob = GlobalScope.launch(supervisorJob + Dispatchers.IO) {
            val call = streamVideo.call(callId.type, callId.id)
            streamVideo.state.addRingingCall(call)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun observeCallState(callId: StreamCallId, streamVideo: StreamVideo) {
        // Ringing state
        observeRingingState = GlobalScope.launch(supervisorJob + Dispatchers.IO) {
            val call = streamVideo.call(callId.type, callId.id)
            call.state.ringingState.collect {
                logger.i { "Ringing state: $it" }
                when (it) {
                    is RingingState.RejectedByAll -> {
                        stopService()
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
        }

        // Call state
        observeCallState = GlobalScope.launch(supervisorJob + Dispatchers.IO) {
            val call = streamVideo.call(callId.type, callId.id)
            call.subscribe {
                logger.i { "Received event in service: $it" }
                when (it) {
                    is CallRejectedEvent -> {
                        // When call is rejected by the caller
                        stopService()
                    }

                    is CallEndedEvent -> {
                        // When call ends for any reason
                        stopService()
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeCallAndSocket(
        streamVideo: StreamVideo,
        callId: StreamCallId,
    ) {
        // Update call
        updateCallJob = GlobalScope.launch(supervisorJob + Dispatchers.IO) {
            val call = streamVideo.call(callId.type, callId.id)
            val update = call.get()
            if (update.isFailure) {
                update.errorOrNull()?.let {
                    logger.e { it.message }
                } ?: let {
                    logger.e { "Failed to update call." }
                }
                stopService() // Failed to update call
                return@launch
            }
        }

        // Monitor coordinator socket
        startCoordinatorSocketJob = GlobalScope.launch(supervisorJob + Dispatchers.IO) {
            streamVideo.connectIfNotAlreadyConnected()
        }
    }

    override fun onDestroy() {
        stopService()
        super.onDestroy()
    }

    private fun stopService() {
        // Cancel the notification
        callId?.let {
            val notificationId = callId.hashCode()
            NotificationManagerCompat.from(this).cancel(notificationId)
        }
        // Stop any jobs
        observeRingingState?.cancel()
        updateRingingStateJob?.cancel()
        observeCallState?.cancel()
        updateCallJob?.cancel()
        startCoordinatorSocketJob?.cancel()
        supervisorJob.cancel()

        // Optionally (no-op if already stopping)
        stopSelf()
    }

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null
}
