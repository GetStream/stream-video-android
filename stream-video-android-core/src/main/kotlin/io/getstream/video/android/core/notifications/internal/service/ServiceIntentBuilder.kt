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

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_KEY
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_OUTGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.core.utils.safeCallWithDefault

class ServiceIntentBuilder {

    private val logger by taggedLogger("TelecomIntentBuilder")

    fun buildStartIntent(context: Context, startService: StartServiceParam): Intent {
        val serviceClass = startService.callServiceConfiguration.serviceClass
        logger.i { "Resolved service class: $serviceClass" }
        val serviceIntent = Intent(context, serviceClass)
        serviceIntent.putExtra(INTENT_EXTRA_CALL_CID, startService.callId)

        when (startService.trigger) {
            TRIGGER_INCOMING_CALL -> {
                serviceIntent.putExtra(TRIGGER_KEY, TRIGGER_INCOMING_CALL)
                serviceIntent.putExtra(INTENT_EXTRA_CALL_DISPLAY_NAME, startService.callDisplayName)
            }

            TRIGGER_OUTGOING_CALL -> {
                serviceIntent.putExtra(TRIGGER_KEY, TRIGGER_OUTGOING_CALL)
            }

            TRIGGER_ONGOING_CALL -> {
                serviceIntent.putExtra(TRIGGER_KEY, TRIGGER_ONGOING_CALL)
            }

            TRIGGER_REMOVE_INCOMING_CALL -> {
                serviceIntent.putExtra(TRIGGER_KEY, TRIGGER_REMOVE_INCOMING_CALL)
            }

            else -> {
                throw IllegalArgumentException(
                    "Unknown ${startService.trigger}, must be one of: $TRIGGER_INCOMING_CALL, $TRIGGER_OUTGOING_CALL, $TRIGGER_ONGOING_CALL",
                )
            }
        }
        return serviceIntent
    }

    fun buildStopIntent(context: Context, stopServiceParam: StopServiceParam): Intent {
        val serviceClass = stopServiceParam.callServiceConfiguration.serviceClass

        return if (isServiceRunning(context, serviceClass)) {
            Intent(context, serviceClass)
        } else {
            if (true) throw IllegalStateException("Why the fuck are we here?") // TODO Rahul
            Intent(context, TelecomVoipService::class.java)
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean =
        safeCallWithDefault(true) {
            val activityManager = context.getSystemService(
                Context.ACTIVITY_SERVICE,
            ) as ActivityManager
            val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
            for (service in runningServices) {
                if (serviceClass.name == service.service.className) {
                    logger.w { "Service is running: $serviceClass" }
                    return true
                }
            }
            logger.w { "Service is NOT running: $serviceClass" }
            return false
        }
}
