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

package io.getstream.video.android.compose.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList

/**
 * Previews for the [VideoComponentFactory] default implementations that are not reached through
 * the component slots covered by the other snapshot tests. Rendering them pins the defaults that
 * are otherwise only exercised at runtime, such as the null fallbacks for `title`, `onCallAction`
 * and `actions`.
 */

@Preview
@Composable
private fun VideoComponentFactoryCallAppBarRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryCallAppBarPreview()
    }
}

@Composable
internal fun VideoComponentFactoryCallAppBarPreview() {
    VideoTheme.componentFactory.CallAppBar(
        params = CallAppBarParams(call = previewCall),
    )
}

@Preview
@Composable
private fun VideoComponentFactoryControlActionsRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryControlActionsPreview()
    }
}

@Composable
internal fun VideoComponentFactoryControlActionsPreview() {
    VideoTheme.componentFactory.ControlActions(
        params = ControlActionsParams(call = previewCall),
    )
}

@Preview
@Composable
private fun VideoComponentFactoryLobbyControlsRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryLobbyControlsPreview()
    }
}

@Composable
internal fun VideoComponentFactoryLobbyControlsPreview() {
    VideoTheme.componentFactory.CallLobbyControlsContent(
        params = CallLobbyControlsContentParams(
            call = previewCall,
            isCameraEnabled = true,
            isMicrophoneEnabled = false,
        ),
    )
}

@Preview
@Composable
private fun VideoComponentFactoryPictureInPictureRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryPictureInPicturePreview()
    }
}

@Composable
internal fun VideoComponentFactoryPictureInPicturePreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        VideoTheme.componentFactory.CallContentPictureInPictureContent(
            params = CallContentPictureInPictureContentParams(call = previewCall),
        )
    }
}

@Preview
@Composable
private fun VideoComponentFactoryIncomingCallRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryIncomingCallPreview()
    }
}

@Composable
internal fun VideoComponentFactoryIncomingCallPreview() {
    VideoTheme.componentFactory.IncomingCallContent(
        params = IncomingCallContentParams(call = previewCall),
    )
}

@Preview
@Composable
private fun VideoComponentFactoryOutgoingCallRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryOutgoingCallPreview()
    }
}

@Composable
internal fun VideoComponentFactoryOutgoingCallPreview() {
    VideoTheme.componentFactory.OutgoingCallContent(
        params = OutgoingCallContentParams(call = previewCall),
    )
}

@Preview
@Composable
private fun VideoComponentFactoryModerationWarningRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryModerationWarningPreview()
    }
}

@Composable
internal fun VideoComponentFactoryModerationWarningPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        VideoTheme.componentFactory.CallContentVideoModerationWarning(
            params = CallContentVideoModerationWarningParams(
                call = previewCall,
                message = "Please keep the conversation respectful.",
            ),
        )
    }
}

@Preview
@Composable
private fun VideoComponentFactoryVideoFallbackRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryVideoFallbackPreview()
    }
}

@Composable
internal fun VideoComponentFactoryVideoFallbackPreview() {
    Box(modifier = Modifier.size(220.dp)) {
        VideoTheme.componentFactory.ParticipantVideoFallbackContent(
            params = ParticipantVideoFallbackContentParams(
                call = previewCall,
                participant = previewParticipantsList[1],
            ),
        )
    }
}

@Preview
@Composable
private fun VideoComponentFactoryScreenSharingFallbackRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryScreenSharingFallbackPreview()
    }
}

@Composable
internal fun VideoComponentFactoryScreenSharingFallbackPreview() {
    Box(modifier = Modifier.size(220.dp)) {
        VideoTheme.componentFactory.ParticipantsLayoutScreenSharingFallbackContent(
            params = ParticipantsLayoutScreenSharingFallbackContentParams(
                session = ScreenSharingSession(participant = previewParticipantsList[1]),
            ),
        )
    }
}

@Preview
@Composable
private fun VideoComponentFactoryEmptyDefaultsRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoComponentFactoryEmptyDefaultsPreview()
    }
}

/**
 * The extension points below are intentionally empty by default, so only the marker text is
 * expected in the golden. The golden changes if any of them ever gains default content.
 */
@Composable
internal fun VideoComponentFactoryEmptyDefaultsPreview() {
    Column {
        Text(
            text = "Empty factory defaults render nothing below:",
            style = VideoTheme.typography.bodyS,
            color = VideoTheme.colors.basePrimary,
        )
        with(VideoTheme.componentFactory) {
            CallContentVideoOverlayContent(
                params = CallContentVideoOverlayContentParams(call = previewCall),
            )
            CallContentClosedCaptions(
                params = CallContentClosedCaptionsParams(call = previewCall),
            )
            CallContentVideoModerationBlur(
                params = CallContentVideoModerationBlurParams(call = previewCall),
            )
            IncomingCallHeaderContent(
                params = IncomingCallHeaderContentParams(call = previewCall),
            )
            OutgoingCallHeaderContent(
                params = OutgoingCallHeaderContentParams(call = previewCall),
            )
            AudioOnlyCallHeaderContent(
                params = AudioOnlyCallHeaderContentParams(call = previewCall),
            )
        }
    }
}
