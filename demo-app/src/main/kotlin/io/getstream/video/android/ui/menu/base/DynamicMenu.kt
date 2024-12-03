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

package io.getstream.video.android.ui.menu.base

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamToggleButton
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.ui.menu.TranscriptionAvailableUiState
import io.getstream.video.android.ui.menu.debugSubmenu
import io.getstream.video.android.ui.menu.defaultStreamMenu
import io.getstream.video.android.ui.menu.reconnectMenu

/**
 * A composable capable of loading a menu based on a list structure of menu items and sub menus.
 * There are three types of items:
 *  - [ActionMenuItem] - shown normally as an item that can be clicked.
 *  - [SubMenuItem] - that contains another list of [ActionMenuItem] o [SubMenuItem] which will be shown when clicked.
 *  - [DynamicSubMenuItem] - that shows a spinner and calls a loading function before behaving as [SubMenuItem]
 *
 *  The transition and history between the items is automatic.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicMenu(header: (@Composable LazyItemScope.() -> Unit)? = null, items: List<MenuItem>) {
    val history = remember { mutableStateListOf<Pair<String, SubMenuItem>>() }
    val dynamicItems = remember { mutableStateListOf<MenuItem>() }
    var loadedItems by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = VideoTheme.colors.baseSheetPrimary,
                shape = VideoTheme.shapes.dialog,
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    shape = VideoTheme.shapes.sheet,
                    color = VideoTheme.colors.baseSheetPrimary,
                )
                .padding(12.dp),
        ) {
            if (history.isEmpty()) {
                header?.let {
                    item(content = header)
                }
                menuItems(items) {
                    history.add(Pair(it.title, it))
                }
            } else {
                val lastContent = history.last()
                stickyHeader {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(VideoTheme.colors.baseSheetPrimary)
                            .fillMaxWidth(),
                    ) {
                        IconButton(onClick = { history.removeLastOrNull() }) {
                            Icon(
                                tint = VideoTheme.colors.basePrimary,
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                        Text(
                            text = lastContent.first,
                            style = VideoTheme.typography.subtitleS,
                            color = VideoTheme.colors.basePrimary,
                        )
                    }
                }

                val subMenu = lastContent.second
                val dynamicMenu = subMenu as? DynamicSubMenuItem

                if (dynamicMenu != null) {
                    if (!loadedItems) {
                        dynamicItems.clear()
                        loadingItems(dynamicMenu) {
                            loadedItems = true
                            dynamicItems.addAll(it)
                        }
                    }
                    if (dynamicItems.isNotEmpty()) {
                        menuItems(dynamicItems) {
                            history.add(Pair(it.title, it))
                        }
                    } else if (loadedItems) {
                        noItems()
                    }
                } else {
                    if (subMenu.items.isEmpty()) {
                        noItems()
                    } else {
                        menuItems(subMenu.items) {
                            history.add(Pair(it.title, it))
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.loadingItems(
    dynamicMenu: DynamicSubMenuItem,
    onLoaded: (List<MenuItem>) -> Unit,
) {
    item {
        LaunchedEffect(key1 = dynamicMenu) {
            onLoaded(dynamicMenu.itemsLoader.invoke())
        }
        LinearProgressIndicator(
            modifier = Modifier
                .padding(33.dp)
                .fillMaxWidth(),
            color = VideoTheme.colors.basePrimary,
        )
    }
}

private fun LazyListScope.noItems() {
    item {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            textAlign = TextAlign.Center,
            text = "No items",
            style = VideoTheme.typography.subtitleS,
            color = VideoTheme.colors.basePrimary,
        )
    }
}

private fun LazyListScope.menuItems(
    items: List<MenuItem>,
    onNewSubmenu: (SubMenuItem) -> Unit,
) {
    items(items.size) { index ->
        val item = items[index]
        val highlight = item.highlight
        StreamToggleButton(
            onText = item.title,
            offText = item.title,
            onIcon = item.icon,
            onStyle = VideoTheme.styles.buttonStyles.toggleButtonStyleOn(StyleSize.XS).copy(
                iconStyle = VideoTheme.styles.iconStyles.customColorIconStyle(
                    color = if (highlight) VideoTheme.colors.brandPrimary else VideoTheme.colors.basePrimary,
                ),
            ),
            onClick = {
                val actionItem = item as? ActionMenuItem
                actionItem?.action?.invoke()
                val menuItem = item as? SubMenuItem
                menuItem?.let {
                    onNewSubmenu(it)
                }
            },
        )
    }
}

@Preview
@Composable
private fun DynamicMenuPreview() {
    VideoTheme {
        DynamicMenu(
            items = defaultStreamMenu(
                codecList = emptyList(),
                onCodecSelected = {},
                isScreenShareEnabled = false,
                onToggleScreenShare = { },
                onShowCallStats = { },
                onToggleAudioFilterClick = { },
                onRestartSubscriberIceClick = { },
                onRestartPublisherIceClick = { },
                onSfuRejoinClick = { },
                onSfuFastReconnectClick = {},
                onSwitchSfuClick = { },
                availableDevices = emptyList(),
                onDeviceSelected = {},
                onShowFeedback = {},
                onNoiseCancellation = {},
                onSelectScaleType = {},
                loadRecordings = { emptyList() },
                transcriptionUiState = TranscriptionAvailableUiState,
                onToggleTranscription = {},
                transcriptionList = { emptyList() },
            ),
        )
    }
}

@Preview
@Composable
private fun DynamicMenuDebugOptionPreview() {
    VideoTheme {
        DynamicMenu(
            items = defaultStreamMenu(
                showDebugOptions = true,
                codecList = emptyList(),
                onCodecSelected = {},
                isScreenShareEnabled = true,
                onToggleScreenShare = { },
                onShowCallStats = { },
                onToggleAudioFilterClick = { },
                onRestartSubscriberIceClick = { },
                onRestartPublisherIceClick = { },
                onSfuRejoinClick = { },
                onSfuFastReconnectClick = {},
                onSwitchSfuClick = { },
                availableDevices = emptyList(),
                onDeviceSelected = {},
                onShowFeedback = {},
                onSelectScaleType = { },
                onNoiseCancellation = {},
                loadRecordings = { emptyList() },
                transcriptionUiState = TranscriptionAvailableUiState,
                onToggleTranscription = {},
                transcriptionList = { emptyList() },
            ),
        )
    }
}

@Preview
@Composable
private fun DynamicMenuDebugPreview() {
    VideoTheme {
        DynamicMenu(
            items = debugSubmenu(
                codecList = emptyList(),
                onCodecSelected = {},
                onSfuRejoinClick = { },
                onSfuFastReconnectClick = {},
                onRestartPublisherIceClick = { },
                onRestartSubscriberIceClick = { },
                onToggleAudioFilterClick = { },
                onSelectScaleType = { },
                onSwitchSfuClick = { },
            ),
        )
    }
}

@Preview
@Composable
private fun DynamicMenuReconnectPreview() {
    VideoTheme {
        DynamicMenu(
            items = reconnectMenu(
                onSfuRejoinClick = { },
                onSfuFastReconnectClick = {},
                onRestartPublisherIceClick = { },
                onRestartSubscriberIceClick = { },
                onSwitchSfuClick = { },
            ),
        )
    }
}
