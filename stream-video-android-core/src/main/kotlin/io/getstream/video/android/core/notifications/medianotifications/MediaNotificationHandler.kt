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

package io.getstream.video.android.core.notifications.medianotifications

import androidx.core.app.NotificationCompat
import io.getstream.video.android.model.StreamCallId

@Deprecated(message = "This interface is deprecated. Use the notification interceptors instead.")
interface MediaNotificationHandler {

    @Deprecated(
        message = "This interface is deprecated. Use the notification interceptors instead. (StreamNotificationBuilderInterceptors and StreamNotificationUpdateInterceptors)",
    )
    fun createMinimalMediaStyleNotification(
        callId: StreamCallId,
        mediaNotificationConfig: MediaNotificationConfig,
        remoteParticipantCount: Int,
    ): NotificationCompat.Builder?

    @Deprecated(
        message = "This interface is deprecated. Use the notification interceptors instead. (StreamNotificationBuilderInterceptors and StreamNotificationUpdateInterceptors)",
    )
    fun getMediaNotificationConfig(): MediaNotificationConfig
}
