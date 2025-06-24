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

package io.getstream.video.android.compose.ui.components.livestream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.core.Call
import io.getstream.video.android.ui.common.R

@Composable
internal fun LivestreamErrorUi(call: Call, onRetryJoin: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = VideoTheme.colors.baseSheetPrimary),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(
                    id = R.string.stream_video_livestreaming_error_joining,
                ),
                fontSize = 18.sp,
                color = VideoTheme.colors.basePrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            StreamButton(
                text = "Retry",
                onClick = onRetryJoin,
                style = ButtonStyles.secondaryButtonStyle(size = StyleSize.M),
            )
        }
    }
}
