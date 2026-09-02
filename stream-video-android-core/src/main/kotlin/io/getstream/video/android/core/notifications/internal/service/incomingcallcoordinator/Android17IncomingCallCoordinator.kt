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

package io.getstream.video.android.core.notifications.internal.service.incomingcallcoordinator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.IncomingRingtoneOwner
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.core.notifications.internal.service.IncomingCallPresenter
import io.getstream.video.android.core.notifications.internal.service.IncomingCallRequest
import io.getstream.video.android.core.notifications.internal.service.JetpackTelecomRepositoryProvider
import io.getstream.video.android.core.notifications.internal.service.ShowIncomingCallResult
import io.getstream.video.android.core.notifications.internal.service.models.ServiceRoute
import io.getstream.video.android.core.notifications.internal.service.observers.CallRejectionObserver
import io.getstream.video.android.core.notifications.internal.service.observers.CallServiceEventObserver
import io.getstream.video.android.core.notifications.internal.service.observers.TelecomNotificationUpdateObserver
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
import io.getstream.video.android.core.notifications.internal.telecom.TelecomHelper
import io.getstream.video.android.core.notifications.internal.telecom.TelecomPermissions
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

/** Coordinates the Android 17 incoming-call path that does not start [io.getstream.video.android.core.notifications.internal.service.CallService]. */
internal class Android17IncomingCallCoordinator(
    private val context: Context,
    private val client: StreamVideoClient,
    private val incomingCallPresenter: IncomingCallPresenter,
    private val telecomPermissions: TelecomPermissions,
    private val telecomHelper: TelecomHelper,
    private val jetpackTelecomRepositoryProvider: JetpackTelecomRepositoryProvider,
    private val fallbackCoordinator: PreAndroid17IncomingCallCoordinator,
    private val telecomCallController: TelecomCallController,
) : IncomingCallCoordinator {
    private val logger by taggedLogger("Android17IncomingCallCoordinator")

    @SuppressLint("MissingPermission", "NewApi")
    override fun showIncomingCall(request: IncomingCallRequest) {
        val canUseTelecom = telecomPermissions.canUseTelecom(context) &&
            telecomHelper.canUseJetpackTelecom()

        if (!canUseTelecom || !hasNotificationPermission(context)) {
            logger.w {
                "[showIncomingCall] Telecom unavailable; " +
                    "using the pre-Android 17 incoming-call route"
            }
            fallbackCoordinator.showIncomingCall(
                request = request,
                ringtoneOwner = IncomingRingtoneOwner.Notification,
            )
            return
        }

        val call = client.call(request.callId.type, request.callId.id)
        call.state.updateServiceRoute(ServiceRoute.TELECOM)

        safeCallWithResult {
            val appSchema = client.telecomConfig?.schema
            val addressUri = "$appSchema:${request.callId.id}".toUri()
            val formattedCallDisplayName = request.callDisplayName
                ?.takeIf { it.isNotBlank() }
                ?: VideoPushDelegate.DEFAULT_CALL_TEXT

            initTelecomRepository(call, request)
            call.scope.launch {
                call.state.jetpackTelecomRepository?.let { jetpackTelecomRepository ->
                    jetpackTelecomRepository.registerCall(
                        formattedCallDisplayName,
                        addressUri,
                        true,
                        request.isVideo,
                        {
                            logger.d {
                                "[showIncomingCall] Telecom registered; posting notification"
                            }
                            val notification = request.notificationProvider(
                                IncomingRingtoneOwner.Notification,
                            )
                            val result = incomingCallPresenter.showIncomingCallNotification(
                                context = context,
                                callId = request.callId,
                                notification = notification,
                            )
                            logger.d { "[showIncomingCall] notification result: $result" }
                            when (result) {
                                ShowIncomingCallResult.ONLY_NOTIFICATION -> {
                                    client.state.addRingingCall(call, RingingState.Incoming())
                                    call.state.updateRingingState()
                                    connectCoordinatorWS(call)
                                    observeCallRejection(call)
                                    observeCallEvents(call)
                                    handleNotificationUpdates(call)
                                }

                                else -> {
                                    // Do nothing
                                }
                            }
                        },
                        { ex ->
                            logger.e { "[showIncomingCall] Telecom registration with: $ex" }
                            fallbackCoordinator.showIncomingCall(
                                request = request,
                                ringtoneOwner = IncomingRingtoneOwner.Notification,
                            )
                        },
                    )
                }
            }
        }.onError { error ->
            logger.e { "[showIncomingCall] Telecom registration failed: $error" }
            fallbackCoordinator.showIncomingCall(
                request = request,
                ringtoneOwner = IncomingRingtoneOwner.Notification,
            )
        }
    }

    private fun observeCallRejection(call: Call) {
        CallRejectionObserver(call, client)
            .observe()
    }

    private fun observeCallEvents(call: Call) {
        CallServiceEventObserver(call, client)
            .observe({}, {})
    }

    private fun handleNotificationUpdates(call: Call) {
        if (client.enableCallNotificationUpdates) {
            TelecomNotificationUpdateObserver(call, client, call.scope)
                .observe()
        }
    }

    private fun connectCoordinatorWS(call: Call) {
        call.scope.launch {
            logger.d {
                "[showIncomingCall] Connecting coordinator WebSocket after Telecom registration"
            }
            client.connectIfNotAlreadyConnected()
            logger.d {
                "[showIncomingCall] Coordinator WebSocket connection request completed"
            }
        }
    }

    override fun dismissIncomingCall(
        callId: StreamCallId,
        config: CallServiceConfig,
    ) {
        val notificationId = callId.getNotificationId(NotificationType.Incoming)
        logger.d { "[dismissIncomingCall] notificationId: $notificationId" }
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    override fun finishIncomingCall(call: Call) {
        dismissIncomingCall(
            callId = StreamCallId.fromCallCid(call.cid),
            config = DefaultCallConfigurations.default,
        )
        telecomCallController.leaveCall(call)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initTelecomRepository(
        call: Call,
        request: IncomingCallRequest,
    ): JetpackTelecomRepository? {
        if (call.state.jetpackTelecomRepository != null) {
            return call.state.jetpackTelecomRepository
        }
        return jetpackTelecomRepositoryProvider.get(request.callId).also {
            call.state.jetpackTelecomRepository = it
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
}
