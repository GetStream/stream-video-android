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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.ParticipantAvatars
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewParticipantsList
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.R

/**
 * Component that renders an image or a name as an avatar. If the image is `null` or unavailable, it uses the name initials.
 * Can also show an "is online" indicator.
 * If needed, the initials font size is gradually decreased automatically until the text fits within the avatar boundaries.
 *
 * @param modifier Modifier used for styling.
 * @param userImage The URL of the image to be displayed. Usually [User.image].
 * @param userName The name to be used for the initials. Usually [User.name].
 * @param shape The shape of the avatar. `CircleShape` by default.
 * @param imageScale The scale rule used for the image. `Crop` by default.
 * @param imageDescription The image content description for accessibility. `Null` by default.
 * @param imageRequestSize The image size to be requested. Original size by default.
 * @param loadingPlaceholder Placeholder image to be displayed while loading the remote image.
 * @param previewModePlaceholder Placeholder image to be displayed in Compose previews (IDE).
 * @param textStyle The [TextStyle] to be used for the initials text. The `fontSize`, `fontFamily` and `fontWeight` properties are used.
 * If the font size is too large, it will be gradually decreased automatically.
 * @param textOffset Offset to be applied to the initials text.
 * @param isShowingOnlineIndicator Flag used to display/hide the "is online" indicator. `False` by default.
 * @param onlineIndicatorAlignment Alignment of the "is online" indicator. `TopEnd` by default.
 * @param onlineIndicator A custom composable to represent the "is online" indicator. [DefaultOnlineIndicator] by default.
 * @param onClick Handler to be called when the user clicks on the avatar.
 *
 * @see [ParticipantAvatars]
 * @see [UserAvatarBackground]
 */
@Composable
public fun UserAvatar(
    modifier: Modifier = Modifier,
    userImage: String? = null,
    userName: String? = null,
    shape: Shape = VideoTheme.shapes.circle,
    imageScale: ContentScale = ContentScale.Crop,
    imageDescription: String? = null,
    imageRequestSize: IntSize = IntSize(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE),
    @DrawableRes loadingPlaceholder: Int? = LocalAvatarPreviewProvider.getLocalAvatarLoadingPlaceholder(),
    @DrawableRes previewModePlaceholder: Int = LocalAvatarPreviewProvider.getLocalAvatarPreviewPlaceholder(),
    textStyle: TextStyle = VideoTheme.typography.titleM,
    textOffset: DpOffset = DpOffset(0.dp, 0.dp),
    isShowingOnlineIndicator: Boolean = false,
    onlineIndicatorAlignment: OnlineIndicatorAlignment = OnlineIndicatorAlignment.TopEnd,
    onlineIndicator: @Composable BoxScope.() -> Unit = {
        DefaultOnlineIndicator(onlineIndicatorAlignment)
    },
    onClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier) {
        Avatar(
            modifier = Modifier
                .fillMaxSize()
                .testTag("Stream_UserAvatar"),
            imageUrl = userImage,
            fallbackText = userName,
            shape = shape,
            imageScale = imageScale,
            imageDescription = imageDescription,
            imageRequestSize = imageRequestSize,
            loadingPlaceholder = loadingPlaceholder,
            previewModePlaceholder = previewModePlaceholder,
            textStyle = textStyle,
            textOffset = textOffset,
            onClick = onClick,
        )

        if (isShowingOnlineIndicator) {
            onlineIndicator()
        }
    }
}

/**
 * The default "is online" indicator for [UserAvatar].
 */
@Composable
internal fun BoxScope.DefaultOnlineIndicator(onlineIndicatorAlignment: OnlineIndicatorAlignment) {
    OnlineIndicator(modifier = Modifier.align(onlineIndicatorAlignment.alignment))
}

@Preview
@Composable
private fun UserAvatarPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        val participant = previewParticipantsList[0]
        val userImage by participant.image.collectAsStateWithLifecycle()
        val userName by participant.userNameOrId.collectAsStateWithLifecycle()

        UserAvatar(
            modifier = Modifier.size(82.dp),
            userImage = userImage,
            userName = userName,
            previewModePlaceholder = R.drawable.stream_video_call_sample,
            isShowingOnlineIndicator = true,
        )
    }
}
