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

package io.getstream.video.android.compose.ui.components.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.model.CallParticipantState

@Composable
internal fun OverlayScreenSharingAppBar(
    sharingParticipant: CallParticipantState,
    onBackPressed: () -> Unit
) {
    Row(
        modifier = Modifier.background(color = VideoTheme.colors.overlay),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            enabled = true, // TODO - we would need the info of displayed overlays
            onClick = onBackPressed,
            modifier = Modifier.padding(
                start = VideoTheme.dimens.callAppBarLeadingContentSpacingStart,
                end = VideoTheme.dimens.callAppBarLeadingContentSpacingEnd
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(id = R.string.back_button_content_description),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(
                id = io.getstream.video.android.R.string.stream_screen_sharing_title,
                sharingParticipant.name.ifEmpty { sharingParticipant.id }
            ),
            color = Color.White,
            style = VideoTheme.typography.title3Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
