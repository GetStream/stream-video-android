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
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.R

/**
 * Component that displays a user avatar and a background that reflects the avatar.
 *
 * @param modifier Modifier used for styling.
 * @param userImage The URL of the image to be displayed. Usually [User.image].
 * @param userName The name to be used for the initials fallback. Usually [User.name].
 * @param shape The shape of the avatar. `CircleShape` by default.
 * @param avatarSize The size of the avatar.
 * @param imageScale The scale rule used for the image. `Crop` by default.
 * @param imageDescription The image content description for accessibility. `Null` by default.
 * @param imageRequestSize The image size to be requested. Original size by default.
 * @param loadingPlaceholder Placeholder image to be displayed while loading the remote image.
 * @param previewModePlaceholder Placeholder image to be displayed in Compose previews (IDE).
 * @param textStyle The [TextStyle] to be used for the initials text fallback. The `fontSize`, `fontFamily` and `fontWeight` properties are used.
 * If the font size is too large, it will be gradually decreased automatically.
 * @param textOffset Offset to be applied to the initials text.
 *
 * @see [UserAvatar]
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
            userName = user.name.takeUnless { it.isNullOrBlank() } ?: user.id,
            previewModePlaceholder = R.drawable.stream_video_call_sample,
        )
    }
}
