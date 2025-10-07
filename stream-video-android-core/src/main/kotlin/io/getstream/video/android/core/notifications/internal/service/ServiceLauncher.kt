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

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.telecom.CallsManager
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.DEFAULT_CALL_TEXT
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.telecom.IncomingCallTelecomAction
import io.getstream.video.android.core.notifications.internal.telecom.TelecomHelper
import io.getstream.video.android.core.notifications.internal.telecom.TelecomPermissions
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.TelecomCall
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.TelecomCallAction
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ServiceLauncher(val context: Context) {

    private val logger by taggedLogger("ServiceTriggers")
    private val serviceIntentBuilder = ServiceIntentBuilder()
    private val incomingCallPresenter = IncomingCallPresenter(serviceIntentBuilder)
    private val telecomHelper = TelecomHelper()
    private val telecomPermissions = TelecomPermissions()

    @SuppressLint("MissingPermission", "NewApi")
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
        logger.d { "[showIncomingCall] service start result: $result" }
        if (telecomPermissions.canUseTelecom(context)) {
            if (telecomHelper.canUseJetpackTelecom()) {
                when (result) {
                    ShowIncomingCallResult.FG_SERVICE -> {
                        updateIncomingCallNotification(notification, streamVideo, callId)

                        val jetpackTelecomRepository = getJetpackTelecomRepository(callId)

                        val appSchema = (streamVideo as StreamVideoClient).telecomConfig?.schema
                        val addressUri = "$appSchema:${callId.id}".toUri()
                        val formattedCallDisplayName = callDisplayName?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT

                        val call = streamVideo.call(callId.type, callId.id)

                        call.state.jetpackTelecomRepository = (jetpackTelecomRepository)

                        call.scope.launch {
                            jetpackTelecomRepository.registerCall(
                                formattedCallDisplayName,
                                addressUri,
                                true,
                                isVideo,
                            )
                        }
                    }
                    else -> {}
                }
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
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    @SuppressLint("NewApi")
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

        /**
         * TODO We don't have api to directly render text as display name. Need more research
         */
        val callDisplayName = "NOT SET YET"

        val telecomPermissions = TelecomPermissions()
        val telecomHelper = TelecomHelper()
        if (telecomPermissions.canUseTelecom(context)) {
            if (telecomHelper.canUseJetpackTelecom()) {
                val jetpackTelecomRepository = getJetpackTelecomRepository(callId)

                val appSchema = streamVideo.telecomConfig?.schema
                val addressUri = "$appSchema:${callId.id}".toUri()
                val formattedCallDisplayName =
                    callDisplayName?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT

                call.state.jetpackTelecomRepository = jetpackTelecomRepository

                call.scope.launch(Dispatchers.Default) {
                    launch {
                        jetpackTelecomRepository.registerCall(
                            formattedCallDisplayName,
                            addressUri,
                            false,
                            call.isVideoEnabled(),
                        )
                    }
                    launch {
                        delay(2000L)
                        val result = (jetpackTelecomRepository.currentCall.value as? TelecomCall.Registered)?.processAction(
                            TelecomCallAction.Activate,
                        )
                        logger.d { "Telecom is activated: $result" }
                    }
                }
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

    fun stopService(call: Call) {
        stopCallServiceInternal(call)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getJetpackTelecomRepository(callId: StreamCallId): JetpackTelecomRepository {
        val callsManager = CallsManager(context).apply {
            registerAppWithTelecom(
                capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                    CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
            )
        }

        val streamVideo = StreamVideo.instance()
        val incomingCallTelecomAction =
            IncomingCallTelecomAction(streamVideo)
        logger.d { "[getJetpackTelecomRepository] hashcode callsManager:${callsManager.hashCode()}" }
        return JetpackTelecomRepository(callsManager, callId, incomingCallTelecomAction)
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
