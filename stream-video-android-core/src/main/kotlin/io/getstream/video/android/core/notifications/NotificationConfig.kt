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

package io.getstream.video.android.core.notifications

import io.getstream.android.push.PushDeviceGenerator
import io.getstream.video.android.core.call.CallType
import io.getstream.video.android.core.notifications.internal.NoOpNotificationHandler

public data class NotificationConfig(
    val pushDeviceGenerators: List<PushDeviceGenerator> = emptyList(),
    val requestPermissionOnAppLaunch: () -> Boolean = { true },
    val notificationHandler: NotificationHandler = NoOpNotificationHandler,
    val autoRegisterPushDevice: Boolean = true,
    val requestPermissionOnDeviceRegistration: () -> Boolean = { true },
    /**
     * Set this to true if you want to make the ringing notifications as low-priority
     * in case the application is in foreground. This will prevent the notification from
     * interrupting the user while he is in the app. In this case you need to make sure to
     * handle this call state and display an incoming call screen.
     * NOTE: This setting has only an effect if you don't set a custom [NotificationHandler]!
     */
    val hideRingingNotificationInForeground: Boolean = false,
    /**
     * If `true`, call notifications will receive subsequent updates, based on the number of participants and other data.
     */
    val enableCallNotificationUpdates: Boolean = true,
    /**
     * List of call types (as Strings) for which media notifications should be enabled.
     * Allows support for custom user-defined call types.
     */
    val mediaNotificationCallTypes: Set<String> = hashSetOf(CallType.Livestream.name),
)
