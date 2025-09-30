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

package io.getstream.video.android.core.notifications.internal.service.triggers

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.telecom.CallsManager
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StopForegroundServiceSource
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.DEFAULT_CALL_TEXT
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.core.notifications.internal.service.ServiceIntentBuilder
import io.getstream.video.android.core.notifications.internal.service.StartServiceParam
import io.getstream.video.android.core.notifications.internal.service.StopServiceParam
import io.getstream.video.android.core.notifications.internal.service.TelecomHelper
import io.getstream.video.android.core.notifications.internal.telecom.IncomingCallTelecomAction
import io.getstream.video.android.core.notifications.internal.telecom.JetpackTelecomRepository
import io.getstream.video.android.core.telecom.TelecomPermissions
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

/**
 * TODO Rahul change the name of the class its a decision maker class which will decide which
 * which service to pick
 */
class ServiceLauncher(val context: Context) {

    private val logger by taggedLogger("ServiceTriggers")
    private val serviceIntentBuilder = ServiceIntentBuilder()
    private val incomingCallPresenter = IncomingCallPresenter(serviceIntentBuilder)
    private val telecomHelper = TelecomHelper()

    private val telecomServiceLauncher = TelecomServiceLauncher()

    @SuppressLint("MissingPermission")
    fun showIncomingCall(
        context: Context,
        callId: StreamCallId,
        callDisplayName: String?,
        callServiceConfiguration: CallServiceConfig,
        isVideo: Boolean,
        payload: Map<String, Any?>,
        streamVideo: StreamVideo,
        notification: Notification?,
    ) {
        val result = incomingCallPresenter.showIncomingCall(
            context,
            callId,
            callDisplayName,
            callServiceConfiguration,
            notification,
        )
        val telecomPermissions = TelecomPermissions()
        if (telecomPermissions.canUseTelecom(context)) {
            // TODO Rahul, correctly use result to launch telecom
            when (result) {
                ShowIncomingCallResult.FG_SERVICE -> {}
                ShowIncomingCallResult.SERVICE -> {}
                ShowIncomingCallResult.ONLY_NOTIFICATION -> {}
                ShowIncomingCallResult.ERROR -> {}
            }

            updateIncomingCallNotification(notification, streamVideo, callId)

            if (telecomHelper.canUseJetpackTelecom()) {

                val jetpackTelecomRepository = getJetpackTelecomRepository(callId)

                val appSchema = (streamVideo as StreamVideoClient).telecomConfig?.schema
                val addressUri = "$appSchema:${callId.id}".toUri()
                val formattedCallDisplayName = callDisplayName?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT

                val call = streamVideo.call(callId.type, callId.id)

                call.state.jetpackTelecomRepository = jetpackTelecomRepository

                call.scope.launch {
                    jetpackTelecomRepository.registerCall(
                        formattedCallDisplayName,
                        addressUri,
                        true,
                    )
                }
            } else {
                telecomServiceLauncher.addIncomingCallToTelecom(
                    context,
                    callId,
                    callDisplayName,
                    callServiceConfiguration,
                    isVideo,
                    payload,
                    streamVideo,
                    notification,
                )
            }
        }
    }

    fun showOnGoingCall(call: Call, trigger: String, streamVideo: StreamVideo) {
        val client = streamVideo as StreamVideoClient
        val callConfig = client.callServiceConfigRegistry.get(call.type)
        if (!callConfig.runCallServiceInForeground) {
            return
        }
        val callId = StreamCallId.fromCallCid(call.cid)
        val context = client.context
        val serviceIntent = ServiceIntentBuilder().buildStartIntent(
            context,
            StartServiceParam(
                callId,
                trigger,
                callServiceConfiguration = callConfig,
            ),
        )
        val callDisplayName = "ON GOING CALL NOT SET" // TODO Rahul Later
        val telecomPermissions = TelecomPermissions()
        if (telecomPermissions.canUseTelecom(context)) {
            if (telecomHelper.canUseJetpackTelecom()) {

                /**
                 * Do nothing, the logic already handled in [StreamCallActivity.accept()]
                 */
//                val jetpackTelecomRepository = getJetpackTelecomRepository(callId)
//
//                val appSchema = streamVideo.telecomConfig?.schema
//                val addressUri = "$appSchema:${callId.id}".toUri()
//                val formattedCallDisplayName = callDisplayName?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT
//
//                call.state.jetpackTelecomRepository = jetpackTelecomRepository
//
//                call.scope.launch {
//                    jetpackTelecomRepository.registerCall(
//                        formattedCallDisplayName,
//                        addressUri,
//                        true,
//                    )
//                }
            } else {
                telecomServiceLauncher.addOnGoingCall(
                    context,
                    callId = StreamCallId(call.type, call.id),
                    callDisplayName = callDisplayName,
                    isVideo = call.isVideoEnabled(),
                    streamVideo = streamVideo,
                )
            }
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }

    fun showOutgoingCall(call: Call, trigger: String, streamVideo: StreamVideo) {
        val callConfig = (streamVideo as StreamVideoClient).callServiceConfigRegistry.get(call.type)
        if (!callConfig.runCallServiceInForeground) {
            return
        }
        val callId = StreamCallId.fromCallCid(call.cid)
        val serviceIntent = ServiceIntentBuilder().buildStartIntent(
            context,
            StartServiceParam(
                callId,
                trigger,
                callServiceConfiguration = callConfig,
            ),
        )

        ContextCompat.startForegroundService(context, serviceIntent)

        val callDisplayName = "NOT SET YET" //TODO Rahul

        val telecomPermissions = TelecomPermissions()
        val telecomHelper = TelecomHelper()
        if(telecomPermissions.canUseTelecom(context)){
            if(telecomHelper.canUseJetpackTelecom()) {
                val jetpackTelecomRepository = getJetpackTelecomRepository(callId)

                val appSchema = streamVideo.telecomConfig?.schema
                val addressUri = "$appSchema:${callId.id}".toUri()
                val formattedCallDisplayName = callDisplayName?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT

                call.state.jetpackTelecomRepository = jetpackTelecomRepository

                call.scope.launch {
                    jetpackTelecomRepository.registerCall(
                        formattedCallDisplayName,
                        addressUri,
                        false,
                    )
                }
            } else {
                //TODO Rahul pending, use telecom platform api
            }
        }


    }

    /**
     * Because we need to retrieve the notification
     * in [io.getstream.video.android.core.notifications.internal.telecom.connection.SuccessIncomingTelecomConnection]
     */
    private fun updateIncomingCallNotification(
        notification: Notification?,
        streamVideo: StreamVideo,
        callId: StreamCallId,
    ) {
        notification?.let {
            streamVideo.call(callId.type, callId.id)
                .state.updateNotification(notification)
        }
    }

    fun removeIncomingCall(
        context: Context,
        callId: StreamCallId,
        config: CallServiceConfig = DefaultCallConfigurations.default,
    ) {
        safeCallWithResult {
            context.startService(
                serviceIntentBuilder.buildStartIntent(
                    context,
                    StartServiceParam(
                        callId,
                        TRIGGER_REMOVE_INCOMING_CALL,
                        callServiceConfiguration = config,
                    ),
                ),
            )!!
        }.onError {
            NotificationManagerCompat.from(context)
                .cancel(callId.getNotificationId(NotificationType.Incoming))
        }
    }

    fun stopService(
        call: Call,
        stopForegroundServiceSource: StopForegroundServiceSource,
    ) {
        logger.d { "stopService, call id: ${call.cid}, source: ${stopForegroundServiceSource.source}" }
        stopTelecomInternal(call, stopForegroundServiceSource)
        stopCallServiceInternal(call)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getJetpackTelecomRepository(callId: StreamCallId): JetpackTelecomRepository {
        val callsManager = CallsManager(context).apply {
            // Register with the telecom interface with the supported capabilities
            registerAppWithTelecom(
                capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                        CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
            )
        }
        val streamVideo = StreamVideo.instance()
        val incomingCallPresenter = IncomingCallPresenter(ServiceIntentBuilder())
        val incomingCallTelecomAction =
            IncomingCallTelecomAction(context, streamVideo, incomingCallPresenter)

        return JetpackTelecomRepository(callsManager, callId, incomingCallTelecomAction)
    }

    private fun stopTelecomInternal(
        call: Call,
        stopForegroundServiceSource: StopForegroundServiceSource,
    ) {
        val telecomPermissions = TelecomPermissions()
        if (telecomPermissions.canUseTelecom(context)) {
            call.state.telecomConnection.value?.let {
                when (stopForegroundServiceSource) {
                    StopForegroundServiceSource.CallAccept -> {}
                    StopForegroundServiceSource.RemoveActiveCall -> {
                        it.setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
                        it.destroy()
                    }

                    StopForegroundServiceSource.RemoveRingingCall -> {}
                    StopForegroundServiceSource.SetActiveCall -> {}
                }
            }
        }
    }

    private fun stopCallServiceInternal(call: Call) {
        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient
        streamVideo?.let { streamVideoClient ->
            val callConfig = streamVideoClient.callServiceConfigRegistry.get(call.type)
            if (callConfig.runCallServiceInForeground) {
                val context = streamVideoClient.context

                val serviceIntent = serviceIntentBuilder.buildStopIntent(
                    context,
                    StopServiceParam(call, callConfig),
                )
                logger.d { "Building stop intent for call_id: ${call.cid}" }
                serviceIntent.extras?.let {
                    logBundle(it)
                }
                context.startService(serviceIntent)
            }
        }
    }

    private fun logBundle(bundle: Bundle) {
        val keys = bundle.keySet()
        if (keys != null) {
            val sb = StringBuilder()
            for (key in keys) {
                val itemInBundle = bundle[key]
                val text = "key:$key, value=$itemInBundle"
                sb.append(text)
                sb.append("\n")
            }
            if (sb.toString().isNotEmpty()) {
                logger.d { " [maybeStopForegroundService], stop intent extras: $sb" }
            }
        }
    }
}
