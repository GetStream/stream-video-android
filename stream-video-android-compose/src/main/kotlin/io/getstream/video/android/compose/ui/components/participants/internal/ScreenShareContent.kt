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

package io.getstream.video.android.compose.ui.components.participants.internal

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.getstream.video.android.compose.ui.components.connection.ConnectionQualityIndicator
import io.getstream.video.android.compose.ui.components.participants.ParticipantLabel
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.ScreenSharingSession
import stream.video.sfu.models.TrackType

/**
 * Represents the content of a screen sharing session.
 *
 * @param call The call state.
 * @param session The screen sharing session to show.
 * @param modifier Modifier for styling.
 * @param onRender Handler when the video content renders.
 */
@Composable
public fun ScreenShareContent(
    call: Call?,
    session: ScreenSharingSession,
    modifier: Modifier = Modifier,
    labelPosition: Alignment = Alignment.BottomStart,
    isShowConnectionQualityIndicator: Boolean = true,
    onRender: (View) -> Unit = {}
) {
    val screenShareParticipant = session.participant

    Box(modifier = modifier) {
        VideoRenderer(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            call = call,
            videoTrack = session.track,
            onRender = onRender,
            trackType = TrackType.TRACK_TYPE_SCREEN_SHARE,
            sessionId = session.participant.sessionId
        )

        ParticipantLabel(screenShareParticipant, labelPosition)

        if (isShowConnectionQualityIndicator) {
            ConnectionQualityIndicator(
                connectionQuality = screenShareParticipant.connectionQuality,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * TODO - we should fetch this info from the BE or something as we can't guess all screen sharing
 * will be in 16:9, it can be 4:3, 1:1 or even ultra-wide aspect.
 */
internal const val ScreenShareAspectRatio: Float = 16f / 9f
