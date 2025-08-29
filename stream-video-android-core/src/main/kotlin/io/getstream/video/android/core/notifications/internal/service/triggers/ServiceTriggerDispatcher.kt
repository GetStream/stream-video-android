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

import android.app.Notification
import android.content.Context
import android.telecom.DisconnectCause
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StopForegroundServiceSource
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.core.notifications.internal.service.ServiceIntentBuilder
import io.getstream.video.android.core.notifications.internal.service.StartServiceParam
import io.getstream.video.android.core.notifications.internal.service.StopServiceParam
import io.getstream.video.android.core.telecom.TelecomPermissions
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.StreamCallId
import kotlin.math.log

/**
 * TODO Rahul change the name of the class its a decision maker class which will decide which
 * which service to pick
 */
class ServiceTriggerDispatcher(val context: Context) {

    private val logger by taggedLogger("ServiceTriggers")
    private val serviceIntentBuilder = ServiceIntentBuilder()
    private val legacyServiceTrigger = LegacyServiceTrigger(serviceIntentBuilder)

    private val telecomServiceTrigger = TelecomServiceTrigger(context, serviceIntentBuilder)

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
        val telecomPermissions = TelecomPermissions()
        if (telecomPermissions.canUseTelecom(context)) {
            telecomServiceTrigger.addIncomingCallToTelecom(
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

        legacyServiceTrigger.showIncomingCall(
            context,
            callId,
            callDisplayName,
            callServiceConfiguration,
            isVideo,
            payload,
            notification,
        )
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
            NotificationManagerCompat.from(context).cancel(INCOMING_CALL_NOTIFICATION_ID)
        }
    }

    fun showOnGoingCall(call: Call, trigger: String, streamVideo: StreamVideo){
        val client = streamVideo as StreamVideoClient
        val callConfig = client.callServiceConfigRegistry.get(call.type)
        if (!callConfig.runCallServiceInForeground) {
            return
        }

        val context = client.context
        val serviceIntent = ServiceIntentBuilder().buildStartIntent(
            context,
            StartServiceParam(StreamCallId.fromCallCid(call.cid),
                trigger,
                callServiceConfiguration = callConfig),
        )

        val telecomPermissions = TelecomPermissions()
        if (telecomPermissions.canUseTelecom(context)) {
            telecomServiceTrigger.addOnGoingCall(
                context,
                callId = StreamCallId(call.type, call.id),
                callDisplayName = "ON GOING CALL NOT SET", // TODO Rahul Later
                isVideo = call.isVideoEnabled(),
                streamVideo = streamVideo,
            )
        }

        ContextCompat.startForegroundService(context, serviceIntent)

    }

    fun showOutgoingCall(call: Call, trigger: String, streamVideo: StreamVideo) {
        val callConfig = (streamVideo as StreamVideoClient).callServiceConfigRegistry.get(call.type)
        if (!callConfig.runCallServiceInForeground) {
            return
        }

        val serviceIntent = ServiceIntentBuilder().buildStartIntent(
            context,
            StartServiceParam(
                StreamCallId.fromCallCid(call.cid),
                trigger,
                callServiceConfiguration = callConfig,
            ),

        )

        val telecomPermissions = TelecomPermissions()
        if (telecomPermissions.canUseTelecom(context)) {
            telecomServiceTrigger.addOutgoingCallToTelecom(
                context,
                callId = StreamCallId(call.type, call.id),
                callDisplayName = "NOT SET YET", // TODO Rahul Later
                isVideo = call.isVideoEnabled(),
                streamVideo = streamVideo,
            )
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }



    fun stopService(call: Call, stopForegroundServiceSource: StopForegroundServiceSource) {
        logger.d { "stopService, call id: ${call.cid}, source: ${stopForegroundServiceSource.source}" }
        val telecomPermissions = TelecomPermissions()
        if (telecomPermissions.canUseTelecom(context)) {
            call.state.telecomConnection.value?.let {
                when(stopForegroundServiceSource){
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

        val streamVideo = StreamVideo.instanceOrNull() as? StreamVideoClient
        streamVideo?.let { streamVideoClient ->
            val callConfig = streamVideoClient.callServiceConfigRegistry.get(call.type)
            if (callConfig.runCallServiceInForeground) {
                val context = streamVideoClient.context
                val serviceIntent = ServiceIntentBuilder().buildStopIntent(
                    context,
                    stopServiceParam = StopServiceParam(callConfig),
                )
                serviceIntent?.let {
                    context.stopService(serviceIntent)
                }
            }
        }
    }
}
