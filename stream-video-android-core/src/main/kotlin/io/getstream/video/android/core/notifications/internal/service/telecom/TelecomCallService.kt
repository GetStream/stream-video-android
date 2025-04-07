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

package io.getstream.video.android.core.notifications.internal.service.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Connection
import android.telecom.Connection.STATE_RINGING
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.R
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A ConnectionService replacement for your old CallService.
 *
 * This handles INCOMING and OUTGOING calls via Telecom.
 * - No onStartCommand(...)
 * - Instead, onCreateIncomingConnection(...) / onCreateOutgoingConnection(...)
 *
 * It reuses your old logic for camera toggles, call sound, call event subscription, etc.
 */
internal class TelecomCallService : ConnectionService() {
    private val logger by taggedLogger("TelecomCallService")

    // We'll keep a scope for any coroutines we launch
    private val handler = CoroutineExceptionHandler { _, exception ->
        logger.e(exception) { "[TelecomCallService] Uncaught exception: $exception" }
    }
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.IO + handler)

    // For toggling the camera on screen on/off
    private val toggleCameraBroadcastReceiver = ToggleCameraBroadcastReceiver(serviceScope)
    private var isToggleCameraBroadcastReceiverRegistered = false

    // Audio-related
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var ringtone: Ringtone? = null

    // ---------------------------------------------------------------------------------------
    // INCOMING CALLS
    // ---------------------------------------------------------------------------------------
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest,
    ): Connection {
        // 1) Parse call info from request.extras
        val extras: Bundle = request.extras
        val intentCallId = extras.getString(NotificationHandler.INTENT_EXTRA_CALL_CID)
        val intentCallDisplayName = extras.getString(
            NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME,
        )

        logger.i {
            "#telecom; [onCreateIncomingConnection] callId: $intentCallId, cid hash: ${intentCallId.hashCode()}, name: $intentCallDisplayName"
        }

        // 2) If no callId or StreamVideo client, we can reject or return a failed Connection
        val streamVideoClient = StreamVideo.instanceOrNull() as? StreamVideoClient
        if (streamVideoClient == null) {
            logger.e { "StreamVideoClient is null" }
        }
        val streamVideo = streamVideoClient
            ?: return Connection.createFailedConnection(DisconnectCause(DisconnectCause.ERROR))
        if (intentCallId == null) {
            return Connection.createFailedConnection(DisconnectCause(DisconnectCause.ERROR))
        }

        // 3) Create a custom Connection
        val callId = StreamCallId.fromCallCid(intentCallId)
        val connection = TelecomConnection.createAndStore(
            context = applicationContext,
            callId = callId,
            callConfig = streamVideo.callServiceConfigRegistry.get(callId.type),
            displayName = intentCallDisplayName ?: "Unknown caller",
            isRinging = true,
        )

        showIncomingNotification(connection, streamVideo)

        serviceScope.launch {
            initializeCallAndSocket(streamVideo, callId)
            updateRingingCall(streamVideo, callId, RingingState.Incoming())
            instantiateMediaPlayer()
            observeCall(callId, streamVideo)
            registerToggleCameraBroadcastReceiver()
        }

        logger.d {
            "[onCreateIncomingConnection] #telecom; Created TelecomConnection ${connection.hashCode()}"
        }

        return connection
    }

    // ---------------------------------------------------------------------------------------
    // OUTGOING CALLS
    // ---------------------------------------------------------------------------------------
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest,
    ): Connection {
        val extras: Bundle = request.extras
        val intentCallId = extras.getString(NotificationHandler.INTENT_EXTRA_CALL_CID)
        val intentCallDisplayName = extras.getString(
            NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME,
        )

        logger.i {
            "[onCreateOutgoingConnection] #telecom; callId: $intentCallId, name: $intentCallDisplayName"
        }

        // If no callId or no client
        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient
            ?: return Connection.createFailedConnection(DisconnectCause(DisconnectCause.ERROR))
        if (intentCallId == null) {
            return Connection.createFailedConnection(DisconnectCause(DisconnectCause.ERROR))
        }

        val callId = StreamCallId.fromCallCid(intentCallId)
        val connection = TelecomConnection.createAndStore(
            context = applicationContext,
            callId = callId,
            callConfig = streamVideo.callServiceConfigRegistry.get(callId.type),
            displayName = intentCallDisplayName ?: getString(R.string.stream_video_outgoing_call_notification_title),
            isDialing = true,
        )

        // Optionally show a custom notification if you want
        showOutgoingNotification(connection, streamVideo)

        // Copy your old "outgoing" logic
        serviceScope.launch {
            initializeCallAndSocket(streamVideo, callId)
            instantiateMediaPlayer()
            observeCall(callId, streamVideo)
            registerToggleCameraBroadcastReceiver()
        }

        logger.d {
            "[onCreateOutgoingConnection] #telecom; Created TelecomConnection ${connection.hashCode()}"
        }

        return connection
    }

    // ---------------------------------------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------------------------------------
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        logger.i { "[onTaskRemoved]" }
        // You can define your logic to end or leave the call here if the user swipes the app away
        // ...
        serviceScope.cancel()
    }

    override fun onDestroy() {
        logger.i { "[onDestroy]" }
        unregisterToggleCameraBroadcastReceiver()
        cleanAudioResources()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------------
    // REPLACED LOGIC from old `CallService`
    // ---------------------------------------------------------------------------------------

    /**
     * Called from our serviceScope (see onCreateIncomingConnection or onCreateOutgoingConnection).
     * Mimics your old code's "initializeCallAndSocket" logic.
     */
    private suspend fun initializeCallAndSocket(
        streamVideo: StreamVideoClient,
        callId: StreamCallId,
    ) {
        val call = streamVideo.call(callId.type, callId.id)
        val result = call.get()
        if (result.isFailure) {
            logger.e { "Failed to update call: ${result.errorOrNull()}" }
            // Optionally end the connection or show error
            return
        }

        // Monitor coordinator socket if needed
        safeCallWithResult {
            streamVideo.connectIfNotAlreadyConnected()
        }
    }

    private fun updateRingingCall(
        streamVideo: StreamVideoClient,
        callId: StreamCallId,
        ringingState: RingingState,
    ) {
        serviceScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            streamVideo.state.addRingingCall(call, ringingState)
        }
    }

    private fun instantiateMediaPlayer() {
        synchronized(this) {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
        }
    }

    private fun observeCall(callId: StreamCallId, streamVideo: StreamVideoClient) {
        observeRingingState(callId, streamVideo)
        observeCallEvents(callId, streamVideo)
        // Optionally observe notification updates if you still want dynamic notifications
    }

    private fun observeRingingState(callId: StreamCallId, streamVideo: StreamVideoClient) {
        serviceScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            call.state.ringingState.collect { state ->
                logger.i { "Ringing state: $state" }
                when (state) {
                    is RingingState.Incoming -> {
                        if (!state.acceptedByMe) {
                            playCallSound(streamVideo.sounds.ringingConfig.incomingCallSoundUri)
                        } else {
                            stopCallSound()
                        }
                    }

                    is RingingState.Outgoing -> {
                        if (!state.acceptedByCallee) {
                            playCallSound(streamVideo.sounds.ringingConfig.outgoingCallSoundUri)
                        } else {
                            stopCallSound()
                        }
                    }

                    is RingingState.Active -> {
                        stopCallSound()
                    }

                    is RingingState.RejectedByAll -> {
                        stopCallSound()
                        // Possibly disconnect
                        endCall(callId)
                    }

                    is RingingState.TimeoutNoAnswer -> {
                        stopCallSound()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun playCallSound(soundUri: Uri?) {
        try {
            synchronized(this) {
                requestAudioFocus(
                    context = applicationContext,
                    onGranted = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            playWithRingtone(soundUri)
                        } else {
                            playWithMediaPlayer(soundUri)
                        }
                    },
                )
            }
        } catch (e: Exception) {
            logger.d { "[Sounds] Error playing call sound: ${e.message}" }
        }
    }

    private fun requestAudioFocus(context: Context, onGranted: () -> Unit) {
        if (audioManager == null) {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .build()
            }
            audioManager?.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_RING,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        if (isGranted) onGranted()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun playWithRingtone(soundUri: Uri?) {
        soundUri?.let {
            if (ringtone?.isPlaying == true) ringtone?.stop()
            ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
            if (ringtone?.isPlaying == false) {
                ringtone?.isLooping = true
                ringtone?.play()
            }
        }
    }

    private fun playWithMediaPlayer(soundUri: Uri?) {
        soundUri?.let {
            mediaPlayer?.let { mp ->
                if (!mp.isPlaying) {
                    mp.reset()
                    mp.setDataSource(applicationContext, it)
                    mp.isLooping = true
                    mp.prepare()
                    mp.start()
                }
            }
        }
    }

    private fun stopCallSound() {
        synchronized(this) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (ringtone?.isPlaying == true) ringtone?.stop()
                } else {
                    if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
                }
            } catch (e: Exception) {
                logger.d { "[Sounds] Error stopping call sound: ${e.message}" }
            } finally {
                abandonAudioFocus()
            }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            audioManager?.abandonAudioFocus(null)
        }
    }

    private fun observeCallEvents(callId: StreamCallId, streamVideo: StreamVideoClient) {
        serviceScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            call.subscribe { event ->
                logger.i { "Received event in TelecomCallService: $event" }
                when (event) {
                    is CallAcceptedEvent -> {
                        handleIncomingCallAcceptedByMeOnAnotherDevice(
                            event.user.id,
                            streamVideo.userId,
                            call.state.ringingState.value,
                        )

                        telecomConnections[callId.cid]?.let {
                            showOngoingNotification(it, streamVideo)
                        }
                    }
                    is CallRejectedEvent -> {
                        handleIncomingCallRejectedByMeOrCaller(
                            event.user.id,
                            streamVideo.userId,
                            call.state.createdBy.value?.id,
                            streamVideo.state.activeCall.value != null,
                            callId,
                        )

                        cancelNotification(notificationIdFromCallId(callId))
                    }
                    is CallEndedEvent -> {
                        stopCallSound()
                        cancelNotification(notificationIdFromCallId(callId))
                    }
                }
            }
        }
    }

    private fun handleIncomingCallAcceptedByMeOnAnotherDevice(
        acceptedByUserId: String,
        myUserId: String,
        callRingingState: RingingState,
    ) {
        if (acceptedByUserId == myUserId && callRingingState is RingingState.Incoming) {
            // Stop ringing on this device
            stopCallSound()
        }
    }

    private fun handleIncomingCallRejectedByMeOrCaller(
        rejectedByUserId: String,
        myUserId: String,
        createdByUserId: String?,
        activeCallExists: Boolean,
        callId: StreamCallId,
    ) {
        if (rejectedByUserId == myUserId || rejectedByUserId == createdByUserId) {
            // This device no longer needs to ring or handle the call
            stopCallSound()
            endCall(callId)
        }
    }

    /**
     * Example method to remove any notifications and do a final cleanup on a call.
     */
    private fun endCall(callId: StreamCallId) {
        // Cancel any ongoing notifications
        NotificationManagerCompat.from(this).cancel(callId.hashCode())

        // Optionally do more logic, e.g. call.leave() or call.reject() if you want to end
        // the call for everyone. Up to your design.

        // Also destroy the connections if needed. The MyVoipConnection might do it in onDisconnect().
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
                    logger.d { "Unable to register ToggleCameraBroadcastReceiver: $e" }
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
                logger.d { "Unable to unregister ToggleCameraBroadcastReceiver: $e" }
            }
        }
    }

    private fun cleanAudioResources() {
        logger.d { "[Sounds] Cleaning audio resources" }
        if (ringtone?.isPlaying == true) ringtone?.stop()
        ringtone = null
        mediaPlayer?.release()
        mediaPlayer = null
        audioManager = null
        audioFocusRequest = null
    }

    // ---------------------------------------------------------------------------------------
    // NOTIFICATION LOGIC (OPTIONAL)
    // ---------------------------------------------------------------------------------------

    private fun showIncomingNotification(connection: TelecomConnection, streamVideo: StreamVideo) {
        val notification = streamVideo.getRingingCallNotification(
            ringingState = RingingState.Incoming(),
            callId = connection.callId,
            callDisplayName = connection.callerDisplayName,
            shouldHaveContentIntent = streamVideo.state.activeCall.value == null,
        )

        if (notification != null) {
            postNotification(notification, connection, streamVideo)
        }
    }

    private fun showOutgoingNotification(connection: TelecomConnection, streamVideo: StreamVideo) {
        val notification = streamVideo.getRingingCallNotification(
            ringingState = RingingState.Outgoing(),
            callId = connection.callId,
            callDisplayName = connection.callerDisplayName ?: getString(R.string.stream_video_outgoing_call_notification_title),
        )

        if (notification != null) {
            postNotification(notification, connection, streamVideo)
        }
    }

    private fun showOngoingNotification(connection: TelecomConnection, streamVideo: StreamVideo) {
        val notification = streamVideo.getOngoingCallNotification(
            callId = connection.callId,
            isOutgoingCall = false,
        )

        if (notification != null) {
            postNotification(notification, connection, streamVideo)
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(
        notification: Notification,
        connection: TelecomConnection,
        streamVideo: StreamVideo,
    ) {
        if (!hasNotificationsPermission()) {
            logger.e { "[postNotification] #telecom; POST_NOTIFICATIONS permission not granted" }
            return
        } else {
            val notify: (Notification) -> Unit = {
                logger.i {
                    "[postNotification] #telecom; Posting notification for connection ${connection.hashCode()}"
                }
                NotificationManagerCompat
                    .from(applicationContext)
                    .notify(notificationIdFromCallId(connection.callId), it)
            }

            cancelNotification(notificationIdFromCallId(connection.callId))

            if (connection.state == STATE_RINGING) {
                notify(notification)
            } else {
                if (connection.callConfig.runCallServiceInForeground) {
                    notify(notification)
                }
            }
        }
    }

    private fun cancelNotification(notificationId: Int) {
        logger.d { "[cancelNotification] #telecom;" }
        NotificationManagerCompat.from(applicationContext).cancel(notificationId)
    }

    private fun hasNotificationsPermission(): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
}
