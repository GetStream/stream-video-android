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

package io.getstream.video.android.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import io.getstream.video.android.compose.R

/**
 * Contains all the icons in our UI components. Each icon is used for various things an can be changed to
 * customize the app design style.
 */
@Immutable
public data class StreamIcons(
    public val call: Painter,
    public val callEnd: Painter,
    public val arrowBack: Painter,
    public val cameraRotate: Painter,
    public val message: Painter,
    public val micOff: Painter,
    public val micOn: Painter,
    public val participants: Painter,
    public val avatarPreview: Painter,
    public val videoCam: Painter,
    public val videoCamOff: Painter,
) {
    public companion object {
        /**
         * Builds the default icons for our theme.
         *
         * @return A [StreamIcons] instance holding our default dimensions.
         */
        @Composable
        public fun defaultIcons(): StreamIcons = StreamIcons(
            call = painterResource(id = R.drawable.ic_call),
            callEnd = painterResource(id = R.drawable.ic_call_end),
            arrowBack = painterResource(id = R.drawable.ic_arrow_back),
            cameraRotate = painterResource(id = R.drawable.ic_camera_rotate),
            message = painterResource(id = R.drawable.ic_message),
            micOff = painterResource(id = R.drawable.ic_mic_off),
            micOn = painterResource(id = R.drawable.ic_mic_on),
            participants = painterResource(id = R.drawable.ic_participants),
            avatarPreview = painterResource(id = R.drawable.ic_preview_avatar),
            videoCam = painterResource(id = R.drawable.ic_videocam),
            videoCamOff = painterResource(id = R.drawable.ic_videocam_off),
        )
    }
}
