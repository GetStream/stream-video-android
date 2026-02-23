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

package io.getstream.video.android.core.notifications.internal.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.handlers.StreamDefaultNotificationHandler
import io.getstream.video.android.core.notifications.internal.Debouncer
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.EXTRA_STOP_SERVICE
import io.getstream.video.android.core.notifications.internal.service.controllers.ServiceStateController
import io.getstream.video.android.core.notifications.internal.service.managers.CallServiceLifecycleManager
import io.getstream.video.android.core.notifications.internal.service.managers.CallServiceNotificationManager
import io.getstream.video.android.core.notifications.internal.service.models.CallIntentParams
import io.getstream.video.android.core.notifications.internal.service.observers.CallServiceEventObserver
import io.getstream.video.android.core.notifications.internal.service.observers.CallServiceNotificationUpdateObserver
import io.getstream.video.android.core.notifications.internal.service.observers.CallServiceRingingStateObserver
import io.getstream.video.android.core.notifications.internal.service.permissions.ForegroundServicePermissionManager
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
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * A foreground service that is running when there is an active call.
 */
internal open class CallService : Service() {
    internal open val logger by taggedLogger("CallService")
    internal open val permissionManager = ForegroundServicePermissionManager()

    internal val callServiceLifecycleManager = CallServiceLifecycleManager()

    internal val serviceStateController = ServiceStateController()

    /**
     * A debouncer used to delay the final stopping of the service.
     *
     * This is a workaround for an Android framework behavior where killing a Foreground Service
     * too quickly (e.g., within ~2 seconds) can prevent its associated notification from being
     * dismissed, especially if the notification tray is open. By debouncing the stop action,
     * we ensure enough time has passed for the system to process the notification removal.
     */
    internal val debouncer = Debouncer()
    internal val serviceScope: CoroutineScope =
        CoroutineScope(Dispatchers.IO.limitedParallelism(1) + handler + SupervisorJob())

    private val notificationManager =
        CallServiceNotificationManager(serviceStateController, serviceScope)

    open val serviceType: Int
        @SuppressLint("InlinedApi")
        get() {
            return if (permissionManager.hasAllPermissions(baseContext)) {
                permissionManager.allPermissionsServiceType()
            } else {
                permissionManager.noPermissionServiceType()
            }
        }

    private val serviceNotificationRetriever = ServiceNotificationRetriever()

    internal companion object {
        private const val TAG = "CallServiceCompanion"
        private const val TRIGGER_INCOMING_CALL = "incoming_call"
        private const val TRIGGER_REMOVE_INCOMING_CALL = "remove_call"
        private const val TRIGGER_OUTGOING_CALL = "outgoing_call"
        private const val TRIGGER_ONGOING_CALL = "ongoing_call"
        internal const val TRIGGER_SHARE_SCREEN = "share_screen"
        const val EXTRA_STOP_SERVICE = "io.getstream.video.android.core.stop_service"

        const val SERVICE_DESTROY_THRESHOLD_TIME_MS = 2_000L
        const val SERVICE_DESTROY_THROTTLE_TIME_MS = 1_000L

        private val logger by taggedLogger("CallService")

        val handler = CoroutineExceptionHandler { _, exception ->
            logger.e(exception) { "[CallService#Scope] Uncaught exception: $exception" }
        }

        sealed class Trigger private constructor(val name: String) {
            object RemoveIncomingCall : Trigger(TRIGGER_REMOVE_INCOMING_CALL)
            object OutgoingCall : Trigger(TRIGGER_OUTGOING_CALL)
            object OnGoingCall : Trigger(TRIGGER_ONGOING_CALL)
            object IncomingCall : Trigger(TRIGGER_INCOMING_CALL)
            object ShareScreen : Trigger(TRIGGER_SHARE_SCREEN)
            object None : Trigger("none")
            override fun toString(): String = name

            internal companion object {
                const val TRIGGER_KEY =
                    "io.getstream.video.android.core.notifications.internal.service.CallService.call_trigger"

                fun toTrigger(trigger: String): Trigger {
                    return when (trigger) {
                        TRIGGER_REMOVE_INCOMING_CALL -> Trigger.RemoveIncomingCall
                        TRIGGER_OUTGOING_CALL -> Trigger.OutgoingCall
                        TRIGGER_ONGOING_CALL -> Trigger.OnGoingCall
                        TRIGGER_INCOMING_CALL -> Trigger.IncomingCall
                        TRIGGER_SHARE_SCREEN -> Trigger.ShareScreen
                        else -> Trigger.None
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceStateController.setStartTime(SystemClock.elapsedRealtime())
    }

    private fun shouldStopService(intent: Intent?): Boolean {
        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        val shouldStopService = intent?.getBooleanExtra(EXTRA_STOP_SERVICE, false) ?: false
        logger.d {
            "[shouldStopService]: service hashcode: ${this.hashCode()}, serviceState.currentCallId!=null : ${serviceStateController.currentCallId != null}, serviceState.currentCallId == intentCallId && shouldStopService : ${serviceStateController.currentCallId == intentCallId && shouldStopService}"
        }

        if (serviceStateController.state.value.currentCallId != null && serviceStateController.currentCallId == intentCallId && shouldStopService) {
            logger.d { "[shouldStopService]: true, call_cid:${intentCallId?.cid}" }
            return true
        }
        logger.d { "[shouldStopService]: false, call_cid:${intentCallId?.cid}" }
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

    private fun logIntentExtras(intent: Intent?) {
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d { "[onStartCommand], intent = $intent, flags:$flags, startId:$startId" }
        logIntentExtras(intent)
        // Early exit conditions
        when {
            shouldStopService(intent) -> {
                stopServiceGracefully()
                return START_NOT_STICKY
            }

            isIntentForExpiredCall(intent) -> return START_NOT_STICKY
        }

        val params = extractIntentParams(intent) ?: run {
            logger.e { "Failed to extract required parameters from intent" }
            stopServiceGracefully()
            return START_REDELIVER_INTENT
        }

        return handleCallIntent(params, intent, flags, startId)
    }

    private fun handleCallIntent(
        params: CallIntentParams,
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        maybeHandleMediaIntent(intent, params.callId)
        val notificationId = serviceNotificationRetriever.getNotificationId(
            params.trigger,
            params.streamVideo,
            params.callId,
        )

        /**
         * Mandatory, if not called then it will throw exception if we directly decide to stop the service.
         * For example: it will stop the service if [verifyPermissions] is false
         */
        promoteToFgServiceIfNoActiveCall(
            params.streamVideo,
            notificationId,
            params.trigger,
        )
        val call = params.streamVideo.call(params.callId.type, params.callId.id)

        // Rendering incoming call does not need audio/video permissions
        if (params.trigger != Trigger.IncomingCall) {
            if (!verifyPermissions(
                    params.streamVideo,
                    call,
                    params.callId,
                    params.trigger,
                )
            ) {
                stopServiceGracefully()
                return START_NOT_STICKY
            }
        }

        val (notification, _) = getNotificationPair(
            params.trigger,
            params.streamVideo,
            params.callId,
            params.displayName,
        )

        val handleNotificationResult = handleNotification(
            notification,
            notificationId,
            params.callId,
            params.trigger,
            call,
        )

        return when (handleNotificationResult) {
            CallServiceHandleNotificationResult.START -> {
                initializeService(params.streamVideo, call, params.trigger)
                START_NOT_STICKY
            }

            CallServiceHandleNotificationResult.REDELIVER -> {
                logger.w { "Foreground service did not start!" }
                stopServiceGracefully()
                START_REDELIVER_INTENT
            }

            CallServiceHandleNotificationResult.START_NO_CHANGE -> START_NOT_STICKY
        }
    }

    private fun initializeService(
        streamVideo: StreamVideoClient,
        call: Call,
        trigger: Trigger,
    ) {
        callServiceLifecycleManager.initializeCallAndSocket(
            serviceScope,
            streamVideo,
            call,
        ) { error ->
            stopServiceGracefully(error?.message)
        }

        if (trigger == Trigger.IncomingCall) {
            callServiceLifecycleManager.updateRingingCall(
                serviceScope,
                streamVideo,
                call,
                RingingState.Incoming(),
            )
        }

        serviceStateController.setSoundPlayer(streamVideo.callSoundAndVibrationPlayer)
        logger.d {
            "[initializeService] soundPlayer's hashcode: ${serviceStateController.soundPlayer?.hashCode()}"
        }

        observeCall(call, streamVideo)
        serviceStateController.registerToggleCameraBroadcastReceiver(this, serviceScope)
    }

    private fun handleNotification(
        notification: Notification?,
        notificationId: Int,
        callId: StreamCallId,
        trigger: CallService.Companion.Trigger,
        call: Call,
    ): CallServiceHandleNotificationResult {
        logHandleStart(trigger, call, notificationId)

        if (notification == null) { // TODO Rahul check if livestream guest/host will get stuck here
            return handleNullNotification(trigger, callId, call, notificationId)
        }

        serviceStateController.setCurrentCallId(callId)
        notificationManager.observeCallNotification(call)

        return when (trigger) {
            CallService.Companion.Trigger.IncomingCall -> {
                showIncomingCall(callId, notificationId, notification)
                CallServiceHandleNotificationResult.START
            }

            CallService.Companion.Trigger.OnGoingCall ->
                startForegroundForCall(
                    call,
                    callId,
                    notification,
                    NotificationType.Ongoing,
                    trigger,
                )

            CallService.Companion.Trigger.OutgoingCall ->
                startForegroundForCall(
                    call,
                    callId,
                    notification,
                    NotificationType.Outgoing,
                    trigger,
                )

            else ->
                startForegroundForCall(
                    call,
                    callId,
                    notification,
                    null,
                    trigger,
                )
        }
    }

    private fun handleNullNotification(
        trigger: CallService.Companion.Trigger,
        callId: StreamCallId,
        call: Call,
        fallbackNotificationId: Int,
    ): CallServiceHandleNotificationResult {
        if (trigger != CallService.Companion.Trigger.RemoveIncomingCall) {
            logger.e {
                "[handleNullNotification], Could not get notification for trigger: $trigger, callId: ${callId.id}"
            }
            return CallServiceHandleNotificationResult.REDELIVER
        }

        val serviceStartedForThisCall = serviceStateController.currentCallId?.id == callId.id

        return if (serviceStartedForThisCall) {
            removeIncomingCall(call)
            CallServiceHandleNotificationResult.START
        } else {
            /**
             * Means we only posted notification for this call, Service was never started for this call
             */
            val notificationId =
                call.state.notificationIdFlow.value
                    ?: callId.getNotificationId(NotificationType.Incoming)

            NotificationManagerCompat.from(this).cancel(notificationId)
            CallServiceHandleNotificationResult.START_NO_CHANGE
        }
    }

    private fun startForegroundForCall(
        call: Call,
        callId: StreamCallId,
        notification: Notification,
        type: NotificationType?,
        trigger: CallService.Companion.Trigger,
    ): CallServiceHandleNotificationResult {
        val resolvedNotificationId =
            call.state.notificationIdFlow.value
                ?: type?.let { callId.getNotificationId(it) }
                ?: callId.hashCode()

        logger.d {
            "[startForegroundForCall] trigger=$trigger, " +
                "call.state.notificationId=${call.state.notificationIdFlow.value}, " +
                "notificationId=$resolvedNotificationId, " +
                "hashcode=${hashCode()}"
        }

        call.state.updateNotification(resolvedNotificationId, notification)

        startForegroundWithServiceType(
            resolvedNotificationId,
            notification,
            trigger,
            permissionManager.getServiceType(baseContext, trigger),
        )

        return CallServiceHandleNotificationResult.START
    }

    private fun logHandleStart(
        trigger: CallService.Companion.Trigger,
        call: Call,
        notificationId: Int,
    ) {
        logger.d {
            "[logHandleStart] trigger=$trigger, " +
                "call.state.notificationId=${call.state.notificationIdFlow.value}, " +
                "notificationId=$notificationId, " +
                "hashcode=${hashCode()}"
        }
    }

    private fun verifyPermissions(
        streamVideo: StreamVideoClient,
        call: Call,
        callId: StreamCallId,
        trigger: Trigger,
    ): Boolean {
        val (hasPermissions, missingPermissions) =
            streamVideo.permissionCheck.checkAndroidPermissionsGroup(
                applicationContext,
                call,
            )
        logger.d {
            "[verifyPermissions] hasPermissions: $hasPermissions, missingPermissions: ${missingPermissions.joinToString()}"
        }
        if (!hasPermissions) {
            val exception = IllegalStateException(
                """
                CallService attempted to start without required permissions: ${missingPermissions.joinToString()}.
                call_id: $callId, trigger: $trigger
                Ensure all required permissions are granted before calling Call.join().
                """.trimIndent(),
            )

            if (streamVideo.crashOnMissingPermission) {
                throw exception
            } else {
                logger.e(exception) { "Make sure all required permissions are granted!" }
            }
            return false
        }
        return true
    }

    private fun extractIntentParams(intent: Intent?): CallIntentParams? {
        val trigger = intent?.getStringExtra(Trigger.TRIGGER_KEY) ?: return null
        val callId = intent.streamCallId(INTENT_EXTRA_CALL_CID) ?: return null
        val streamVideo = (StreamVideo.instanceOrNull() as? StreamVideoClient) ?: return null
        val displayName = intent.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME)

        return CallIntentParams(streamVideo, callId, Trigger.toTrigger(trigger), displayName)
    }

    private fun maybeHandleMediaIntent(intent: Intent?, callId: StreamCallId?) = safeCall {
        val handler = streamDefaultNotificationHandler()
        if (handler != null && callId != null) {
            val isMediaNotification = notificationConfig()?.mediaNotificationCallTypes?.contains(
                callId.type,
            )
            if (isMediaNotification == true) {
                logger.d { "[maybeHandleMediaIntent] Handling media intent" }
                MediaButtonReceiver.handleIntent(
                    handler.mediaSession(callId),
                    intent,
                )
            }
        }
    }

    open fun getNotificationPair(
        trigger: CallService.Companion.Trigger,
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

    private fun showIncomingCall(
        callId: StreamCallId,
        notificationId: Int,
        notification: Notification,
    ) {
        StreamVideo.instanceOrNull()?.let { client ->
            logger.d { "[showIncomingCall] notificationId: $notificationId" }
            val hasActiveCall = client.state.activeCall.value != null

            if (!hasActiveCall) {
                client.call(callId.type, callId.id)
                    .state.updateNotification(notificationId, notification)

                startForegroundWithServiceType(
                    notificationId,
                    notification,
                    Trigger.IncomingCall,
                    permissionManager.getServiceType(baseContext, Trigger.IncomingCall),
                ).onError {
                    logger.e { "[showIncomingCall] Failed to start foreground: $it" }
                    notificationManager.justNotify(this, callId, notificationId, notification)
                }
            } else {
                notificationManager.justNotify(this, callId, notificationId, notification)
            }
        }
    }

    private fun removeIncomingCall(call: Call) {
        logger.d { "[removeIncomingCall] call_cid:${call.cid}" }
        if (serviceStateController.currentCallId?.cid == call.cid) {
            stopServiceGracefully()
        }
    }

    private fun observeCall(call: Call, streamVideo: StreamVideoClient) {
        CallServiceRingingStateObserver(
            call,
            serviceStateController.soundPlayer,
            streamVideo,
            serviceScope,
        )
            .observe { stopServiceGracefully() }

        CallServiceEventObserver(call, streamVideo, serviceScope)
            .observe(
                onServiceStop = { stopServiceGracefully() },
                onRemoveIncoming = {
                    removeIncomingCall(call)
                },
            )

        if (streamVideo.enableCallNotificationUpdates) {
            CallServiceNotificationUpdateObserver(
                call,
                streamVideo,
                serviceScope,
                permissionManager,
            ) {
                    notificationId: Int,
                    notification: Notification,
                    trigger: Trigger,
                    foregroundServiceType: Int,
                ->
                startForegroundWithServiceType(
                    notificationId,
                    notification,
                    trigger,
                    foregroundServiceType,
                )
            }
                .observe(baseContext)
        }
    }

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        logger.w { "[onTimeout] Timeout received from the system, service will stop." }
        stopServiceGracefully(null)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        logger.w { "[onTaskRemoved]" }

        callServiceLifecycleManager.endCall(serviceScope, serviceStateController.currentCallId)
        stopServiceGracefully(null)
    }

    override fun onDestroy() {
        logger.d {
            "[onDestroy], hashcode: ${hashCode()}, call_cid: ${serviceStateController.currentCallId?.cid}"
        }
        serviceStateController.soundPlayer?.cleanUpAudioResources()
        debouncer.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun streamDefaultNotificationHandler(): StreamDefaultNotificationHandler? {
        val client = StreamVideo.instanceOrNull() as? StreamVideoClient ?: return null
        val handler =
            client.streamNotificationManager.notificationConfig.notificationHandler as? StreamDefaultNotificationHandler
        return handler
    }

    private fun notificationConfig(): NotificationConfig? {
        val client = StreamVideo.instanceOrNull() as? StreamVideoClient ?: return null
        return client.streamNotificationManager.notificationConfig
    }

    /**
     * Handle all aspects of stopping the service.
     * Should be invoke carefully for the calls which are still present in [StreamVideoClient.calls]
     * Else stopping service by an expired call can cancel current call's notification and the service itself
     */
    private fun stopServiceGracefully(source: String? = null) {
        source?.let {
            logger.w { "[stopServiceGracefully] source: $source" }
        }

        serviceStateController.startTimeElapsedRealtime?.let { startTime ->

            val elapsedMs = SystemClock.elapsedRealtime() - startTime
            val differenceInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs)
            val debouncerThresholdTimeInSeconds = SERVICE_DESTROY_THRESHOLD_TIME_MS / 1_000
            logger.d { "[stopServiceGracefully] differenceInSeconds: $differenceInSeconds" }
            if (differenceInSeconds >= debouncerThresholdTimeInSeconds) {
                internalStopServiceGracefully()
            } else {
                debouncer.submit(debouncerThresholdTimeInSeconds) {
                    internalStopServiceGracefully()
                }
            }
        }
    }

    private fun internalStopServiceGracefully() {
        logger.d { "[internalStopServiceGracefully] hashcode: ${hashCode()}" }

        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceStateController.currentCallId?.let {
            notificationManager.cancelNotifications(this, it)
        }

        serviceStateController.unregisterToggleCameraBroadcastReceiver(this)

        /**
         * Temp Fix!! The observeRingingState scope was getting cancelled and as a result,
         * ringing state was not properly updated
         */
        serviceStateController.soundPlayer?.stopCallSound() // TODO should check which call owns the sound
        serviceScope.cancel()
        stopSelf()
    }

    private fun promoteToFgServiceIfNoActiveCall(
        videoClient: StreamVideoClient,
        notificationId: Int,
        trigger: Trigger,
    ) {
        val hasActiveCall = videoClient.state.activeCall.value != null
        val not = if (hasActiveCall) " not" else ""

        logger.d {
            "[promoteToFgServiceIfNoActiveCall] hasActiveCall: $hasActiveCall. Will$not call startForeground early."
        }

        if (!hasActiveCall) {
            videoClient.getSettingUpCallNotification()?.let { notification ->
                startForegroundWithServiceType(
                    notificationId,
                    notification,
                    trigger,
                    permissionManager.getServiceType(baseContext, trigger),
                )
            }
        }
    }

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null
}
