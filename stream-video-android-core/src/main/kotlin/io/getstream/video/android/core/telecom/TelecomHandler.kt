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
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@TargetApi(Build.VERSION_CODES.O)
internal class TelecomHandler private constructor(
    private val applicationContext: Context,
    private val callManager: CallsManager,
    private val callSoundPlayer: CallSoundPlayer,
) {
    private val logger by taggedLogger(TELECOM_LOG_TAG)
    private var streamVideo: StreamVideo? = null
    private val calls = mutableMapOf<String, TelecomCall>()
    private val exceptionHandler = CoroutineExceptionHandler { _, ex ->
        logger.e(ex) { "[telecomHandlerScope]" }
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
        logger.d { "[init]" }

        safeCall(exceptionLogTag = TELECOM_LOG_TAG) {
            callManager.registerAppWithTelecom(
                capabilities = CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING and
                    CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING,
            )
        }

        streamVideo = StreamVideo.instanceOrNull()
    }

    fun registerCall(
        call: StreamCall,
        callConfig: CallServiceConfig,
        wasTriggeredByIncomingNotification: Boolean = false,
    ) {
        logger.d {
            "[registerCall] Call ID: ${call.id}, wasTriggeredByIncomingNotification: $wasTriggeredByIncomingNotification"
        }

        if (wasTriggeredByIncomingNotification) prepareIncomingCall(call)

        if (calls.contains(call.cid)) {
            logger.i { "[registerCall] Call already registered, ignoring" }
        } else {
            calls[call.cid] = TelecomCall(
                streamCall = call,
                config = callConfig,
                parentScope = telecomHandlerScope,
            )

            logger.d { "[registerCall] New call registered" }
        }
    }

    private fun prepareIncomingCall(call: StreamCall) {
        logger.d { "[prepareIncomingCall]" }

        telecomHandlerScope.launch {
            streamVideo?.connectIfNotAlreadyConnected()
            call.get()
            streamVideo?.state?.addRingingCall(call, RingingState.Incoming())
        }
    }

    fun changeCallState(call: StreamCall, newState: TelecomCallState) {
        val telecomCall = calls[call.cid]

        logger.i { "[changeCallState] currentState: ${telecomCall?.state}, newState: $newState" }

        if (telecomCall == null || telecomCall.state == newState) {
            val cause = if (telecomCall == null) "call not registered" else "same state"
            logger.i { "[changeCallState] Ignoring method call: $cause" }
        } else {
            var wasPreviouslyAdded = false
            with(telecomCall) {
                wasPreviouslyAdded = (state == TelecomCallState.INCOMING || state == TelecomCallState.OUTGOING)
                updateState(newState)
                postNotification(this)
            }

            telecomHandlerScope.launch {
                safeCall(exceptionLogTag = TELECOM_LOG_TAG) {
                    if (wasPreviouslyAdded) {
                        logger.i { "[changeCallState] Call was already added to Telecom, skipping CallsManager#addCall()" }
                    } else {
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
                                logger.i { "[changeCallState] Added call to Telecom" }
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
            logger.e { "[postNotification] POST_NOTIFICATIONS permission missing" }
            return
        } else {
            (streamVideo as? StreamVideoClient)?.let { streamVideo ->
                val notification = when (telecomCall.state) {
                    TelecomCallState.INCOMING -> {
                        logger.d { "[postNotification] Creating incoming notification" }

                        callSoundPlayer.playCallSound(
                            streamVideo.sounds.ringingConfig.incomingCallSoundUri,
                        )

                        streamVideo.getRingingCallNotification(
                            ringingState = RingingState.Incoming(),
                            callId = StreamCallId.fromCallCid(telecomCall.streamCall.cid),
                            callInfo = telecomCall.attributes.displayName.toString(),
                            shouldHaveContentIntent = streamVideo.state.activeCall.value == null,
                        )
                    }

                    TelecomCallState.OUTGOING, TelecomCallState.ONGOING -> {
                        val isOutgoingCall = telecomCall.state == TelecomCallState.OUTGOING
                        val getNotification = {
                            logger.d {
                                "[postNotification] Creating ${if (isOutgoingCall) "outgoing" else "ongoing"} notification"
                            }

                            streamVideo.getOngoingCallNotification(
                                callId = StreamCallId.fromCallCid(telecomCall.streamCall.cid),
                                isOutgoingCall = isOutgoingCall,
                            )
                        }

                        if (isOutgoingCall) {
                            callSoundPlayer.playCallSound(
                                streamVideo.sounds.ringingConfig.outgoingCallSoundUri,
                            )
                            getNotification()
                        } else {
                            callSoundPlayer.stopCallSound()
                            if (telecomCall.config.runCallServiceInForeground) getNotification() else null
                        }
                    }

                    else -> {
                        logger.w { "[postNotification] Will not post any notification" }
                        null
                    }
                }

                notification?.let {
                    logger.i { "[postNotification] Posting ${telecomCall.state} notification" }

                    NotificationManagerCompat
                        .from(applicationContext)
                        .notify(telecomCall.notificationId, it)
                } ?: cancelNotification(telecomCall.notificationId)
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
        logger.d { "[cancelNotification]" }
        NotificationManagerCompat.from(applicationContext).cancel(notificationId)
    }

    fun setDeviceListener(call: StreamCall, listener: DeviceListener) {
        logger.d { "[setDeviceListener] Call ID: ${call.id}" }

        calls[call.cid]?.deviceListener = listener
        sendInitialEndpoints(listener)
    }

    private fun sendInitialEndpoints(listener: DeviceListener) {
        telecomHandlerScope.launch {
            val devices = try {
                callManager.getAvailableStartingCallEndpoints().first().map { it.toStreamAudioDevice() }
            } catch (e: Exception) {
                logger.w { "[setDeviceListener] Error when collecting endpoints: ${e.message}" }
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
                        logger.d { "[selectDevice] New device: ${device.name}, requestEndpointChange result: $result" }
                    }
                }
            }
        }
    }

    fun cleanUp() = runBlocking {
        logger.d { "[cleanUp]" }

        calls.forEach { unregisterCall(it.value.streamCall) }
        telecomHandlerScope.cancel()
        callSoundPlayer.cleanUpAudioResources()
        instance = null
    }

    fun unregisterCall(call: StreamCall) {
        logger.i { "[unregisterCall]" }

        calls.remove(call.cid)?.let { telecomCall ->
            safeCall(exceptionLogTag = TELECOM_LOG_TAG) {
                callSoundPlayer.stopCallSound()
                cancelNotification(telecomCall.notificationId)
                telecomCall.cleanUp()
            }
        }
    }
}
