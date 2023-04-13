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

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Surface
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
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 *
 * @param callMediaState The state of the media devices for the current user.
 * @param isScreenSharing If there is a screen sharing session active.
 * @param modifier Modifier for styling.
 * @param actions Actions to show to the user with different controls.
 * @param onCallAction Handler when the user triggers an action.
 */
@Composable
internal fun RegularCallControls(
    callMediaState: CallMediaState,
    isScreenSharing: Boolean,
    modifier: Modifier = Modifier,
    actions: List<CallControlAction> = buildDefaultCallControlActions(callMediaState = callMediaState),
    onCallAction: (CallAction) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = VideoTheme.shapes.callControls,
        color = VideoTheme.colors.barsBackground,
        elevation = 8.dp
    ) {
        RegularCallControlsActions(
            actions = actions,
            isScreenSharing = isScreenSharing,
            onCallAction = onCallAction
        )
    }
}

/**
 * Represents the list of Call Control actions the user can trigger while in a call.
 *
 * @param actions The list of actions to render.
 * @param isScreenSharing If there is a screen sharing session active.
 * @param onCallAction Handler when a given action is triggered.
 */
@Composable
public fun RegularCallControlsActions(
    actions: List<CallControlAction>,
    isScreenSharing: Boolean,
    onCallAction: (CallAction) -> Unit
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(actions) { action ->
            val isEnabled = !(action.callAction is FlipCamera && isScreenSharing)

            Card(
                modifier = Modifier.size(VideoTheme.dimens.callControlButtonSize),
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
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegularCallControlsActionsPreview() {
    VideoTheme {
        RegularCallControls(
            callMediaState = CallMediaState(),
            isScreenSharing = true
        ) {
        }
    }
}
