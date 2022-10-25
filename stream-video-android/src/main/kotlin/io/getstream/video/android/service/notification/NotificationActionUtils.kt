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

package io.getstream.video.android.service.notification

import android.content.Intent
import io.getstream.video.android.model.state.StreamCallGuid

private const val KEY_TYPE = "type"
private const val KEY_ID = "id"
private const val KEY_CID = "cid"

internal fun Intent.extractNotificationAction(): NotificationAction {
    val callGuid = extractCallGuid()
    return when (val action = action) {
        NotificationAction.Accept.NAME -> NotificationAction.Accept(callGuid)
        NotificationAction.Reject.NAME -> NotificationAction.Reject(callGuid)
        NotificationAction.Cancel.NAME -> NotificationAction.Cancel(callGuid)
        else -> error("unexpected action: $action")
    }
}

internal fun Intent.setNotificationAction(action: NotificationAction): Intent {
    putExtra(KEY_TYPE, action.guid.type)
    putExtra(KEY_CID, action.guid.id)
    putExtra(KEY_TYPE, action.guid.cid)
    this.action = when (action) {
        is NotificationAction.Accept -> NotificationAction.Accept.NAME
        is NotificationAction.Cancel -> NotificationAction.Reject.NAME
        is NotificationAction.Reject -> NotificationAction.Cancel.NAME
    }
    return this
}

private fun Intent.extractCallGuid() = StreamCallGuid(
    type = getStringExtra(KEY_TYPE) ?: error("no type found"),
    id = getStringExtra(KEY_ID) ?: error("no id found"),
    cid = getStringExtra(KEY_CID) ?: error("no cid found")
)
