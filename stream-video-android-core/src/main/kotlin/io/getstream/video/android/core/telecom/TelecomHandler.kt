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
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent

@TargetApi(Build.VERSION_CODES.O)
internal class TelecomHandler private constructor(
    private val applicationContext: Context,
    private val callManager: CallsManager,
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
    private var callControlScope: CallControlScope? = null

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
                // TODO-Telecom: take audio_room and livestream (can be just audio) cases into account?
                capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                    CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
            )
        }

        streamVideo = StreamVideo.instanceOrNull()
    }

    /*
    TODO-Telecom:
    Add deprecation instructions for service
    Add sounds to notifications & check all sounds that are heard
    Should check for audio/video permissions in registerCall, similar to CallService?
    Do we need telecomHandler in StreamVideoImpl or just TelecomCompat?
    Have a look at SpeakerManager
     */

    fun registerCall(call: StreamCall, wasTriggeredByIncomingNotification: Boolean = false) {
        logger.d {
            "[registerCall] Call ID: ${call.id}, wasTriggeredByIncomingNotification: $wasTriggeredByIncomingNotification"
        }

        if (wasTriggeredByIncomingNotification) prepareIncomingCall(call)

        if (calls.contains(call.cid)) {
            logger.i { "[registerCall] Call already registered, ignoring" }
        } else {
            calls[call.cid] = TelecomCall(
                streamCall = call,
                state = TelecomCallState.IDLE,
                notificationId = call.cid.hashCode(),
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
            val wasAlreadyAdded = telecomCall.state.let {
                it == TelecomCallState.INCOMING || it == TelecomCallState.OUTGOING
            }
            telecomCall.state = newState

            val telecomToStreamEventBridge = TelecomToStreamEventBridge(call)
            val streamToTelecomEventBridge = StreamToTelecomEventBridge(call)

            telecomHandlerScope.launch {
                safeCall(exceptionLogTag = TELECOM_LOG_TAG) {
                    postNotification(telecomCall)

                    if (wasAlreadyAdded) {
                        logger.i { "[changeCallState] Call was already added to Telecom, ignoring" }
                    } else {
                        callManager.addCall(
                            callAttributes = call.telecomCallAttributes,
                            onAnswer = telecomToStreamEventBridge::onAnswer,
                            onDisconnect = telecomToStreamEventBridge::onDisconnect,
                            onSetActive = telecomToStreamEventBridge::onSetActive,
                            onSetInactive = telecomToStreamEventBridge::onSetInactive,
                            block = {
                                telecomCall.callControlScope = this
                                streamToTelecomEventBridge.onEvent(this)

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
            logger.d { "[postNotification] Call ID: ${telecomCall.streamCall.id}, state: ${telecomCall.state}" }

            streamVideo?.let { streamVideo ->
                val notification = when (telecomCall.state) {
                    TelecomCallState.INCOMING -> {
                        logger.d { "[postNotification] Creating incoming notification" }

                        streamVideo.getRingingCallNotification(
                            ringingState = RingingState.Incoming(),
                            callId = StreamCallId.fromCallCid(telecomCall.streamCall.cid),
                            incomingCallDisplayName = telecomCall.streamCall.incomingCallDisplayName,
                            shouldHaveContentIntent = streamVideo.state.activeCall.value == null,
                        )
                    }

                    TelecomCallState.OUTGOING, TelecomCallState.ONGOING -> {
                        val isOutgoingCall = telecomCall.state == TelecomCallState.OUTGOING

                        logger.d {
                            "[postNotification] Creating ${if (isOutgoingCall) "outgoing" else "ongoing"} notification"
                        }

                        streamVideo.getOngoingCallNotification(
                            callId = StreamCallId.fromCallCid(telecomCall.streamCall.cid),
                            isOutgoingCall = isOutgoingCall,
                        )
                    }

                    else -> {
                        logger.w { "[postNotification] Will not post any notification" }
                        null
                    }
                }

                notification?.let {
                    logger.i { "[postNotification] Posting ${telecomCall.state.toString().lowercase()} notification" }

                    NotificationManagerCompat
                        .from(applicationContext)
                        .notify(telecomCall.notificationId, it)
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

    fun unregisterCall(call: StreamCall) {
        logger.i { "[unregisterCall]" }

        calls.remove(call.cid)?.let { telecomCall ->
            safeCall(exceptionLogTag = TELECOM_LOG_TAG) {
                cancelNotification(telecomCall.notificationId)
                telecomCall.cleanUp()
            }
        }
    }

    private fun cancelNotification(notificationId: Int) {
        logger.d { "[cancelNotification]" }
        NotificationManagerCompat.from(applicationContext).cancel(notificationId)
    }

    fun registerAvailableDevicesListener(call: StreamCall, listener: AvailableDevicesListener) {
        logger.d { "[registerAvailableDevicesListener] Call ID: ${call.id}" }
        calls[call.cid]?.deviceListener = listener
    }

    fun selectDevice(device: CallEndpointCompat) {
        logger.d { "[selectDevice] device: $device" }

        callControlScope?.let {
            it.launch { it.requestEndpointChange(device) }
        }
    }

    fun cleanUp() = runBlocking {
        logger.d { "[cleanUp]" }

        calls.forEach { unregisterCall(it.value.streamCall) }
        telecomHandlerScope.cancel()
        instance = null
    }
}

private class TelecomToStreamEventBridge(private val call: StreamCall) {
    // TODO-Telecom: review what needs to be called here and take results into account

    private val logger by taggedLogger(TELECOM_LOG_TAG)

    suspend fun onAnswer(callType: Int) {
        logger.d { "[TelecomToStreamEventBridge#onAnswer]" }
        call.accept()
        call.join()
    }

    suspend fun onDisconnect(cause: DisconnectCause) {
        logger.d { "[TelecomToStreamEventBridge#onDisconnect]" }
        call.leave()
    }

    // TODO-Telecom: onhold support & missed calls?
    suspend fun onSetActive() {
        logger.d { "[TelecomToStreamEventBridge#onSetActive]" }
        call.join()
    }

    suspend fun onSetInactive() {
        logger.d { "[TelecomToStreamEventBridge#onSetInactive]" }
//        call.leave() // TODO-Telecom: uncomment and test
    }
}

private class StreamToTelecomEventBridge(private val call: StreamCall) {

    private val logger by taggedLogger(TELECOM_LOG_TAG)

    fun onEvent(callControlScope: CallControlScope) {
        call.subscribeFor(
            CallAcceptedEvent::class.java,
            CallRejectedEvent::class.java,
            CallEndedEvent::class.java,
        ) { event ->
            logger.d { "[StreamToTelecomEventMapper#onEvent] Received event: ${event.getEventType()}" }

            with(callControlScope) {
                launch {
                    when (event) {
                        is CallAcceptedEvent -> {
                            logger.d { "[StreamToTelecomEventBridge#onEvent] Will call CallControlScope#answer" }
                            answer(call.telecomCallType)
                        }
                        // TODO-Telecom: Correct DisconnectCause below
                        is CallRejectedEvent -> {
                            logger.d { "[StreamToTelecomEventBridge#onEvent] Will call CallControlScope#disconnect" }
                            disconnect(DisconnectCause(DisconnectCause.REJECTED))
                        }
                        is CallEndedEvent -> {
                            logger.d { "[StreamToTelecomEventBridge#onEvent] Will call CallControlScope#disconnect" }
                            disconnect(DisconnectCause(DisconnectCause.REMOTE))
                        }
                    }
                }
            }
        }

        callControlScope.launch {
            call.microphone.selectedDevice.collectLatest {
            }
        }
    }
}

private val StreamCall.telecomCallType: Int
    get() = when (type) {
        "default", "livestream" -> CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        else -> CallAttributesCompat.CALL_TYPE_AUDIO_CALL
    }

private val StreamCall.telecomCallAttributes: CallAttributesCompat
    get() = CallAttributesCompat(
        displayName = id,
        address = Uri.parse("https://getstream.io/video/join/$cid"),
        direction = if (state.ringingState.value is RingingState.Incoming) { // TODO-Telecom: Race condition with ringing state
            CallAttributesCompat.DIRECTION_INCOMING
        } else {
            CallAttributesCompat.DIRECTION_OUTGOING
        },
        callType = telecomCallType,
    )

private val StreamCall.incomingCallDisplayName: String
    get() = state.createdBy.value?.userNameOrId ?: "Unknown"

@TargetApi(Build.VERSION_CODES.O)
private fun CallEndpointCompat.toStreamAudioDevice(): StreamAudioDevice = when (this.type) {
    CallEndpointCompat.TYPE_BLUETOOTH -> StreamAudioDevice.BluetoothHeadset(telecomDevice = this)
    CallEndpointCompat.TYPE_EARPIECE -> StreamAudioDevice.Earpiece(telecomDevice = this)
    CallEndpointCompat.TYPE_SPEAKER -> StreamAudioDevice.Speakerphone(telecomDevice = this)
    CallEndpointCompat.TYPE_WIRED_HEADSET -> StreamAudioDevice.WiredHeadset(telecomDevice = this)
    else -> StreamAudioDevice.Earpiece()
}

private data class TelecomCall(
    val streamCall: StreamCall,
    var state: TelecomCallState,
    val notificationId: Int,
    val parentScope: CoroutineScope,
) {
    private val localScope = CoroutineScope(parentScope.coroutineContext + Job())
    private val devices = MutableStateFlow<Pair<List<StreamAudioDevice>, StreamAudioDevice>?>(null)

    var callControlScope: CallControlScope? = null
        set(value) {
            if (value != null) {
                field = value

                with(value) {
                    launch {
                        val combinedEndpoints =
                            combine(availableEndpoints, currentCallEndpoint) { list, device ->
                                Pair(
                                    list.map { it.toStreamAudioDevice() },
                                    device.toStreamAudioDevice(),
                                )
                            }

                        combinedEndpoints
                            .distinctUntilChanged()
                            .onEach {
                                StreamLog.d(TELECOM_LOG_TAG) {
                                    "[TelecomCall#callControlScope] Publishing devices: available devices: ${it.first.map { it.name }}, selected device: ${it.second.name}"
                                }
                                devices.value = it
                            }
                            .launchIn(this)
                    }
                }
            }
        }

    var deviceListener: AvailableDevicesListener? = null
        set(value) {
            value?.let { listener ->
                StreamLog.d(TELECOM_LOG_TAG) {
                    "[TelecomCall#deviceListener] Setting deviceListener"
                }

                field = value

                devices
                    .onEach { deviceStatus ->
                        deviceStatus?.let {
                            StreamLog.d(TELECOM_LOG_TAG) {
                                "[TelecomCall#deviceListener] Collecting devices & calling listener with: available devices: ${it.first.map { it.name }}, selected device: ${it.second.name}"
                            }

                            listener(it.first, it.second)
                        }
                    }
                    .launchIn(localScope)
            }
        }

    suspend fun disconnect() {
        callControlScope?.disconnect(DisconnectCause(DisconnectCause.LOCAL)).let { result ->
            StreamLog.d(TELECOM_LOG_TAG) { "[TelecomCall#disconnect] Disconnect result: $result" }
        }
    }

    fun cleanUp() {
        StreamLog.d(TELECOM_LOG_TAG) { "[TelecomCall#cleanUp]" }

        runBlocking {
            disconnect()
            localScope.cancel()
        }
    }
}

internal enum class TelecomCallState {
    IDLE,
    INCOMING,
    OUTGOING,
    ONGOING,
}
