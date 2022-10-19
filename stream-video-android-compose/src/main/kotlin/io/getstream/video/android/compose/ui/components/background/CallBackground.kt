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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.getstream.video.android.compose.R
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.CallUser

@Composable
public fun CallBackground(
    participants: List<CallUser>,
    callType: CallType,
    isIncoming: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
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
        AsyncImage(
            modifier = modifier.fillMaxSize(),
            model = firstUser.imageUrl,
            contentScale = ContentScale.Crop,
            contentDescription = null
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
