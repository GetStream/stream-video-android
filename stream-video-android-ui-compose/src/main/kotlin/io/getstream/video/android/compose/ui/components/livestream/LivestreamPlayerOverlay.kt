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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleSpeakerphoneAction
import io.getstream.video.android.core.Call

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
            .padding(StreamTokens.spacingXs),
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
                    color = VideoTheme.colors.accentPrimary,
                    shape = RoundedCornerShape(StreamTokens.radius4xl),
                )
                .padding(horizontal = StreamTokens.spacingMd, vertical = StreamTokens.spacing2xs),
            text = stringResource(
                id = io.getstream.video.android.ui.common.R.string.stream_video_live,
            ),
            color = VideoTheme.colors.textOnAccent,
        )

        Spacer(modifier = Modifier.width(StreamTokens.spacingSm))

        Image(
            modifier = Modifier.size(StreamTokens.iconSizeMd),
            painter = painterResource(
                id = io.getstream.video.android.compose.R.drawable.stream_design_ic_livestream_fill,
            ),
            contentDescription = stringResource(
                id = io.getstream.video.android.ui.common.R.string.stream_video_live,
            ),
        )

        Text(
            modifier = Modifier.padding(horizontal = StreamTokens.spacingXs),
            text = totalParticipants.toString(),
            color = VideoTheme.colors.textOnAccent,
            style = VideoTheme.typography.captionEmphasis,
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
                .size(StreamTokens.size8)
                .clip(CircleShape)
                .background(VideoTheme.colors.accentError),
        )

        Text(
            modifier = Modifier.padding(horizontal = StreamTokens.spacingXs),
            text = (duration ?: 0).toString(),
            color = VideoTheme.colors.textOnAccent,
            style = VideoTheme.typography.captionEmphasis,
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
        modifier = Modifier.align(Alignment.CenterEnd),
        isSpeakerphoneEnabled = isAudioEnabled,
        onCallAction = { callAction ->
            onAudioToggle(callAction.isEnabled)
        },
    )
}
