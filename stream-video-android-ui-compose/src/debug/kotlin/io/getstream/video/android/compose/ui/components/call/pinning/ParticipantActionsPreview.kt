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

package io.getstream.video.android.compose.ui.components.call.pinning

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipant

@Preview
@Composable
private fun ParticipantActionDialogPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantActionsDialog(
                call = previewCall,
                participant = previewParticipant,
                actions = participantActions,
                offset = IntOffset(
                    x = 0,
                    y = 50,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantActionsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantActionsWithoutState(
                actions = participantActions,
                call = previewCall,
                participant = previewParticipant,
                showDialog = true,
            ) {
            }
        }
    }
}

@Preview
@Composable
private fun ParticipantActionsKickPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantActionsWithoutState(
                actions = participantActions,
                call = previewCall,
                participant = previewParticipant,
                showDialog = true,
            ) {
            }
        }
    }
}
