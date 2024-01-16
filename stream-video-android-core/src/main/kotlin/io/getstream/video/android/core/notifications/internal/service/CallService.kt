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
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.annotation.RawRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.R
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallDisplayName
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

/**
 * A foreground service that is running when there is an active call.
 */
internal class CallService : Service() {
    private val logger by taggedLogger("CallService")

    // Data
    private var callId: StreamCallId? = null
    private var callDisplayName: String? = null

    // Service scope
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    // Camera handling receiver
    private val toggleCameraBroadcastReceiver = ToggleCameraBroadcastReceiver(serviceScope)
    private var isToggleCameraBroadcastReceiverRegistered = false

    // Call sounds
    private var mediaPlayer: MediaPlayer? = null

    internal companion object {
        const val TRIGGER_KEY =
            "io.getstream.video.android.core.notifications.internal.service.CallService.call_trigger"
        const val TRIGGER_INCOMING_CALL = "incoming_call"
        const val TRIGGER_OUTGOING_CALL = "outgoing_call"
        const val TRIGGER_ONGOING_CALL = "ongoing_call"

        /**
         * Build start intent.
         *
         * @param context the context.
         * @param callId the call id.
         * @param trigger one of [TRIGGER_INCOMING_CALL], [TRIGGER_OUTGOING_CALL] or [TRIGGER_ONGOING_CALL]
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

                TRIGGER_OUTGOING_CALL -> {
                    serviceIntent.putExtra(TRIGGER_KEY, TRIGGER_OUTGOING_CALL)
                }

                TRIGGER_ONGOING_CALL -> {
                    serviceIntent.putExtra(TRIGGER_KEY, TRIGGER_ONGOING_CALL)
                }

                else -> {
                    throw IllegalArgumentException(
                        "Unknown $trigger, must be one of: $TRIGGER_INCOMING_CALL, $TRIGGER_OUTGOING_CALL, $TRIGGER_ONGOING_CALL",
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Leave the call
        callId?.let {
            StreamVideo.instanceOrNull()?.call(it.type, it.id)?.leave()
            logger.i { "Left ongoing call." }
        }

        // Stop the service
        stopService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.i { "Starting CallService. $intent" }
        callId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        callDisplayName = intent?.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME)
        val trigger = intent?.getStringExtra(TRIGGER_KEY)
        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoImpl
        val started = if (callId != null && streamVideo != null && trigger != null) {
            val notificationData: Pair<Notification?, Int> =
                when (trigger) {
                    TRIGGER_ONGOING_CALL -> Pair(
                        first = streamVideo.getOngoingCallNotification(
                            callId = callId!!,
                        ),
                        second = callId.hashCode(),
                    )

                    TRIGGER_INCOMING_CALL -> Pair(
                        first = streamVideo.getRingingCallNotification(
                            ringingState = RingingState.Incoming(),
                            callId = callId!!,
                            callDisplayName = callDisplayName!!,
                        ),
                        second = INCOMING_CALL_NOTIFICATION_ID,
                    )

                    TRIGGER_OUTGOING_CALL -> Pair(
                        first = streamVideo.getRingingCallNotification(
                            ringingState = RingingState.Outgoing(),
                            callId = callId!!,
                            callDisplayName = getString(
                                R.string.stream_video_ongoing_call_notification_description,
                            ),
                        ),
                        second = INCOMING_CALL_NOTIFICATION_ID, // Same for incoming and outgoing
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
                            TRIGGER_OUTGOING_CALL -> ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
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
                logger.e { "Could not get notification for ongoing call." }
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
            initializeCallAndSocket(streamVideo!!, callId!!)

            if (trigger == TRIGGER_INCOMING_CALL) {
                updateRingingCall(streamVideo, callId!!, RingingState.Incoming())
                if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            } else if (trigger == TRIGGER_OUTGOING_CALL) {
                if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            }
            observeCallState(callId!!, streamVideo)
            registerToggleCameraBroadcastReceiver()
        }
        return START_NOT_STICKY
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateRingingCall(
        streamVideo: StreamVideo,
        callId: StreamCallId,
        ringingState: RingingState,
    ) {
        serviceScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            streamVideo.state.addRingingCall(call, ringingState)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun observeCallState(callId: StreamCallId, streamVideo: StreamVideoImpl) {
        // Ringing state
        serviceScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            call.state.ringingState.collect {
                logger.i { "Ringing state: $it" }
                when (it) {
                    is RingingState.Incoming -> {
                        if (!it.acceptedByMe) {
                            playCallSound(streamVideo.sounds.incomingCallSound)
                        } else {
                            stopCallSound() // Stops sound sooner than Active. More responsive.
                        }
                    }
                    is RingingState.Outgoing -> {
                        if (!it.acceptedByCallee) {
                            playCallSound(streamVideo.sounds.outgoingCallSound)
                        } else {
                            stopCallSound() // Stops sound sooner than Active. More responsive.
                        }
                    }
                    is RingingState.Active -> { // Handle Active to make it more reliable
                        stopCallSound()
                    }
                    is RingingState.RejectedByAll -> {
                        stopCallSound()
                        stopService()
                    }
                    is RingingState.TimeoutNoAnswer -> {
                        stopCallSound()
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
        }

        // Call state
        serviceScope.launch {
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

    private fun playCallSound(@RawRes sound: Int?) {
        sound?.let {
            try {
                mediaPlayer?.let {
                    if (!it.isPlaying) {
                        setMediaPlayerDataSource(it, sound)
                        it.start()
                    }
                }
            } catch (e: IllegalStateException) {
                logger.d { "Error playing call sound." }
            }
        }
    }

    private fun setMediaPlayerDataSource(mediaPlayer: MediaPlayer, @RawRes resId: Int) {
        mediaPlayer.reset()
        val afd = resources.openRawResourceFd(resId)
        if (afd != null) {
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
        }
        mediaPlayer.isLooping = true
        mediaPlayer.prepare()
    }

    private fun stopCallSound() {
        try {
            if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
        } catch (e: IllegalStateException) {
            logger.d { "Error stopping call sound. MediaPlayer might have already been released." }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeCallAndSocket(
        streamVideo: StreamVideo,
        callId: StreamCallId,
    ) {
        // Update call
        serviceScope.launch {
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
        serviceScope.launch {
            streamVideo.connectIfNotAlreadyConnected()
        }
    }

    override fun onDestroy() {
        stopService()
        super.onDestroy()
    }

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null

    // Internal logic
    /**
     * Handle all aspects of stopping the service.
     */
    private fun stopService() {
        // Cancel the notification
        val notificationManager = NotificationManagerCompat.from(this)
        callId?.let {
            val notificationId = callId.hashCode()
            notificationManager.cancel(notificationId)
        }

        // Optionally cancel any incoming call notification
        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID)

        // Camera privacy
        unregisterToggleCameraBroadcastReceiver()

        // Call sounds
        clearMediaPlayer()

        // Stop any jobs
        serviceScope.cancel()

        // Optionally (no-op if already stopping)
        stopSelf()
    }
    private fun registerToggleCameraBroadcastReceiver() {
        if (!isToggleCameraBroadcastReceiverRegistered) {
            try {
                registerReceiver(
                    toggleCameraBroadcastReceiver,
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                        addAction(Intent.ACTION_USER_PRESENT)
                    },
                )
                isToggleCameraBroadcastReceiverRegistered = true
            } catch (e: Exception) {
                logger.d { "Unable to register ToggleCameraBroadcastReceiver." }
            }
        }
    }

    private fun unregisterToggleCameraBroadcastReceiver() {
        if (isToggleCameraBroadcastReceiverRegistered) {
            try {
                unregisterReceiver(toggleCameraBroadcastReceiver)
                isToggleCameraBroadcastReceiverRegistered = false
            } catch (e: Exception) {
                logger.d { "Unable to unregister ToggleCameraBroadcastReceiver." }
            }
        }
    }

    private fun clearMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
