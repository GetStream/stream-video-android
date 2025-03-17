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

package io.getstream.video.android.compose.ui.components.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.GenericContainer
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.ui.common.R
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

/**
 * Represents the default AppBar that's shown in calls. Exposes handlers for the two default slot
 * component implementations (leading and trailing).
 *
 * Exposes slots required to customize the look and feel.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the default leading content slot.
 * @param leadingContent The leading content, by default [DefaultCallAppBarLeadingContent].
 * @param centerContent The center content, by default [DefaultCallAppBarCenterContent].
 * @param trailingContent The trailing content, by default [DefaultCallAppBarTrailingContent].
 * */
@Composable
public fun CallAppBar(
    call: Call,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = {},
    title: String = stringResource(id = R.string.stream_video_default_app_bar_title),
    leadingContent: (@Composable RowScope.() -> Unit)? = {
        DefaultCallAppBarLeadingContent(onBackPressed)
    },
    centerContent: (@Composable (RowScope.() -> Unit))? = {
        DefaultCallAppBarCenterContent(call, title)
    },
    trailingContent: (@Composable RowScope.() -> Unit)? = {
        LeaveCallAction {
            onCallAction(LeaveCall)
        }
    },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VideoTheme.dimens.componentHeightL),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        leadingContent?.invoke(this)
        centerContent?.invoke(this)
        trailingContent?.invoke(this)
    }
}

/**
 * Default leading slot, representing the back button.
 */
@Composable
internal fun DefaultCallAppBarLeadingContent(
    onBackButtonClicked: () -> Unit,
) {
    IconButton(
        onClick = onBackButtonClicked,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.stream_video_ic_arrow_back),
            contentDescription = stringResource(
                id = R.string.stream_video_back_button_content_description,
            ),
            tint = VideoTheme.colors.basePrimary,
        )
    }
}

/**
 * Default center slot, representing the call title.
 */
@Composable
internal fun RowScope.DefaultCallAppBarCenterContent(call: Call, title: String) {
    val isReconnecting by call.state.isReconnecting.collectAsStateWithLifecycle()
    val isRecording by call.state.recording.collectAsStateWithLifecycle()
    val duration by call.state.duration.collectAsStateWithLifecycle()
    CalLCenterContent(
        modifier = Modifier.align(CenterVertically),
        text = duration?.toString() ?: title,
        isRecording = isRecording,
        isReconnecting = isReconnecting,
    )
}

@Composable
private fun CalLCenterContent(
    modifier: Modifier = Modifier,
    text: String,
    isRecording: Boolean,
    isReconnecting: Boolean,
) {
    GenericContainer(modifier = modifier) {
        Row {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(VideoTheme.dimens.componentHeightS)
                        .clip(VideoTheme.shapes.circle)
                        .background(
                            color = VideoTheme.colors.alertWarning,
                            shape = VideoTheme.shapes.circle,
                        )
                        .border(2.dp, VideoTheme.colors.basePrimary, VideoTheme.shapes.circle),
                )
            }
            Text(
                modifier = Modifier
                    .padding(
                        start = VideoTheme.dimens.componentPaddingStart,
                        end = VideoTheme.dimens.componentPaddingEnd,
                    ),
                text = if (isReconnecting) {
                    stringResource(id = R.string.stream_video_call_reconnecting)
                } else if (isRecording) {
                    stringResource(id = R.string.stream_video_call_recording)
                } else {
                    text
                },
                fontSize = VideoTheme.dimens.textSizeS,
                color = VideoTheme.colors.baseSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Preview
@Composable
@ExperimentalTime
private fun CallTopAppbarPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Column {
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = false,
                    isReconnecting = false,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = true,
                    isReconnecting = false,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = false,
                    isReconnecting = false,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = false,
                    isReconnecting = true,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = true,
                    isReconnecting = false,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}
