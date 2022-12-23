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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.CallParticipant
import io.getstream.video.android.compose.ui.components.participants.FloatingParticipantItem
import io.getstream.video.android.model.Call

/**
 * Renders call participants based on the number of people in a call.
 *
 * @param call The state of the call.
 * @param onRender Handler when the video content renders.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun BoxScope.Participants(
    call: Call,
    onRender: (View) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    parentSize: IntSize = IntSize(0, 0)
) {
    val primarySpeaker by call.primarySpeaker.collectAsState(initial = null)
    val roomParticipants by call.callParticipants.collectAsState(emptyList())
    val nonLocal = roomParticipants.filter { !it.isLocal }

    when (roomParticipants.size) {
        0 -> Unit
        1 -> {
            val participant = roomParticipants.first()

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
            val fourthParticipant = roomParticipants.first { it.isLocal }

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

    if (roomParticipants.size in 2..3) {
        val currentLocal = roomParticipants.first { it.isLocal }

        FloatingParticipantItem(
            call = call,
            localParticipant = currentLocal,
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
