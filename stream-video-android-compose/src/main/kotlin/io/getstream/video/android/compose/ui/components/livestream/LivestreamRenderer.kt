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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call

@Composable
internal fun LivestreamRenderer(
    call: Call,
    onPausedPlayer: ((isPaused: Boolean) -> Unit)? = {},
) {
    val livestream by call.state.livestream.collectAsState()
    var isPaused by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        VideoRenderer(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = onPausedPlayer != null) {
                    if (onPausedPlayer != null) {
                        isPaused = !isPaused
                        // TODO: We should implement pause & resume methods for VideoRenderer & TextureView
                        livestream?.track?.video?.setEnabled(!isPaused)
                        onPausedPlayer.invoke(isPaused)
                    }
                },
            call = call,
            video = livestream,
        )

        AnimatedVisibility(
            visible = isPaused,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy), initialScale = 1.5f),
            exit = scaleOut(tween(150)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Image(
                modifier = Modifier.alpha(0.75f),
                painter = painterResource(
                    id = io.getstream.video.android.ui.common.R.drawable.stream_video_ic_play,
                ),
                contentDescription = null,
            )
        }
    }
}
