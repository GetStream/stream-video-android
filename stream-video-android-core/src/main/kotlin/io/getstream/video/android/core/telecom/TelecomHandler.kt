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

package io.getstream.video.android.core.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.sounds.CallSoundPlayer
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@TargetApi(Build.VERSION_CODES.O)
internal class TelecomHandler
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal constructor(
    private val applicationContext: Context,
    private val callManager: CallsManager,
    private val callSoundPlayer: CallSoundPlayer,
) {
    private val logger by taggedLogger(TAG)

    private var streamVideo: StreamVideo? = StreamVideo.instanceOrNull()

    private val calls = mutableMapOf<String, TelecomCall>()

    private val exceptionHandler = CoroutineExceptionHandler { _, ex ->
        logger.e(ex) { "[telecomHandlerScope] #telecom;" }
    }

    private val telecomHandlerScope = CoroutineScope(
        DispatcherProvider.Default + SupervisorJob() + exceptionHandler,
    )

    companion object {
        @Volatile
        private var instance: TelecomHandler? = null

        fun getInstance(context: Context): TelecomHandler? {
            return instance ?: synchronized(this) {
                context.applicationContext.let { applicationContext ->
                    if (isSupported(applicationContext)) {
                        TelecomHandler(
                            applicationContext = applicationContext,
                            callManager = CallsManager(applicationContext),
                            callSoundPlayer = CallSoundPlayer(applicationContext),
                        ).also { telecomHandler ->
                            instance = telecomHandler
                        }
                    } else {
                        null
                    }
                }
            }
        }

        private fun isSupported(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 14+, check the TELECOM feature directly
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELECOM)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For API 26+ assume that Telecom is supported if the device supports SIM.
            // According to the docs, most devices with a SIM card support Telecom.
            // https://developer.android.com/develop/connectivity/telecom/voip-app#supported_telecom_device
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_GSM)
        } else {
            false
        }

//        fun isSupported(context: Context) = false // TODO-Telecom: remove
    }

    init {
        logger.d { "[init] #telecom;" }

        safeCall(exceptionLogTag = TAG) {
            callManager.registerAppWithTelecom(
                capabilities = CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING and
                    CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING,
            )
        }
    }

    fun registerCall(
        call: StreamCall,
        callConfig: CallServiceConfig,
        wasTriggeredByIncomingNotification: Boolean = false,
    ) {
        logger.d {
            "[registerCall] #telecom; Call ID: ${call.id}, wasTriggeredByIncomingNotification: $wasTriggeredByIncomingNotification"
        }

        if (wasTriggeredByIncomingNotification) prepareIncomingCall(call)

        if (calls.contains(call.cid)) {
            logger.i { "[registerCall] #telecom; Call already registered, ignoring" }
        } else {
            calls[call.cid] = TelecomCall(
                context = applicationContext,
                streamCall = call,
                config = callConfig,
                telecomHandler = this,
            )

            logger.d { "[registerCall] #telecom; New call registered" }
        }
    }

    private fun prepareIncomingCall(call: StreamCall) {
        logger.d { "[prepareIncomingCall] #telecom;" }

        telecomHandlerScope.launch {
            withContext(DispatcherProvider.IO) {
                streamVideo?.connectIfNotAlreadyConnected()
                call.get()
            }
            streamVideo?.state?.addRingingCall(call, RingingState.Incoming())
        }
    }

    fun changeCallState(call: StreamCall, newState: TelecomCallState) {
        val telecomCall = calls[call.cid]

        logger.i {
            "[changeCallState] #telecom; currentState: ${telecomCall?.state}, newState: $newState, call ID: ${call.id}"
        }

        if (telecomCall == null || telecomCall.state == newState) {
            val cause = if (telecomCall == null) "call not registered" else "same state"
            logger.i { "[changeCallState] #telecom; Ignoring method call: $cause" }
        } else {
            telecomCall.state = newState
            postNotification(telecomCall)

            val wasPreviouslyAdded = telecomCall.previousState in listOf(TelecomCallState.INCOMING, TelecomCallState.OUTGOING)

            if (wasPreviouslyAdded) {
                logger.i { "[changeCallState] #telecom; Call was already added to Telecom, skipping addCall()" }
                telecomCall.updateInternalTelecomState()
            } else {
                telecomHandlerScope.launch {
                    safeCall(exceptionLogTag = TAG) {
                        callManager.addCall(
                            callAttributes = telecomCall.attributes,
                            onAnswer = {
                                telecomCall.handleTelecomEvent(TelecomEvent.ANSWER)
                            },
                            onDisconnect = {
                                telecomCall.handleTelecomEvent(TelecomEvent.DISCONNECT)
                            },
                            onSetActive = {
                                telecomCall.handleTelecomEvent(TelecomEvent.SET_ACTIVE)
                            },
                            onSetInactive = {
                                telecomCall.handleTelecomEvent(TelecomEvent.SET_INACTIVE)
                            },
                            block = {
                                telecomCall.callControlScope = this
                                telecomCall.updateInternalTelecomState()
                                logger.i { "[changeCallState] #telecom; Added call to Telecom, call ID: ${call.id}" }
                            },
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(telecomCall: TelecomCall) {
        if (!hasNotificationsPermission()) {
            logger.e { "[postNotification] #telecom; POST_NOTIFICATIONS permission missing" }
            return
        } else {
            val streamVideo = streamVideo as? StreamVideoClient ?: return

            val notificationToPost = when (telecomCall.state) {
                TelecomCallState.INCOMING -> {
                    logger.d { "[postNotification] #telecom; Creating incoming notification" }

                    callSoundPlayer.playCallSound(
                        streamVideo.sounds.ringingConfig.incomingCallSoundUri,
                    )

                    streamVideo.getRingingCallNotification(
                        ringingState = RingingState.Incoming(),
                        callId = telecomCall.streamCall.buildStreamCallId(),
                        callInfo = telecomCall.attributes.displayName.toString(),
                        shouldHaveContentIntent = streamVideo.state.activeCall.value == null,
                    )
                }

                TelecomCallState.OUTGOING, TelecomCallState.ONGOING -> {
                    val isOutgoingCall = telecomCall.state == TelecomCallState.OUTGOING
                    val getNotification = {
                        logger.d {
                            "[postNotification] #telecom; Creating ${if (isOutgoingCall) "outgoing" else "ongoing"} notification"
                        }

                        streamVideo.getOngoingCallNotification(
                            callId = telecomCall.streamCall.buildStreamCallId(),
                            isOutgoingCall = isOutgoingCall,
                        )
                    }

                    if (isOutgoingCall) {
                        val outgoingCallSound = streamVideo.sounds.ringingConfig.outgoingCallSoundUri
                        callSoundPlayer.playCallSound(outgoingCallSound)
                    } else {
                        callSoundPlayer.stopCallSound()
                    }

                    getNotification()
                }

                else -> {
                    logger.w { "[postNotification] #telecom; Will not post any notification" }
                    null
                }
            }

            notificationToPost?.let { notification ->
                val notify: (Notification) -> Unit = {
                    logger.i { "[postNotification] #telecom; Posting ${telecomCall.state} notification" }
                    NotificationManagerCompat
                        .from(applicationContext)
                        .notify(telecomCall.notificationId, it)
                }

                if (telecomCall.state == TelecomCallState.INCOMING) {
                    notify(notification)
                } else {
                    if (telecomCall.config.runCallServiceInForeground) {
                        notify(notification)

                        maybeObserveNotificationUpdates(
                            telecomCall = telecomCall,
                            streamVideo = streamVideo,
                            onUpdate = { updatedNotification ->
                                logger.d { "[postNotification] #telecom; Updating notification" }
                                notify(updatedNotification)
                            },
                        )
                    } else {
                        cancelNotification(telecomCall.notificationId)
                    }
                }
            }
        }
    }

    private fun hasNotificationsPermission(): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun cancelNotification(notificationId: Int) {
        logger.d { "[cancelNotification] #telecom;" }
        NotificationManagerCompat.from(applicationContext).cancel(notificationId)
    }

    @SuppressLint("MissingPermission")
    private fun maybeObserveNotificationUpdates(
        telecomCall: TelecomCall,
        streamVideo: StreamVideoClient,
        onUpdate: (Notification) -> Unit,
    ) {
        if (streamVideo.enableCallNotificationUpdates && telecomCall.notificationUpdateJob == null) {
            telecomCall.notificationUpdateJob = streamVideo.getNotificationUpdates(
                coroutineScope = telecomHandlerScope, // Improvement: use callControlScope (but not always available here)
                call = telecomCall.streamCall,
                localUser = streamVideo.user,
            ) { updatedNotification -> onUpdate(updatedNotification) }

            logger.d { "[observeNotificationUpdates] #telecom; Added job" }
        }
    }

    fun setDeviceListener(call: StreamCall, listener: DeviceListener) {
        logger.d { "[setDeviceListener] #telecom; Call ID: ${call.id}" }

        calls[call.cid]?.deviceListener = listener
        sendInitialDevices(listener)
    }

    private fun sendInitialDevices(listener: DeviceListener) {
        telecomHandlerScope.launch {
            val devices = try {
                callManager.getAvailableStartingCallEndpoints().first().map { it.toStreamAudioDevice() }
            } catch (e: Exception) {
                logger.w { "[setDeviceListener] #telecom; Error when collecting endpoints: ${e.message}" }
                emptyList()
            }
            listener(devices, null)
        }
    }

    fun selectDevice(call: StreamCall, device: CallEndpointCompat) {
        calls[call.cid]?.callControlScope?.let {
            with(it) {
                launch {
                    requestEndpointChange(device).let { result ->
                        logger.d { "[selectDevice] #telecom; New device: ${device.name}, change result: $result" }
                    }
                }
            }
        }
    }

    fun cleanUp() {
        logger.d { "[cleanUp] #telecom;" }

        calls.forEach { unregisterCall(it.value.streamCall) }
        telecomHandlerScope.cancel()
        callSoundPlayer.cleanUpAudioResources()
        instance = null
    }

    fun unregisterCall(call: StreamCall) {
        logger.i { "[unregisterCall] #telecom; Call ID: ${call.id}" }

        calls.remove(call.cid)?.let { telecomCall ->
            safeCall(exceptionLogTag = TAG) {
                callSoundPlayer.stopCallSound()
                cancelNotification(telecomCall.notificationId)
                telecomCall.cleanUp()
            }
        }
    }
}

private const val TAG = "StreamVideo:TelecomHandler"
