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

package io.getstream.video.android.compose.ui.components.call.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.buildDefaultCallControlActions
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 * You can simply custom the controls button by giving a list of custom call actions to [actions].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier The modifier to be applied to the call controls.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param actions A list of composable call actions that will be arranged in the layout.
 */
@Composable
public fun ControlActions(
    call: Call,
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit = { DefaultOnCallActionHandler.onCallAction(call, it) },
    actions: List<(@Composable () -> Unit)> = buildDefaultCallControlActions(
        call = call,
        onCallAction,
    ),
) {
    Box(
        modifier = modifier,
    ) {
        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                VideoTheme.dimens.spacingM,
                Alignment.CenterHorizontally,
            ),
        ) {
            items(actions) { action ->
                action.invoke()
            }
        }
    }
}

@Preview
@Composable
private fun CallControlsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    Column {
        VideoTheme {
            ControlActions(call = previewCall, onCallAction = {})
        }
    }
}
