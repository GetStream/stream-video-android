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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallControls
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

/**
 * Audio call content represents the UI of an active audio only call.
 *
 * @param call The call includes states and will be rendered with participants.
 * @param modifier Modifier for styling.
 * @param permissions the permissions required for the call to work (e.g. manifest.RECORD_AUDIO)
 * @param onBackPressed Handler when the user taps on the back button.
 * @param permissions Android permissions that should be required to render a video call properly.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param controlsContent Content is shown that allows users to trigger different actions to control a joined call.
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
    onCallAction: (CallAction) -> Unit = { action: CallAction ->
        DefaultOnCallActionHandler.onCallAction(call, action)
    },
    durationPlaceholder: String = "",
    isShowingHeader: Boolean = true,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    detailsContent: (
        @Composable ColumnScope.(
            participants: List<MemberState>,
            topPadding: Dp,
        ) -> Unit
    )? = null,
    controlsContent: (@Composable BoxScope.() -> Unit)? = null,
    onBackPressed: () -> Unit = {},
) {
    val duration by call.state.duration.collectAsStateWithLifecycle()
    val durationText = duration?.toString() ?: durationPlaceholder

    DefaultPermissionHandler(videoPermission = permissions)

    OutgoingCallContent(
        modifier = modifier,
        isShowingHeader = isShowingHeader,
        headerContent = headerContent,
        call = call,
        isVideoType = false,
        detailsContent = detailsContent ?: { members, topPadding ->
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = topPadding),
            ) {
                ParticipantInformation(
                    isVideoType = false,
                    callStatus = CallStatus.Calling(durationText),
                    participants = members,
                )
                Spacer(modifier = Modifier.size(16.dp))
                ParticipantAvatars(participants = members)
            }
        },
        onBackPressed = onBackPressed,
        controlsContent = controlsContent ?: {
            OutgoingCallControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(VideoTheme.dimens.spacingM),
                isVideoCall = false,
                isCameraEnabled = false,
                isMicrophoneEnabled = isMicrophoneEnabled,
                onCallAction = onCallAction,
            )
        },
    )
}

@Preview
@Composable
private fun AudioCallPreview() {
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
