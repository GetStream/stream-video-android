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

package io.getstream.video.android.compose.ui.components.audio

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.log.StreamLog
import io.getstream.video.android.compose.lifecycle.MediaPiPLifecycle
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.permission.rememberMicrophonePermissionState
import io.getstream.video.android.compose.pip.enterPictureInPicture
import io.getstream.video.android.compose.pip.rememberIsInPipMode
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.pip.PictureInPictureConfiguration
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

/**
 * Represents audio room content based on the call state provided from the [call].

 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param permissions Android permissions that should be required to render a audio call properly.
 * @param title A title that will be shown on the app bar.
 * @param appBarContent Content shown that a call information or an additional actions.
 * @param controlsContent Content is shown that allows users to trigger different actions to control a joined call.
 * @param audioRenderer A single audio renderer renders each individual participant.
 * @param onLeaveRoom A lambda that will be invoked when the leave quietly button was clicked.
 * @param pictureInPictureConfiguration User can provide Picture-In-Picture configuration.
 * @param pictureInPictureContent Content shown when the user enters Picture in Picture mode, if it's been enabled in the app.
 * @param audioContent Content is shown by rendering audio when we're connected to a call successfully.
 */
@Composable
public fun AudioRoomContent(
    modifier: Modifier = Modifier,
    call: Call,
    isShowingAppBar: Boolean = true,
    permissions: VideoPermissionsState = rememberMicrophonePermissionState(call = call),
    title: String =
        stringResource(
            id = io.getstream.video.android.ui.common.R.string.stream_video_audio_room_title,
        ),
    appBarContent: @Composable (call: Call) -> Unit = {
        AudioAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = title,
        )
    },
    style: AudioRendererStyle = RegularAudioRendererStyle(),
    audioRenderer: @Composable (
        participant: ParticipantState,
        style: AudioRendererStyle,
    ) -> Unit = { audioParticipant, audioStyle ->
        ParticipantAudio(
            participant = audioParticipant,
            style = audioStyle,
        )
    },
    audioContent: @Composable BoxScope.(call: Call) -> Unit = {
        val participants by call.state.participants.collectAsStateWithLifecycle()
        AudioParticipantsGrid(
            modifier = Modifier
                .testTag("audio_content")
                .fillMaxSize(),
            participants = participants,
            style = style,
            audioRenderer = audioRenderer,
        )
    },
    onLeaveRoom: (() -> Unit)? = null,
    onBackPressed: () -> Unit = {},
    pictureInPictureConfiguration: PictureInPictureConfiguration =
        PictureInPictureConfiguration(true),
    pictureInPictureContent: @Composable (
        call: Call,
        orientation: Int,
    ) -> Unit = { call, _ -> DefaultPictureInPictureContent(call, audioContent) },
    controlsContent: @Composable (call: Call) -> Unit = {
        AudioControlActions(
            modifier = Modifier
                .testTag("audio_controls_content")
                .fillMaxWidth(),
            call = call,
            onLeaveRoom = onLeaveRoom,
        )
    },
) {
    val context = LocalContext.current
    val orientation = LocalConfiguration.current.orientation
    val isInPictureInPicture = rememberIsInPipMode()

    DefaultPermissionHandler(videoPermission = permissions)

    MediaPiPLifecycle(
        call = call,
        pictureInPictureConfiguration,
    )

    BackHandler {
        if (pictureInPictureConfiguration.enable) {
            try {
                enterPictureInPicture(context = context, call = call, pictureInPictureConfiguration)
            } catch (e: Exception) {
                StreamLog.e(tag = "AudioRoomContent") { e.stackTraceToString() }
                call.leave()
            }
        } else {
            onBackPressed.invoke()
        }
    }

    if (isInPictureInPicture && pictureInPictureConfiguration.enable) {
        pictureInPictureContent(call, orientation)
    } else {
        Scaffold(
            modifier = modifier
                .background(VideoTheme.colors.baseSheetPrimary)
                .padding(32.dp),
            contentColor = VideoTheme.colors.baseSheetPrimary,
            topBar = {
                if (isShowingAppBar) {
                    appBarContent.invoke(call)
                }
            },
            bottomBar = { controlsContent.invoke(call) },
            content = { paddings ->
                Box(
                    modifier = Modifier
                        .background(color = VideoTheme.colors.baseSheetPrimary)
                        .padding(paddings),
                ) {
                    audioContent.invoke(this, call)
                }
            },
        )
    }
}

@Deprecated(
    "Use AudioRoomContent with pictureInPictureConfiguration",
)
@Composable
public fun AudioRoomContent(
    modifier: Modifier = Modifier,
    call: Call,
    isShowingAppBar: Boolean = true,
    permissions: VideoPermissionsState = rememberMicrophonePermissionState(call = call),
    title: String =
        stringResource(
            id = io.getstream.video.android.ui.common.R.string.stream_video_audio_room_title,
        ),
    appBarContent: @Composable (call: Call) -> Unit = {
        AudioAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = title,
        )
    },
    style: AudioRendererStyle = RegularAudioRendererStyle(),
    audioRenderer: @Composable (
        participant: ParticipantState,
        style: AudioRendererStyle,
    ) -> Unit = { audioParticipant, audioStyle ->
        ParticipantAudio(
            participant = audioParticipant,
            style = audioStyle,
        )
    },
    audioContent: @Composable BoxScope.(call: Call) -> Unit = {
        val participants by call.state.participants.collectAsStateWithLifecycle()
        AudioParticipantsGrid(
            modifier = Modifier
                .testTag("audio_content")
                .fillMaxSize(),
            participants = participants,
            style = style,
            audioRenderer = audioRenderer,
        )
    },
    onLeaveRoom: (() -> Unit)? = null,
    onBackPressed: () -> Unit = {},
    enableInPictureInPicture: Boolean,
    pictureInPictureContent: @Composable (
        call: Call,
        orientation: Int,
    ) -> Unit = { call, _ -> DefaultPictureInPictureContent(call, audioContent) },
    controlsContent: @Composable (call: Call) -> Unit = {
        AudioControlActions(
            modifier = Modifier
                .testTag("audio_controls_content")
                .fillMaxWidth(),
            call = call,
            onLeaveRoom = onLeaveRoom,
        )
    },
) {
    AudioRoomContent(
        modifier, call, isShowingAppBar, permissions, title, appBarContent, style, audioRenderer, audioContent, onLeaveRoom, onBackPressed,
        PictureInPictureConfiguration(enableInPictureInPicture),
        pictureInPictureContent, controlsContent,
    )
}

@Composable
internal fun DefaultPictureInPictureContent(
    call: Call,
    audioContent: @Composable BoxScope.(call: Call) -> Unit,
) {
    Box(
        modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
    ) {
        audioContent.invoke(this, call)
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
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720)
@Composable
private fun AudioRoomPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        AudioRoomContent(
            modifier = Modifier.fillMaxSize(),
            call = previewCall,
        )
    }
}
