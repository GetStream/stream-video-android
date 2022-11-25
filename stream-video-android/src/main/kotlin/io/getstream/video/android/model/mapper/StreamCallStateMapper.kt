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

package io.getstream.video.android.model.mapper

import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.StreamSfuSessionId
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.model.state.StreamDate

/**
 * Converts [StreamCallState.Outgoing] into [CallMetadata].
 */
internal fun StreamCallState.Outgoing.toMetadata(): CallMetadata =
    CallMetadata(
        cid = callGuid.cid,
        type = callGuid.type,
        id = callGuid.id,
        kind = callKind,
        users = users,
        createdAt = (createdAt as? StreamDate.Specified)?.date?.time ?: 0,
        updatedAt = (updatedAt as? StreamDate.Specified)?.date?.time ?: 0,
        createdByUserId = createdByUserId,
        broadcastingEnabled = broadcastingEnabled,
        recordingEnabled = recordingEnabled,
        extraData = emptyMap()
    )

/**
 * Converts [StreamCallState.InCall] into [StreamCallState.Connecting].
 */
internal fun StreamCallState.Joined.toConnecting(sfuSessionId: StreamSfuSessionId) = StreamCallState.Connecting(
    callGuid = callGuid,
    callKind = callKind,
    callUrl = callUrl,
    createdByUserId = createdByUserId,
    broadcastingEnabled = broadcastingEnabled,
    recordingEnabled = recordingEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    users = users,
    iceServers = iceServers,
    sfuSessionId = sfuSessionId,
    sfuToken = sfuToken,
)

/**
 * Converts [StreamCallState.Connecting] into [StreamCallState.Connected].
 */
internal fun StreamCallState.Connecting.toConnected() = StreamCallState.Connected(
    callGuid = callGuid,
    callKind = callKind,
    callUrl = callUrl,
    createdByUserId = createdByUserId,
    broadcastingEnabled = broadcastingEnabled,
    recordingEnabled = recordingEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    users = users,
    iceServers = iceServers,
    sfuSessionId = sfuSessionId,
    sfuToken = sfuToken,
)
