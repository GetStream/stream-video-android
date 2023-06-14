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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockParticipantList

@Composable
public fun AudioRoom(
    modifier: Modifier = Modifier,
    call: Call,
    style: AudioRendererStyle = RegularAudioRendererStyle(),
    gridCellCount: Int = 4
) {
    val participants by call.state.participants.collectAsStateWithLifecycle()

    AudioRoom(
        modifier = modifier.background(VideoTheme.colors.appBackground),
        participants = participants,
        style = style,
        gridCellCount = gridCellCount
    )
}

@Composable
public fun AudioRoom(
    modifier: Modifier = Modifier,
    participants: List<ParticipantState>,
    style: AudioRendererStyle = RegularAudioRendererStyle(),
    gridCellCount: Int = 4
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(gridCellCount),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 22.dp)
    ) {
        items(items = participants, key = { it.sessionId }) { participant ->
            ParticipantAudio(
                participant = participant,
                style = style
            )
        }
    }
}

@Preview
@Composable
private fun AudioRoomPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        AudioRoom(
            modifier = Modifier.fillMaxSize(),
            participants = mockParticipantList
        )
    }
}
