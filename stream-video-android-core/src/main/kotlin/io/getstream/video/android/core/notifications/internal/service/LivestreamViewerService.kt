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

import android.app.Notification
import android.content.pm.ServiceInfo
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.core.notifications.DefaultStreamIntentResolver
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationConfig
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationContent
import io.getstream.video.android.core.notifications.medianotifications.MediaNotificationVisuals
import io.getstream.video.android.model.StreamCallId

/**
 * Due to the nature of the livestream calls, the service that is used is of different type.
 */
internal class LivestreamViewerService : LivestreamCallService() {
    override val logger: TaggedLogger by taggedLogger("LivestreamViewerService")
    override val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK

    @OptIn(ExperimentalStreamVideoApi::class)
    override fun getNotificationPair(
        trigger: String,
        streamVideo: StreamVideoClient,
        streamCallId: StreamCallId,
        intentCallDisplayName: String?,
    ): Pair<Notification?, Int> {
        return when (trigger) {
            TRIGGER_ONGOING_CALL -> Pair(
                first = streamVideo.createMediaNotification( // Noob livestream viewer 1 (ongoing) , livestream viewer 1 (leave)
                    callId = streamCallId,
                    config = getNotificationConfig(streamCallId),
                ),
                second = streamCallId.hashCode(),
            )

            else -> super.getNotificationPair(
                trigger,
                streamVideo,
                streamCallId,
                intentCallDisplayName,
            )
        }
    }

    fun getNotificationConfig(streamCallId: StreamCallId) = MediaNotificationConfig(
        MediaNotificationContent("Title", "Subtitle"),
        MediaNotificationVisuals(0, null),
        contentIntent = DefaultStreamIntentResolver(StreamVideo.instance().context)
            .searchLiveCallPendingIntent(streamCallId, streamCallId.hashCode()),
    )
}
