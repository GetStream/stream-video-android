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

package io.getstream.video.android.compose.ui.components.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.transformation.blur.BlurTransformationPlugin
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.AvatarImagePreview
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.utils.toCallUser
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockMemberStateList
import io.getstream.video.android.ui.common.R

/**
 * Renders a call background that shows either a static image or user images based on the call state.
 *
 * @param participants The list of participants in the call.
 * @param isVideoType The type of call, Audio or Video.
 * @param isIncoming If the call is incoming from other users or if we're calling other people.
 * @param modifier Modifier for styling.
 * @param content The content to render on top of the background.
 */
@Composable
public fun CallBackground(
    modifier: Modifier = Modifier,
    participants: List<MemberState>,
    isVideoType: Boolean = true,
    isIncoming: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    val callUser by remember(participants) { derivedStateOf { participants.map { it.toCallUser() } } }

    Box(modifier = modifier) {
        if (isIncoming) {
            IncomingCallBackground(callUser)
        } else {
            OutgoingCallBackground(callUser, isVideoType)
        }

        content()
    }
}

@Composable
private fun IncomingCallBackground(callUsers: List<CallUser>) {
    if (callUsers.size == 1) {
        ParticipantImageBackground(
            userImage = callUsers.first().imageUrl
        )
    } else {
        DefaultCallBackground()
    }
}

@Composable
private fun OutgoingCallBackground(callUsers: List<CallUser>, isVideoType: Boolean) {
    if (!isVideoType) {
        if (callUsers.size == 1) {
            ParticipantImageBackground(callUsers.first().imageUrl)
        } else {
            DefaultCallBackground()
        }
    } else {
        if (callUsers.isNotEmpty()) {
            ParticipantImageBackground(userImage = callUsers.first().imageUrl)
        } else {
            DefaultCallBackground()
        }
    }
}

/**
 * A background that displays a different background depending on the [userImage].
 * If the [userImage] is valid, the background will display a blurred user image.
 * If the [userImage] is invalid, the background will display a gradient color.
 *
 * @param userImage A user image that will be blurred for the background.
 * @param modifier Modifier for styling.
 * @param blurRadius A blur radius value to be applied on the background.
 */
@Composable
public fun ParticipantImageBackground(
    userImage: String?,
    modifier: Modifier = Modifier,
    blurRadius: Int = 20,
) {
    if (!userImage.isNullOrEmpty()) {
        CoilImage(
            modifier =
            if (LocalInspectionMode.current) {
                modifier
                    .fillMaxSize()
                    .blur(15.dp)
            } else {
                modifier.fillMaxSize()
            },
            imageModel = { userImage },
            previewPlaceholder = R.drawable.stream_video_call_sample,
            imageOptions = ImageOptions(
                contentScale = ContentScale.Crop,
                contentDescription = null
            ),
            component = rememberImageComponent {
                +CrossfadePlugin()
                +BlurTransformationPlugin(blurRadius)
            }
        )
    } else {
        DefaultCallBackground()
    }
}

@Composable
private fun DefaultCallBackground() {
    val backgroundBrush = Brush.linearGradient(
        listOf(
            VideoTheme.colors.callGradientStart,
            VideoTheme.colors.callGradientEnd,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
    )
}

@Preview
@Composable
private fun CallBackgroundPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallBackground(
            participants = mockMemberStateList.take(1),
            isVideoType = true,
            isIncoming = true
        ) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                AvatarImagePreview()
            }
        }
    }
}
