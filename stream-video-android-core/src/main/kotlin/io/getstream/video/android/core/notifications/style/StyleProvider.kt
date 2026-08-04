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

package io.getstream.video.android.core.notifications.style

import android.app.Application
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CallStyle
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import io.getstream.video.android.core.R

internal class StyleProvider(val application: Application) {
    fun getOutgoingCallStyle(
        callDisplayName: String,
        remoteParticipantCount: Int,
        hangUpIntent: PendingIntent,
    ): NotificationCompat.CallStyle {
        return CallStyle.forOngoingCall(
            Person.Builder().setName(callDisplayName).apply {
                if (remoteParticipantCount == 0) {
                    // Just one user in the call
                    setIcon(
                        IconCompat.createWithResource(
                            application,
                            R.drawable.stream_video_ic_user,
                        ),
                    )
                } else if (remoteParticipantCount > 1) {
                    // More than one user in the call
                    setIcon(
                        IconCompat.createWithResource(
                            application,
                            R.drawable.stream_video_ic_user_group,
                        ),
                    )
                }
            }.build(),
            hangUpIntent,
        )
    }

    fun getIncomingCallStyle(
        callDisplayName: String?,
        rejectCallPendingIntent: PendingIntent,
        acceptCallPendingIntent: PendingIntent,
    ): CallStyle {
        return CallStyle.forIncomingCall(
            Person.Builder().setName(callDisplayName ?: "Unknown").apply {
                if (callDisplayName == null) {
                    setIcon(
                        IconCompat.createWithResource(
                            application,
                            R.drawable.stream_video_ic_user,
                        ),
                    )
                }
            }.build(),
            rejectCallPendingIntent,
            acceptCallPendingIntent,
        )
    }
}
