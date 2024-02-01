/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

@file:OptIn(ExperimentalLayoutApi::class)

package io.getstream.video.android.ui.call

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamToggleButton
import io.getstream.video.android.compose.ui.components.call.renderer.LayoutType
import io.getstream.video.android.mock.StreamPreviewDataUtils

private data class LayoutChooserDataItem(
    val which: LayoutType,
    val text: String = "",
)

private val layouts = arrayOf(
    LayoutChooserDataItem(LayoutType.DYNAMIC, "Dynamic"),
    LayoutChooserDataItem(LayoutType.SPOTLIGHT, "Spotlight"),
    LayoutChooserDataItem(LayoutType.GRID, "Grid"),
)

/**
 * Reactions menu. The reaction menu is a dialog displaying the list of reactions found in
 * [DefaultReactionsMenuData].
 * @param current
 * @param onDismiss on dismiss listener.
 */
@Composable
internal fun LayoutChooser(
    current: LayoutType,
    onLayoutChoice: (LayoutType) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(onDismissRequest = onDismiss) {
        Column(Modifier.background(VideoTheme.colors.baseSheetPrimary)) {
            layouts.forEach { layout ->

                val state = ToggleableState(layout.which == current)
                val icon = when (layout.which) {
                    LayoutType.DYNAMIC -> Icons.Default.AutoAwesome
                    LayoutType.SPOTLIGHT -> ImageVector.vectorResource(R.drawable.ic_layout_spotlight)
                    LayoutType.GRID -> ImageVector.vectorResource(R.drawable.ic_layout_grid)
                }
                StreamToggleButton(
                    onText = layout.text,
                    offText = layout.text,
                    toggleState = rememberUpdatedState(newValue = state),
                    onIcon = icon,
                    onStyle = VideoTheme.styles.buttonStyles.toggleButtonStyleOn(),
                    offStyle = VideoTheme.styles.buttonStyles.toggleButtonStyleOff(),
                ) {
                    onLayoutChoice(layout.which)
                }
            }
        }
    }
}

@Preview
@Composable
private fun LayoutChooserPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutChooser(
            current = LayoutType.GRID,
            onLayoutChoice = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun LayoutChooserPreview2() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutChooser(
            current = LayoutType.SPOTLIGHT,
            onLayoutChoice = {},
            onDismiss = {},
        )
    }
}
@Preview
@Composable
private fun LayoutChooserPreview3() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutChooser(
            current = LayoutType.DYNAMIC,
            onLayoutChoice = {},
            onDismiss = {},
        )
    }
}
