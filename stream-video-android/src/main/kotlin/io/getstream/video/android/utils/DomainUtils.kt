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

import io.getstream.video.android.model.domain.CallMetadata

internal fun stream.video.coordinator.call_v1.Call.toCall(): CallMetadata {
    // val extraDataJson = custom_json.toByteArray().decodeToString() // TODO - check this

    return CallMetadata(
        cid = call_cid,
        id = id,
        type = type,
        createdBy = created_by_user_id,
        createdAt = created_at?.epochSecond ?: 0,
        updatedAt = updated_at?.epochSecond ?: 0,
        recordingEnabled = options?.recording?.enabled ?: false,
        broadcastingEnabled = options?.broadcasting?.enabled ?: false,
        extraData = emptyMap() // Json.decodeFromString<Map<String, String>>(extraDataJson)
    )
}
