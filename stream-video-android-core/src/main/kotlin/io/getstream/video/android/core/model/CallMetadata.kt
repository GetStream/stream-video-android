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

package io.getstream.video.android.core.model

import java.io.Serializable
import java.util.Date

public data class CallMetadata(
    val cid: StreamCallCid,
    val id: StreamCallId,
    val type: StreamCallType,
    val kind: StreamCallKind,
    val createdByUserId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val recordingEnabled: Boolean,
    val broadcastingEnabled: Boolean,
//    val callEgress: CallEgress,
    val callDetails: CallDetails,
    val users: Map<String, CallUser>,
    val custom: Map<String, Any>,
) : Serializable {
    companion object {
        fun empty(): CallMetadata {
            val details = CallDetails(emptyList(), emptyMap(), emptyList())
            return CallMetadata("", "", "", StreamCallKind.MEETING, "123", 0, 0, false, false, details, emptyMap(), emptyMap())
        }
    }
}

public fun CallMetadata.toInfo(): CallInfo = CallInfo(
    cid = cid,
    id = id,
    type = type,
    createdByUserId = createdByUserId,
    broadcastingEnabled = broadcastingEnabled,
    recordingEnabled = recordingEnabled,
    createdAt = Date(createdAt),
    updatedAt = Date(updatedAt),
//    callEgress = callEgress,
    custom = custom
)
