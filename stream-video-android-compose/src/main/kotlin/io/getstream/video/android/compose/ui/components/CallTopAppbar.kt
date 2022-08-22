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

package io.getstream.video.android.compose.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
public fun CallTopAppbar(
    modifier: Modifier = Modifier,
    title: String = "Messages",
    onBackButtonClicked: () -> Unit = {},
    onParticipantsClicked: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(VideoTheme.dimens.topAppbarHeightSize)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { onBackButtonClicked() },
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = VideoTheme.colors.textHighEmphasis
            )

            Spacer(modifier = Modifier.width(36.dp))

            Text(
                text = title,
                fontSize = VideoTheme.dimens.topAppbarTextSize,
                color = VideoTheme.colors.textHighEmphasis,
                textAlign = TextAlign.Center,
            )
        }

        Icon(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.TopEnd)
                .clickable { onParticipantsClicked() },
            painter = painterResource(id = R.drawable.ic_participants),
            contentDescription = "Participants",
            tint = VideoTheme.colors.textHighEmphasis
        )
    }
}

@Preview
@Composable
private fun CallTopAppbarPreview() {
    VideoTheme {
        CallTopAppbar()
    }
}
