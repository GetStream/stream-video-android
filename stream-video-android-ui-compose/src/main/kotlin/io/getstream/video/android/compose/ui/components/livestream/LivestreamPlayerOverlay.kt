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

package io.getstream.video.android.compose.ui.components.livestream

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleSpeakerphoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

@Composable
public fun BoxScope.LivestreamPlayerOverlay(
    call: Call,
    isAudioEnabled: Boolean = true,
    onAudioToggle: (Boolean) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(6.dp),
    ) {
        LiveBadge(call = call)

        LiveDuration(call = call)

        LiveControls(call = call, isAudioEnabled, onAudioToggle)
    }
}

@Composable
private fun BoxScope.LiveBadge(call: Call) {
    val totalParticipants by call.state.totalParticipants.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.CenterStart),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .background(
                    color = VideoTheme.colors.brandPrimary,
                    shape = VideoTheme.shapes.container,
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            text = stringResource(
                id = io.getstream.video.android.ui.common.R.string.stream_video_live,
            ),
            color = Color.White,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Image(
            modifier = Modifier.size(22.dp),
            painter = painterResource(
                id = io.getstream.video.android.ui.common.R.drawable.stream_video_ic_live,
            ),
            contentDescription = stringResource(
                id = io.getstream.video.android.ui.common.R.string.stream_video_live,
            ),
        )

        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = totalParticipants.toString(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BoxScope.LiveDuration(call: Call) {
    val duration by call.state.duration.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier.align(Alignment.Center),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(VideoTheme.colors.alertWarning),
        )

        Text(
            modifier = Modifier.padding(horizontal = 6.dp),
            text = (duration ?: 0).toString(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BoxScope.LiveControls(
    call: Call,
    isAudioEnabled: Boolean = true,
    onAudioToggle: (Boolean) -> Unit = {},
) {
    ToggleSpeakerphoneAction(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .size(45.dp),
        isSpeakerphoneEnabled = isAudioEnabled,
        onCallAction = { callAction ->
            onAudioToggle(callAction.isEnabled)
        },
    )
}

@Preview
@Composable
private fun LivestreamPlayerOverlayPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            LivestreamPlayerOverlay(call = previewCall)
        }
    }
}
