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

@file:OptIn(StreamVideoUiDelicateApi::class)

package io.getstream.video.android.compose.ui.components.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.ui.common.util.StreamVideoUiDelicateApi

@Preview
@Composable
private fun VideoRendererPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoRenderer(
            call = previewCall,
            video = ParticipantState.Video(
                track = VideoTrack("", org.webrtc.VideoTrack(123)),
                enabled = true,
                sessionId = "",
                paused = false,
            ),
        )
    }
}

@Preview
@Composable
private fun VideoRendererPausedPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        VideoRenderer(
            call = previewCall,
            video = ParticipantState.Video(
                track = VideoTrack("", org.webrtc.VideoTrack(123)),
                enabled = true,
                sessionId = "",
                paused = true,
            ),
        )
    }
}

@Preview
@Composable
private fun VideoRendererPausedPreview2() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        DefaultBadNetworkFallbackContent(
            call = previewCall,
            modifier = Modifier,
        )
    }
}

@Preview
@Composable
private fun VideoRendererFallbackPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        DefaultMediaTrackFallbackContent(modifier = Modifier, call = previewCall)
    }
}
