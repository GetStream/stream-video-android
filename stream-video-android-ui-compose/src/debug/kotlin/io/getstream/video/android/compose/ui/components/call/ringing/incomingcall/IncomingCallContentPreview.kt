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

package io.getstream.video.android.compose.ui.components.call.ringing.incomingcall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.LocalAvatarPreviewPlaceholder
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewMemberListState
import io.getstream.video.android.ui.common.R

@Preview
@Composable
private fun IncomingCallPreview1() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CompositionLocalProvider(
            LocalAvatarPreviewPlaceholder provides
                R.drawable.stream_video_call_sample,
        ) {
            IncomingCallContent(
                call = previewCall,
                participants = previewMemberListState.takeLast(1),
                isVideoType = true,
                isCameraEnabled = false,
                onBackPressed = {},
            ) {}
        }
    }
}

@Preview
@Composable
private fun IncomingCallPreview2() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CompositionLocalProvider(
            LocalAvatarPreviewPlaceholder provides
                R.drawable.stream_video_call_sample,
        ) {
            IncomingCallContent(
                call = previewCall,
                participants = previewMemberListState,
                isVideoType = true,
                isCameraEnabled = false,
                onBackPressed = {},
            ) {}
        }
    }
}
