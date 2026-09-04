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
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.DEFAULT_CALL_TEXT
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
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

/** Coordinates the Android 17 incoming-call path without starting CallService. */
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
        if (!telecomPermissions.canUseTelecom(context) ||
            !telecomHelper.canUseJetpackTelecom() ||
            !hasNotificationPermission()
        ) {
            fallbackToCallService(request)
            return
        }

        val call = client.call(request.callId.type, request.callId.id)
        call.state.updateServiceRoute(ServiceRoute.TELECOM)
        val repository = initTelecomRepository(call, request)
        val address = "${client.telecomConfig?.schema}:${request.callId.id}".toUri()
        val displayName = request.callDisplayName?.takeIf(String::isNotBlank) ?: DEFAULT_CALL_TEXT

        call.scope.launch {
            repository.registerCall(
                displayName = displayName,
                address = address,
                isIncoming = true,
                isVideoCall = request.isVideo,
                onRegistered = {
                    val notification = request.notificationProvider(
                        IncomingRingtoneOwner.Notification,
                    )
                    val result = incomingCallPresenter.showIncomingCallNotification(
                        context,
                        request.callId,
                        notification,
                    )
                    if (result == ShowIncomingCallResult.ONLY_NOTIFICATION) {
                        client.state.addRingingCall(call, RingingState.Incoming())
                        call.state.updateRingingState()
                        connectCoordinatorWebSocket(call)
                        CallRejectionObserver(call, client).observe()
                        CallServiceEventObserver(call, client, call.scope).observe({}, {})
                        if (client.enableCallNotificationUpdates) {
                            TelecomNotificationUpdateObserver(call, client, call.scope).observe()
                        }
                    }
                },
                onException = { error ->
                    logger.e(error) { "[showIncomingCall] Telecom registration failed" }
                    fallbackToCallService(request)
                },
            )
        }
    }

    override fun dismissIncomingCall(callId: StreamCallId, config: CallServiceConfig) {
        NotificationManagerCompat.from(context)
            .cancel(callId.getNotificationId(NotificationType.Incoming))
    }

    override fun finishIncomingCall(call: Call) {
        dismissIncomingCall(StreamCallId.fromCallCid(call.cid), DefaultCallConfigurations.default)
        telecomCallController.leaveCall(call)
    }

    private fun fallbackToCallService(request: IncomingCallRequest) {
        logger.w { "[showIncomingCall] Telecom unavailable; falling back to CallService" }
        fallbackCoordinator.showIncomingCall(request, IncomingRingtoneOwner.Notification)
    }

    private fun connectCoordinatorWebSocket(call: Call) {
        call.scope.launch { client.connectIfNotAlreadyConnected() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initTelecomRepository(
        call: Call,
        request: IncomingCallRequest,
    ): JetpackTelecomRepository = call.state.jetpackTelecomRepository
        ?: jetpackTelecomRepositoryProvider.get(request.callId).also {
            call.state.jetpackTelecomRepository = it
        }

    private fun hasNotificationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
}
