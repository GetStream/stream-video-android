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
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.handlers.StreamDefaultNotificationHandler
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.sounds.CallSoundPlayer
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

    // Service type
    open val serviceType: Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL

    // Data
//    private var callId: StreamCallId? = null

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
    private var callSoundPlayer: CallSoundPlayer? = null
    private val serviceNotificationRetriever = ServiceNotificationRetriever()

    // Data classes for better organization
    private data class StartCommandParams(
        val trigger: String,
        val streamVideo: StreamVideoClient,
        val callId: StreamCallId,
        val callDisplayName: String?,
    )

    private data class ServiceStartResult(
        val success: Boolean,
        val shouldContinue: Boolean = true,
    )

    internal companion object {
        private const val TAG = "CallServiceCompanion"
        const val TRIGGER_KEY =
            "io.getstream.video.android.core.notifications.internal.service.CallService.call_trigger"
        const val TRIGGER_INCOMING_CALL = "incoming_call"
        const val TRIGGER_REMOVE_INCOMING_CALL = "remove_call"
        const val TRIGGER_OUTGOING_CALL = "outgoing_call"
        const val TRIGGER_ONGOING_CALL = "ongoing_call"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val trigger = intent?.getStringExtra(TRIGGER_KEY)
        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient

        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)

        logger.d {
            "[onStartCommand] Starting with intent trigger:$trigger, streamVideo: ${streamVideo != null}, intentCallId: ${intentCallId != null}, callId: ${intentCallId?.id}, trigger: $trigger, Callservice hashcode: ${hashCode()}, startId = $startId"
        }
        return handleStartCommand(intent, startId)
    }

    private fun handleStartCommand(intent: Intent?, startId: Int): Int {
        // Handle media intent early
        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        maybeHandleMediaIntent(intent, intentCallId)

        // Validate and extract parameters
        val params = extractAndValidateParams(intent)
            ?: return handleStartCommandFailure("Invalid parameters")

        // Handle the specific trigger
        val result = handleTrigger(params)

        return if (result.success && result.shouldContinue) {
            initializeServiceForCall(params)
            START_NOT_STICKY
        } else {
            handleStartCommandFailure("Service failed to start properly")
        }
    }

    private fun extractAndValidateParams(intent: Intent?): StartCommandParams? {
        val trigger = intent?.getStringExtra(TRIGGER_KEY)
        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient
        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        val intentCallDisplayName = intent?.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME)

        logger.d {
            "[extractAndValidateParams] streamVideo: ${streamVideo != null}, " +
                "intentCallId: ${intentCallId != null}, trigger: $trigger"
        }

        return if (intentCallId != null && streamVideo != null && trigger != null) {
            // Validate permissions early
            if (!validatePermissions(streamVideo, intentCallId)) {
                return null
            }

            StartCommandParams(trigger, streamVideo, intentCallId, intentCallDisplayName)
        } else {
            logger.e {
                "Missing required parameters - streamVideo: ${streamVideo != null}, " +
                    "intentCallId: ${intentCallId != null}, trigger: $trigger"
            }
            null
        }
    }

    private fun initializeServiceForCall(params: StartCommandParams) {
        logger.d { "[initializeServiceForCall] Initializing for call: ${params.callId.id}" }

        initializeCallAndSocket(params.streamVideo, params.callId)

        if (params.trigger == TRIGGER_INCOMING_CALL) {
            updateRingingCall(params.streamVideo, params.callId, RingingState.Incoming())
        }

        initializeCallSoundPlayer()
        observeCall(params.callId, params.streamVideo)
        registerToggleCameraBroadcastReceiver()
    }

    private fun initializeCallSoundPlayer() {
        if (callSoundPlayer == null) {
            callSoundPlayer = CallSoundPlayer(applicationContext)
            logger.d {
                "[initializeCallSoundPlayer] Created callSoundPlayer with hashcode: ${callSoundPlayer?.hashCode()}, " +
                    "service hashcode: ${hashCode()}"
            }
        }
    }

    private fun handleTrigger(params: StartCommandParams): ServiceStartResult {
        return when (params.trigger) {
            TRIGGER_INCOMING_CALL -> handleIncomingCallTrigger(params)
            TRIGGER_OUTGOING_CALL -> handleOutgoingCallTrigger(params)
            TRIGGER_ONGOING_CALL -> handleOngoingCallTrigger(params)
            TRIGGER_REMOVE_INCOMING_CALL -> handleRemoveIncomingCallTrigger(params)
            else -> {
                logger.e { "Unknown trigger: ${params.trigger}" }
                ServiceStartResult(success = false)
            }
        }
    }

    private fun handleIncomingCallTrigger(params: StartCommandParams): ServiceStartResult {
        logger.d { "[handleIncomingCallTrigger] Processing incoming call for: ${params.callId.id}" }

        maybePromoteToForegroundService(
            params.streamVideo,
            params.callId.hashCode(),
            params.trigger,
        )

        val (notification, notificationId) = getNotificationPair(
            params.trigger,
            params.streamVideo,
            params.callId,
            params.callDisplayName,
        )

        return if (notification != null) {
            showIncomingCall(params.callId, notificationId, notification)
            ServiceStartResult(success = true)
        } else {
            logger.e { "Could not get notification for incoming call: ${params.callId.id}" }
            ServiceStartResult(success = false)
        }
    }

    private fun handleOutgoingCallTrigger(params: StartCommandParams): ServiceStartResult {
        logger.d { "[handleOutgoingCallTrigger] Processing outgoing call for: ${params.callId.id}" }

        return handleStandardCallTrigger(params)
    }

    private fun handleOngoingCallTrigger(params: StartCommandParams): ServiceStartResult {
        logger.d { "[handleOngoingCallTrigger] Processing ongoing call for: ${params.callId.id}" }

        return handleStandardCallTrigger(params)
    }

    private fun handleRemoveIncomingCallTrigger(params: StartCommandParams): ServiceStartResult {
        logger.d { "[handleRemoveIncomingCallTrigger] Removing incoming call: ${params.callId.id}" }

        val (_, notificationId) = getNotificationPair(
            params.trigger,
            params.streamVideo,
            params.callId,
            params.callDisplayName,
        )

        removeIncomingCall(notificationId)
        return ServiceStartResult(success = true, shouldContinue = false)
    }

    private fun handleStandardCallTrigger(params: StartCommandParams): ServiceStartResult {
        val (notification, notificationId) = getNotificationPair(
            params.trigger,
            params.streamVideo,
            params.callId,
            params.callDisplayName,
        )

        return if (notification != null) {
            StreamVideo.instanceOrNull()?.let {
                it.state.callServiceRepository.addCallId(params.callId)
            }

            val call = params.streamVideo.call(params.callId.type, params.callId.id)
            call.state.updateNotification(notification)

            startForegroundWithServiceType(
                notificationId,
                notification,
                params.trigger,
                serviceType,
            )
            ServiceStartResult(success = true)
        } else {
            logger.e {
                "Could not get notification for trigger: ${params.trigger}, callId: ${params.callId.id}"
            }
            ServiceStartResult(success = false)
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

    private fun validatePermissions(streamVideo: StreamVideoClient, callId: StreamCallId): Boolean {
        val call = streamVideo.call(callId.type, callId.id)
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
                return false
            }
        }
        return true
    }

    open fun getNotificationPair(
        trigger: String,
        streamVideo: StreamVideoClient,
        streamCallId: StreamCallId,
        intentCallDisplayName: String?,
    ): Pair<Notification?, Int> {
        return serviceNotificationRetriever.getNotificationPair(
            this,
            trigger,
            streamVideo,
            streamCallId,
            intentCallDisplayName,
        )
    }

    private fun handleStartCommandFailure(reason: String): Int {
        logger.w { "[handleStartCommandFailure] $reason - stopping service" }
        stopService()
        return START_REDELIVER_INTENT
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
                    serviceType,
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
                serviceType,
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
        if(StreamVideo.instanceOrNull()?.state?.callServiceRepository?.callId == null) {
            stopService()
        }
    }

    /**
     * This is failing and stopping the service
     * The service has larger scope than a call, multiple calls and co-exits within same service
     * Any error in service shouldn't directly stop the service unless there is no calls left
     */
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
                    logger.e { "[initializeCallAndSocket] Failed for callid:${call.id} ${it.message}" }
                } ?: let {
                    logger.e { "[initializeCallAndSocket] Failed for callid:${call.id} to update call." }
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
                logger.i { "[observeRingingState] Ringing state: $it" }

                when (it) {
                    is RingingState.Incoming -> {
                        if (!it.acceptedByMe) {
                            call.state.startRingingTimer()
                            callSoundPlayer?.playCallSound(
                                callId,
                                streamVideo.sounds.ringingConfig.incomingCallSoundUri,
                                streamVideo.sounds.mutedRingingConfig?.playIncomingSoundIfMuted
                                    ?: false,
                            )
                        } else {
                            if(true) throw IllegalStateException("Why the fuck I am here?")
                            callSoundPlayer?.stopCallSound(callId,"RingingState.Incoming but accepted by me") // Stops sound sooner than Active. More responsive.
                        }
                    }

                    is RingingState.Outgoing -> {
                        if (!it.acceptedByCallee) {
                            call.state.startRingingTimer()
                            callSoundPlayer?.playCallSound(
                                callId,
                                streamVideo.sounds.ringingConfig.outgoingCallSoundUri,
                                streamVideo.sounds.mutedRingingConfig?.playOutgoingSoundIfMuted
                                    ?: false,
                            )
                        } else {
                            if(true) throw IllegalStateException("Why the fuck I am here?")
                            callSoundPlayer?.stopCallSound(callId, "RingState is outgoing but accepted by me") // Stops sound sooner than Active. More responsive.
                        }
                    }

                    is RingingState.Active -> { // Handle Active to make it more reliable
                        call.state.cancelTimeout()
                        callSoundPlayer?.stopCallSound(callId, "Ringing State is active")
                    }

                    is RingingState.RejectedByAll -> {
                        call.state.cancelTimeout()
                        ClientScope().launch {
                            call.reject(RejectReason.Decline)
                        }
                        callSoundPlayer?.stopCallSound(callId, "Ringing State is rejected by all")
                        stopService()
                    }

                    is RingingState.TimeoutNoAnswer -> {
                        call.state.cancelTimeout()
                        callSoundPlayer?.stopCallSound(callId, "Ringstate state reached time out")
                    }

                    is RingingState.Idle -> {
                        //Do nothing
                    }

                    is RingingState.ActiveOnOtherDevice -> {
                        call.state.cancelTimeout()
                        callSoundPlayer?.stopCallSound(callId, "Ringstate state is in else case: $it")
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

    private fun handleIncomingCallRejectedByMeOrCaller(
        rejectedByUserId: String,
        myUserId: String,
        createdByUserId: String?,
        activeCallExists: Boolean,
    ) {
        // If rejected event was received (even from another device), with event user being me OR the caller, remove incoming call / stop service.
        if (rejectedByUserId == myUserId || rejectedByUserId == createdByUserId) {
            if (activeCallExists) {
                removeIncomingCall(INCOMING_CALL_NOTIFICATION_ID)
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
                                serviceType,
                            )
                        }

                        is RingingState.Outgoing -> {
                            logger.d { "[observeNotificationUpdates] Showing outgoing call notification" }
                            startForegroundWithServiceType(
                                INCOMING_CALL_NOTIFICATION_ID,
                                notification,
                                TRIGGER_OUTGOING_CALL,
                                serviceType,
                            )
                        }

                        is RingingState.Incoming -> {
                            logger.d { "[observeNotificationUpdates] Showing incoming call notification" }
                            startForegroundWithServiceType(
                                INCOMING_CALL_NOTIFICATION_ID,
                                notification,
                                TRIGGER_INCOMING_CALL,
                                serviceType,
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

        StreamVideo.instanceOrNull()?.state?.callServiceRepository?.callId?.let { callId ->
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
        logger.d { "[onDestroy], Callservice hashcode: ${hashCode()}" }
        stopService()
        callSoundPlayer?.cleanUpAudioResources()
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
        StreamVideo.instanceOrNull()?.state?.callServiceRepository?.callId?.let { callId->
            val notificationId = callId.hashCode()
            notificationManager.cancel(notificationId)

            logger.i { "[stopService]. Cancelled notificationId: $notificationId" }
        }

        safeCall {
            StreamVideo.instanceOrNull()?.state?.callServiceRepository?.callId?.let { callId ->

                val handler = streamDefaultNotificationHandler()
                handler?.clearMediaSession(callId)
            }
        }

        // Optionally cancel any incoming call notification
        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID)
        logger.i { "[stopService]. Cancelled incoming call notificationId: $INCOMING_CALL_NOTIFICATION_ID" }

        // Camera privacy
        unregisterToggleCameraBroadcastReceiver()

        // Call sounds
        /**
         * Temp Fix!! The observeRingingState scope was getting cancelled and as a result,
         * ringing state was not properly updated
         */
        callSoundPlayer?.forceStopCallSound()

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
