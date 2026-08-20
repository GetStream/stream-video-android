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

package io.getstream.video.android.compose.ui

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList
import io.getstream.video.android.mock.previewUsers

/**
 * [previewParticipantsList] plus a seventh participant, for grid variants that need more
 * participants than the shared preview data provides. The extra participant is deliberately
 * not registered in [previewCall]'s state: registration would outlive the test in the shared
 * singleton and make snapshots of other tests depend on execution order.
 */
internal fun previewSevenParticipants(): List<ParticipantState> =
    previewParticipantsList + ParticipantState(
        initialUserId = previewUsers[0].id,
        sessionId = "session-6-${previewUsers[0].id}",
        scope = previewCall.state.scope,
        callActions = previewCall.state.callActions,
    )
