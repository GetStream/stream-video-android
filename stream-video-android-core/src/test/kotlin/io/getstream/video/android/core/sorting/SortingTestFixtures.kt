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

package io.getstream.video.android.core.sorting

import io.getstream.video.android.core.CallActions
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.VisibilityOnScreenState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import org.threeten.bp.OffsetDateTime
import stream.video.sfu.models.ParticipantSource

internal fun stubCallActions(): CallActions =
    mockk<CallActions>(relaxed = true) {
        every { isLocalParticipant(any()) } returns false
    }

internal fun participant(
    sessionId: String,
    scope: CoroutineScope,
    callActions: CallActions = stubCallActions(),
    name: String = sessionId,
    userId: String = sessionId,
    videoEnabled: Boolean = false,
    audioEnabled: Boolean = false,
    screenSharingEnabled: Boolean = false,
    dominantSpeaker: Boolean = false,
    speaking: Boolean = false,
    roles: List<String> = emptyList(),
    source: ParticipantSource = ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
    visibility: VisibilityOnScreenState = VisibilityOnScreenState.UNKNOWN,
    joinedAt: OffsetDateTime? = null,
    lastSpeakingAt: OffsetDateTime? = null,
): ParticipantState = ParticipantState(
    sessionId = sessionId,
    scope = scope,
    callActions = callActions,
    initialUserId = userId,
    source = source,
).apply {
    _name.value = name
    _videoEnabled.value = videoEnabled
    _audioEnabled.value = audioEnabled
    _screenSharingEnabled.value = screenSharingEnabled
    _dominantSpeaker.value = dominantSpeaker
    _speaking.value = speaking
    _roles.value = roles
    _visibleOnScreen.value = visibility
    _joinedAt.value = joinedAt
    _lastSpeakingAt.value = lastSpeakingAt
}

/**
 * Six participants A-F matching React's participant-data.ts. Visibility defaults to
 * VISIBLE per the original fixture; individual tests override it.
 */
internal fun participantsAF(
    scope: CoroutineScope,
    callActions: CallActions = stubCallActions(),
): List<ParticipantState> = listOf(
    participant(
        sessionId = "1",
        scope = scope,
        callActions = callActions,
        name = "A",
        videoEnabled = true,
        audioEnabled = true,
        visibility = VisibilityOnScreenState.VISIBLE,
    ),
    participant(
        sessionId = "2",
        scope = scope,
        callActions = callActions,
        name = "B",
        videoEnabled = true,
        audioEnabled = true,
        screenSharingEnabled = true,
        visibility = VisibilityOnScreenState.VISIBLE,
    ),
    participant(
        sessionId = "3",
        scope = scope,
        callActions = callActions,
        name = "C",
        visibility = VisibilityOnScreenState.VISIBLE,
    ),
    participant(
        sessionId = "4",
        scope = scope,
        callActions = callActions,
        name = "D",
        audioEnabled = true,
        dominantSpeaker = true,
        visibility = VisibilityOnScreenState.VISIBLE,
    ),
    participant(
        sessionId = "5",
        scope = scope,
        callActions = callActions,
        name = "E",
        screenSharingEnabled = true,
        visibility = VisibilityOnScreenState.VISIBLE,
    ),
    participant(
        sessionId = "6",
        scope = scope,
        callActions = callActions,
        name = "F",
        videoEnabled = true,
        audioEnabled = true,
        visibility = VisibilityOnScreenState.VISIBLE,
    ),
)

/**
 * Fifteen participants P1..P15 with a visible window of P8..P12. Off-screen visibility
 * is configurable so tests can exercise both INVISIBLE (default) and UNKNOWN.
 */
internal fun participants15(
    scope: CoroutineScope,
    callActions: CallActions = stubCallActions(),
    offscreenVisibility: VisibilityOnScreenState = VisibilityOnScreenState.INVISIBLE,
): List<ParticipantState> = (1..15).map { n ->
    val visible = n in 8..12
    participant(
        sessionId = n.toString(),
        scope = scope,
        callActions = callActions,
        name = "P$n",
        visibility = if (visible) VisibilityOnScreenState.VISIBLE else offscreenVisibility,
    )
}
