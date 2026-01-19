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

package io.getstream.video.android.compose.ui.components.audio

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewParticipantsList

/**
 * Renders all the participants to construct a audio room, based on the number of people in a call and the call state.
 *
 * @param modifier Modifier for styling.
 * @param participants A list of participant to construct the grid.
 * @param style Represents a regular audio call render styles.
 * @param audioRenderer A single audio renderer renders each individual participant.
 */
@Composable
public fun AudioParticipantsGrid(
    modifier: Modifier = Modifier,
    participants: List<ParticipantState>,
    style: AudioRendererStyle = RegularAudioRendererStyle(),
    audioRenderer: @Composable (
        participant: ParticipantState,
        style: AudioRendererStyle,
    ) -> Unit = { audioParticipant, audioStyle ->
        ParticipantAudio(
            participant = audioParticipant,
            style = audioStyle,
        )
    },
) {
    val orientation = LocalConfiguration.current.orientation

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(VideoTheme.dimens.genericMax),
        contentPadding = PaddingValues(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(
            VideoTheme.dimens.spacingM,
        ),
        horizontalArrangement = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Arrangement.spacedBy(VideoTheme.dimens.spacingM)
        } else {
            Arrangement.spacedBy(0.dp)
        },
    ) {
        items(items = participants, key = { it.sessionId }) { participant ->
            audioRenderer.invoke(participant, style)
        }
    }
}

@Preview
@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun AudioParticipantsGridPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        AudioParticipantsGrid(
            modifier = Modifier.fillMaxSize(),
            participants = previewParticipantsList,
        )
    }
}
