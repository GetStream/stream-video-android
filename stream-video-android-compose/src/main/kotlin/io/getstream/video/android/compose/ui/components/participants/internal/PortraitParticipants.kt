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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntSize
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.CallParticipant
import io.getstream.video.android.compose.ui.components.participants.LocalVideoContent
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.CallParticipantState

/**
 * Renders call participants based on the number of people in a call, in portrait mode.
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
internal fun BoxScope.PortraitParticipants(
    call: Call,
    primarySpeaker: CallParticipantState?,
    callParticipants: List<CallParticipantState>,
    modifier: Modifier,
    paddingValues: PaddingValues,
    parentSize: IntSize,
    onRender: (View) -> Unit
) {
    val nonLocal = callParticipants.filter { !it.isLocal }

    when (callParticipants.size) {
        0 -> Unit
        1 -> {
            val participant = callParticipants.first()

            CallParticipant(
                modifier = modifier,
                call = call,
                participant = participant,
                onRender = onRender,
                isFocused = primarySpeaker?.id == participant.id,
                paddingValues = paddingValues
            )
        }
        2 -> {
            val participant = nonLocal.first()

            CallParticipant(
                modifier = modifier,
                call = call,
                participant = participant,
                onRender = onRender,
                isFocused = primarySpeaker?.id == participant.id,
                paddingValues = paddingValues
            )
        }
        3 -> {
            val firstParticipant = nonLocal[0]
            val secondParticipant = nonLocal[1]

            Column(modifier) {
                CallParticipant(
                    modifier = Modifier.weight(1f),
                    call = call,
                    participant = firstParticipant,
                    isFocused = primarySpeaker?.id == firstParticipant.id
                )

                CallParticipant(
                    modifier = Modifier.weight(1f),
                    call = call,
                    participant = secondParticipant,
                    onRender = onRender,
                    isFocused = primarySpeaker?.id == secondParticipant.id,
                    paddingValues = paddingValues
                )
            }
        }
        else -> {
            /**
             * More than three participants, we only show the first four.
             */
            val firstParticipant = nonLocal[0]
            val secondParticipant = nonLocal[1]
            val thirdParticipant = nonLocal[2]
            val fourthParticipant = callParticipants.first { it.isLocal }

            Column(modifier) {
                Row(modifier = Modifier.weight(1f)) {
                    CallParticipant(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = firstParticipant,
                        isFocused = primarySpeaker?.id == firstParticipant.id
                    )

                    CallParticipant(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = secondParticipant,
                        isFocused = primarySpeaker?.id == secondParticipant.id
                    )
                }

                Row(modifier = Modifier.weight(1f)) {
                    CallParticipant(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = thirdParticipant,
                        isFocused = primarySpeaker?.id == thirdParticipant.id,
                        paddingValues = paddingValues
                    )

                    CallParticipant(
                        modifier = Modifier.weight(1f),
                        call = call,
                        participant = fourthParticipant,
                        onRender = onRender,
                        isFocused = primarySpeaker?.id == fourthParticipant.id,
                        paddingValues = paddingValues
                    )
                }
            }
        }
    }

    if (callParticipants.size in 2..3) {
        val currentLocal = callParticipants.first { it.isLocal }

        LocalVideoContent(
            call = call,
            localParticipant = currentLocal,
            parentBounds = parentSize,
            modifier = Modifier
                .size(
                    height = VideoTheme.dimens.floatingVideoHeight,
                    width = VideoTheme.dimens.floatingVideoWidth
                )
                .clip(VideoTheme.shapes.floatingParticipant)
                .align(Alignment.TopEnd),
            paddingValues = paddingValues
        )
    }
}
