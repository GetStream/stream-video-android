/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.compose.ui.components.call.controls.actions.AcceptCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.DeclineCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.GenericAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CustomAction
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi

// Extends the ComposeStreamCallActivity class to provide a custom UI for the calling screen.
@Suppress("UNCHECKED_CAST")
class BusyCallActivity : ComposeStreamCallActivity() {

    // Internal delegate to customize the UI aspects of the call.
    private val _internalDelegate = CustomUiDelegate()

    // Getter for UI delegate, specifies the custom UI delegate for handling UI related functionality.
    override val uiDelegate: StreamActivityUiDelegate<StreamCallActivity>
        get() = _internalDelegate

    @OptIn(StreamCallActivityDelicateApi::class)
    override fun onCallAction(call: Call, action: CallAction) {
        when (action) {
            is BusyCall -> reject(call, RejectReason.Busy, onSuccessFinish, onErrorFinish)
            else -> super.onCallAction(call, action)
        }
    }

    // Custom delegate class to define specific UI behaviors and layouts for call states.
    private class CustomUiDelegate : StreamCallActivityComposeDelegate() {

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
                    val isCameraEnabled by call.camera.isEnabled.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = VideoTheme.dimens.componentHeightM)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        BusyCallAction(
                            onCallAction = onCallAction,
                        )

                        DeclineCallAction(
                            onCallAction = onCallAction,
                        )

                        if (isVideoType) {
                            ToggleCameraAction(
                                onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle(),
                                offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
                                isCameraEnabled = isCameraEnabled,
                                onCallAction = onCallAction,
                            )
                        }

                        AcceptCallAction(
                            onCallAction = onCallAction,
                        )
                    }
                },
                onBackPressed = onBackPressed,
                onCallAction = onCallAction,
            )
        }
    }
}

@Composable
fun BusyCallAction(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCallAction: (BusyCall) -> Unit,
    icon: ImageVector? = null,
    bgColor: Color? = null,
    iconTint: Color? = null,
): Unit = GenericAction(
    modifier = modifier,
    enabled = enabled,
    onAction = { onCallAction(BusyCall) },
    icon = icon ?: Icons.Default.Close,
    color = bgColor ?: VideoTheme.colors.alertWarning,
    iconTint = iconTint ?: VideoTheme.colors.basePrimary,
)

data object BusyCall : CustomAction(tag = "busy")
