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

package io.getstream.video.android.compose.ui.components.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ShowCallInfo
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.utils.formatAsTitle
import io.getstream.video.android.ui.common.R

@Composable
internal fun OverlayAppBar(
    callState: StreamCallState,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VideoTheme.dimens.landscapeTopAppBarHeight)
            .background(color = VideoTheme.colors.overlay),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            enabled = true, // TODO - we would need the info of displayed overlays
            onClick = onBackPressed,
            modifier = Modifier.padding(
                start = VideoTheme.dimens.callAppBarLeadingContentSpacingStart,
                end = VideoTheme.dimens.callAppBarLeadingContentSpacingEnd
            )
        ) {
            Icon(
                modifier = Modifier,
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(id = R.string.back_button_content_description),
                tint = Color.White
            )
        }

        val callId = when (callState) {
            is StreamCallState.Active -> callState.callGuid.id
            else -> ""
        }
        val status = callState.formatAsTitle(LocalContext.current)

        val title = when (callId.isBlank()) {
            true -> status
            else -> "$status: $callId"
        }

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            color = Color.White,
            style = VideoTheme.typography.title3Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(
            onClick = { onCallAction(ShowCallInfo) },
            modifier = Modifier.padding(
                start = VideoTheme.dimens.callAppBarLeadingContentSpacingStart,
                end = VideoTheme.dimens.callAppBarLeadingContentSpacingEnd
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_participants),
                contentDescription = stringResource(id = R.string.call_participants_menu_content_description),
                tint = Color.White
            )
        }
    }
}
