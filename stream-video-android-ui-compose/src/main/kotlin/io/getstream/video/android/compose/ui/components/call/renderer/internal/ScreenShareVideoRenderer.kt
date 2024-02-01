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

package io.getstream.video.android.compose.ui.components.call.renderer.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantLabel
import io.getstream.video.android.compose.ui.components.indicator.NetworkQualityIndicator
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipant
import io.getstream.video.android.mock.previewParticipantsList
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

/**
 * Represents the content of a screen sharing session.
 *
 * @param call The call state.
 * @param session The screen sharing session to show.
 * @param modifier Modifier for styling.
 */
@Composable
public fun ScreenShareVideoRenderer(
    call: Call,
    session: ScreenSharingSession,
    modifier: Modifier = Modifier,
    labelPosition: Alignment = Alignment.BottomStart,
    isShowConnectionQualityIndicator: Boolean = true,
    isZoomable: Boolean = true,
) {
    val screenShareParticipant = session.participant
    val screenSharing by screenShareParticipant.screenSharing.collectAsStateWithLifecycle()

    val zoomableModifier = if (isZoomable) {
        Modifier.zoomable(rememberZoomableState())
    } else {
        Modifier
    }

    Box(modifier = modifier) {
        VideoRenderer(
            modifier = zoomableModifier
                .wrapContentSize()
                .align(Alignment.Center),
            call = call,
            video = screenSharing,
            videoScalingType = VideoScalingType.SCALE_ASPECT_FIT,
        )

        ParticipantLabel(call, screenShareParticipant, labelPosition)

        if (isShowConnectionQualityIndicator) {
            val connectionQuality by screenShareParticipant.networkQuality.collectAsStateWithLifecycle()
            NetworkQualityIndicator(
                networkQuality = connectionQuality,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Preview
@Composable
private fun ScreenShareVideoRendererPreview() {
    VideoTheme {
        StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
        ScreenShareVideoRenderer(
            call = previewCall, session = ScreenSharingSession(
                participant = previewParticipantsList[0],
            )
        )
    }
}
