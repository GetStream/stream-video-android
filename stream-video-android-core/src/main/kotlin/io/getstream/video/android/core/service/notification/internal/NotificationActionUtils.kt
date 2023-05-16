/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.service.notification.internal

import android.content.Intent
import io.getstream.video.android.core.service.notification.NotificationAction
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId

private const val KEY_CAll_ID = "call_id"

internal fun Intent.extractNotificationAction(): NotificationAction {
    val callId = extractCallId()
    return when (val action = action) {
        NotificationAction.Accept.NAME -> NotificationAction.Accept(callId)
        NotificationAction.Reject.NAME -> NotificationAction.Reject(callId)
        NotificationAction.Cancel.NAME -> NotificationAction.Cancel(callId)
        else -> error("unexpected action: $action")
    }
}

internal fun Intent.setNotificationAction(action: NotificationAction): Intent {
    putExtra(KEY_CAll_ID, action.callId)
    this.action = when (action) {
        is NotificationAction.Accept -> NotificationAction.Accept.NAME
        is NotificationAction.Reject -> NotificationAction.Reject.NAME
        is NotificationAction.Cancel -> NotificationAction.Cancel.NAME
    }
    return this
}

private fun Intent.extractCallId(): StreamCallId =
    streamCallId(KEY_CAll_ID) ?: error("no type found")
