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

package io.getstream.video.android.compose.ui.components.call.controls

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.call.state.FlipCamera
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
public fun LandscapeCallControls(
    callMediaState: CallMediaState,
    modifier: Modifier = Modifier,
    isScreenSharing: Boolean,
    onCallAction: (CallAction) -> Unit
) {
    val orientation = LocalConfiguration.current.orientation
    val defaultActions =
        buildDefaultCallControlActions(callMediaState = callMediaState)

    val actions = if (orientation == ORIENTATION_LANDSCAPE && isScreenSharing) {
        defaultActions.filter { it.callAction !is FlipCamera }
    } else {
        defaultActions
    }

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        items(actions) { action ->
            Card(
                modifier = Modifier.size(VideoTheme.dimens.landscapeCallControlButtonSize),
                shape = VideoTheme.shapes.callControlsButton,
                backgroundColor = action.actionBackgroundTint
            ) {
                Icon(
                    modifier = Modifier
                        .padding(10.dp)
                        .clickable { onCallAction(action.callAction) },
                    tint = action.iconTint,
                    painter = action.icon,
                    contentDescription = action.description
                )
            }
        }
    }
}
