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
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewParticipantsList
import io.getstream.video.android.model.User

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
 * @param contentScale The scale option used for the content.
 * @param contentDescription The content description of the avatar.
 * @param requestSize The actual request size.
 * @param previewPlaceholder A placeholder that will be displayed on the Compose preview (IDE).
 * @param loadingPlaceholder A placeholder that will be displayed while loading an image.
 * @param isShowingOnlineIndicator Represents to show an online indicator.
 * @param onlineIndicatorAlignment An alignment for positioning the online indicator.
 * @param onlineIndicator A custom composable to represent an online indicator.
 * @param initialsAvatarOffset The initials offset to apply to the avatar.
 * @param onClick The handler when the user clicks on the avatar.
 */
@Composable
public fun UserAvatar(
    userName: String?,
    userImage: String?,
    modifier: Modifier = Modifier,
    shape: Shape = VideoTheme.shapes.circle,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    textSize: StyleSize = StyleSize.XL,
    requestSize: IntSize = IntSize(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE),
    @DrawableRes previewPlaceholder: Int = LocalAvatarPreviewProvider.getLocalAvatarPreviewPlaceholder(),
    @DrawableRes loadingPlaceholder: Int? = LocalAvatarPreviewProvider.getLocalAvatarLoadingPlaceholder(),
    initialsAvatarOffset: DpOffset = DpOffset(0.dp, 0.dp),
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
            textSize = textSize,
            imageUrl = userImage,
            initials = userName,
            shape = shape,
            contentScale = contentScale,
            contentDescription = contentDescription,
            requestSize = requestSize,
            loadingPlaceholder = loadingPlaceholder,
            previewPlaceholder = previewPlaceholder,
            onClick = onClick,
            initialsAvatarOffset = initialsAvatarOffset,
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
            userImage = userImage,
            userName = userName,
            modifier = Modifier.size(82.dp),
            isShowingOnlineIndicator = true,
            previewPlaceholder = io.getstream.video.android.ui.common.R.drawable.stream_video_call_sample,
        )
    }
}
