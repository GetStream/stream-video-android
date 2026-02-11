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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamToggleButton
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.ui.closedcaptions.ClosedCaptionUiState
import io.getstream.video.android.ui.menu.AudioUsageVoiceCommunicationUiState
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DynamicMenu(header: (@Composable LazyItemScope.() -> Unit)? = null, items: List<MenuItem>) {
    val historyTitles = remember { mutableStateListOf<String>() }
    // Map of title -> (DynamicSubMenuItem, loaded items) for each dynamic submenu visited
    val dynamicMenuCache = remember { mutableStateMapOf<String, DynamicMenuEntry>() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = VideoTheme.colors.baseSheetPrimary,
                shape = VideoTheme.shapes.dialog,
            )
            .semantics { testTagsAsResourceId = true },
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
            if (historyTitles.isEmpty()) {
                // First Level Menu
                header?.let {
                    item(content = header)
                }
                menuItems(items) { subMenuItem ->
                    onNavigateToSubmenu(subMenuItem, historyTitles, dynamicMenuCache)
                }
            } else {
                // Nested Level Menu
                val currentTitle = historyTitles.last()
                submenuStickyHeader(currentTitle) {
                    // Remove menu's reference from cache because it will be fetched freshly from source
                    val removed = historyTitles.removeLastOrNull()
                    if (removed != null) {
                        dynamicMenuCache.remove(removed)
                    }
                }

                val dynamicEntry = dynamicMenuCache[currentTitle]

                if (dynamicEntry != null) {
                    dynamicSubmenuItems(dynamicEntry) { subMenuItem ->
                        onNavigateToSubmenu(subMenuItem, historyTitles, dynamicMenuCache)
                    }
                } else {
                    staticSubmenuItems(items, currentTitle) { subMenuItem ->
                        onNavigateToSubmenu(subMenuItem, historyTitles, dynamicMenuCache)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.submenuStickyHeader(currentTitle: String, onBackClick: () -> Unit) {
    stickyHeader {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(VideoTheme.colors.baseSheetPrimary)
                .fillMaxWidth(),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    tint = VideoTheme.colors.basePrimary,
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Text(
                text = currentTitle,
                style = VideoTheme.typography.subtitleS,
                color = VideoTheme.colors.basePrimary,
            )
        }
    }
}
private fun onNavigateToSubmenu(
    subMenuItem: SubMenuItem,
    historyTitles: MutableList<String>,
    dynamicMenuCache: MutableMap<String, DynamicMenuEntry>,
) {
    if (subMenuItem is DynamicSubMenuItem) {
        dynamicMenuCache[subMenuItem.title] = DynamicMenuEntry(subMenuItem)
    }
    historyTitles.add(subMenuItem.title)
}

private class DynamicMenuEntry(
    val menu: DynamicSubMenuItem,
    val items: MutableList<MenuItem> = mutableStateListOf(),
    var loaded: Boolean = false,
)

private fun LazyListScope.dynamicSubmenuItems(
    dynamicEntry: DynamicMenuEntry,
    onNewSubmenu: (SubMenuItem) -> Unit,
) {
    if (!dynamicEntry.loaded) {
        loadingItems(dynamicEntry.menu) { loadedItems ->
            dynamicEntry.items.addAll(loadedItems)
            dynamicEntry.loaded = true
        }
    }
    if (dynamicEntry.items.isNotEmpty()) {
        menuItems(dynamicEntry.items, onNewSubmenu)
    } else if (dynamicEntry.loaded) {
        noItems()
    }
}

private fun LazyListScope.staticSubmenuItems(
    items: List<MenuItem>,
    currentTitle: String,
    onNewSubmenu: (SubMenuItem) -> Unit,
) {
    val currentSubMenu = findSubMenuItem(items, currentTitle)

    if (currentSubMenu == null || currentSubMenu.items.isEmpty()) {
        noItems()
    } else {
        menuItems(currentSubMenu.items, onNewSubmenu)
    }
}

private fun findSubMenuItem(items: List<MenuItem>, title: String): SubMenuItem? {
    for (item in items) {
        if (item is SubMenuItem && item.title == title) return item
        if (item is SubMenuItem) {
            findSubMenuItem(item.items, title)?.let { return it }
        }
    }
    return null
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
                selectedIncomingVideoResolution = null,
                onSelectIncomingVideoResolution = {},
                isIncomingVideoEnabled = true,
                onToggleIncomingVideoEnabled = {},
                onSelectScaleType = {},
                loadRecordings = { emptyList() },
                transcriptionUiState = TranscriptionAvailableUiState,
                onToggleTranscription = {},
                loadTranscriptions = { emptyList() },
                onToggleClosedCaptions = {},
                closedCaptionUiState = ClosedCaptionUiState.Available,
                audioUsageUiState = AudioUsageVoiceCommunicationUiState,
                onToggleAudioUsage = {},
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
                selectedIncomingVideoResolution = null,
                onSelectIncomingVideoResolution = {},
                isIncomingVideoEnabled = true,
                onToggleIncomingVideoEnabled = {},
                loadRecordings = { emptyList() },
                onToggleClosedCaptions = {},
                closedCaptionUiState = ClosedCaptionUiState.Available,
                transcriptionUiState = TranscriptionAvailableUiState,
                onToggleTranscription = {},
                loadTranscriptions = { emptyList() },
                audioUsageUiState = AudioUsageVoiceCommunicationUiState,
                onToggleAudioUsage = {},
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
                loadRecordings = { emptyList() },
                onShowFeedback = { },
                selectedIncomingVideoResolution = null,
                onSelectIncomingVideoResolution = { },
                isIncomingVideoEnabled = true,
                onToggleIncomingVideoEnabled = { },
                loadTranscriptions = { emptyList() },
                audioUsageUiState = AudioUsageVoiceCommunicationUiState,
                onToggleAudioUsage = {},
                selectedRecordingTypes = emptySet(),
                onSelectRecordingType = { },
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
