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

package io.getstream.video.android.core.notifications.internal.service.managers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.handlers.StreamDefaultNotificationHandler
import io.getstream.video.android.core.notifications.internal.service.controllers.ServiceStateController
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.getValue

internal class CallServiceNotificationManager(val stateController: ServiceStateController, val scope: CoroutineScope) {
    private val logger by taggedLogger("CallServiceNotificationManager")

    fun observeCallNotification(call: Call) {
        scope.launch {
            call.state.notificationIdFlow.filterNotNull()
                .collect { stateController.setCallNotificationId(it) }
        }
    }

    @SuppressLint("MissingPermission")
    fun justNotify(
        service: Service,
        callId: StreamCallId,
        notificationId: Int,
        notification: Notification,
    ) {
        if (ActivityCompat.checkSelfPermission(
                service, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            StreamVideo.Companion.instanceOrNull()?.getStreamNotificationDispatcher()
                ?.notify(callId, notificationId, notification)
        }
    }

    fun cancelNotifications(service: Service, callId: StreamCallId) {
        val notificationManager = NotificationManagerCompat.from(service)

        callId.let {
            logger.d { "[cancelNotifications], notificationId via hashcode: ${it.hashCode()}" }
            notificationManager.cancel(it.hashCode())
        }

        stateController.notificationId?.let { notificationId ->
            logger.d { "[cancelNotifications], notificationId from stateController: $notificationId" }
            notificationManager.cancel(notificationId)
        }

        safeCall {
            val handler = (StreamVideo.Companion.instanceOrNull() as? StreamVideoClient)
                ?.streamNotificationManager
                ?.notificationConfig
                ?.notificationHandler as? StreamDefaultNotificationHandler
            handler?.clearMediaSession(callId)
        }
    }
}
