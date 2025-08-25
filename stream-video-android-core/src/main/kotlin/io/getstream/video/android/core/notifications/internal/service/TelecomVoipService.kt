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
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.DEFAULT_CALL_TEXT
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.triggers.ServiceTriggerDispatcher
import io.getstream.video.android.core.notifications.internal.telecom.TelecomConnectionIncomingCallData
import io.getstream.video.android.core.notifications.internal.telecom.connection.ErrorTelecomConnection
import io.getstream.video.android.core.notifications.internal.telecom.connection.SuccessTelecomConnection
import io.getstream.video.android.core.notifications.internal.telecom.notificationtrigger.TelecomSelfManagedNotificationTrigger
import io.getstream.video.android.core.sounds.CallSoundPlayer
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.startForegroundWithServiceType
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class TelecomVoipService : ConnectionService(), CallingServiceContract {

    internal open val logger by taggedLogger("TelecomVoipService")
    val serviceNotificationRetriever = ServiceNotificationRetriever()
    lateinit var serviceTriggerDispatcher: ServiceTriggerDispatcher

    private var callId: StreamCallId? = null
    val serviceIncomingCallHandler = ServiceIncomingCallHandler()

    val handler = CoroutineExceptionHandler { _, exception ->
        logger.e(exception) { "[CallService#Scope] Uncaught exception: $exception" }
    }
    private val serviceScope: CoroutineScope =
        CoroutineScope(Dispatchers.IO + handler + SupervisorJob())

    private var callSoundPlayer: CallSoundPlayer? = null
    var callServiceStatePresenter: CallServiceStatePresenter? = null
    val telecomSelfManagedNotificationTrigger = TelecomSelfManagedNotificationTrigger()

    override fun onCreate() {
        super.onCreate()
        callSoundPlayer = StreamVideo.instanceOrNull()?.state?.soundPlayer
        serviceTriggerDispatcher = ServiceTriggerDispatcher(applicationContext)
    }

    open fun getServiceType(): Int {
        var serviceType = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE // TODO Rahul, maybe not required
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
        }
        return serviceType
    }

    /**
     * This is invoked when
     */
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection {
        logger.d { "[onCreateIncomingConnection]" }
        try {
            super.onCreateIncomingConnection(connectionManagerPhoneAccount, request)
            val streamVideoClient = (StreamVideo.instanceOrNull() as? StreamVideoClient)
            if (streamVideoClient != null) {
                val callCid = request?.extras?.getString(NotificationHandler.INTENT_EXTRA_CALL_CID)
                val displayName =
                    request?.extras?.getString(NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME)
                        ?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT
                val isVideo =
                    request?.extras?.getBoolean(NotificationHandler.INTENT_EXTRA_IS_VIDEO) ?: false

                val callType = callCid?.split(":")?.get(0) ?: ""
                val streamCallId =
                    StreamCallId(type = callType, id = callCid?.split(":")?.get(1) ?: "")
                logger.d { "[onCreateIncomingConnection], streamCallId = $streamCallId" }
                val callServiceConfiguration =
                    streamVideoClient.callServiceConfigRegistry.get(streamCallId.type)
                val call = streamVideoClient.call(streamCallId.type, streamCallId.id)
                val telecomConnectionIncomingCallData = TelecomConnectionIncomingCallData(
                    streamCallId,
                    callDisplayName = displayName,
                    callServiceConfiguration = callServiceConfiguration,
                    isVideo = isVideo,
                    notification = call.state.atomicNotification.get(),
                )

                val connection = SuccessTelecomConnection(
                    applicationContext,
                    streamVideoClient,
                    telecomSelfManagedNotificationTrigger,
                    telecomConnectionIncomingCallData,
                )
//                val address = Uri.fromParts(PhoneAccount.SCHEME_TEL, "0000", null)
                /**
                 * Current status, the wearable is displaying unknown as caller name.
                 * Unable to find a way to render the caller name at the moment
                 * The wearable expects a phone number on the address variable
                 */
                val address = Uri.fromParts(PhoneAccount.SCHEME_SIP, displayName, null)
                logger.d { "Request address = ${request?.address}, address = $address, displayName = $displayName" }

                with(connection) {
                    setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
                    setCallerDisplayName(displayName, TelecomManager.PRESENTATION_ALLOWED)
                }

                /**
                 * Step 1 update connection to call
                 */
                call.state.updateTelecomConnection(connection)

                /**
                 * Step 2 Render Notification
                 */
//                call.state.atomicNotification.get()?.let { notification->
//                    streamVideoClient.getStreamNotificationDispatcher().notify(
//                        streamCallId,
//                        INCOMING_CALL_NOTIFICATION_ID,
//                        notification,
//                    )
//                }

                /**
                 * Step 2 Render Notification + Start FG Service
                 */

//                call.state.atomicNotification.get()?.let { notification->
//                    streamVideoClient.getStreamNotificationDispatcher().notify(
//                        streamCallId,
//                        INCOMING_CALL_NOTIFICATION_ID,
//                        notification,
//                    )
//                }
                connection.setRinging()
                return connection
            } else {
                val failedConn = ErrorTelecomConnection(applicationContext)
                failedConn.setDisconnected(
                    DisconnectCause(
                        DisconnectCause.ERROR,
                        "StreamVideoClient is null",
                    ),
                )
                return failedConn
            }
        } catch (e: Exception) {
            val failedConn = ErrorTelecomConnection(applicationContext)
            failedConn.setDisconnected(DisconnectCause(DisconnectCause.ERROR, e.message))
            return failedConn
        }
    }

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        super.onStartCommand(intent, flags, startId)
//        val trigger = intent?.getStringExtra(TRIGGER_KEY)
//        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient
//
//        val intentCallId = intent?.streamCallId(INTENT_EXTRA_CALL_CID)
//        val intentCallDisplayName = intent?.streamCallDisplayName(INTENT_EXTRA_CALL_DISPLAY_NAME)
//
//        logger.i {
//            "[onStartCommand]. callId: ${intentCallId?.id}, trigger: $trigger, Callservice hashcode: ${hashCode()}"
//        }
//        logger.d {
//            "[onStartCommand] streamVideo: ${streamVideo != null}, intentCallId: ${intentCallId != null}, trigger: $trigger"
//        }
//
//        maybeHandleMediaIntent(intent, intentCallId)
//
//        val started = if (intentCallId != null && streamVideo != null && trigger != null) {
//            logger.d { "[onStartCommand] All required parameters available, proceeding with service start" }
//            // Promote early to foreground service
//            maybePromoteToForegroundService(
//                videoClient = streamVideo,
//                notificationId = intentCallId.hashCode(),
//                trigger,
//            )
//
//            val type = intentCallId.type
//            val id = intentCallId.id
//            val call = streamVideo.call(type, id)
//
//            val permissionCheckPass =
//                streamVideo.permissionCheck.checkAndroidPermissions(applicationContext, call)
//            if (!permissionCheckPass) {
//                // Crash early with a meaningful message if Call is used without system permissions.
//                val exception = IllegalStateException(
//                    "\nCallService attempted to start without required permissions (e.g. android.manifest.permission.RECORD_AUDIO).\n" + "This can happen if you call [Call.join()] without the required permissions being granted by the user.\n" + "If you are using compose and [LaunchCallPermissions] ensure that you rely on the [onRequestResult] callback\n" + "to ensure that the permission is granted prior to calling [Call.join()] or similar.\n" + "Optionally you can use [LaunchPermissionRequest] to ensure permissions are granted.\n" + "If you are not using the [stream-video-android-ui-compose] library,\n" + "ensure that permissions are granted prior calls to [Call.join()].\n" + "You can re-define your permissions and their expected state by overriding the [permissionCheck] in [StreamVideoBuilder]\n",
//                )
//                if (streamVideo.crashOnMissingPermission) {
//                    throw exception
//                } else {
//                    logger.e(exception) { "Make sure all the required permissions are granted!" }
//                }
//            }
//
//            logger.d {
//                "[onStartCommand] Getting notification for trigger: $trigger, callId: ${intentCallId.id}"
//            }
//            val notificationData: Pair<Notification?, Int> =
//                serviceNotificationRetriever.getNotificationPair(applicationContext, trigger, streamVideo, intentCallId, intentCallDisplayName)
//
//            val notification = notificationData.first
//            logger.d {
//                "[onStartCommand] Notification generated: ${notification != null}, notificationId: ${notificationData.second}"
//            }
//            if (notification != null) {
//                if (trigger == TRIGGER_INCOMING_CALL) {
//                    logger.d { "[onStartCommand] Handling incoming call trigger" }
//                    showIncomingCall(
//                        callId = intentCallId,
//                        notificationId = notificationData.second,
//                        notification = notification,
//                    )
//                } else {
//                    logger.d { "[onStartCommand] Handling non-incoming call trigger: $trigger" }
//                    callId = intentCallId
//
//                    call.state.updateNotification(notification)
//
//                    startForegroundWithServiceType(
//                        intentCallId.hashCode(),
//                        notification,
//                        trigger,
//                        serviceType,
//                    )
//                }
//                true
//            } else {
//                if (trigger == TRIGGER_REMOVE_INCOMING_CALL) {
//                    logger.d { "[onStartCommand] Removing incoming call" }
//                    removeIncomingCall(notificationId = notificationData.second)
//                    true
//                } else {
//                    // Service not started no notification
//                    logger.e { "Could not get notification for trigger: $trigger, callId: ${intentCallId.id}" }
//                    false
//                }
//            }
//        } else {
//            // Service not started, no call Id or stream video
//            logger.e {
//                "Call id or streamVideo or trigger are not available. streamVideo: ${streamVideo != null}, intentCallId: ${intentCallId != null}, trigger: $trigger"
//            }
//            false
//        }
//
//        if (!started) {
//            logger.w { "Foreground service did not start!" }
//            // Call stopSelf() and return START_REDELIVER_INTENT.
//            // Because of stopSelf() the service is not restarted.
//            // Because START_REDELIVER_INTENT is returned
//            // the exception RemoteException: Service did not call startForeground... is not thrown.
//            stopService()
//            return START_REDELIVER_INTENT
//        } else {
//            initializeCallAndSocket(streamVideo!!, intentCallId!!)
//
//            if (trigger == TRIGGER_INCOMING_CALL) {
//                serviceScope.launch {
//                    serviceIncomingCallHandler.updateRingingCall(streamVideo, intentCallId, RingingState.Incoming())
//                }
//
//            }
//
//            if (callServiceStatePresenter == null) {
//                callServiceStatePresenter =
//                    CallServiceStatePresenter(serviceScope, callSoundPlayer!!, serviceType) //TODO Rahul remove force non null
//            }
//
//            logger.d {
//                "[onStartCommand]. callSoundPlayer's hashcode: ${callSoundPlayer?.hashCode()}, Callservice hashcode: ${hashCode()}"
//            }
//            callServiceStatePresenter?.observeCall(intentCallId, streamVideo)
//            serviceScope.launch {
//                callServiceStatePresenter?.stopServiceState?.collectLatest {
//                    if(it) stopService()
//                }
//            }
//
//            serviceScope.launch {
//                callServiceStatePresenter?.removeIncomingCallEvent?.collectLatest { event ->
//                    event?.let {
//                        removeIncomingCall(it.notificationId)
//                    }
//                }
//            }
//
//            callServiceStatePresenter?.registerToggleCameraBroadcastReceiver(this)
//            return START_NOT_STICKY
//        }
//
//    }

    override fun maybeHandleMediaIntent(intent: Intent?, callId: StreamCallId?) {
        // TODO RAHUL
    }

    override fun maybePromoteToForegroundService(
        videoClient: StreamVideoClient,
        notificationId: Int,
        trigger: String,
    ) {
        // TODO RAHUL
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
                getServiceType(),
            ).onError {
                logger.e {
                    "[showIncomingCall] Failed to start foreground service, falling back to justNotify: $it"
                }
                callServiceStatePresenter?.justNotify(this, callId, notificationId, notification)
            }
        } else {
            // Else, we show a simple notification (the service was already started as a foreground service).
            logger.d { "[showIncomingCall] Service already running, showing simple notification" }
            callServiceStatePresenter?.justNotify(this, callId, notificationId, notification)
        }
    }

    private fun removeIncomingCall(notificationId: Int) {
        NotificationManagerCompat.from(this).cancel(notificationId)

        if (callId == null) {
            stopService()
        }
    }

    override fun initializeCallAndSocket(
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

    override fun stopService() {
        // Cancel the notification
        val notificationManager = NotificationManagerCompat.from(this)
        callId?.let {
            val notificationId = callId.hashCode()
            notificationManager.cancel(notificationId)

            logger.i { "[stopService]. Cancelled notificationId: $notificationId" }
        }

        safeCall {
            val handler = serviceNotificationRetriever.streamDefaultNotificationHandler()
            handler?.clearMediaSession(callId)
        }

        // Optionally cancel any incoming call notification
        notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID)
        logger.i { "[stopService]. Cancelled incoming call notificationId: $INCOMING_CALL_NOTIFICATION_ID" }

        // Camera privacy
        callServiceStatePresenter?.unregisterToggleCameraBroadcastReceiver(this)

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

    // TODO Rahul for missed call
    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }
}
