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

package io.getstream.video.android.compose.ui.components.call.lobby

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

@Preview
@Composable
private fun CallLobbyRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobbyPreview()
    }
}

@Preview
@Composable
private fun CallLobbyMicAndCameraOffRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobbyMicAndCameraOffPreview()
    }
}

@Preview
@Composable
private fun CallLobbyMicAndCameraIssueRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobbyMicAndCameraIssuePreview()
    }
}

/** The lobby with the camera and the microphone on, and the default join action. */
@Composable
internal fun CallLobbyPreview() {
    CallLobby(
        modifier = Modifier
            .fillMaxWidth()
            .padding(StreamTokens.spacingMd),
        call = previewCall,
        onJoinCall = {},
    )
}

/** The lobby after the user turned both devices off: avatar preview and destructive toggles. */
@Composable
internal fun CallLobbyMicAndCameraOffPreview() {
    CallLobby(
        modifier = Modifier
            .fillMaxWidth()
            .padding(StreamTokens.spacingMd),
        call = previewCall,
        isCameraEnabled = false,
        isMicrophoneEnabled = false,
        onJoinCall = {},
    )
}

/** The lobby when both permissions were denied: the toggles carry an error badge. */
@Composable
internal fun CallLobbyMicAndCameraIssuePreview() {
    CallLobby(
        modifier = Modifier
            .fillMaxWidth()
            .padding(StreamTokens.spacingMd),
        call = previewCall,
        isCameraEnabled = false,
        isMicrophoneEnabled = false,
        permissions = DeniedPermissionsState,
        onJoinCall = {},
    )
}

private object DeniedPermissionsState : VideoPermissionsState {
    override val allPermissionsGranted: Boolean = false
    override val shouldShowRationale: Boolean = false
    override val isCameraPermissionDenied: Boolean = true
    override val isMicrophonePermissionDenied: Boolean = true
    override fun launchPermissionRequest() = Unit
}

@Preview
@Composable
private fun CallLobbyDeprecatedOverloadRootPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallLobbyDeprecatedOverloadPreview()
    }
}

/**
 * Pins the deprecated [CallLobby] overload (the one taking `labelPosition`), which must keep
 * rendering exactly like the current overload without a join action until it is removed.
 */
@Suppress("DEPRECATION")
@Composable
internal fun CallLobbyDeprecatedOverloadPreview() {
    CallLobby(
        modifier = Modifier
            .fillMaxWidth()
            .padding(StreamTokens.spacingMd),
        call = previewCall,
        labelPosition = Alignment.BottomStart,
    )
}
