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

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ToggleScreenConfiguration
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.ScreenSharingSession
import stream.video.sfu.models.TrackType
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Represents the content of a screen sharing session.
 *
 * @param call The call state.
 * @param session The screen sharing session to show.
 * @param isFullscreen If the UI is currently in full screen mode.
 * @param modifier Modifier for styling.
 * @param onRender Handler when the video content renders.
 * @param onCallAction Handler for various call actions.
 */
@Composable
public fun ScreenShareContent(
    call: Call?,
    session: ScreenSharingSession,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
    onRender: (View) -> Unit = {},
    onCallAction: (CallAction) -> Unit
) {
    Box(modifier = modifier) {
        VideoRenderer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ScreenShareAspectRatio, false)
                .align(Alignment.Center),
            call = call,
            videoTrack = session.track,
            onRender = onRender,
            trackType = TrackType.TRACK_TYPE_SCREEN_SHARE,
            sessionId = session.participant.sessionId
        )

        Row(modifier = Modifier.align(Alignment.BottomEnd)) {
            val orientation = LocalConfiguration.current.orientation

            IconButton(
                onClick = {
                    val shouldLandscape = orientation != ORIENTATION_LANDSCAPE

                    onCallAction(
                        ToggleScreenConfiguration(
                            isFullscreen = false,
                            isLandscape = shouldLandscape
                        )
                    )
                },
                content = {
                    val drawable = if (orientation == ORIENTATION_LANDSCAPE) {
                        RCommon.drawable.ic_portrait_mode
                    } else {
                        RCommon.drawable.ic_landscape_mode
                    }

                    Icon(
                        modifier = Modifier
                            .background(
                                shape = CircleShape,
                                color = VideoTheme.colors.barsBackground
                            )
                            .padding(8.dp),
                        painter = painterResource(id = drawable),
                        contentDescription = stringResource(
                            id = RCommon.string.change_orientation
                        ),
                        tint = VideoTheme.colors.textHighEmphasis
                    )
                }
            )

            IconButton(
                onClick = {
                    onCallAction(
                        ToggleScreenConfiguration(
                            isFullscreen = !isFullscreen,
                            isLandscape = if (!isFullscreen) {
                                true
                            } else {
                                orientation == ORIENTATION_LANDSCAPE
                            }
                        )
                    )
                },
                content = {
                    val drawable = if (isFullscreen) {
                        RCommon.drawable.ic_fullscreen_exit
                    } else {
                        RCommon.drawable.ic_fullscreen
                    }

                    Icon(
                        modifier = Modifier
                            .background(
                                shape = CircleShape,
                                color = VideoTheme.colors.barsBackground
                            )
                            .padding(8.dp),
                        painter = painterResource(id = drawable),
                        contentDescription = stringResource(
                            id = RCommon.string.toggle_fullscreen
                        ),
                        tint = VideoTheme.colors.textHighEmphasis
                    )
                }
            )
        }
    }
}

/**
 * TODO - we should fetch this info from the BE or something as we can't guess all screen sharing
 * will be in 16:9, it can be 4:3, 1:1 or even ultra-wide aspect.
 */
internal const val ScreenShareAspectRatio: Float = 16f / 9f
