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

package io.getstream.video.android.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults
import io.getstream.video.android.compose.ui.components.base.StreamTextButton
import io.getstream.video.android.compose.ui.components.call.renderer.LayoutType
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.tooling.extensions.toPx

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
    Popup(
        offset = IntOffset(
            0,
            (44.dp + 8.dp).toPx().toInt(),
        ),
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier.background(
                color = VideoTheme.colors.backgroundCoreApp,
                shape = RoundedCornerShape(16.dp),
            )
                .width(300.dp),
        ) {
            layouts.forEach { layout ->

                val selected = layout.which == current
                val icon = when (layout.which) {
                    LayoutType.DYNAMIC -> rememberVectorPainter(Icons.Default.AutoAwesome)
                    LayoutType.SPOTLIGHT -> painterResource(R.drawable.ic_layout_spotlight)
                    LayoutType.GRID -> painterResource(R.drawable.ic_layout_grid)
                }
                StreamTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onLayoutChoice(layout.which) },
                    text = layout.text,
                    leadingIcon = icon,
                    style = if (selected) StreamButtonStyleDefaults.primarySolid else StreamButtonStyleDefaults.secondaryGhost,
                )
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
