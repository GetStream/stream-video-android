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

package io.getstream.video.android.compose.ui.components.audio

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.permission.rememberMicrophonePermissionState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.buildDefaultAudioControlActions
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockParticipantList

@Composable
public fun AudioParticipantsGrid(
    modifier: Modifier = Modifier,
    call: Call,
    gridCellCount: Int = 4,
    isShowingOverlayAppBar: Boolean = true,
    permissions: VideoPermissionsState = rememberMicrophonePermissionState(call = call),
    onCallAction: (CallAction) -> Unit = { DefaultOnCallActionHandler.onCallAction(call, it) },
    appBarContent: @Composable (call: Call) -> Unit = {
        CallAppBar(
            call = call,
            leadingContent = null,
            onCallAction = onCallAction
        )
    },
    style: AudioRendererStyle = RegularAudioRendererStyle(),
    audioRenderer: @Composable (
        participant: ParticipantState,
        style: AudioRendererStyle
    ) -> Unit = { audioParticipant, audioStyle ->
        ParticipantAudio(
            participant = audioParticipant,
            style = audioStyle
        )
    },
    audioContent: @Composable RowScope.(call: Call) -> Unit = {
        val participants by call.state.participants.collectAsStateWithLifecycle()
        AudioParticipantsGrid(
            modifier = modifier
                .fillMaxSize()
                .padding(top = VideoTheme.dimens.audioContentTopPadding),
            participants = participants,
            style = style,
            gridCellCount = gridCellCount,
            audioRenderer = audioRenderer
        )
    },
    controlsContent: @Composable (call: Call) -> Unit = {
        ControlActions(
            call = call,
            actions = buildDefaultAudioControlActions(call = call, onCallAction = onCallAction)
        )
    },
) {
    val orientation = LocalConfiguration.current.orientation

    DefaultPermissionHandler(videoPermission = permissions)

    Scaffold(
        modifier = modifier,
        contentColor = VideoTheme.colors.appBackground,
        topBar = { },
        bottomBar = {
            if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                controlsContent.invoke(call)
            }
        },
        content = {
            val paddings = PaddingValues(
                top = it.calculateTopPadding(),
                start = it.calculateStartPadding(layoutDirection = LocalLayoutDirection.current),
                end = it.calculateEndPadding(layoutDirection = LocalLayoutDirection.current),
                bottom = (it.calculateBottomPadding() - VideoTheme.dimens.callControllerBottomPadding)
                    .coerceAtLeast(0.dp)
            )

            Row(
                modifier = modifier
                    .background(color = VideoTheme.colors.appBackground)
                    .padding(paddings)
            ) {
                audioContent.invoke(this, call)

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    controlsContent.invoke(call)
                }
            }

            if (isShowingOverlayAppBar) {
                appBarContent.invoke(call)
            }
        }
    )
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
private fun AudioRoomPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        AudioParticipantsGrid(
            modifier = Modifier.fillMaxSize(),
            participants = mockParticipantList
        )
    }
}
