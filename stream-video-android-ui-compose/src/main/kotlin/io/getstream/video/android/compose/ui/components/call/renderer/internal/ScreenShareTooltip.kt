/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.call.renderer.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewParticipant
import io.getstream.video.android.ui.common.R

@Composable
internal fun ScreenShareTooltip(
    modifier: Modifier = Modifier,
    sharingParticipant: ParticipantState,
) {
    val userNameOrId by sharingParticipant.userNameOrId.collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .height(VideoTheme.dimens.componentHeightM)
            .wrapContentWidth()
            .clip(RoundedCornerShape(bottomEnd = 8.dp))
            .background(
                color = VideoTheme.colors.baseSheetQuarternary,
                shape = RoundedCornerShape(bottomEnd = 8.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(
                start = VideoTheme.dimens.spacingXs,
                end = VideoTheme.dimens.spacingXs,
            ),
            painter = painterResource(id = R.drawable.stream_video_ic_screensharing),
            tint = VideoTheme.colors.basePrimary,
            contentDescription = "Presenting",
        )

        Text(
            modifier = Modifier.padding(
                end = VideoTheme.dimens.spacingXs,
            ).testTag("Stream_ParticipantScreenSharingLabel"),
            text = stringResource(id = R.string.stream_video_screen_sharing_title, userNameOrId),
            color = VideoTheme.colors.basePrimary,
            style = VideoTheme.typography.titleXs,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun ScreenShareTooltipPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ScreenShareTooltip(sharingParticipant = previewParticipant)
    }
}
