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

package io.getstream.video.android.compose.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import org.webrtc.SurfaceViewRenderer

@Composable
public fun VideoItem(
    room: Room,
    videoTrack: VideoTrack,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                room.initVideoRenderer(this)
                videoTrack.addRenderer(this)
            }
        },
        update = { renderer ->
            renderer.release()
            room.initVideoRenderer(renderer)
            videoTrack.addRenderer(renderer)
        },
    )
}
