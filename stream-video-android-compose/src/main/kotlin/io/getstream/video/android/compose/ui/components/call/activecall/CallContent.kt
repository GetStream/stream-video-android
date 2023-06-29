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

package io.getstream.video.android.compose.ui.components.call.activecall

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.log.StreamLog
import io.getstream.video.android.compose.lifecycle.CallMediaLifecycle
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.permission.rememberCallPermissionsState
import io.getstream.video.android.compose.pip.enterPictureInPicture
import io.getstream.video.android.compose.pip.isInPictureInPictureMode
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantsGrid
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall

/**
 * Represents the UI in an Active call that shows participants and their video, as well as some
 * extra UI features to control the call settings, browse participants and more.
 *
 * @param call The call includes states and will be rendered with participants.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param permissions Android permissions that should be required to render a video call properly.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param appBarContent Content is shown that calls information or additional actions.
 * @param style Defined properties for styling a single video call track.
 * @param videoRenderer A single video renderer renders each individual participant.
 * @param videoContent Content is shown that renders all participants' videos.
 * @param controlsContent Content is shown that allows users to trigger different actions to control a joined call.
 * @param enableInPictureInPicture If the user has engaged in Picture-In-Picture mode.
 * @param pictureInPictureContent Content shown when the user enters Picture in Picture mode, if it's been enabled in the app.
 */
@Composable
public fun CallContent(
    call: Call,
    modifier: Modifier = Modifier,
    isShowingOverlayAppBar: Boolean = true,
    permissions: VideoPermissionsState = rememberCallPermissionsState(call = call),
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = { DefaultOnCallActionHandler.onCallAction(call, it) },
    appBarContent: @Composable (call: Call) -> Unit = {
        CallAppBar(
            call = call,
            leadingContent = null,
            onCallAction = onCallAction
        )
    },
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        ParticipantVideo(
            modifier = videoModifier,
            call = videoCall,
            participant = videoParticipant,
            style = videoStyle
        )
    },
    videoContent: @Composable RowScope.(call: Call) -> Unit = {
        ParticipantsGrid(
            call = call,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            style = style,
            videoRenderer = videoRenderer
        )
    },
    controlsContent: @Composable (call: Call) -> Unit = {
        ControlActions(
            call = call,
            onCallAction = onCallAction
        )
    },
    enableInPictureInPicture: Boolean = false,
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) }
) {
    val context = LocalContext.current
    val orientation = LocalConfiguration.current.orientation
    val isInPictureInPicture = context.isInPictureInPictureMode

    DefaultPermissionHandler(videoPermission = permissions)

    CallMediaLifecycle(
        call = call,
        enableInPictureInPicture = enableInPictureInPicture
    )

    BackHandler {
        if (enableInPictureInPicture) {
            try {
                enterPictureInPicture(context = context, call = call)
            } catch (e: Exception) {
                StreamLog.e(tag = "CallContent") { e.stackTraceToString() }
                call.leave()
            }
        } else {
            onBackPressed.invoke()
        }
    }

    if (isInPictureInPicture && enableInPictureInPicture) {
        pictureInPictureContent(call)
    } else {
        Scaffold(
            modifier = modifier,
            contentColor = VideoTheme.colors.appBackground,
            topBar = { },
            bottomBar = {
                if (orientation != ORIENTATION_LANDSCAPE) {
                    controlsContent.invoke(call)
                }
            },
            content = {
                val paddings = PaddingValues(
                    top = it.calculateTopPadding(),
                    start = it.calculateStartPadding(layoutDirection = LocalLayoutDirection.current),
                    end = it.calculateEndPadding(layoutDirection = LocalLayoutDirection.current),
                    bottom = (it.calculateBottomPadding() - VideoTheme.dimens.controlActionsBottomPadding)
                        .coerceAtLeast(0.dp)
                )

                Row(
                    modifier = modifier
                        .background(color = VideoTheme.colors.appBackground)
                        .padding(paddings)
                ) {
                    videoContent.invoke(this, call)

                    if (orientation == ORIENTATION_LANDSCAPE) {
                        controlsContent.invoke(call)
                    }
                }

                if (isShowingOverlayAppBar) {
                    appBarContent.invoke(call)
                }
            }
        )
    }
}

/**
 * Renders the default PiP content, using the call state that's provided.
 *
 * @param call The state of the call, with its participants.
 */
@Composable
internal fun DefaultPictureInPictureContent(call: Call) {
    val screenSharingSession by call.state.screenSharingSession.collectAsStateWithLifecycle()
    val session = screenSharingSession
    val video = session?.participant?.video?.collectAsStateWithLifecycle()
    val pictureInPictureAspectRatio: Float = 16f / 9f

    if (session != null) {
        VideoRenderer(
            modifier = Modifier.aspectRatio(pictureInPictureAspectRatio, false),
            call = call,
            video = video?.value
        )
    } else {
        val activeSpeakers by call.state.activeSpeakers.collectAsStateWithLifecycle()
        val me by call.state.me.collectAsStateWithLifecycle()

        if (activeSpeakers.isNotEmpty()) {
            ParticipantVideo(
                call = call,
                participant = activeSpeakers.first(),
                style = RegularVideoRendererStyle(labelPosition = Alignment.BottomStart)
            )
        } else if (me != null) {
            ParticipantVideo(
                call = call,
                participant = me!!,
                style = RegularVideoRendererStyle(labelPosition = Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun DefaultPermissionHandler(
    videoPermission: VideoPermissionsState,
) {
    if (LocalInspectionMode.current) return

    LaunchedEffect(key1 = videoPermission) {
        videoPermission.launchPermissionRequest()
    }
}

@Preview
@Composable
private fun CallContentPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallContent(call = mockCall)
    }
}
