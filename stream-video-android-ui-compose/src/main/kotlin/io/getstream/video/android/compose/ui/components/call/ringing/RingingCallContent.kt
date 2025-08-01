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

package io.getstream.video.android.compose.ui.components.call.ringing

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

/**
 * Represents different outgoing/incoming call content based on the call state provided from the [call].
 *
 * Depending on the call state, we show [CallContent], [IncomingCallContent] or [OutgoingCallContent] respectively.
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
 * @param onAcceptedContent Content is shown when the call is accepted.
 * @param onRejectedContent Content is shown when the call is rejected.
 * @param onNoAnswerContent Content is shown when a receiver did not answer on time.
 */
@Composable
public fun RingingCallContent(
    call: Call,
    modifier: Modifier = Modifier,
    isVideoType: Boolean = true,
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
    onCallAction: (CallAction) -> Unit = { DefaultOnCallActionHandler.onCallAction(call, it) },
    onAcceptedContent: @Composable () -> Unit,
    onRejectedContent: @Composable () -> Unit = {},
    onNoAnswerContent: @Composable () -> Unit = {},
    onIncomingContent: (
        @Composable (
            modifier: Modifier,
            call: Call,
            isVideoType: Boolean,
            isShowingHeader: Boolean,
            headerContent: (@Composable ColumnScope.() -> Unit)?,
            detailsContent: (
                @Composable ColumnScope.(
                    participants: List<MemberState>,
                    topPadding: Dp,
                ) -> Unit
            )?,
            controlsContent: (@Composable BoxScope.() -> Unit)?,
            onBackPressed: () -> Unit,
            onCallAction: (CallAction) -> Unit,
        ) -> Unit
    )? = null,
    onOutgoingContent: (
        @Composable (
            modifier: Modifier,
            call: Call,
            isVideoType: Boolean,
            isShowingHeader: Boolean,
            headerContent: (@Composable ColumnScope.() -> Unit)?,
            detailsContent: (
                @Composable ColumnScope.(
                    participants: List<MemberState>,
                    topPadding: Dp,
                ) -> Unit
            )?,
            controlsContent: (@Composable BoxScope.() -> Unit)?,
            onBackPressed: () -> Unit,
            onCallAction: (CallAction) -> Unit,
        ) -> Unit
    )? = null,
    onIdle: @Composable () -> Unit = {},
) {
    val ringingState by call.state.ringingState.collectAsStateWithLifecycle()
    when (ringingState) {
        is RingingState.Incoming -> {
            onIncomingContent?.invoke(
                modifier,
                call,
                isVideoType,
                isShowingHeader,
                headerContent,
                detailsContent,
                controlsContent,
                onBackPressed,
                onCallAction,
            ) ?: IncomingCallContent(
                call = call,
                isVideoType = isVideoType,
                modifier = modifier,
                isShowingHeader = isShowingHeader,
                headerContent = headerContent,
                detailsContent = detailsContent,
                controlsContent = controlsContent,
                onBackPressed = onBackPressed,
                onCallAction = onCallAction,
            )
        }

        is RingingState.Outgoing -> {
            onOutgoingContent?.invoke(
                modifier,
                call,
                isVideoType,
                isShowingHeader,
                headerContent,
                detailsContent,
                controlsContent,
                onBackPressed,
                onCallAction,
            ) ?: OutgoingCallContent(
                call = call,
                isVideoType = isVideoType,
                modifier = modifier,
                isShowingHeader = isShowingHeader,
                headerContent = headerContent,
                detailsContent = detailsContent,
                controlsContent = controlsContent,
                onBackPressed = onBackPressed,
                onCallAction = onCallAction,
            )
        }

        RingingState.RejectedByAll -> {
            onRejectedContent.invoke()
        }

        RingingState.TimeoutNoAnswer -> {
            onNoAnswerContent.invoke()
        }

        RingingState.Active -> {
            onAcceptedContent.invoke()
        }

        RingingState.Idle -> {
            // Includes Idle
            onIdle.invoke()
        }
    }
}

@Preview
@Composable
private fun RingingCallContentPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        RingingCallContent(
            call = previewCall,
            isVideoType = true,
            onAcceptedContent = {},
            onRejectedContent = {},
        )
    }
}
