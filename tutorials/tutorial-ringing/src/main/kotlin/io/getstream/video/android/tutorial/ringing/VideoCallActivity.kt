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

package io.getstream.video.android.tutorial.ringing

import android.os.Bundle
import android.os.PersistableBundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.compose.ui.components.call.controls.actions.AcceptCallAction
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi

// Extends the ComposeStreamCallActivity class to provide a custom UI for the calling screen.
@Suppress("UNCHECKED_CAST")
class VideoCallActivity : ComposeStreamCallActivity() {

    // Internal delegate to customize the UI aspects of the call.
    private val _internalDelegate = CustomUiDelegate()

    // Getter for UI delegate, specifies the custom UI delegate for handling UI related functionality.
    override val uiDelegate: StreamActivityUiDelegate<StreamCallActivity>
        get() = _internalDelegate

    @OptIn(StreamCallActivityDelicateApi::class)
    override fun onCreate(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
        call: Call,
    ) {
        super.onCreate(savedInstanceState, persistentState, call)
        call.camera.setEnabled(isVideoCall(call))
    }

    // Custom delegate class to define specific UI behaviors and layouts for call states.
    private class CustomUiDelegate : StreamCallActivityComposeDelegate() {

        @Composable
        override fun StreamCallActivity.OutgoingCallContent(
            modifier: Modifier,
            call: Call,
            isVideoType: Boolean,
            isShowingHeader: Boolean,
            headerContent:
            @Composable()
            (ColumnScope.() -> Unit)?,
            detailsContent:
            @Composable()
            (
                ColumnScope.(participants: List<MemberState>, topPadding: Dp) -> Unit
            )?,
            controlsContent:
            @Composable()
            (BoxScope.() -> Unit)?,
            onBackPressed: () -> Unit,
            onCallAction: (CallAction) -> Unit,
        ) {
            io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent(
                call = call,
                isVideoType = isVideoType,
                modifier = modifier,
                isShowingHeader = isShowingHeader,
                headerContent = headerContent,
                detailsContent = detailsContent,
                controlsContent = controlsContent,
                onBackPressed = onBackPressed,
                onCallAction = onCallAction,
                backgroundContent = {
                    val cameraEnabled by call.camera.isEnabled.collectAsStateWithLifecycle()
                    if (cameraEnabled) {
                        VideoRenderer(
                            call = call,
                            video = ParticipantState.Video(
                                sessionId = call.sessionId,
                                track = VideoTrack(
                                    streamId = call.sessionId,
                                    video = call.camera.mediaManager.videoTrack,
                                ),
                                enabled = true,
                                paused = false,
                            ),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
            )
        }

        @Composable
        override fun StreamCallActivity.IncomingCallContent(
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
        ) {
            io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent(
                call = call,
                isVideoType = isVideoType,
                modifier = modifier,
                isShowingHeader = isShowingHeader,
                headerContent = headerContent,
                detailsContent = detailsContent,
                controlsContent = {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = VideoTheme.dimens.componentHeightM)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        CustomRejectAction(
                            reason = "custom-decline-reason",
                            onCallAction = onCallAction,
                        )

                        AcceptCallAction(
                            onCallAction = onCallAction,
                        )
                    }
                },
                onBackPressed = onBackPressed,
                onCallAction = onCallAction,
                backgroundContent = {
                    val cameraEnabled by call.camera.isEnabled.collectAsStateWithLifecycle()
                    if (cameraEnabled) {
                        VideoRenderer(
                            call = call,
                            video = ParticipantState.Video(
                                sessionId = call.sessionId,
                                track = VideoTrack(
                                    streamId = call.sessionId,
                                    video = call.camera.mediaManager.videoTrack,
                                ),
                                enabled = true,
                                paused = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        }
    }
}
