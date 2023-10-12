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

package io.getstream.video.android.compose.ui.components.livestream

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call

/**
 * Represents livestreaming content based on the call state provided from the [call].

 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param backstageContent Content shown when the host has not yet started the live stream.
 * @param rendererContent The rendered stream originating from the host.
 * @param overlayContent Content displayed to indicate participant counts, live stream duration, and device settings controls.
 */
@Composable
public fun LivestreamPlayer(
    modifier: Modifier = Modifier,
    call: Call,
    backstageContent: @Composable (call: Call) -> Unit = {
        BackStageContent()
    },
    rendererContent: @Composable (call: Call) -> Unit = {
        LivestreamRenderer(call = call)
    },
    overlayContent: @Composable BoxScope.(call: Call) -> Unit = {
        LivestreamPlayerOverlay(call = call)
    },
) {
    val backstage by call.state.backstage.collectAsState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (backstage) {
            backstageContent.invoke(call = call)
        } else {
            rendererContent.invoke(call = call)

            overlayContent.invoke(this, call = call)
        }
    }
}

@Composable
private fun BackStageContent() {
    Text(
        text = stringResource(
            id = io.getstream.video.android.ui.common.R.string.stream_video_livestreaming_on_backstage,
        ),
        fontSize = 14.sp,
        color = VideoTheme.colors.textHighEmphasis,
    )
}

@Composable
private fun LivestreamRenderer(
    call: Call,
) {
    val livestream by call.state.livestream.collectAsState()

    VideoRenderer(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        call = call,
        video = livestream,
    )
}
