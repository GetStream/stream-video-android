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

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.IncomingRingtoneOwner
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.DEFAULT_CALL_TEXT
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.IncomingCallPresenter
import io.getstream.video.android.core.notifications.internal.service.IncomingCallRequest
import io.getstream.video.android.core.notifications.internal.service.JetpackTelecomRepositoryProvider
import io.getstream.video.android.core.notifications.internal.service.ServiceIntentBuilder
import io.getstream.video.android.core.notifications.internal.service.ShowIncomingCallResult
import io.getstream.video.android.core.notifications.internal.service.StartServiceParam
import io.getstream.video.android.core.notifications.internal.telecom.TelecomHelper
import io.getstream.video.android.core.notifications.internal.telecom.TelecomPermissions
import io.getstream.video.android.core.utils.isAndroid17OrHigher
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

/** Coordinates the existing incoming-call service and optional Telecom registration path. */
internal class PreAndroid17IncomingCallCoordinator(
    private val context: Context,
    private val client: StreamVideoClient,
    private val incomingCallPresenter: IncomingCallPresenter,
    private val serviceIntentBuilder: ServiceIntentBuilder,
    private val telecomPermissions: TelecomPermissions,
    private val telecomHelper: TelecomHelper,
    private val jetpackTelecomRepositoryProvider: JetpackTelecomRepositoryProvider,
) : IncomingCallCoordinator {

    private val logger by taggedLogger("PreAndroid17IncomingCallCoordinator")

    @SuppressLint("MissingPermission", "NewApi")
    override fun showIncomingCall(request: IncomingCallRequest) {
        val ringtoneOwner = if (isAndroid17OrHigher()) {
            IncomingRingtoneOwner.Notification
        } else {
            IncomingRingtoneOwner.Legacy
        }
        val notification = request.notificationProvider(ringtoneOwner)
        val result = incomingCallPresenter.showIncomingCall(
            context = context,
            callId = request.callId,
            callDisplayName = request.callDisplayName,
            callServiceConfiguration = request.callServiceConfiguration,
            notification = notification,
        )
        logger.d { "[showIncomingCall] service start result: $result" }

        if (result != ShowIncomingCallResult.FG_SERVICE ||
            !telecomPermissions.canUseTelecom(request.callServiceConfiguration, context) ||
            !telecomHelper.canUseJetpackTelecom()
        ) {
            return
        }

        updateIncomingCallNotification(request, client, notification)
        val jetpackTelecomRepository = jetpackTelecomRepositoryProvider.get(request.callId)
        val addressUri = "${client.telecomConfig?.schema}:${request.callId.id}".toUri()
        val formattedCallDisplayName = request.callDisplayName
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_CALL_TEXT
        val call = client.call(request.callId.type, request.callId.id)

        call.state.jetpackTelecomRepository = jetpackTelecomRepository
        call.scope.launch {
            jetpackTelecomRepository.registerCall(
                displayName = formattedCallDisplayName,
                address = addressUri,
                isIncoming = true,
                isVideoCall = request.isVideo,
            )
        }
    }

    override fun dismissIncomingCall(
        callId: StreamCallId,
        config: CallServiceConfig,
    ) {
        safeCallWithResult {
            context.startService(
                serviceIntentBuilder.buildStartIntent(
                    context,
                    StartServiceParam(
                        callId = callId,
                        trigger = TRIGGER_REMOVE_INCOMING_CALL,
                        callServiceConfiguration = config,
                    ),
                ),
            )!!
        }.onError {
            val notificationId = callId.getNotificationId(NotificationType.Incoming)
            logger.d { "[dismissIncomingCall] notificationId: $notificationId" }
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }

    private fun updateIncomingCallNotification(
        request: IncomingCallRequest,
        client: StreamVideoClient,
        notification: android.app.Notification?,
    ) {
        notification?.let {
            val notificationId = request.callId.getNotificationId(NotificationType.Incoming)
            client.call(request.callId.type, request.callId.id)
                .state.updateNotification(notificationId, notification)
        }
    }
}
