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
import android.app.ActivityManager
import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.R
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.sounds.CallSoundPlayer
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.core.utils.safeCallWithResult
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
    private var callSoundPlayer: CallSoundPlayer? = null

    internal companion object {
        private const val TAG = "CallServiceCompanion"
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
            callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
        ): Intent {
            val serviceClass = callServiceConfiguration.serviceClass
            StreamLog.i(TAG) { "Resolved service class: $serviceClass" }
            val serviceIntent = Intent(context, serviceClass)
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
        fun buildStopIntent(
            context: Context,
            callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
        ) = safeCallWithDefault(Intent(context, CallService::class.java)) {
            val serviceClass = callServiceConfiguration.serviceClass

            if (isServiceRunning(context, serviceClass)) {
                Intent(context, serviceClass)
            } else {
                Intent(context, CallService::class.java)
            }
        }

        fun showIncomingCall(
            context: Context,
            callId: StreamCallId,
            callDisplayName: String?,
            callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
            notification: Notification?,
        ) {
            StreamLog.d(TAG) {
                "[showIncomingCall] callId: ${callId.id}, callDisplayName: $callDisplayName, notification: ${notification != null}"
            }
            val hasActiveCall = StreamVideo.instanceOrNull()?.state?.activeCall?.value != null
            StreamLog.d(TAG) { "[showIncomingCall] hasActiveCall: $hasActiveCall" }
            safeCallWithResult {
                val result = if (!hasActiveCall) {
                    StreamLog.d(TAG) { "[showIncomingCall] Starting foreground service" }
                    ContextCompat.startForegroundService(
                        context,
                        buildStartIntent(
                            context,
                            callId,
                            TRIGGER_INCOMING_CALL,
                            callDisplayName,
                            callServiceConfiguration,
                        ),
                    )
                    ComponentName(context, CallService::class.java)
                } else {
                    StreamLog.d(TAG) { "[showIncomingCall] Starting regular service" }
                    context.startService(
                        buildStartIntent(
                            context,
                            callId,
                            TRIGGER_INCOMING_CALL,
                            callDisplayName,
                            callServiceConfiguration,
                        ),
                    )
                }
                result!!
            }.onError {
                // Show notification
                StreamLog.e(TAG) { "Could not start service, showing notification only: $it" }
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                StreamLog.i(TAG) { "Has permission: $hasPermission" }
                StreamLog.i(TAG) { "Notification: $notification" }
                if (hasPermission && notification != null) {
                    StreamLog.d(TAG) {
                        "[showIncomingCall] Showing notification fallback with ID: $INCOMING_CALL_NOTIFICATION_ID"
                    }
                    NotificationManagerCompat.from(context)
                        .notify(INCOMING_CALL_NOTIFICATION_ID, notification)
                } else {
                    StreamLog.w(TAG) {
                        "[showIncomingCall] Cannot show notification - hasPermission: $hasPermission, notification: ${notification != null}"
                    }
                }
            }
        }

        fun removeIncomingCall(
            context: Context,
            callId: StreamCallId,
            config: CallServiceConfig = DefaultCallConfigurations.default,
        ) {
            safeCallWithResult {
                context.startService(
                    buildStartIntent(
                        context,
                        callId,
                        TRIGGER_REMOVE_INCOMING_CALL,
                        callServiceConfiguration = config,
                    ),
                )!!
            }.onError {
                NotificationManagerCompat.from(context).cancel(INCOMING_CALL_NOTIFICATION_ID)
            }
        }

        private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean =
            safeCallWithDefault(true) {
                val activityManager = context.getSystemService(
                    Context.ACTIVITY_SERVICE,
                ) as ActivityManager
                val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
                for (service in runningServices) {
                    if (serviceClass.name == service.service.className) {
                        StreamLog.w(TAG) { "Service is running: $serviceClass" }
                        return true
                    }
                }
                StreamLog.w(TAG) { "Service is NOT running: $serviceClass" }
                return false
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                        notificationId = notificationData.second,
                        notification = notification,
                    )
                } else {
                    logger.d { "[onStartCommand] Handling non-incoming call trigger: $trigger" }
                    callId = intentCallId

                    startForegroundWithServiceType(
                        intentCallId.hashCode(),
                        notification,
                        trigger,
                        serviceType,
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
                "Call id or streamVideo or trigger are not available. streamVideo: ${streamVideo != null}, intentCallId: ${intentCallId != null}, trigger: $trigger"
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

            if (callSoundPlayer == null) {
                callSoundPlayer = CallSoundPlayer(applicationContext)
            }
            logger.d {
                "[onStartCommand]. callSoundPlayer's hashcode: ${callSoundPlayer?.hashCode()}, Callservice hashcode: ${hashCode()}"
            }
            observeCall(intentCallId, streamVideo)
            registerToggleCameraBroadcastReceiver()
            return START_NOT_STICKY
        }
    }

    open fun getNotificationPair(
        trigger: String,
        streamVideo: StreamVideoClient,
        streamCallId: StreamCallId,
        intentCallDisplayName: String?,
    ): Pair<Notification?, Int> {
        logger.d {
            "[getNotificationPair] trigger: $trigger, callId: ${streamCallId.id}, callDisplayName: $intentCallDisplayName"
        }
        val notificationData: Pair<Notification?, Int> = when (trigger) {
            TRIGGER_ONGOING_CALL -> {
                logger.d { "[getNotificationPair] Creating ongoing call notification" }
                Pair(
                    first = streamVideo.getOngoingCallNotification(
                        callId = streamCallId,
                        callDisplayName = intentCallDisplayName,
                    ),
                    second = streamCallId.hashCode(),
                )
            }

            TRIGGER_INCOMING_CALL -> {
                logger.d { "[getNotificationPair] Creating incoming call notification" }
                val shouldHaveContentIntent = streamVideo.state.activeCall.value == null
                logger.d { "[getNotificationPair] shouldHaveContentIntent: $shouldHaveContentIntent" }
                Pair(
                    first = streamVideo.getRingingCallNotification(
                        ringingState = RingingState.Incoming(),
                        callId = streamCallId,
                        callDisplayName = intentCallDisplayName,
                        shouldHaveContentIntent = shouldHaveContentIntent,
                    ),
                    second = INCOMING_CALL_NOTIFICATION_ID,
                )
            }

            TRIGGER_OUTGOING_CALL -> {
                logger.d { "[getNotificationPair] Creating outgoing call notification" }
                Pair(
                    first = streamVideo.getRingingCallNotification(
                        ringingState = RingingState.Outgoing(),
                        callId = streamCallId,
                        callDisplayName = getString(
                            R.string.stream_video_outgoing_call_notification_title,
                        ),
                    ),
                    second = INCOMING_CALL_NOTIFICATION_ID, // Same for incoming and outgoing
                )
            }

            TRIGGER_REMOVE_INCOMING_CALL -> {
                logger.d { "[getNotificationPair] Removing incoming call notification" }
                Pair(null, INCOMING_CALL_NOTIFICATION_ID)
            }

            else -> {
                logger.w { "[getNotificationPair] Unknown trigger: $trigger" }
                Pair(null, streamCallId.hashCode())
            }
        }
        logger.d {
            "[getNotificationPair] Generated notification: ${notificationData.first != null}, notificationId: ${notificationData.second}"
        }
        return notificationData
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

    private fun justNotify(notificationId: Int, notification: Notification) {
        logger.d { "[justNotify] notificationId: $notificationId" }
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
            logger.d { "[justNotify] Notification shown with ID: $notificationId" }
        } else {
            logger.w {
                "[justNotify] Permission not granted, cannot show notification with ID: $notificationId"
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showIncomingCall(notificationId: Int, notification: Notification) {
        logger.d { "[showIncomingCall] notificationId: $notificationId" }
        val hasActiveCall = StreamVideo.instanceOrNull()?.state?.activeCall?.value != null
        logger.d { "[showIncomingCall] hasActiveCall: $hasActiveCall" }

        if (!hasActiveCall) { // If there isn't another call in progress
            // The service was started with startForegroundService() (from companion object), so we need to call startForeground().
            logger.d { "[showIncomingCall] Starting foreground service with notification" }
            startForegroundWithServiceType(
                notificationId,
                notification,
                TRIGGER_INCOMING_CALL,
                serviceType,
            ).onError {
                logger.e {
                    "[showIncomingCall] Failed to start foreground service, falling back to justNotify: $it"
                }
                justNotify(notificationId, notification)
            }
        } else {
            // Else, we show a simple notification (the service was already started as a foreground service).
            logger.d { "[showIncomingCall] Service already running, showing simple notification" }
            justNotify(notificationId, notification)
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
        observeNotificationUpdates(callId, streamVideo)
    }

    private fun observeRingingState(callId: StreamCallId, streamVideo: StreamVideoClient) {
        serviceScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            call.state.ringingState.collect {
                logger.i { "Ringing state: $it" }

                when (it) {
                    is RingingState.Incoming -> {
                        if (!it.acceptedByMe) {
                            callSoundPlayer?.playCallSound(
                                streamVideo.sounds.ringingConfig.incomingCallSoundUri,
                                streamVideo.sounds.mutedRingingConfig?.playIncomingSoundIfMuted
                                    ?: false,
                            )
                        } else {
                            callSoundPlayer?.stopCallSound() // Stops sound sooner than Active. More responsive.
                        }
                    }

                    is RingingState.Outgoing -> {
                        if (!it.acceptedByCallee) {
                            callSoundPlayer?.playCallSound(
                                streamVideo.sounds.ringingConfig.outgoingCallSoundUri,
                                streamVideo.sounds.mutedRingingConfig?.playOutgoingSoundIfMuted
                                    ?: false,
                            )
                        } else {
                            callSoundPlayer?.stopCallSound() // Stops sound sooner than Active. More responsive.
                        }
                    }

                    is RingingState.Active -> { // Handle Active to make it more reliable
                        callSoundPlayer?.stopCallSound()
                    }

                    is RingingState.RejectedByAll -> {
                        ClientScope().launch {
                            call.reject(RejectReason.Decline)
                        }
                        callSoundPlayer?.stopCallSound()
                        stopService()
                    }

                    is RingingState.TimeoutNoAnswer -> {
                        callSoundPlayer?.stopCallSound()
                    }

                    else -> {
                        callSoundPlayer?.stopCallSound()
                    }
                }
            }
        }
    }

    private fun observeCallEvents(callId: StreamCallId, streamVideo: StreamVideoClient) {
        serviceScope.launch {
            val call = streamVideo.call(callId.type, callId.id)
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
            val notificationUpdateTriggers = streamVideo.streamNotificationManager.notificationConfig.notificationUpdateTriggers(call) ?: combine(
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
                            justNotify(callId.hashCode(), notification)
                        }
                        is RingingState.Outgoing -> {
                            logger.d { "[observeNotificationUpdates] Showing outgoing call notification" }
                            justNotify(INCOMING_CALL_NOTIFICATION_ID, notification)
                        }
                        is RingingState.Incoming -> {
                            logger.d { "[observeNotificationUpdates] Showing incoming call notification" }
                            justNotify(INCOMING_CALL_NOTIFICATION_ID, notification)
                        }
                        else -> {
                            logger.d { "[observeNotificationUpdates] Unhandled ringing state: $ringingState" }
                        }
                    }
                } else {
                    logger.w {
                        "[observeNotificationUpdates] No notification generated for ringing state: $ringingState"
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
     */
    private fun stopService() {
        // Cancel the notification
        val notificationManager = NotificationManagerCompat.from(this)
        callId?.let {
            val notificationId = callId.hashCode()
            notificationManager.cancel(notificationId)

            logger.i { "[stopService]. Cancelled notificationId: $notificationId" }
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
        callSoundPlayer?.stopCallSound()

        // Stop any jobs
        serviceScope.cancel()

        // Optionally (no-op if already stopping)
        stopSelf()
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
