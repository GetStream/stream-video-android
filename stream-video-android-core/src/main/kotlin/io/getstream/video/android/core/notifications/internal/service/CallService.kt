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
import android.content.Intent
import android.os.IBinder
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
import io.getstream.video.android.core.notifications.internal.service.managers.CallServiceLifecycleManager
import io.getstream.video.android.core.notifications.internal.service.managers.CallServiceNotificationManager
import io.getstream.video.android.core.notifications.internal.service.models.CallIntentParams
import io.getstream.video.android.core.notifications.internal.service.models.ServiceState
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
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import kotlin.math.absoluteValue

/**
 * A foreground service that is running when there is an active call.
 */
internal open class CallService : Service() {
    internal open val logger by taggedLogger("CallService")
    internal open val permissionManager = ForegroundServicePermissionManager()

    internal val callServiceLifecycleManager = CallServiceLifecycleManager()
    private val notificationManager = CallServiceNotificationManager()
    internal val serviceState = ServiceState()

    /**
     * A debouncer used to delay the final stopping of the service.
     *
     * This is a workaround for an Android framework behavior where killing a Foreground Service
     * too quickly (e.g., within ~2 seconds) can prevent its associated notification from being
     * dismissed, especially if the notification tray is open. By debouncing the stop action,
     * we ensure enough time has passed for the system to process the notification removal.
     */
    internal val debouncer = Debouncer()

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
        const val TRIGGER_KEY =
            "io.getstream.video.android.core.notifications.internal.service.CallService.call_trigger"
        const val TRIGGER_INCOMING_CALL = "incoming_call"
        const val TRIGGER_REMOVE_INCOMING_CALL = "remove_call"
        const val TRIGGER_OUTGOING_CALL = "outgoing_call"
        const val TRIGGER_ONGOING_CALL = "ongoing_call"
        const val EXTRA_STOP_SERVICE = "io.getstream.video.android.core.stop_service"

        const val SERVICE_DESTROY_THRESHOLD_TIME_SECONDS = 2L
        const val SERVICE_DESTROY_THROTTLE_TIME_MS = 1_000L

        private val logger by taggedLogger("CallService")

        val handler = CoroutineExceptionHandler { _, exception ->
            logger.e(exception) { "[CallService#Scope] Uncaught exception: $exception" }
        }
        val serviceScope: CoroutineScope =
            CoroutineScope(Dispatchers.IO.limitedParallelism(1) + handler + SupervisorJob())
    }

    override fun onCreate() {
        super.onCreate()
        serviceState.startTime = OffsetDateTime.now()
    }

    private fun shouldStopService(intent: Intent?): Boolean {
        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
        val shouldStopService = intent?.getBooleanExtra(EXTRA_STOP_SERVICE, false) ?: false
        logger.d {
            "[shouldStopService]: service hashcode: ${this.hashCode()}, serviceState.currentCallId!=null : ${serviceState.currentCallId != null}, serviceState.currentCallId == intentCallId && shouldStopService : ${serviceState.currentCallId == intentCallId && shouldStopService}"
        }

        if (serviceState.currentCallId != null && serviceState.currentCallId == intentCallId && shouldStopService) {
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

        val call = params.streamVideo.call(params.callId.type, params.callId.id)

        if (!verifyPermissions(params.streamVideo, call, params.callId, params.trigger)) {
            stopServiceGracefully()
            return START_REDELIVER_INTENT
        }

        val (notification, notificationId) = getNotificationPair(
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
                initializeService(params.streamVideo, params.callId, params.trigger)
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
        callId: StreamCallId,
        trigger: String,
    ) {
        callServiceLifecycleManager.initializeCallAndSocket(serviceScope, streamVideo, callId) {
//            stopServiceGracefully() TODO Rahul enable it before merge
        }

        if (trigger == TRIGGER_INCOMING_CALL) {
            callServiceLifecycleManager.updateRingingCall(
                serviceScope,
                streamVideo,
                callId,
                RingingState.Incoming(),
            )
        }

        serviceState.soundPlayer = streamVideo.callSoundAndVibrationPlayer
        logger.d { "[initializeService] soundPlayer hashcode: ${serviceState.soundPlayer?.hashCode()}" }

        observeCall(callId, streamVideo)
        serviceState.registerToggleCameraBroadcastReceiver(this, serviceScope)
    }

    private fun handleNotification(
        notification: Notification?,
        notificationId: Int,
        callId: StreamCallId,
        trigger: String,
        call: Call,
    ): CallServiceHandleNotificationResult {
        logger.d {
            "[handleNotification] Noob 1, trigger: $trigger, call.state.notificationId: ${call.state.notificationId}, notificationId: $notificationId, hashcode: ${hashCode()}"
        }
        if (notification == null) {
            return if (trigger == TRIGGER_REMOVE_INCOMING_CALL) {
                val ifServiceWasStartedForThisCall = serviceState.currentCallId?.id == callId.id
                if (ifServiceWasStartedForThisCall) {
                    removeIncomingCall(notificationId, call)
                    CallServiceHandleNotificationResult.START
                } else {
                    /**
                     * Means we only posted notification for this call, Service was never started for this call
                     */
                    val notificationId = call.state.notificationId
                        ?: callId.getNotificationId(NotificationType.Incoming)
                    NotificationManagerCompat.from(this).cancel(notificationId)
                    CallServiceHandleNotificationResult.START_NO_CHANGE
                }
            } else {
                logger.e { "Could not get notification for trigger: $trigger, callId: ${callId.id}" }
                CallServiceHandleNotificationResult.REDELIVER
            }
        }

        // TODO Rahul, problematic in-case of getting an incoming call while still on active call

        return when (trigger) {
            TRIGGER_INCOMING_CALL -> {
                serviceState.currentCallId = callId
                showIncomingCall(callId, notificationId, notification)
                CallServiceHandleNotificationResult.START
            }
            TRIGGER_ONGOING_CALL -> {
                serviceState.currentCallId = callId
                val notificationId = call.state.notificationId
                    ?: callId.getNotificationId(NotificationType.Ongoing)
                logger.d {
                    "[handleNotification] Noob 2, trigger: $trigger, call.state.notificationId: ${call.state.notificationId}, notificationId: $notificationId, hashcode: ${hashCode()}"
                }
                call.state.updateNotification(notificationId, notification)
                startForegroundWithServiceType(
                    notificationId,
                    notification,
                    trigger,
                    permissionManager.getServiceType(baseContext, trigger),
                )
                CallServiceHandleNotificationResult.START
            }
            TRIGGER_OUTGOING_CALL -> {
                serviceState.currentCallId = callId
                val notificationId = call.state.notificationId
                    ?: callId.getNotificationId(NotificationType.Outgoing)
                logger.d {
                    "[handleNotification] Noob 3, trigger: $trigger, call.state.notificationId: ${call.state.notificationId}, notificationId: $notificationId, hashcode: ${hashCode()}"
                }
                call.state.updateNotification(notificationId, notification)
                startForegroundWithServiceType(
                    notificationId,
                    notification,
                    trigger,
                    permissionManager.getServiceType(baseContext, trigger),
                )
                CallServiceHandleNotificationResult.START
            }

            else -> {
                serviceState.currentCallId = callId
                val notificationId = call.state.notificationId
                    ?: callId.hashCode() // instead get notification from call object
                logger.d {
                    "[handleNotification] Noob 4, trigger: $trigger, call.state.notificationId: ${call.state.notificationId}, notificationId: $notificationId, hashcode: ${hashCode()}"
                }
                call.state.updateNotification(notificationId, notification)
                startForegroundWithServiceType(
                    notificationId,
                    notification,
                    trigger,
                    permissionManager.getServiceType(baseContext, trigger),
                )
                CallServiceHandleNotificationResult.START
            }
        }
    }

    private fun verifyPermissions(
        streamVideo: StreamVideoClient,
        call: Call,
        callId: StreamCallId,
        trigger: String,
    ): Boolean {
        val (hasPermissions, missingPermissions) =
            streamVideo.permissionCheck.checkAndroidPermissionsGroup(applicationContext, call)

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
        val trigger = intent?.getStringExtra(TRIGGER_KEY) ?: return null
        val callId = intent.streamCallId(INTENT_EXTRA_CALL_CID) ?: return null
        val streamVideo = (StreamVideo.instanceOrNull() as? StreamVideoClient) ?: return null
        val displayName = intent.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME)

        return CallIntentParams(streamVideo, callId, trigger, displayName)
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

    private fun showIncomingCall(
        callId: StreamCallId,
        notificationId: Int,
        notification: Notification,
    ) {
        logger.d { "[showIncomingCall] notificationId: $notificationId" }
        val hasActiveCall = StreamVideo.instanceOrNull()?.state?.activeCall?.value != null

        if (!hasActiveCall) {
            StreamVideo.instanceOrNull()?.call(callId.type, callId.id)
                ?.state?.updateNotification(notificationId, notification)

            startForegroundWithServiceType(
                notificationId,
                notification,
                TRIGGER_INCOMING_CALL,
                permissionManager.getServiceType(baseContext, TRIGGER_INCOMING_CALL),
            ).onError {
                logger.e { "[showIncomingCall] Failed to start foreground: $it" }
                notificationManager.justNotify(this, callId, notificationId, notification)
            }
        } else {
            notificationManager.justNotify(this, callId, notificationId, notification)
        }
    }

    private fun removeIncomingCall(notificationId: Int, call: Call) {
        logger.d { "[removeIncomingCall] notificationId: $notificationId" }
        if (serviceState.currentCallId?.cid == call.cid) {
            stopServiceGracefully()
        }
    }

    private fun observeCall(callId: StreamCallId, streamVideo: StreamVideoClient) {
        val call = streamVideo.call(callId.type, callId.id)

        CallServiceRingingStateObserver(call, serviceState.soundPlayer, streamVideo, serviceScope)
            .observe { stopServiceGracefully() }

        CallServiceEventObserver(call, streamVideo)
            .observe(
                onServiceStop = { stopServiceGracefully() },
                onRemoveIncoming = {
                    removeIncomingCall(
                        callId.getNotificationId(NotificationType.Incoming),
                        call,
                    )
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
                    trigger: String,
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
        stopServiceGracefully()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        logger.w { "[onTaskRemoved]" }

        callServiceLifecycleManager.endCall(serviceScope, serviceState.currentCallId)
        stopServiceGracefully()
    }

    override fun onDestroy() {
        logger.d {
            "Noob, [onDestroy], Callservice hashcode: ${hashCode()}, call_cid: ${serviceState.currentCallId?.cid}"
        }
        serviceState.soundPlayer?.cleanUpAudioResources()
        super.onDestroy()
    }

    fun printLastStackFrames(count: Int = 10) {
        val stack = Thread.currentThread().stackTrace
        logger.d { stack.takeLast(count).joinToString("\n") }
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

    /**
     * Handle all aspects of stopping the service.
     * Should be invoke carefully for the calls which are still present in [StreamVideoClient.calls]
     * Else stopping service by an expired call can cancel current call's notification and the service itself
     */
    private fun stopServiceGracefully() {
        serviceState.startTime?.let { startTime ->

            val currentTime = OffsetDateTime.now()
            val duration = Duration.between(startTime, currentTime)
            val differenceInSeconds = duration.seconds.absoluteValue
            val debouncerThresholdTime = SERVICE_DESTROY_THRESHOLD_TIME_SECONDS
            logger.d { "[stopServiceGracefully] differenceInSeconds: $differenceInSeconds" }
            if (differenceInSeconds >= debouncerThresholdTime) {
                internalStopServiceGracefully()
            } else {
                debouncer.submit(debouncerThresholdTime) {
                    internalStopServiceGracefully()
                }
            }
        }
    }

    private fun internalStopServiceGracefully() {
        logger.d { "[internalStopServiceGracefully]" }

        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceState.currentCallId?.let {
            notificationManager.cancelNotifications(this, it)
        }

        serviceState.unregisterToggleCameraBroadcastReceiver(this)

        /**
         * Temp Fix!! The observeRingingState scope was getting cancelled and as a result,
         * ringing state was not properly updated
         */
        serviceState.soundPlayer?.stopCallSound() // TODO should check which call owns the sound
        serviceScope.cancel()
        stopSelf()
    }

    // This service does not return a Binder
    override fun onBind(intent: Intent?): IBinder? = null
}
