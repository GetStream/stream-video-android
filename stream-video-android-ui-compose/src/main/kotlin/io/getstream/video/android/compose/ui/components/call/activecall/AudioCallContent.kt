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

package io.getstream.video.android.compose.ui.components.call.activecall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.permission.rememberCallPermissionsState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.internal.DefaultPermissionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.participants.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

/**
 * Represents the UI of an active audio only call.
 *
 * @param modifier Modifier for styling.
 * @param call The call to be rendered.
 * @param isMicrophoneEnabled Weather or not the microphone icon will show the mic as enabled or not.
 * @param permissions Android permissions that should be requested.
 * @param isShowingHeader If true, header content is shown.
 * @param headerContent Content that overrides the header.
 * @param durationPlaceholder Content (text) shown while the duration is not available.
 * @param detailsContent Content that overrides the details (the middle part of the screen).
 * @param controlsContent Content that allows users to trigger call actions. See [CallAction].
 * @param onCallAction Handler used when the user triggers a [CallAction].
 * @param onBackPressed Handler used when the user taps on the back button.
 */
@Composable
public fun AudioCallContent(
    modifier: Modifier = Modifier,
    call: Call,
    isMicrophoneEnabled: Boolean,
    permissions: VideoPermissionsState = rememberCallPermissionsState(
        call = call,
        permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
        ),
    ),
    isShowingHeader: Boolean = true,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    durationPlaceholder: String = "",
    detailsContent: (
        @Composable ColumnScope.(
            remoteParticipants: List<ParticipantState>,
            topPadding: Dp,
        ) -> Unit
    )? = null,
    controlsContent: (@Composable BoxScope.() -> Unit)? = null,
    onCallAction: (CallAction) -> Unit = { action: CallAction ->
        DefaultOnCallActionHandler.onCallAction(call, action)
    },
    onBackPressed: () -> Unit = {},
) {
    val remoteParticipants by call.state.remoteParticipants.collectAsStateWithLifecycle()
    val duration by call.state.duration.collectAsStateWithLifecycle()
    val durationText = duration?.toString() ?: durationPlaceholder

    DefaultPermissionHandler(videoPermission = permissions)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = VideoTheme.colors.baseSheetTertiary),
    ) {
        Column {
            if (isShowingHeader) headerContent?.invoke(this)

            detailsContent?.invoke(this, remoteParticipants, VideoTheme.dimens.spacingM) ?: AudioCallDetails(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = VideoTheme.dimens.spacingM),
                participants = remoteParticipants,
                duration = durationText,
            )
        }

        controlsContent?.invoke(this) ?: AudioCallControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VideoTheme.dimens.componentHeightM),
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = onCallAction,
        )
    }
}

@Composable
public fun AudioCallDetails(
    modifier: Modifier = Modifier,
    duration: String,
    participants: List<ParticipantState>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ParticipantAvatars(participants = participants)

        Spacer(modifier = Modifier.height(32.dp))

        ParticipantInformation(
            isVideoType = false,
            callStatus = CallStatus.Ongoing(duration),
            participants = participants,
        )
    }
}

@Composable
public fun AudioCallControls(
    modifier: Modifier,
    isMicrophoneEnabled: Boolean,
    onCallAction: (CallAction) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToggleMicrophoneAction(
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = onCallAction,
            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
            onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle(),
        )

        LeaveCallAction(
            onCallAction = onCallAction,
        )
    }
}

@Preview
@Composable
private fun AudioCallContentPreview() {
    val context = LocalContext.current
    StreamPreviewDataUtils.initializeStreamVideo(context)
    VideoTheme {
        AudioCallContent(
            call = previewCall,
            isMicrophoneEnabled = false,
            durationPlaceholder = "11:45",
        )
    }
}
