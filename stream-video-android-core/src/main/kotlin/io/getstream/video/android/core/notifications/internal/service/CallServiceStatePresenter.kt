package io.getstream.video.android.core.notifications.internal.service

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.internal.receivers.ToggleCameraBroadcastReceiver
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_OUTGOING_CALL
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.sounds.CallSoundPlayer
import io.getstream.video.android.core.utils.startForegroundWithServiceType
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class CallServiceStatePresenter(
    val serviceScope: CoroutineScope,
    val callSoundPlayer: CallSoundPlayer,
    val serviceType: Int
) {

    private val logger by taggedLogger("CallServiceStatePresenter")

    private val _stopServiceState = MutableStateFlow(false)
    val stopServiceState: StateFlow<Boolean> = _stopServiceState

    private val _startForegroundWithServiceTypeState =
        MutableStateFlow<StartForegroundWithServiceType?>(null)
    val startForegroundWithServiceTypeState: StateFlow<StartForegroundWithServiceType?> =
        _startForegroundWithServiceTypeState

    private val toggleCameraBroadcastReceiver = ToggleCameraBroadcastReceiver(serviceScope)
    private var isToggleCameraBroadcastReceiverRegistered = false

    private val _removeIncomingCallEvent = MutableStateFlow<RemoveIncomingCallEvent?>(null)
    val removeIncomingCallEvent: StateFlow<RemoveIncomingCallEvent?> = _removeIncomingCallEvent

    fun observeCall(callId: StreamCallId, streamVideo: StreamVideoClient) {
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
                        _stopServiceState.value = true
                        //stopService() TODO RAHUL no need after _stopServiceState.value = true
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

    fun justNotify(
        context: Context,
        callId: StreamCallId,
        notificationId: Int,
        notification: Notification
    ) {
        logger.d { "[justNotify] notificationId: $notificationId" }
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
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
                        _stopServiceState.value = true
//                        stopService()
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
            _stopServiceState.value = true
//            stopService()
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
                _removeIncomingCallEvent?.value = RemoveIncomingCallEvent(INCOMING_CALL_NOTIFICATION_ID)
//                removeIncomingCall(INCOMING_CALL_NOTIFICATION_ID)
            } else {
                _stopServiceState.value = true
//                stopService()
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
                            _startForegroundWithServiceTypeState.value =
                                StartForegroundWithServiceType(
                                    callId.hashCode(),
                                    notification,
                                    TRIGGER_ONGOING_CALL,
                                    serviceType
                                )
//                            startForegroundWithServiceType(
//                                callId.hashCode(),
//                                notification,
//                                TRIGGER_ONGOING_CALL,
//                                serviceType,
//                            )
                        }

                        is RingingState.Outgoing -> {
                            logger.d { "[observeNotificationUpdates] Showing outgoing call notification" }
//                            startForegroundWithServiceType(
//                                INCOMING_CALL_NOTIFICATION_ID,
//                                notification,
//                                TRIGGER_OUTGOING_CALL,
//                                serviceType,
//                            )
                            _startForegroundWithServiceTypeState.value =
                                StartForegroundWithServiceType(
                                    INCOMING_CALL_NOTIFICATION_ID,
                                    notification,
                                    TRIGGER_OUTGOING_CALL,
                                    serviceType
                                )
                        }

                        is RingingState.Incoming -> {
                            logger.d { "[observeNotificationUpdates] Showing incoming call notification" }
//                            startForegroundWithServiceType(
//                                INCOMING_CALL_NOTIFICATION_ID,
//                                notification,
//                                TRIGGER_INCOMING_CALL,
//                                serviceType,
//                            )
                            _startForegroundWithServiceTypeState.value =
                                StartForegroundWithServiceType(
                                    INCOMING_CALL_NOTIFICATION_ID,
                                    notification,
                                    TRIGGER_INCOMING_CALL,
                                    serviceType
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

    private fun removeIncomingCall(context: Context, callId: StreamCallId?, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)

        if (callId == null) {
            _stopServiceState.value = true
//            stopService()
        }
    }

    fun registerToggleCameraBroadcastReceiver(context: Context) {
        if (!isToggleCameraBroadcastReceiverRegistered) {
            try {
                context.registerReceiver(
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

    fun unregisterToggleCameraBroadcastReceiver(context: Context) {
        if (isToggleCameraBroadcastReceiverRegistered) {
            try {
                context.unregisterReceiver(toggleCameraBroadcastReceiver)
                isToggleCameraBroadcastReceiverRegistered = false
            } catch (e: Exception) {
                logger.d { "Unable to unregister ToggleCameraBroadcastReceiver." }
            }
        }
    }

}

data class StartForegroundWithServiceType(
    val notificationId: Int,
    val notification: Notification,
    val trigger: String,
    val foregroundServiceType: Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
)

data class RemoveIncomingCallEvent(val notificationId: Int)