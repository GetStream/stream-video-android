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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.android.video.generated.models.LocalCallMissedEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.handlers.StreamDefaultNotificationHandler
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.sounds.CallSoundAndVibrationPlayer
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.startForegroundWithServiceType
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallDisplayName
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * A foreground service that is running when there is an active call.
 */
internal open class CallService : Service() {
    internal open val logger by taggedLogger("CallService")

    @SuppressLint("InlinedApi")
    internal open val requiredForegroundTypes: Set<Int> = setOf(
        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
    )

    /**
     * Map each service type to the permission it requires (if any).
     * Subclasses can reuse or extend this mapping.
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK] requires Q
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL] requires Q
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA] requires R
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE] requires R
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE] requires UPSIDE_DOWN_CAKE
     */

    @SuppressLint("InlinedApi")
    internal open val foregroundTypePermissionsMap: Map<Int, String?> = mapOf(
        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA to Manifest.permission.CAMERA,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE to Manifest.permission.RECORD_AUDIO,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK to null, // playback doesn’t need permission
        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL to null,
    )

    private fun getServiceTypeForStartingFGService(trigger: String): Int {
        return when (trigger) {
            CallService.TRIGGER_ONGOING_CALL -> { serviceType }
            else -> noPermissionServiceType()
        }
    }

    open val serviceType: Int
        @SuppressLint("InlinedApi")
        get() {
            return if (hasAllPermission(baseContext)) {
                hasAllPermissionServiceType()
            } else {
                noPermissionServiceType()
            }
        }

    private fun hasAllPermissionServiceType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // or of all requiredForegroundTypes types
            requiredForegroundTypes.reduce { acc, type -> acc or type }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            androidQServiceType()
        } else {
            /**
             * Android Pre-Q Service Type (no need to bother)
             * We don't start foreground service with type
             */
            0
        }
    }

    @SuppressLint("InlinedApi")
    internal open fun noPermissionServiceType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
        }
    }

    @SuppressLint("InlinedApi")
    internal open fun androidQServiceType() = if (requiredForegroundTypes.contains(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
        )
    ) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
    } else {
        /**
         *  Existing behavior
         *  [ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE] requires [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]
         */
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
    }

    @RequiresApi(Build.VERSION_CODES.R)
    internal fun hasAllPermission(context: Context): Boolean {
        return requiredForegroundTypes.all { type ->
            val permission = foregroundTypePermissionsMap[type]
            permission == null || ContextCompat.checkSelfPermission(
                context,
                permission,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Data
    private var callId: StreamCallId? = null

    // Service scope
    val handler = CoroutineExceptionHandler { _, exception ->
        logger.e(exception) { "[CallService#Scope] Uncaught exception: $exception" }
    }
    private val serviceScope: CoroutineScope =
        CoroutineScope(Dispatchers.IO + handler + SupervisorJob())

    // Camera handling receiver
    private val toggleCameraBroadcastReceiver = ToggleCameraBroadcastReceiver(serviceScope)
    private var isToggleCameraBroadcastReceiverRegistered = false

    // Call sounds
    private var callSoundPlayer: CallSoundAndVibrationPlayer? = null
    private val serviceNotificationRetriever = ServiceNotificationRetriever()

    internal companion object {
        private const val TAG = "CallServiceCompanion"
        const val TRIGGER_KEY =
            "io.getstream.video.android.core.notifications.internal.service.CallService.call_trigger"
        const val TRIGGER_INCOMING_CALL = "incoming_call"
        const val TRIGGER_REMOVE_INCOMING_CALL = "remove_call"
        const val TRIGGER_OUTGOING_CALL = "outgoing_call"
        const val TRIGGER_ONGOING_CALL = "ongoing_call"
        const val EXTRA_STOP_SERVICE = "io.getstream.video.android.core.stop_service"
    }

    private fun shouldStopServiceFromIntent(intent: Intent?): Boolean {
        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        val shouldStopService = intent?.getBooleanExtra(EXTRA_STOP_SERVICE, false) ?: false
        if (callId != null && callId == intentCallId && shouldStopService) {
            logger.d { "shouldStopServiceFromIntent: true, call_cid:${intentCallId?.cid}" }
            return true
        }
        logger.d { "shouldStopServiceFromIntent: false, call_cid:${intentCallId?.cid}" }
        return false
    }

    /**
     * Useful when we events come late and we get [CallRejected] Event for an expired call
     * This logic is triggered when we want to stop the service [EXTRA_STOP_SERVICE] which is
     * usually when we get [CallRejected] event
     */
    private fun isIntentForExpiredCall(intent: Intent?): Boolean {
        var isCallExpired = false
        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        val shouldStopService = intent?.getBooleanExtra(EXTRA_STOP_SERVICE, false) ?: false
        if (shouldStopService) {
            intentCallId?.let {
                StreamVideo.instanceOrNull()?.let { streamVideo ->
                    val call = streamVideo.call(intentCallId.type, intentCallId.id)

                    isCallExpired = call.state.ringingState.value is RingingState.Idle
                    logger.d {
                        "[isIntentForExpiredCall] isCallExpired:$isCallExpired, call_id: ${intentCallId.cid}, ringing state: ${call.state.ringingState.value}, "
                    }
                }
            }
        }
        return isCallExpired // message:[handlePushMessage], [showIncomingCall] callId, [reject] #ringing;
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d { "[onStartCommand], intent = $intent, flags:$flags, startId:$startId" }
        if (intent != null) {
            val bundle = intent.extras
            val keys = bundle?.keySet()
            if (keys != null) {
                val sb = StringBuilder()
                for (key in keys) {
                    val itemInBundle = bundle[key]
                    val text = "key:$key, value=$itemInBundle"
                    sb.append(text)
                    sb.append("\n")
                }
                if (sb.toString().isNotEmpty()) {
                    logger.d { "[onStartCommand], intent extras: $sb" }
                }
            }
        }

        // STOP SERVICE LOGIC STARTS
        if (shouldStopServiceFromIntent(intent)) {
            stopService()
            return START_NOT_STICKY
        }
        // STOP SERVICE LOGIC ENDS

        if (isIntentForExpiredCall(intent)) {
            return START_NOT_STICKY
        }

        val trigger = intent?.getStringExtra(TRIGGER_KEY)
        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient

        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        val intentCallDisplayName = intent?.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME)

        logger.i {
            "[onStartCommand]. callId: ${intentCallId?.id}, trigger: $trigger, Callservice hashcode: ${hashCode()}"
        }
        logger.d {
            "[onStartCommand] streamVideo: ${streamVideo != null}, intentCallId: ${intentCallId != null}, trigger: $trigger"
        }

        maybeHandleMediaIntent(intent, intentCallId)

        val started = if (intentCallId != null && streamVideo != null && trigger != null) {
            logger.d { "[onStartCommand] All required parameters available, proceeding with service start" }
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
                streamVideo.permissionCheck.checkAndroidPermissionsGroup(applicationContext, call)
            if (!permissionCheckPass.first) {
                // Crash early with a meaningful message if Call is used without system permissions.
                val missingPermissions = permissionCheckPass.second.joinToString(",")
                val exception = IllegalStateException(
                    """
                        CallService attempted to start without required permissions $missingPermissions.
                        Details: call_id:$callId, trigger:$trigger,
                        This can happen if you call [Call.join()] without the required permissions being granted by the user.
                        If you are using compose and [LaunchCallPermissions] ensure that you rely on the [onRequestResult] callback
                        to ensure that the permission is granted prior to calling [Call.join()] or similar.
                        Optionally you can use [LaunchPermissionRequest] to ensure permissions are granted.
                        If you are not using the [stream-video-android-ui-compose] library,
                        ensure that permissions are granted prior calls to [Call.join()].
                        You can re-define your permissions and their expected state by overriding the [permissionCheck] in [StreamVideoBuilder]
                    """.trimIndent(),
                )
                if (streamVideo.crashOnMissingPermission) {
                    throw exception
                } else {
                    logger.e(exception) { "Make sure all the required permissions are granted!" }
                }
            }

            logger.d {
                "[onStartCommand] Getting notification for trigger: $trigger, callId: ${intentCallId.id}"
            }
            val notificationData: Pair<Notification?, Int> =
                getNotificationPair(trigger, streamVideo, intentCallId, intentCallDisplayName)

            val notification = notificationData.first
            logger.d {
                "[onStartCommand] Notification generated: ${notification != null}, notificationId: ${notificationData.second}"
            }
            if (notification != null) {
                if (trigger == TRIGGER_INCOMING_CALL) {
                    logger.d { "[onStartCommand] Handling incoming call trigger" }
                    showIncomingCall(
                        callId = intentCallId,
                        notificationId = notificationData.second,
                        notification = notification,
                    )
                } else {
                    logger.d { "[onStartCommand] Handling non-incoming call trigger: $trigger" }
                    callId = intentCallId

                    call.state.updateNotification(notification)

                    startForegroundWithServiceType(
                        intentCallId.hashCode(),
                        notification,
                        trigger,
                        getServiceTypeForStartingFGService(trigger),
                    )
                }
                true
            } else {
                if (trigger == TRIGGER_REMOVE_INCOMING_CALL) {
                    logger.d { "[onStartCommand] Removing incoming call" }
                    removeIncomingCall(notificationId = notificationData.second)
                    true
                } else {
                    // Service not started no notification
                    logger.e { "Could not get notification for trigger: $trigger, callId: ${intentCallId.id}" }
                    false
                }
            }
        } else {
            // Service not started, no call Id or stream video
            logger.e {
                "Call id or streamVideo or trigger are not available. streamVideo is not null: ${streamVideo != null}, intentCallId is not null: ${intentCallId != null}, trigger: $trigger"
            }
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
            }

            callSoundAndVibrationPlayer = streamVideo.callSoundAndVibrationPlayer

            logger.d {
                "[onStartCommand]. callSoundPlayer's hashcode: ${callSoundAndVibrationPlayer?.hashCode()}, Callservice hashcode: ${hashCode()}"
            }
            observeCall(intentCallId, streamVideo)
            registerToggleCameraBroadcastReceiver()
            return START_NOT_STICKY
        }
    }

    private fun maybeHandleMediaIntent(intent: Intent?, callId: StreamCallId?) = safeCall {
        val handler = streamDefaultNotificationHandler()
        if (handler != null && callId != null) {
            val isMediaNotification = notificationConfig().mediaNotificationCallTypes.contains(
                callId.type,
            )
            if (isMediaNotification) {
                logger.d { "[maybeHandleMediaIntent] Handling media intent" }
                MediaButtonReceiver.handleIntent(
                    handler.mediaSession(callId),
                    intent,
                )
            }
        }
    }

    open fun getNotificationPair(
        trigger: String,
        streamVideo: StreamVideoClient,
        streamCallId: StreamCallId,
        intentCallDisplayName: String?,
    ): Pair<Notification?, Int> {
        return serviceNotificationRetriever.getNotificationPair(
            applicationContext,
            trigger,
            streamVideo,
            streamCallId,
            intentCallDisplayName,
        )
    }

    private fun maybePromoteToForegroundService(
        videoClient: StreamVideoClient,
        notificationId: Int,
        trigger: String,
    ) {
        val hasActiveCall = videoClient.state.activeCall.value != null
        val not = if (hasActiveCall) " not" else ""

        logger.d {
            "[maybePromoteToForegroundService] hasActiveCall: $hasActiveCall. Will$not call startForeground early."
        }

        if (!hasActiveCall) {
            videoClient.getSettingUpCallNotification()?.let { notification ->
                startForegroundWithServiceType(
                    notificationId,
                    notification,
                    trigger,
                    getServiceTypeForStartingFGService(trigger),
                )
            }
        }
    }

    private fun justNotify(callId: StreamCallId, notificationId: Int, notification: Notification) {
        logger.d { "[justNotify] notificationId: $notificationId" }
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            StreamVideo.instanceOrNull()?.getStreamNotificationDispatcher()?.notify(
                callId,
                notificationId,
                notification,
            )
            logger.d { "[justNotify] Notification shown with ID: $notificationId" }
        } else {
            logger.w {
                "[justNotify] Permission not granted, cannot show notification with ID: $notificationId"
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showIncomingCall(
        callId: StreamCallId,
        notificationId: Int,
        notification: Notification,
    ) {
        logger.d { "[showIncomingCall] notificationId: $notificationId" }
        val hasActiveCall = StreamVideo.instanceOrNull()?.state?.activeCall?.value != null
        logger.d { "[showIncomingCall] hasActiveCall: $hasActiveCall" }

        if (!hasActiveCall) { // If there isn't another call in progress
            // The service was started with startForegroundService() (from companion object), so we need to call startForeground().
            logger.d { "[showIncomingCall] Starting foreground service with notification" }

            StreamVideo.instanceOrNull()?.call(callId.type, callId.id)
                ?.state?.updateNotification(notification)

            startForegroundWithServiceType(
                notificationId,
                notification,
                TRIGGER_INCOMING_CALL,
                getServiceTypeForStartingFGService(TRIGGER_INCOMING_CALL),
            ).onError {
                logger.e {
                    "[showIncomingCall] Failed to start foreground service, falling back to justNotify: $it"
                }
                justNotify(callId, notificationId, notification)
            }
        } else {
            // Else, we show a simple notification (the service was already started as a foreground service).
            logger.d { "[showIncomingCall] Service already running, showing simple notification" }
            justNotify(callId, notificationId, notification)
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
            streamVideo.state.addRingingCall(call, ringingState)
        }
    }

    private fun observeCall(callId: StreamCallId, streamVideo: StreamVideoClient) {
        observeRingingState(callId, streamVideo)
        observeCallEvents(callId, streamVideo)
        if (streamVideo.enableCallNotificationUpdates) {
            observeNotificationUpdates(callId, streamVideo)
        }
    }

    private fun observeRingingState(callId: StreamCallId, streamVideo: StreamVideoClient) {
        val call = streamVideo.call(callId.type, callId.id)
        call.scope.launch {
            call.state.ringingState.collect {
                logger.i { "Ringing state: $it" }

                when (it) {
                    is RingingState.Incoming -> {
                        if (!it.acceptedByMe) {
                            logger.d { "[vibrate] Vibration config: ${streamVideo.vibrationConfig}" }
                            if (streamVideo.vibrationConfig.enabled) {
                                val pattern = streamVideo.vibrationConfig.vibratePattern
                                callSoundAndVibrationPlayer?.vibrate(pattern)
                            }
                            callSoundAndVibrationPlayer?.playCallSound(
                                streamVideo.sounds.ringingConfig.incomingCallSoundUri,
                                streamVideo.sounds.mutedRingingConfig?.playIncomingSoundIfMuted
                                    ?: false,
                            )
                        } else {
                            callSoundAndVibrationPlayer?.stopCallSound() // Stops sound sooner than Active. More responsive.
                        }
                    }

                    is RingingState.Outgoing -> {
                        if (!it.acceptedByCallee) {
                            callSoundAndVibrationPlayer?.playCallSound(
                                streamVideo.sounds.ringingConfig.outgoingCallSoundUri,
                                streamVideo.sounds.mutedRingingConfig?.playOutgoingSoundIfMuted
                                    ?: false,
                            )
                        } else {
                            callSoundAndVibrationPlayer?.stopCallSound() // Stops sound sooner than Active. More responsive.
                        }
                    }

                    is RingingState.Active -> { // Handle Active to make it more reliable
                        callSoundAndVibrationPlayer?.stopCallSound()
                    }

                    is RingingState.RejectedByAll -> {
                        ClientScope().launch {
                            call.reject(
                                source = "RingingState.RejectedByAll",
                                RejectReason.Decline,
                            )
                        }
                        callSoundAndVibrationPlayer?.stopCallSound()
                        stopService()
                    }

                    is RingingState.TimeoutNoAnswer -> {
                        callSoundAndVibrationPlayer?.stopCallSound()
                    }

                    else -> {
                        callSoundAndVibrationPlayer?.stopCallSound()
                    }
                }
            }
        }
    }

    private fun observeCallEvents(callId: StreamCallId, streamVideo: StreamVideoClient) {
        val call = streamVideo.call(callId.type, callId.id)
        /**
         * This scope will be cleaned as soon as call is destroyed via rejection/decline
         */
        call.scope.launch {
            call.events.collect { event ->
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
                            call,
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

                    is LocalCallMissedEvent -> handleSlowCallRejectedEvent(call)
                }
            }
        }

        call.scope.launch {
            call.state.connection.collectLatest { event ->
                when (event) {
                    is RealtimeConnection.Failed -> {
                        if (call.id == streamVideo.state.ringingCall.value?.id) {
                            streamVideo.state.removeRingingCall(call)
                            streamVideo.onCallCleanUp(call)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun handleIncomingCallAcceptedByMeOnAnotherDevice(
        acceptedByUserId: String,
        myUserId: String,
        callRingingState: RingingState,
    ) {
        // If accepted event was received, with event user being me, but current device is still ringing, it means the call was accepted on another device
        if (acceptedByUserId == myUserId && callRingingState is RingingState.Incoming) {
            // So stop ringing on this device
            stopService()
        }
    }

    private fun handleSlowCallRejectedEvent(call: Call) {
        val callId = StreamCallId(call.type, call.id)
        removeIncomingCall(callId.getNotificationId(NotificationType.Incoming))
    }

    private fun handleIncomingCallRejectedByMeOrCaller(
        call: Call,
        rejectedByUserId: String,
        myUserId: String,
        createdByUserId: String?,
        activeCallExists: Boolean,
    ) {
        // If rejected event was received (even from another device), with event user being me OR the caller, remove incoming call / stop service.
        if (rejectedByUserId == myUserId || rejectedByUserId == createdByUserId) {
            if (activeCallExists) {
                val callId = StreamCallId(call.type, call.id)
                removeIncomingCall(callId.getNotificationId(NotificationType.Incoming))
            } else {
                stopService()
            }
        }
    }

    @OptIn(ExperimentalStreamVideoApi::class)
    private fun observeNotificationUpdates(callId: StreamCallId, streamVideo: StreamVideoClient) {
        serviceScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            logger.d { "Observing notification updates for call: ${call.cid}" }
            val notificationUpdateTriggers =
                streamVideo.streamNotificationManager.notificationConfig.notificationUpdateTriggers(
                    call,
                ) ?: combine(
                    call.state.ringingState,
                    call.state.members,
                    call.state.remoteParticipants,
                    call.state.backstage,
                ) { ringingState, members, remoteParticipants, backstage ->
                    listOf(ringingState, members, remoteParticipants, backstage)
                }.distinctUntilChanged()

            notificationUpdateTriggers.collectLatest { state ->
                val ringingState = call.state.ringingState.value
                logger.d { "[observeNotificationUpdates] ringingState: $ringingState" }
                val notification = streamVideo.onCallNotificationUpdate(
                    call = call,
                )
                logger.d { "[observeNotificationUpdates] notification: ${notification != null}" }
                if (notification != null) {
                    when (ringingState) {
                        is RingingState.Active -> {
                            logger.d { "[observeNotificationUpdates] Showing active call notification" }
                            startForegroundWithServiceType(
                                callId.hashCode(),
                                notification,
                                TRIGGER_ONGOING_CALL,
                                getServiceTypeForStartingFGService(TRIGGER_ONGOING_CALL),
                            )
                        }

                        is RingingState.Outgoing -> {
                            logger.d { "[observeNotificationUpdates] Showing outgoing call notification" }
                            startForegroundWithServiceType(
                                callId.getNotificationId(NotificationType.Incoming),
                                notification,
                                TRIGGER_OUTGOING_CALL,
                                getServiceTypeForStartingFGService(TRIGGER_OUTGOING_CALL),
                            )
                        }

                        is RingingState.Incoming -> {
                            logger.d { "[observeNotificationUpdates] Showing incoming call notification" }
                            startForegroundWithServiceType(
                                callId.getNotificationId(NotificationType.Incoming),
                                notification,
                                TRIGGER_INCOMING_CALL,
                                getServiceTypeForStartingFGService(TRIGGER_INCOMING_CALL),
                            )
                        }

                        else -> {
                            logger.d { "[observeNotificationUpdates] Unhandled ringing state: $ringingState" }
                        }
                    }
                } else {
                    logger.w {
                        "[observeNotificationUpdates] No notification generated for updating."
                    }
                }
            }
        }
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
                        call.reject(
                            "CallService.EndCall",
                            RejectReason.Custom("Android Service Task Removed"),
                        )
                        logger.i { "[onTaskRemoved] Ended outgoing call for all users." }
                    }
                } else if (ringingState is RingingState.Incoming) {
                    // If I'm receiving a call...
                    val memberCount = call.state.members.value.size
                    logger.i { "[onTaskRemoved] Total members: $memberCount" }
                    if (memberCount == 2) {
                        // ...and I'm the only one being called, end the call for both users
                        serviceScope.launch {
                            call.reject(source = "memberCount == 2")
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
        logger.d { "[onDestroy], Callservice hashcode: ${hashCode()}, call_cid: ${callId?.cid}" }
        stopService()
        callSoundAndVibrationPlayer?.cleanUpAudioResources()
        super.onDestroy()
    }

    override fun stopService(name: Intent?): Boolean {
        logger.d { "[stopService(name)], Callservice hashcode: ${hashCode()}" }
        stopService()
        return super.stopService(name)
    }

    /**
     * Handle all aspects of stopping the service.
     * Should be invoke carefully for the calls which are still present in [StreamVideoClient.calls]
     * Else stopping service by an expired call can cancel current call's notification and the service itself
     */
    private fun stopService() {
        // Cancel the notification
        val notificationManager = NotificationManagerCompat.from(this)
        callId?.let {
            val notificationId = callId.hashCode()
            notificationManager.cancel(notificationId)

            logger.i { "[stopService]. Cancelled notificationId: $notificationId" }
        }

        safeCall {
            val handler = streamDefaultNotificationHandler()
            handler?.clearMediaSession(callId)
        }

        // Optionally cancel any incoming call notification
        val incomingNotificationId = callId?.getNotificationId(NotificationType.Incoming)
        callId?.let {
            notificationManager.cancel(it.getNotificationId(NotificationType.Incoming))
            logger.i { "[stopService]. Cancelled incoming call notificationId: $incomingNotificationId" }
        }

        // Camera privacy
        unregisterToggleCameraBroadcastReceiver()

        // Call sounds
        /**
         * Temp Fix!! The observeRingingState scope was getting cancelled and as a result,
         * ringing state was not properly updated
         */
        callSoundAndVibrationPlayer?.stopCallSound()

        // Stop any jobs
        serviceScope.cancel()

        // Optionally (no-op if already stopping)
        stopSelf()
    }

    private fun streamDefaultNotificationHandler(): StreamDefaultNotificationHandler? {
        val client = StreamVideo.instanceOrNull() as StreamVideoClient
        val handler =
            client.streamNotificationManager.notificationConfig.notificationHandler as? StreamDefaultNotificationHandler
        return handler
    }

    private fun notificationConfig(): NotificationConfig {
        val client = StreamVideo.instanceOrNull() as StreamVideoClient
        return client.streamNotificationManager.notificationConfig
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

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null
}
