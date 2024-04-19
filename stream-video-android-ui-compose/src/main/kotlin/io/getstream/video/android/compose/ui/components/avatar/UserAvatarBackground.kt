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

package io.getstream.video.android.compose.ui.components.avatar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewUsers
import io.getstream.video.android.ui.common.R

/**
 * A background that displays a user avatar and a background that reflects the avatar.
 *
 * @param modifier Modifier for styling.
 * @param shape The shape of the avatar.
 * @param avatarSize The size to decide avatar image.
 * @param contentScale The scale option used for the content.
 * @param contentDescription The content description of the avatar.
 * @param requestSize The actual request size.
 * @param initialsAvatarOffset The initials offset to apply to the avatar.
 * @param previewPlaceholder A placeholder that will be displayed on the Compose preview (IDE).
 * @param loadingPlaceholder A placeholder that will be displayed while loading an image.
 */
@Composable
public fun UserAvatarBackground(
    modifier: Modifier = Modifier,
    userImage: String?,
    userName: String?,
    shape: Shape = VideoTheme.shapes.circle,
    avatarSize: Dp = VideoTheme.dimens.genericMax,
    imageScale: ContentScale = ContentScale.Crop,
    imageDescription: String? = null,
    imageRequestSize: IntSize = IntSize(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE),
    @DrawableRes loadingPlaceholder: Int? = LocalAvatarPreviewProvider.getLocalAvatarLoadingPlaceholder(),
    @DrawableRes previewModePlaceholder: Int = LocalAvatarPreviewProvider.getLocalAvatarPreviewPlaceholder(),
    textStyle: TextStyle = VideoTheme.typography.titleM,
    textOffset: DpOffset = DpOffset(0.dp, 0.dp),
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .align(Alignment.Center),
        ) {
            UserAvatar(
                userImage = userImage,
                userName = userName,
                shape = shape,
                imageScale = imageScale,
                imageDescription = imageDescription,
                imageRequestSize = imageRequestSize,
                loadingPlaceholder = loadingPlaceholder,
                previewModePlaceholder = previewModePlaceholder,
                textStyle = textStyle,
                textOffset = textOffset,
            )
        }
    }
}

@Preview
@Composable
private fun UserAvatarBackgroundPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val user = previewUsers[0]
        UserAvatarBackground(
            modifier = Modifier.fillMaxSize(),
            userImage = user.image,
            userName = user.name.ifBlank { user.id },
            previewModePlaceholder = R.drawable.stream_video_call_sample,
        )
    }
}
