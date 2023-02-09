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

package io.getstream.video.android.compose.ui.components.participants.internal

import android.view.View
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.CallParticipant
import io.getstream.video.android.compose.ui.components.participants.LocalVideoContent
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.CallParticipantState

/**
 * Renders call participants based on the number of people in a call, in landscape mode.
 *
 * @param call The state of the call.
 * @param primarySpeaker The primary speaker in the call.
 * @param callParticipants The list of participants in the call.
 * @param modifier Modifier for styling.
 * @param paddingValues The padding within the parent.
 * @param parentSize The size of the parent.
 * @param onRender Handler when the video content renders.
 */
@Composable
internal fun BoxScope.LandscapeParticipants(
    call: Call,
    primarySpeaker: CallParticipantState?,
    callParticipants: List<CallParticipantState>,
    modifier: Modifier,
    paddingValues: PaddingValues,
    parentSize: IntSize,
    onRender: (View) -> Unit
) {
    val nonLocal = callParticipants.filter { !it.isLocal }.take(3)

    val renderedParticipantCount = 1 + nonLocal.size
    val rowItemWeight = 1f / renderedParticipantCount

    Row(modifier = modifier) {
        nonLocal.forEach { participant ->
            CallParticipant(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(rowItemWeight),
                call = call,
                participant = participant,
                isFocused = primarySpeaker?.id == participant.id
            )
        }

        if (callParticipants.size == 1 || callParticipants.size >= 4) {
            val local = callParticipants.firstOrNull { it.isLocal }

            if (local != null) {
                CallParticipant(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(rowItemWeight),
                    call = call,
                    participant = local,
                    onRender = onRender,
                    isFocused = primarySpeaker?.id == local.id,
                    paddingValues = paddingValues
                )
            }
        }
    }

    if (renderedParticipantCount in 2..3) {
        LocalVideoContent(
            call = call,
            localParticipant = callParticipants.first { it.isLocal },
            parentBounds = parentSize,
            modifier = Modifier
                .size(
                    height = VideoTheme.dimens.floatingVideoHeight,
                    width = VideoTheme.dimens.floatingVideoWidth
                )
                .clip(RoundedCornerShape(16.dp))
                .align(Alignment.TopEnd),
            paddingValues = paddingValues
        )
    }
}
