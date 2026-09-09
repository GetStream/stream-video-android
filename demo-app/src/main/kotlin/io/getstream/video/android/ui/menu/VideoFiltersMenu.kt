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

package io.getstream.video.android.ui.menu

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults
import io.getstream.video.android.compose.ui.components.base.StreamIconButton
import io.getstream.video.android.mock.StreamPreviewDataUtils

@Composable
internal fun VideoFiltersMenu(selectedFilterIndex: Int = 0, onSelectFilter: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        availableVideoFilters.forEachIndexed { index, filter ->
            val toggleState = if (index == selectedFilterIndex) ToggleableState.On else ToggleableState.Off

            when (filter) {
                is VideoFilter.None -> BlurredBackgroundToggleItem(
                    icon = Icons.Default.AccountCircle,
                    toggleState = toggleState,
                    onClick = { onSelectFilter(index) },
                )
                is VideoFilter.BlurredBackground -> BlurredBackgroundToggleItem(
                    icon = Icons.Default.BlurOn,
                    toggleState = toggleState,
                    onClick = { onSelectFilter(index) },
                )
                is VideoFilter.VirtualBackground -> VirtualBackgroundToggleItem(
                    drawable = filter.drawable,
                    toggleState = toggleState,
                    onClick = { onSelectFilter(index) },
                )
            }
        }
    }
}

val availableVideoFilters = listOf(
    VideoFilter.None,
    VideoFilter.BlurredBackground,
    VideoFilter.VirtualBackground(R.drawable.amsterdam1),
    VideoFilter.VirtualBackground(R.drawable.amsterdam2),
    VideoFilter.VirtualBackground(R.drawable.boulder1),
    VideoFilter.VirtualBackground(R.drawable.boulder2),
    VideoFilter.VirtualBackground(R.drawable.gradient1),
)

sealed class VideoFilter {
    data object None : VideoFilter()
    data object BlurredBackground : VideoFilter()
    data class VirtualBackground(@DrawableRes val drawable: Int) : VideoFilter()
}

@Composable
private fun BlurredBackgroundToggleItem(
    icon: ImageVector,
    toggleState: ToggleableState,
    onClick: () -> Unit = {},
) {
    StreamIconButton(
        onClick = onClick,
        icon = rememberVectorPainter(icon),
        contentDescription = null,
        modifier = Modifier.testTag("Stream_Background_${icon.name}_${toggleState.name}"),
        style = if (toggleState == ToggleableState.On) {
            StreamButtonStyleDefaults.primarySolid
        } else {
            StreamButtonStyleDefaults.secondarySolid
        },
    )
}

@Composable
private fun VirtualBackgroundToggleItem(
    @DrawableRes drawable: Int,
    toggleState: ToggleableState,
    onClick: () -> Unit = {},
) {
    StreamButton(
        onClick = onClick,
        modifier = Modifier.testTag("Stream_Background_Image_${toggleState.name}"),
        style = if (toggleState == ToggleableState.On) {
            StreamButtonStyleDefaults.primaryOutline
        } else {
            StreamButtonStyleDefaults.secondaryGhost
        },
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoFiltersMenuPreview() {
    VideoTheme {
        StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
        VideoFiltersMenu(selectedFilterIndex = 0, onSelectFilter = {})
    }
}
