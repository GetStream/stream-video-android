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

package io.getstream.video.android.utils

import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.StreamCallKind
import io.getstream.video.android.model.toCallUsers
import stream.video.coordinator.client_v1_rpc.CallEnvelope

internal fun CallEnvelope.toCall(kind: StreamCallKind): CallMetadata {
    // val extraDataJson = custom_json.toByteArray().decodeToString() // TODO - check this

    val call = call!!

    return with(call) {
        CallMetadata(
            cid = call_cid,
            id = id,
            type = type,
            kind = kind,
            createdByUserId = created_by_user_id,
            createdAt = created_at?.epochSecond ?: 0,
            updatedAt = updated_at?.epochSecond ?: 0,
            recordingEnabled = settings_overrides?.recording?.enabled ?: false,
            broadcastingEnabled = settings_overrides?.broadcasting?.enabled ?: false,
            users = users.toCallUsers(),
            extraData = emptyMap() // Json.decodeFromString<Map<String, String>>(extraDataJson)
        )
    }
}
