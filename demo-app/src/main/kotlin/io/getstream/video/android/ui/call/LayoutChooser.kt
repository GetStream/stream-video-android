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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.getstream.video.android.compose.theme.VideoTheme
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
    Dialog(onDismiss) {
        Row(Modifier.background(VideoTheme.colors.appBackground)) {
            layouts.forEach {
                LayoutItem(
                    current = current,
                    item = it,
                    onClicked = onLayoutChoice,
                )
            }
        }
    }
}

@Composable
private fun LayoutItem(
    modifier: Modifier = Modifier,
    current: LayoutType,
    item: LayoutChooserDataItem,
    onClicked: (LayoutType) -> Unit = {},
) {
    val border =
        if (current == item.which) BorderStroke(2.dp, VideoTheme.colors.primaryAccent) else null
    Card(
        modifier = modifier
            .clickable { onClicked(item.which) }
            .padding(12.dp),
        backgroundColor = VideoTheme.colors.appBackground,
        elevation = 3.dp,
        border = border,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(84.dp, 84.dp)
                    .padding(2.dp),
            ) {
                when (item.which) {
                    LayoutType.DYNAMIC -> {
                        DynamicRepresentation()
                    }

                    LayoutType.SPOTLIGHT -> {
                        SpotlightRepresentation()
                    }

                    LayoutType.GRID -> {
                        GridRepresentation()
                    }
                }
            }
            Text(
                modifier = Modifier
                    .align(CenterHorizontally)
                    .padding(12.dp),
                textAlign = TextAlign.Center,
                text = item.text,
                color = VideoTheme.colors.textHighEmphasis,
            )
        }
    }
}

@Composable
private fun DynamicRepresentation() {
    Column {
        Card(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .padding(2.dp),
            backgroundColor = VideoTheme.colors.participantContainerBackground,
        ) {
        }

        Row(modifier = Modifier.weight(1f)) {
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(2.dp),
                backgroundColor = VideoTheme.colors.participantContainerBackground,
            ) {
            }
            Card(
                backgroundColor = VideoTheme.colors.appBackground,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(2.dp),
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    tint = VideoTheme.colors.participantContainerBackground,
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "dynamic",
                )
            }
        }
    }
}

@Composable
private fun GridRepresentation() {
    Column {
        repeat(3) {
            Row {
                repeat(3) {
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .padding(2.dp),
                        backgroundColor = VideoTheme.colors.participantContainerBackground,
                    ) {
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightRepresentation() {
    Column {
        Card(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .padding(2.dp),
            backgroundColor = VideoTheme.colors.participantContainerBackground,
        ) {
        }

        Row(modifier = Modifier.weight(1f)) {
            repeat(3) {
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(2.dp),
                    backgroundColor = VideoTheme.colors.participantContainerBackground,
                ) {
                }
            }
        }
    }
}

@Preview(showBackground = true)
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

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LayoutChooserPreviewDark() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutChooser(
            current = LayoutType.GRID,
            onLayoutChoice = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GridItemPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutItem(
            current = LayoutType.GRID,
            item = layouts[2],
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GridItemPreviewDark() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutItem(
            current = LayoutType.GRID,
            item = layouts[2],
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotlightItemPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutItem(
            current = LayoutType.GRID,
            item = layouts[1],
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SpotlightItemPreviewDark() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutItem(
            current = LayoutType.GRID,
            item = layouts[1],
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DynamicItemPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutItem(
            current = LayoutType.GRID,
            item = layouts[0],
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DynamicItemPreviewDark() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LayoutItem(
            current = LayoutType.GRID,
            item = layouts[0],
        )
    }
}
