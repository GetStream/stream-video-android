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
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.ToggleScreenConfiguration
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.ScreenSharingSession

@Composable
internal fun ScreenShareContent(
    call: Call,
    session: ScreenSharingSession,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean,
    onCallAction: (CallAction) -> Unit,
    onRender: (View) -> Unit
) {
    Box(modifier = modifier) {
        VideoRenderer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ScreenShareAspectRatio, false)
                .align(Alignment.Center),
            call = call,
            videoTrack = session.track,
            onRender = onRender
        )

        Row(modifier = Modifier.align(Alignment.BottomEnd)) {
            val configuration = LocalConfiguration.current

            val orientation = configuration.orientation

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
                        R.drawable.ic_portrait_mode
                    } else {
                        R.drawable.ic_landscape_mode
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
                            id = R.string.change_orientation
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
                        R.drawable.ic_fullscreen_exit
                    } else {
                        R.drawable.ic_fullscreen
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
                            id = R.string.toggle_fullscreen
                        ),
                        tint = VideoTheme.colors.textHighEmphasis
                    )
                }
            )
        }
    }
}

private const val ScreenShareAspectRatio: Float = 16f / 9f