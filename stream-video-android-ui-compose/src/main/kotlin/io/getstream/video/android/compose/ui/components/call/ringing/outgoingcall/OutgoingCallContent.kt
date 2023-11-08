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

package io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.background.CallBackground
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewMemberListState

/**
 * Represents the Outgoing Call state and UI, when the user is calling other people.
 *
 * @param call The call contains states and will be rendered with participants.
 * @param isVideoType Represent the call type is a video or an audio.
 * @param modifier Modifier for styling.
 * @param isShowingHeader Weather or not the app bar will be shown.
 * @param headerContent Content shown for the call header.
 * @param detailsContent Content shown for call details, such as call participant information.
 * @param controlsContent Content shown for controlling call, such as accepting a call or declining a call.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
@Composable
public fun OutgoingCallContent(
    modifier: Modifier = Modifier,
    call: Call,
    isVideoType: Boolean,
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
    onCallAction: (CallAction) -> Unit = {},
) {
    val participants: List<MemberState> by call.state.members.collectAsStateWithLifecycle()

    OutgoingCallContent(
        call = call,
        isVideoType = isVideoType,
        participants = participants,
        modifier = modifier,
        isShowingHeader = isShowingHeader,
        headerContent = headerContent,
        detailsContent = detailsContent,
        controlsContent = controlsContent,
        onBackPressed = onBackPressed,
        onCallAction = onCallAction,
    )
}

/**
 * Represents the Outgoing Call state and UI, when the user is calling other people.
 *
 * @param call The call contains states and will be rendered with participants.
 * @param isVideoType Represent the call type is a video or an audio.
 * @param modifier Modifier for styling.
 * @param participants A list of participants.
 * @param isShowingHeader Weather or not the app bar will be shown.
 * @param headerContent Content shown for the call header.
 * @param detailsContent Content shown for call details, such as call participant information.
 * @param controlsContent Content shown for controlling call, such as accepting a call or declining a call.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
@Composable
public fun OutgoingCallContent(
    modifier: Modifier = Modifier,
    call: Call,
    isVideoType: Boolean = true,
    participants: List<MemberState>,
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
    onCallAction: (CallAction) -> Unit = {},
) {
    val isCameraEnabled by if (LocalInspectionMode.current) {
        remember { mutableStateOf(true) }
    } else {
        call.camera.isEnabled.collectAsStateWithLifecycle()
    }
    val isMicrophoneEnabled by if (LocalInspectionMode.current) {
        remember { mutableStateOf(true) }
    } else {
        call.microphone.isEnabled.collectAsStateWithLifecycle()
    }

    CallBackground(
        modifier = modifier,
        participants = participants,
        isVideoType = isVideoType,
        isIncoming = false,
    ) {
        Column {
            if (isShowingHeader) {
                headerContent?.invoke(this) ?: CallAppBar(
                    call = call,
                    onBackPressed = onBackPressed,
                    onCallAction = onCallAction,
                )
            }

            val topPadding = if (participants.size == 1 || isVideoType) {
                VideoTheme.dimens.singleAvatarAppbarPadding
            } else {
                VideoTheme.dimens.avatarAppbarPadding
            }

            detailsContent?.invoke(this, participants, topPadding) ?: OutgoingCallDetails(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = topPadding),
                participants = participants,
                isVideoType = isVideoType,
            )
        }

        controlsContent?.invoke(this) ?: OutgoingCallControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VideoTheme.dimens.outgoingCallOptionsBottomPadding),
            isCameraEnabled = isCameraEnabled,
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = onCallAction,
        )
    }
}

@Preview
@Composable
private fun OutgoingCallVideoPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        OutgoingCallContent(
            call = previewCall,
            isVideoType = true,
            participants = previewMemberListState,
            onBackPressed = {},
        ) {}
    }
}

@Preview
@Composable
private fun OutgoingCallAudioPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        OutgoingCallContent(
            call = previewCall,
            isVideoType = false,
            participants = previewMemberListState,
            onBackPressed = {},
        ) {}
    }
}
