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

package io.getstream.video.android.core.notifications.internal.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.IBinder
import androidx.annotation.RawRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.R
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.core.utils.startForegroundWithServiceType
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallDisplayName
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent

/**
 * A foreground service that is running when there is an active call.
 */
internal class CallService : Service() {
    private val logger by taggedLogger("CallService")

    // Data
    private var callId: StreamCallId? = null

    // Service scope
    val handler = CoroutineExceptionHandler { _, exception ->
        logger.e(exception) { "[CallService#Scope] Uncaught exception: $exception" }
    }
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.IO + handler)

    // Camera handling receiver
    private val toggleCameraBroadcastReceiver = ToggleCameraBroadcastReceiver(serviceScope)
    private var isToggleCameraBroadcastReceiverRegistered = false

    // Call sounds
    private var mediaPlayer: MediaPlayer? = null

    internal companion object {
        const val TRIGGER_KEY =
            "io.getstream.video.android.core.notifications.internal.service.CallService.call_trigger"
        const val TRIGGER_INCOMING_CALL = "incoming_call"
        const val TRIGGER_REMOVE_INCOMING_CALL = "remove_call"
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

                TRIGGER_REMOVE_INCOMING_CALL -> {
                    serviceIntent.putExtra(TRIGGER_KEY, TRIGGER_REMOVE_INCOMING_CALL)
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

        fun showIncomingCall(context: Context, callId: StreamCallId, callDisplayName: String?) {
            val hasActiveCall = StreamVideo.instanceOrNull()?.state?.activeCall?.value != null

            if (!hasActiveCall) {
                ContextCompat.startForegroundService(
                    context,
                    buildStartIntent(
                        context,
                        callId,
                        TRIGGER_INCOMING_CALL,
                        callDisplayName,
                    ),
                )
            } else {
                context.startService(
                    buildStartIntent(
                        context,
                        callId,
                        TRIGGER_INCOMING_CALL,
                        callDisplayName,
                    ),
                )
            }
        }

        fun removeIncomingCall(context: Context, callId: StreamCallId) {
            context.startService(
                buildStartIntent(
                    context,
                    callId,
                    TRIGGER_REMOVE_INCOMING_CALL,
                ),
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val trigger = intent?.getStringExtra(TRIGGER_KEY)
        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoImpl

        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        val intentCallDisplayName = intent?.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME)

        logger.i { "[onStartCommand]. callId: ${intentCallId?.id}, trigger: $trigger" }

        val started = if (intentCallId != null && streamVideo != null && trigger != null) {
            // Promote early to foreground service
            maybePromoteToForegroundService(
                videoClient = streamVideo,
                notificationId = intentCallId.hashCode(),
                trigger,
            )

            val type = intentCallId.type
            val id = intentCallId.id
            val call = streamVideo.call(type, id)

            val permissionCheckPass =
                streamVideo.permissionCheck.checkAndroidPermissions(applicationContext, call)
            if (!permissionCheckPass) {
                // Crash early with a meaningful message if Call is used without system permissions.
                val exception = IllegalStateException(
                    "\nCallService attempted to start without required permissions (e.g. android.manifest.permission.RECORD_AUDIO).\n" + "This can happen if you call [Call.join()] without the required permissions being granted by the user.\n" + "If you are using compose and [LaunchCallPermissions] ensure that you rely on the [onRequestResult] callback\n" + "to ensure that the permission is granted prior to calling [Call.join()] or similar.\n" + "Optionally you can use [LaunchPermissionRequest] to ensure permissions are granted.\n" + "If you are not using the [stream-video-android-ui-compose] library,\n" + "ensure that permissions are granted prior calls to [Call.join()].\n" + "You can re-define your permissions and their expected state by overriding the [permissionCheck] in [StreamVideoBuilder]\n",
                )
                if (streamVideo.crashOnMissingPermission) {
                    throw exception
                } else {
                    logger.e(exception) { "Make sure all the required permissions are granted!" }
                }
            }

            val notificationData: Pair<Notification?, Int> = when (trigger) {
                TRIGGER_ONGOING_CALL -> Pair(
                    first = streamVideo.getOngoingCallNotification(
                        callDisplayName = intentCallDisplayName,
                        callId = intentCallId,
                    ),
                    second = intentCallId.hashCode(),
                )

                TRIGGER_INCOMING_CALL -> Pair(
                    first = streamVideo.getRingingCallNotification(
                        ringingState = RingingState.Incoming(),
                        callId = intentCallId,
                        callDisplayName = intentCallDisplayName!!,
                        shouldHaveContentIntent = streamVideo.state.activeCall.value == null,
                    ),
                    second = INCOMING_CALL_NOTIFICATION_ID,
                )

                TRIGGER_OUTGOING_CALL -> Pair(
                    first = streamVideo.getRingingCallNotification(
                        ringingState = RingingState.Outgoing(),
                        callId = intentCallId,
                        callDisplayName = getString(
                            R.string.stream_video_ongoing_call_notification_description,
                        ),
                    ),
                    second = INCOMING_CALL_NOTIFICATION_ID, // Same for incoming and outgoing
                )

                TRIGGER_REMOVE_INCOMING_CALL -> Pair(null, INCOMING_CALL_NOTIFICATION_ID)

                else -> Pair(null, intentCallId.hashCode())
            }

            val notification = notificationData.first
            if (notification != null) {
                if (trigger == TRIGGER_INCOMING_CALL) {
                    showIncomingCall(
                        notificationId = notificationData.second,
                        notification = notification,
                    )
                } else {
                    callId = intentCallId
                    startForegroundWithServiceType(intentCallId.hashCode(), notification, trigger)
                }
                true
            } else {
                if (trigger == TRIGGER_REMOVE_INCOMING_CALL) {
                    removeIncomingCall(notificationId = notificationData.second)
                    true
                } else {
                    // Service not started no notification
                    logger.e { "Could not get notification for ongoing call." }
                    false
                }
            }
        } else {
            // Service not started, no call Id or stream video
            logger.e { "Call id or streamVideo or trigger are not available." }
            false
        }

        if (!started) {
            logger.w { "Foreground service did not start!" }
            // Call stopSelf() and return START_REDELIVER_INTENT.
            // Because of stopSelf() the service is not restarted.
            // Because START_REDELIVER_INTENT is returned
            // the exception RemoteException: Service did not call startForeground... is not thrown.
            stopService()
            return START_REDELIVER_INTENT
        } else {
            initializeCallAndSocket(streamVideo!!, intentCallId!!)

            if (trigger == TRIGGER_INCOMING_CALL) {
                updateRingingCall(streamVideo, intentCallId, RingingState.Incoming())
                if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            } else if (trigger == TRIGGER_OUTGOING_CALL) {
                if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            }
            observeCallState(intentCallId, streamVideo)
            registerToggleCameraBroadcastReceiver()
            return START_NOT_STICKY
        }
    }

    private fun maybePromoteToForegroundService(videoClient: StreamVideoImpl, notificationId: Int, trigger: String) {
        val hasActiveCall = videoClient.state.activeCall.value != null
        val not = if (hasActiveCall) " not" else ""

        logger.d {
            "[maybePromoteToForegroundService] hasActiveCall: $hasActiveCall. Will$not call startForeground early."
        }

        if (!hasActiveCall) {
            videoClient.getSettingUpCallNotification()?.let {
                startForegroundWithServiceType(notificationId, it, trigger)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showIncomingCall(notificationId: Int, notification: Notification) {
        if (callId == null) { // If there isn't another call in progress (callId is set in onStartCommand())
            // The service was started with startForegroundService() (from companion object), so we need to call startForeground().
            startForegroundWithServiceType(notificationId, notification, TRIGGER_INCOMING_CALL)
        } else {
            // Else, we show a simple notification (the service was already started as a foreground service).
            NotificationManagerCompat
                .from(this)
                .notify(notificationId, notification)
        }
    }

    private fun removeIncomingCall(notificationId: Int) {
        NotificationManagerCompat.from(this).cancel(notificationId)

        if (callId == null) {
            stopService()
        }
    }

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

    private fun updateRingingCall(
        streamVideo: StreamVideo,
        callId: StreamCallId,
        ringingState: RingingState,
    ) {
        serviceScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
//            streamVideo.state.addRingingCall(call, ringingState) // TODO-Telecom: Wrap with isSupported
        }
    }

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
            call.subscribe { event ->
                logger.i { "Received event in service: $event" }
                when (event) {
                    is CallAcceptedEvent -> {
                        handleIncomingCallAcceptedByMeOnAnotherDevice(
                            acceptedByUserId = event.user.id,
                            myUserId = streamVideo.userId,
                            callRingingState = call.state.ringingState.value,
                        )
                    }

                    is CallRejectedEvent -> {
                        handleIncomingCallRejectedByMeOrCaller(
                            rejectedByUserId = event.user.id,
                            myUserId = streamVideo.userId,
                            createdByUserId = call.state.createdBy.value?.id,
                            activeCallExists = streamVideo.state.activeCall.value != null,
                        )
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

    private fun handleIncomingCallAcceptedByMeOnAnotherDevice(acceptedByUserId: String, myUserId: String, callRingingState: RingingState) {
        // If accepted event was received, with event user being me, but current device is still ringing, it means the call was accepted on another device
        if (acceptedByUserId == myUserId && callRingingState is RingingState.Incoming) {
            // So stop ringing on this device
            stopService()
        }
    }

    private fun handleIncomingCallRejectedByMeOrCaller(rejectedByUserId: String, myUserId: String, createdByUserId: String?, activeCallExists: Boolean) {
        // If rejected event was received (even from another device), with event user being me OR the caller, remove incoming call / stop service.
        if (rejectedByUserId == myUserId || rejectedByUserId == createdByUserId) {
            if (activeCallExists) {
                removeIncomingCall(INCOMING_CALL_NOTIFICATION_ID)
            } else {
                stopService()
            }
        }
    }

    private fun registerToggleCameraBroadcastReceiver() {
        serviceScope.launch {
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

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        logger.w { "Timeout received from the system, service will stop." }
        stopService()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        endCall()
        stopService()
    }

    private fun endCall() {
        callId?.let { callId ->
            StreamVideo.instanceOrNull()?.let { streamVideo ->
                val call = streamVideo.call(callId.type, callId.id)
                val ringingState = call.state.ringingState.value

                if (ringingState is RingingState.Outgoing) {
                    // If I'm calling, end the call for everyone
                    serviceScope.launch {
                        call.reject()
                        logger.i { "[onTaskRemoved] Ended outgoing call for all users." }
                    }
                } else if (ringingState is RingingState.Incoming) {
                    // If I'm receiving a call...
                    val memberCount = call.state.members.value.size
                    logger.i { "[onTaskRemoved] Total members: $memberCount" }
                    if (memberCount == 2) {
                        // ...and I'm the only one being called, end the call for both users
                        serviceScope.launch {
                            call.reject()
                            logger.i { "[onTaskRemoved] Ended incoming call for both users." }
                        }
                    } else {
                        // ...and there are other users other than me and the caller, end the call just for me
                        call.leave()
                        logger.i { "[onTaskRemoved] Ended incoming call for me." }
                    }
                } else {
                    // If I'm in an ongoing call, end the call for me
                    call.leave()
                    logger.i { "[onTaskRemoved] Ended ongoing call for me." }
                }
            }
        }
    }

    override fun onDestroy() {
        stopService()
        super.onDestroy()
    }

    override fun stopService(name: Intent?): Boolean {
        stopService()
        return super.stopService(name)
    }

    /**
     * Handle all aspects of stopping the service.
     */
    private fun stopService() {
        // Cancel the notification
        val notificationManager = NotificationManagerCompat.from(this)
        callId?.let {
            val notificationId = callId.hashCode()
            notificationManager.cancel(notificationId)

            logger.i { "[stopService]. Cancelled notification: $notificationId" }
        }

        // Optionally cancel any incoming call notification
        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID)
        logger.i { "[stopService]. Cancelled incoming call notification: $INCOMING_CALL_NOTIFICATION_ID" }

        // Camera privacy
        unregisterToggleCameraBroadcastReceiver()

        // Call sounds
        clearMediaPlayer()

        // Stop any jobs
        serviceScope.cancel()

        // Optionally (no-op if already stopping)
        stopSelf()
    }

    private fun clearMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null
}
