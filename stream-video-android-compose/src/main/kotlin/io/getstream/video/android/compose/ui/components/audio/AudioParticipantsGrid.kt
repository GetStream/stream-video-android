/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockParticipantList

@Composable
public fun AudioParticipantsGrid(
    modifier: Modifier = Modifier,
    participants: List<ParticipantState>,
    style: AudioRendererStyle = RegularAudioRendererStyle(),
    gridCellCount: Int = 4,
    audioRenderer: @Composable (
        participant: ParticipantState,
        style: AudioRendererStyle
    ) -> Unit = { audioParticipant, audioStyle ->
        ParticipantAudio(
            participant = audioParticipant,
            style = audioStyle
        )
    },
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(gridCellCount),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        items(items = participants, key = { it.sessionId }) { participant ->
            audioRenderer.invoke(participant, style)
        }
    }
}

@Preview
@Composable
private fun AudioParticipantsGridPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        AudioParticipantsGrid(
            modifier = Modifier.fillMaxSize(),
            participants = mockParticipantList
        )
    }
}
