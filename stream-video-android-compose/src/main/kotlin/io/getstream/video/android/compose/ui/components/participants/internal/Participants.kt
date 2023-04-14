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

package io.getstream.video.android.compose.ui.components.participants.internal

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.view.View
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.getstream.video.android.core.Call

/**
 * Renders call participants based on the number of people in a call.
 *
 * @param call The state of the call.
 * @param onRender Handler when the video content renders.
 * @param modifier Modifier for styling.
 * @param paddingValues The padding within the parent.
 * @param parentSize The size of the parent.
 */
@Composable
internal fun BoxScope.Participants(
    call: Call,
    onRender: (View) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    parentSize: IntSize = IntSize(0, 0)
) {
    val primarySpeaker by call.state.dominantSpeaker.collectAsState(initial = null)
    val roomParticipants by call.state.participants.collectAsState(emptyList())

    val orientation = LocalConfiguration.current.orientation

    if (orientation == ORIENTATION_LANDSCAPE) {
        LandscapeVideoRenderer(
            call = call,
            primarySpeaker = primarySpeaker,
            callParticipants = roomParticipants,
            modifier = modifier,
            paddingValues = paddingValues,
            parentSize = parentSize,
            onRender = onRender
        )
    } else {
        PortraitVideoRenderer(
            call = call,
            primarySpeaker = primarySpeaker,
            callParticipants = roomParticipants,
            modifier = modifier,
            paddingValues = paddingValues,
            parentSize = parentSize,
            onRender = onRender
        )
    }
}
