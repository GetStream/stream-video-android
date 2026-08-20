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

package io.getstream.video.android.mock

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo

internal class PreviewGridCall(val call: Call, val participants: List<ParticipantState>)

/**
 * A dedicated [Call] whose state holds exactly [participantCount] participants, for the grid
 * renderer tests. The grid renderers read remote participants from call state for the small
 * layouts, so the shared [previewCall] (which always holds six participants) cannot represent
 * a one, two, or three participant call. A fresh call per test also keeps the shared state
 * untouched, so snapshots stay independent of test execution order.
 */
internal fun previewGridCall(participantCount: Int): PreviewGridCall {
    val call = Call(
        client = StreamVideo.instance(),
        type = "default",
        id = "grid-$participantCount",
        user = previewUsers[0],
    )
    call.sessionId = "session-0-${previewUsers[0].id}"
    val participants = List(participantCount) { index ->
        val user = previewUsers[index % previewUsers.size]
        ParticipantState(
            initialUserId = user.id,
            sessionId = "session-$index-${user.id}",
            scope = call.state.scope,
            callActions = call.state.callActions,
        )
    }
    call.state.upsertParticipants(participants)
    return PreviewGridCall(call, participants)
}
