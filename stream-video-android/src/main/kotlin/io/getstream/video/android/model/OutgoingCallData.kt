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

package io.getstream.video.android.model

public data class OutgoingCallData(
    val callType: CallType,
    val callInfo: CallInfo,
    val participants: List<CallUser>
) : java.io.Serializable

public fun OutgoingCallData.toMetadata(): CallMetadata =
    CallMetadata(
        id = callInfo.callId,
        cid = callInfo.cid,
        type = callInfo.type,
        users = participants.associateBy { it.id },
        createdAt = callInfo.createdAt?.time ?: 0,
        updatedAt = callInfo.updatedAt?.time ?: 0,
        createdBy = callInfo.createdByUserId,
        broadcastingEnabled = false,
        recordingEnabled = false,
        extraData = emptyMap()
    )
