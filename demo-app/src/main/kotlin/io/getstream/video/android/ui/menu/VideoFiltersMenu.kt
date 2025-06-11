/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamDrawableToggleButton
import io.getstream.video.android.compose.ui.components.base.StreamIconToggleButton
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.mock.StreamPreviewDataUtils

@Composable
internal fun VideoFiltersMenu(selectedFilterIndex: Int = 0, onSelectFilter: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VideoTheme.dimens.spacingM),
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
    StreamIconToggleButton(
        modifier = Modifier.testTag("Stream_Background_${icon.name}_${toggleState.name}"),
        toggleState = rememberUpdatedState(newValue = toggleState),
        onIcon = icon,
        onStyle = VideoTheme.styles.buttonStyles.primaryIconButtonStyle(),
        offStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle(),
        onClick = { onClick() },
    )
}

@Composable
private fun VirtualBackgroundToggleItem(
    @DrawableRes drawable: Int,
    toggleState: ToggleableState,
    onClick: () -> Unit = {},
) {
    StreamDrawableToggleButton(
        modifier = Modifier.testTag("Stream_Background_Image_${toggleState.name}"),
        toggleState = rememberUpdatedState(newValue = toggleState),
        onDrawable = drawable,
        onStyle = ButtonStyles.drawableToggleButtonStyleOn(),
        offStyle = ButtonStyles.drawableToggleButtonStyleOff(),
        onClick = { onClick() },
    )
}

@Preview(showBackground = true)
@Composable
private fun VideoFiltersMenuPreview() {
    VideoTheme {
        StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
        VideoFiltersMenu(selectedFilterIndex = 0, onSelectFilter = {})
    }
}
