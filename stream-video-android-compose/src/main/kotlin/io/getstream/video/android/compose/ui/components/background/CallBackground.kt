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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import io.getstream.video.android.common.util.mockUsers
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.AvatarImagePreview
import io.getstream.video.android.core.model.CallType
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.CallUserState
import io.getstream.video.android.ui.common.R

/**
 * Renders a call background that shows either a static image or user images based on the call state.
 *
 * @param participants The list of participants in the call.
 * @param callType The type of call, Audio or Video.
 * @param isIncoming If the call is incoming from other users or if we're calling other people.
 * @param modifier Modifier for styling.
 * @param content The content to render on top of the background.
 */
@Composable
public fun CallBackground(
    participants: List<CallUser>,
    callType: CallType,
    isIncoming: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        if (isIncoming) {
            IncomingCallBackground(participants)
        } else {
            OutgoingCallBackground(participants, callType)
        }

        content()
    }
}

@Composable
private fun IncomingCallBackground(participants: List<CallUser>) {
    if (participants.size == 1) {
        ParticipantImageBackground(participants = participants, modifier = Modifier.blur(20.dp))
    } else {
        DefaultCallBackground()
    }
}

@Composable
private fun OutgoingCallBackground(participants: List<CallUser>, callType: CallType) {
    if (callType == CallType.AUDIO) {
        if (participants.size == 1) {
            ParticipantImageBackground(participants, modifier = Modifier.blur(20.dp))
        } else {
            DefaultCallBackground()
        }
    } else {
        if (participants.isNotEmpty()) {
            ParticipantImageBackground(participants = participants)
        } else {
            DefaultCallBackground()
        }
    }
}

@Composable
private fun ParticipantImageBackground(
    participants: List<CallUser>,
    modifier: Modifier = Modifier
) {
    val firstUser = participants.first()

    if (firstUser.imageUrl.isNotEmpty()) {
        CoilImage(
            modifier = modifier.fillMaxSize(),
            imageModel = { firstUser.imageUrl },
            previewPlaceholder = R.drawable.stream_video_call_sample,
            imageOptions = ImageOptions(
                contentScale = ContentScale.Crop, contentDescription = null
            ),
            component = rememberImageComponent {
                +CrossfadePlugin()
            }
        )
    } else {
        DefaultCallBackground()
    }
}

@Composable
private fun DefaultCallBackground() {
    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(id = R.drawable.bg_call),
        contentScale = ContentScale.FillBounds,
        contentDescription = null
    )
}

@Preview
@Composable
private fun CallBackgroundPreview() {
    VideoTheme {
        CallBackground(
            participants = listOf(

            ),
            callType = CallType.VIDEO, isIncoming = true
        ) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                AvatarImagePreview()
            }
        }
    }
}
