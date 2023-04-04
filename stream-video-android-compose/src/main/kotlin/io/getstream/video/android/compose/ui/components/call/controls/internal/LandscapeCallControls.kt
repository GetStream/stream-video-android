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

package io.getstream.video.android.compose.ui.components.call.controls.internal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.state.ui.call.CallControlAction
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.buildDefaultCallControlActions
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.call.state.FlipCamera

/**
 * Shows the call controls in a different way when in landscape mode.
 *
 * @param callMediaState The state of the call media, such as video, audio.
 * @param isScreenSharing If there's currently an active screen sharing session.
 * @param modifier Modifier for styling.
 * @param actions Actions to show to the user with different controls.
 * @param onCallAction Handler when the user triggers various call actions.
 */
@Composable
internal fun LandscapeCallControls(
    callMediaState: CallMediaState,
    isScreenSharing: Boolean,
    modifier: Modifier = Modifier,
    actions: List<CallControlAction> = buildDefaultCallControlActions(callMediaState = callMediaState),
    onCallAction: (CallAction) -> Unit
) {

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        items(actions) { action ->
            val isEnabled = !(action.callAction is FlipCamera && isScreenSharing)

            Card(
                modifier = Modifier.size(VideoTheme.dimens.landscapeCallControlButtonSize),
                shape = VideoTheme.shapes.callControlsButton,
                backgroundColor = if (isEnabled) action.actionBackgroundTint else VideoTheme.colors.disabled
            ) {
                Icon(
                    modifier = Modifier
                        .padding(10.dp)
                        .clickable(enabled = isEnabled) { onCallAction(action.callAction) },
                    tint = action.iconTint,
                    painter = action.icon,
                    contentDescription = action.description
                )
            }
        }
    }
}

@Preview
@Composable
private fun LandscapeCallControlsPreview() {
    VideoTheme {
        LandscapeCallControls(
            callMediaState = CallMediaState(),
            isScreenSharing = true,
        ) {}
    }
}
