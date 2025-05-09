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

import android.content.Context
import io.getstream.video.android.core.call.CallType
import io.getstream.video.android.core.sounds.MutedRingingConfig
import io.getstream.video.android.core.sounds.RingingConfig
import io.getstream.video.android.core.sounds.defaultMutedRingingConfig
import io.getstream.video.android.core.sounds.defaultResourcesRingingConfig

enum class CallNotificationState {
    INCOMING,
    OUTGOING,
    ONGOING,
    MISSED,
    DECLINED,
    ENDED,
}

data class CallNotificationConfig(
    val states: Map<CallNotificationState, NotificationStateConfig> = emptyMap(),
)

data class NotificationStateConfig(
    val contentText: String? = null,
    val ringingConfigBuilder: ((Context) -> RingingConfig)? = null,
    val mutedRingingConfig: MutedRingingConfig? = null,
    val actionButtons: NotificationActionButtons? = null,
) {
    fun getRingingConfig(context: Context): RingingConfig? = ringingConfigBuilder?.invoke(context)
}

data class NotificationActionButtons(
    val primary: String? = null,
    val secondary: String? = null,
    val extra: String? = null,
)

object DefaultCallNotificationConfigs {

    val audioCall: CallNotificationConfig = CallNotificationConfig(
        states = mapOf(
            CallNotificationState.INCOMING to NotificationStateConfig(
                contentText = "Incoming Audio Call",
                ringingConfigBuilder = { context ->
                    defaultResourcesRingingConfig(context)
                },
                mutedRingingConfig = defaultMutedRingingConfig(),
                actionButtons = NotificationActionButtons(
                    primary = "Accept",
                    secondary = "Decline",
                ),
            ),
            CallNotificationState.OUTGOING to NotificationStateConfig(
                contentText = "Calling...",
                ringingConfigBuilder = { context ->
                    defaultResourcesRingingConfig(context)
                },
                actionButtons = NotificationActionButtons(
                    secondary = "Cancel",
                ),
            ),
            CallNotificationState.ONGOING to NotificationStateConfig(
                contentText = "On Audio Call",
                actionButtons = NotificationActionButtons(
                    primary = "Mute",
                    secondary = "End",
                ),
            ),
        ),
    )

    val livestream: CallNotificationConfig = CallNotificationConfig(
        states = mapOf(
            CallNotificationState.INCOMING to NotificationStateConfig(
                contentText = "Live Event Starting",
                actionButtons = NotificationActionButtons(
                    primary = "Join",
                    secondary = "Dismiss",
                ),
            ),
            CallNotificationState.ONGOING to NotificationStateConfig(
                contentText = "Watching Livestream",
            ),
        ),
    )

    val default: CallNotificationConfig = CallNotificationConfig(
        states = mapOf(
            CallNotificationState.INCOMING to NotificationStateConfig(
                contentText = "Incoming Video Call",
                ringingConfigBuilder = { context ->
                    defaultResourcesRingingConfig(context)
                },
                actionButtons = NotificationActionButtons(
                    primary = "Answer",
                    secondary = "Dismiss",
                ),
            ),
        ),
    )

    val all: Map<String, CallNotificationConfig> = mapOf(
        CallType.AudioCall.name to audioCall,
        CallType.Livestream.name to livestream,
        CallType.Default.name to default,
        CallType.AnyMarker.name to default,
    )
}
