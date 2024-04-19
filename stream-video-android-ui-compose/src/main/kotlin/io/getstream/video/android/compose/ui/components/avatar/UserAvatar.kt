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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewParticipantsList
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.R

/**
 * Represents the [User] avatar that's shown on the Messages screen or in headers of DMs.
 *
 * Based on the state within the [User], we either show an image or their initials.
 *
 * @param userName The user name whose avatar we want to show.
 * @param userImage The user image whose avatar we want to show.
 * @param modifier Modifier for styling.
 * @param shape The shape of the avatar.
 * @param textStyle The [TextStyle] that will be used for the initials.
 * @param imageScale The scale option used for the content.
 * @param imageDescription The content description of the avatar.
 * @param imageRequestSize The actual request size.
 * @param previewPlaceholder A placeholder that will be displayed on the Compose preview (IDE).
 * @param loadingPlaceholder A placeholder that will be displayed while loading an image.
 * @param isShowingOnlineIndicator Represents to show an online indicator.
 * @param onlineIndicatorAlignment An alignment for positioning the online indicator.
 * @param onlineIndicator A custom composable to represent an online indicator.
 * @param textOffset The initials offset to apply to the avatar.
 * @param onClick The handler when the user clicks on the avatar.
 */
@Composable
public fun UserAvatar(
    modifier: Modifier = Modifier,
    userImage: String?,
    userName: String?,
    shape: Shape = VideoTheme.shapes.circle,
    imageScale: ContentScale = ContentScale.Crop,
    imageDescription: String? = null,
    imageRequestSize: IntSize = IntSize(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE),
    @DrawableRes previewPlaceholder: Int = LocalAvatarPreviewProvider.getLocalAvatarPreviewPlaceholder(),
    @DrawableRes loadingPlaceholder: Int? = LocalAvatarPreviewProvider.getLocalAvatarLoadingPlaceholder(),
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
            modifier = Modifier.fillMaxSize(),
            imageUrl = userImage,
            fallbackText = userName,
            shape = shape,
            imageScale = imageScale,
            imageDescription = imageDescription,
            imageRequestSize = imageRequestSize,
            previewPlaceholder = previewPlaceholder,
            loadingPlaceholder = loadingPlaceholder,
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
 * The default online indicator for channel members.
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
            previewPlaceholder = R.drawable.stream_video_call_sample,
            isShowingOnlineIndicator = true,
        )
    }
}
